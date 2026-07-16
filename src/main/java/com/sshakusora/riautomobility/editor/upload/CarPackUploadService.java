package com.sshakusora.riautomobility.editor.upload;

import com.google.gson.JsonObject;
import com.sshakusora.riautomobility.carpack.*;
import com.sshakusora.riautomobility.content.EngineSpec;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.editor.client.VehicleEditorDraft;
import com.sshakusora.riautomobility.model.bbmodel.BbModelParser;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.BeginCarPackUploadPacket;
import com.sshakusora.riautomobility.network.packet.CarPackUploadChunkPacket;
import com.sshakusora.riautomobility.network.packet.CarPackUploadResultPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.network.NetworkDirection;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class CarPackUploadService {
    public static final long MAX_UPLOAD_SIZE = 64L * 1024L * 1024L;
    private static final long UPLOAD_TIMEOUT_MILLIS = 60_000L;
    private static final Map<UUID, Upload> UPLOADS = new HashMap<>();
    private static final ExecutorService UPLOAD_IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "RIAutomobility car pack upload I/O");
        thread.setDaemon(true);
        return thread;
    });

    private CarPackUploadService() {
    }

    public static void begin(ServerPlayer player, BeginCarPackUploadPacket request) {
        if (rejectUnauthorized(player, request.uploadId())) return;
        UPLOAD_IO.execute(() -> beginIo(player, request));
    }

    private static void beginIo(ServerPlayer player, BeginCarPackUploadPacket request) {
        try {
            validateMetadata(request);
            discardPlayerUpload(player.getUUID());
            Path directory = CarPackManager.getRootDirectory().resolve("cache").resolve("uploads");
            Files.createDirectories(directory);
            Path temporary = directory.resolve(request.uploadId() + ".part");
            OutputStream output = Files.newOutputStream(temporary);
            UPLOADS.put(request.uploadId(), new Upload(player.getUUID(), request, temporary, output));
        } catch (Exception exception) {
            fail(player, request.uploadId(), exception.getMessage());
        }
    }

    public static void chunk(ServerPlayer player, CarPackUploadChunkPacket packet) {
        if (rejectUnauthorized(player, packet.uploadId())) return;
        UPLOAD_IO.execute(() -> chunkIo(player, packet));
    }

    private static void chunkIo(ServerPlayer player, CarPackUploadChunkPacket packet) {
        Upload upload = UPLOADS.get(packet.uploadId());
        if (upload == null || !upload.playerId.equals(player.getUUID())) {
            fail(player, packet.uploadId(), "No matching upload session");
            return;
        }
        try {
            if (packet.index() != upload.nextChunk) throw new IOException("Upload chunks arrived out of order");
            if (upload.written + packet.data().length > upload.request.archiveSize())
                throw new IOException("Upload exceeds declared size");
            upload.output.write(packet.data());
            upload.written += packet.data().length;
            upload.nextChunk++;
            upload.lastActivity = System.currentTimeMillis();
        } catch (Exception exception) {
            abort(upload);
            fail(player, packet.uploadId(), exception.getMessage());
        }
    }

    public static void complete(ServerPlayer player, UUID uploadId) {
        if (rejectUnauthorized(player, uploadId)) return;
        UPLOAD_IO.execute(() -> completeIo(player, uploadId));
    }

    private static void completeIo(ServerPlayer player, UUID uploadId) {
        Upload upload = UPLOADS.remove(uploadId);
        if (upload == null || !upload.playerId.equals(player.getUUID())) {
            fail(player, uploadId, "No matching upload session");
            return;
        }
        try {
            upload.output.close();
            if (upload.written != upload.request.archiveSize())
                throw new IOException("Uploaded size does not match declaration");
            String digest = CarPackArchiveStore.sha256(upload.temporary);
            if (!digest.equals(upload.request.sha256()))
                throw new IOException("Uploaded SHA-256 does not match declaration");
            CarPackArchiveStore.validateRiautoArchive(upload.temporary);
            validateEditorArchive(upload.temporary, upload.request);

            Path target = CarPackManager.getRootDirectory()
                    .resolve(upload.request.packName() + CarPackManager.CAR_PACK_EXTENSION).normalize();
            if (!target.getParent().equals(CarPackManager.getRootDirectory().normalize()))
                throw new IOException("Invalid target path");
            installAtomically(upload.temporary, target, upload.request.overwrite(),
                    () -> reloadServer(player.server),
                    () -> reloadServer(player.server));
            success(player, uploadId, "Installed " + target.getFileName());
        } catch (Exception exception) {
            abort(upload);
            fail(player, uploadId, exception.getMessage());
        }
    }

    private static void validateMetadata(BeginCarPackUploadPacket request) throws IOException {
        if (!request.packName().matches("[a-z0-9_.-]{1,96}")) throw new IOException("Invalid pack name");
        if (!request.namespace().equals(VehicleEditorDraft.GENERATED_NAMESPACE))
            throw new IOException("Invalid generated namespace");
        if (!request.componentPath().matches(VehicleEditorDraft.GENERATED_COMPONENT_PREFIX + "[0-9a-f]{32}")) {
            throw new IOException("Invalid generated component id");
        }
        if (!request.target().equals("frame") && !request.target().equals("wheel") && !request.target().equals("engine"))
            throw new IOException("Invalid component target");
        if (request.archiveSize() <= 0 || request.archiveSize() > MAX_UPLOAD_SIZE)
            throw new IOException("Upload size is outside the allowed range");
        if (!request.sha256().matches("[0-9a-f]{64}")) throw new IOException("Invalid SHA-256");
    }

    static void validateEditorArchive(Path archive, BeginCarPackUploadPacket request) throws IOException {
        String namespacePrefix = "assets/" + request.namespace() + "/";
        String dataPrefix = "data/" + request.namespace() + "/";
        String expected = dataPrefix + "riautomobility/" + request.target() + "s/"
                + request.componentPath() + ".json";
        ResourceLocation id = new ResourceLocation(request.namespace(), request.componentPath());
        CarPackArchiveStore.DeclaredComponent declared = CarPackArchiveStore.readDeclaredComponent(archive);
        CarPackArchiveStore.ComponentKind expectedKind = switch (request.target()) {
            case "frame" -> CarPackArchiveStore.ComponentKind.FRAME;
            case "wheel" -> CarPackArchiveStore.ComponentKind.WHEEL;
            case "engine" -> CarPackArchiveStore.ComponentKind.ENGINE;
            default -> throw new IOException("Invalid component target");
        };
        if (declared.kind() != expectedKind || !declared.id().equals(id)) {
            throw new IOException("RIAuto metadata must declare only " + request.target() + " component " + id);
        }
        Map<String, String> files = CarPackArchiveStore.readFileMappings(archive);
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            for (String logicalPath : files.values()) {
                if (!logicalPath.startsWith(namespacePrefix) && !logicalPath.startsWith(dataPrefix)) {
                    throw new IOException("Archive contains content outside its declared namespace: " + logicalPath);
                }
            }
            ZipEntry componentEntry = zip.getEntry(findFile(files, expected));
            if (componentEntry == null) throw new IOException("Archive is missing " + expected);
            JsonObject json;
            try (var reader = new InputStreamReader(zip.getInputStream(componentEntry), StandardCharsets.UTF_8)) {
                json = GsonHelper.parse(reader);
            }
            FrameSpec.ModelSpec model = switch (request.target()) {
                case "frame" -> FrameSpec.fromJson(id, json).model();
                case "wheel" -> WheelSpec.fromJson(id, json).model();
                case "engine" -> EngineSpec.fromJson(id, json).model();
                default -> throw new IOException("Invalid component target");
            };
            String modelPath = "models/entity/automobile/" + request.target() + "/"
                    + request.componentPath() + ".bbmodel";
            ResourceLocation expectedModel = new ResourceLocation(request.namespace(), modelPath);
            if (!"bbmodel".equals(model.type()) || !expectedModel.equals(model.bbModel())) {
                throw new IOException("Vehicle Import Table uploads must use the generated BBModel resource " + expectedModel);
            }
            ZipEntry modelEntry = zip.getEntry(findFile(files,
                    "assets/" + request.namespace() + "/" + modelPath));
            if (modelEntry == null || modelEntry.isDirectory()) {
                throw new IOException("Archive is missing embedded-texture BBModel " + expectedModel);
            }
            JsonObject modelJson;
            try (var reader = new InputStreamReader(zip.getInputStream(modelEntry), StandardCharsets.UTF_8)) {
                modelJson = GsonHelper.parse(reader);
            }
            var document = BbModelParser.parse(modelJson);
            for (BbModelParser.ExternalTexture texture : BbModelParser.requireExternalPngTextures(document)) {
                String textureEntryName = findFile(files, "assets/" + texture.resource().getNamespace()
                        + "/" + texture.resource().getPath());
                ZipEntry textureEntry = zip.getEntry(textureEntryName);
                if (textureEntry == null || textureEntry.isDirectory()) {
                    throw new IOException("Archive is missing external texture " + texture.resource());
                }
                byte[] signature;
                try (var input = zip.getInputStream(textureEntry)) {
                    signature = input.readNBytes(8);
                }
                BbModelParser.requirePngTextureBytes(texture.name(), signature);
            }
        } catch (RuntimeException exception) {
            throw new IOException("Vehicle Import Table archive is invalid: " + exception.getMessage(), exception);
        }
    }

    private static String findFile(Map<String, String> files, String logicalPath) throws IOException {
        return files.entrySet().stream()
                .filter(entry -> entry.getValue().equals(logicalPath))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IOException("Archive is missing mapped resource " + logicalPath));
    }

    private static boolean rejectUnauthorized(ServerPlayer player, UUID uploadId) {
        if (player.hasPermissions(2)) return false;
        fail(player, uploadId, "Server operator permission is required");
        return true;
    }

    private static void reloadServer(MinecraftServer server) throws Exception {
        CarPackComponentDataLoader.LoadedContent content = CarPackRuntime.loadServerContent();
        CarPackArchiveStore.prepareManifest();
        server.submit(() -> {
            CarPackComponentDataLoader.apply(content);
            CarPackEvents.CommonEvents.syncAll(server);
        }).get();
    }

    static void installAtomically(Path source, Path target, boolean overwrite,
                                  CheckedAction apply, CheckedAction rollback) throws Exception {
        boolean targetExists = Files.exists(target);
        if (targetExists && !overwrite) {
            throw new IOException("A car pack with this name already exists");
        }

        Path backup = target.resolveSibling(target.getFileName() + "." + UUID.randomUUID() + ".backup");
        boolean backupCreated = false;
        boolean installed = false;
        try {
            if (targetExists) {
                moveAtomically(target, backup);
                backupCreated = true;
            }
            moveAtomically(source, target);
            installed = true;
            apply.run();
            if (backupCreated) {
                Files.delete(backup);
                backupCreated = false;
            }
        } catch (Exception failure) {
            try {
                if (installed) {
                    Files.deleteIfExists(target);
                }
                if (backupCreated) {
                    moveAtomically(backup, target);
                }
                rollback.run();
            } catch (Exception rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void discardPlayerUpload(UUID playerId) {
        UPLOADS.values().stream().filter(upload -> upload.playerId.equals(playerId)).toList().forEach(CarPackUploadService::abort);
    }

    public static void abortPlayer(UUID playerId) {
        UPLOAD_IO.execute(() -> discardPlayerUpload(playerId));
    }

    public static void expireStaleUploads() {
        UPLOAD_IO.execute(() -> {
            long cutoff = System.currentTimeMillis() - UPLOAD_TIMEOUT_MILLIS;
            UPLOADS.values().stream().filter(upload -> upload.lastActivity < cutoff).toList()
                    .forEach(CarPackUploadService::abort);
        });
    }

    private static void abort(Upload upload) {
        UPLOADS.remove(upload.request.uploadId());
        try {
            upload.output.close();
        } catch (IOException ignored) {
        }
        try {
            Files.deleteIfExists(upload.temporary);
        } catch (IOException ignored) {
        }
    }

    private static void success(ServerPlayer player, UUID uploadId, String detail) {
        send(player, new CarPackUploadResultPacket(uploadId, true, detail));
    }

    private static void fail(ServerPlayer player, UUID uploadId, String detail) {
        send(player, new CarPackUploadResultPacket(uploadId, false, detail == null ? "Upload failed" : detail));
    }

    private static void send(ServerPlayer player, Object packet) {
        player.server.execute(() -> RIAutomobilityNetwork.CHANNEL.sendTo(
                packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT));
    }

    private static final class Upload {
        final UUID playerId;
        final BeginCarPackUploadPacket request;
        final Path temporary;
        final OutputStream output;
        long written;
        int nextChunk;
        long lastActivity = System.currentTimeMillis();

        Upload(UUID playerId, BeginCarPackUploadPacket request, Path temporary, OutputStream output) {
            this.playerId = playerId;
            this.request = request;
            this.temporary = temporary;
            this.output = output;
        }
    }

    @FunctionalInterface
    interface CheckedAction {
        void run() throws Exception;
    }
}
