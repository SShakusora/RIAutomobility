package com.sshakusora.riautomobility.editor.upload;

import com.google.gson.JsonObject;
import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.carpack.CarPackManager;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.BeginCarPackUploadPacket;
import com.sshakusora.riautomobility.network.packet.CarPackUploadChunkPacket;
import com.sshakusora.riautomobility.network.packet.CarPackUploadResultPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.network.NetworkDirection;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class CarPackUploadService {
    public static final long MAX_UPLOAD_SIZE = 64L * 1024L * 1024L;
    private static final long UPLOAD_TIMEOUT_MILLIS = 60_000L;
    private static final Map<UUID, Upload> UPLOADS = new HashMap<>();

    private CarPackUploadService() {}

    public static void begin(ServerPlayer player, BeginCarPackUploadPacket request) {
        try {
            requirePermission(player);
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
        Upload upload = UPLOADS.get(packet.uploadId());
        if (upload == null || !upload.playerId.equals(player.getUUID())) {
            fail(player, packet.uploadId(), "No matching upload session");
            return;
        }
        try {
            requirePermission(player);
            if (packet.index() != upload.nextChunk) throw new IOException("Upload chunks arrived out of order");
            if (upload.written + packet.data().length > upload.request.archiveSize()) throw new IOException("Upload exceeds declared size");
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
        Upload upload = UPLOADS.remove(uploadId);
        if (upload == null || !upload.playerId.equals(player.getUUID())) {
            fail(player, uploadId, "No matching upload session");
            return;
        }
        try {
            requirePermission(player);
            upload.output.close();
            if (upload.written != upload.request.archiveSize()) throw new IOException("Uploaded size does not match declaration");
            String digest = CarPackArchiveStore.sha256(upload.temporary);
            if (!digest.equals(upload.request.sha256())) throw new IOException("Uploaded SHA-256 does not match declaration");
            CarPackArchiveStore.validateArchive(upload.temporary);
            validateEditorArchive(upload.temporary, upload.request);

            Path target = CarPackManager.getRootDirectory().resolve(upload.request.packName() + ".zip").normalize();
            if (!target.getParent().equals(CarPackManager.getRootDirectory().normalize())) throw new IOException("Invalid target path");
            if (Files.exists(target) && !upload.request.overwrite()) throw new IOException("A car pack with this name already exists");
            Files.move(upload.temporary, target, StandardCopyOption.REPLACE_EXISTING);

            var server = player.server;
            server.getPackRepository().reload();
            server.reloadResources(server.getPackRepository().getSelectedIds()).whenComplete((unused, error) -> server.execute(() -> {
                if (error == null) success(player, uploadId, "Installed " + target.getFileName());
                else fail(player, uploadId, "Installed, but reload failed: " + error.getMessage());
            }));
        } catch (Exception exception) {
            abort(upload);
            fail(player, uploadId, exception.getMessage());
        }
    }

    private static void validateMetadata(BeginCarPackUploadPacket request) throws IOException {
        if (!request.packName().matches("[a-z0-9_.-]{1,96}")) throw new IOException("Invalid pack name");
        if (!request.namespace().matches("[a-z0-9_.-]{1,64}")) throw new IOException("Invalid namespace");
        if (!request.componentPath().matches("[a-z0-9/._-]{1,192}") || request.componentPath().contains("..")) throw new IOException("Invalid component path");
        if (!request.target().equals("frame") && !request.target().equals("wheel")) throw new IOException("Invalid component target");
        if (request.archiveSize() <= 0 || request.archiveSize() > MAX_UPLOAD_SIZE) throw new IOException("Upload size is outside the allowed range");
        if (!request.sha256().matches("[0-9a-f]{64}")) throw new IOException("Invalid SHA-256");
    }

    private static void validateEditorArchive(Path archive, BeginCarPackUploadPacket request) throws IOException {
        String namespacePrefix = "assets/" + request.namespace() + "/";
        String dataPrefix = "data/" + request.namespace() + "/";
        String expected = dataPrefix + "riautomobility/" + (request.target().equals("frame") ? "frames/" : "wheels/")
                + request.componentPath() + ".json";
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (!name.equals("pack.mcmeta") && !name.startsWith(namespacePrefix) && !name.startsWith(dataPrefix)) {
                    throw new IOException("Archive contains content outside its declared namespace: " + name);
                }
            }
            ZipEntry componentEntry = zip.getEntry(expected);
            if (componentEntry == null) throw new IOException("Archive is missing " + expected);
            JsonObject json;
            try (var reader = new InputStreamReader(zip.getInputStream(componentEntry), StandardCharsets.UTF_8)) {
                json = GsonHelper.parse(reader);
            }
            ResourceLocation id = new ResourceLocation(request.namespace(), request.componentPath());
            if (request.target().equals("frame")) FrameSpec.fromJson(id, json);
            else WheelSpec.fromJson(id, json);
        } catch (RuntimeException exception) {
            throw new IOException("Component JSON is invalid: " + exception.getMessage(), exception);
        }
    }

    private static void requirePermission(ServerPlayer player) throws IOException {
        if (!player.hasPermissions(2)) throw new IOException("Server operator permission is required");
    }

    private static void discardPlayerUpload(UUID playerId) {
        UPLOADS.values().stream().filter(upload -> upload.playerId.equals(playerId)).toList().forEach(CarPackUploadService::abort);
    }

    public static void abortPlayer(UUID playerId) {
        discardPlayerUpload(playerId);
    }

    public static void expireStaleUploads() {
        long cutoff = System.currentTimeMillis() - UPLOAD_TIMEOUT_MILLIS;
        UPLOADS.values().stream().filter(upload -> upload.lastActivity < cutoff).toList().forEach(CarPackUploadService::abort);
    }

    private static void abort(Upload upload) {
        UPLOADS.remove(upload.request.uploadId());
        try { upload.output.close(); } catch (IOException ignored) {}
        try { Files.deleteIfExists(upload.temporary); } catch (IOException ignored) {}
    }

    private static void success(ServerPlayer player, UUID uploadId, String detail) { send(player, new CarPackUploadResultPacket(uploadId, true, detail)); }
    private static void fail(ServerPlayer player, UUID uploadId, String detail) { send(player, new CarPackUploadResultPacket(uploadId, false, detail == null ? "Upload failed" : detail)); }
    private static void send(ServerPlayer player, Object packet) {
        RIAutomobilityNetwork.CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
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
}
