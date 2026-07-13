package com.sshakusora.riautomobility.carpack;

import com.mojang.logging.LogUtils;
import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class CarPackManager {
    public static final String CAR_PACK_EXTENSION = ".riauto";
    public static final String PACK_ID_PREFIX = "riautomobility/";
    public static final String DISABLED_DIRECTORY_NAME = "disabled";
    public static final String CACHE_DIRECTORY_NAME = "cache";
    private static final String TRANSFER_CACHE_DIRECTORY_NAME = "server-transfers";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int BUFFER_SIZE = 8192;
    private static final AABB WORLD_BOUNDS = new AABB(-3.0E7D, -2048.0D, -3.0E7D, 3.0E7D, 2048.0D, 3.0E7D);
    private static volatile List<CarPack> clientResourcePacks;
    private static volatile CarPack clientPreviewPack;

    private CarPackManager() {
    }

    public static Path getRootDirectory() {
        return FMLPaths.GAMEDIR.get().resolve("riautomobility");
    }

    public static Path getClientPackCacheDirectory() {
        return getRootDirectory().resolve(CACHE_DIRECTORY_NAME).resolve("packs");
    }

    public static Path getTransferCacheDirectory() {
        return getRootDirectory().resolve(CACHE_DIRECTORY_NAME).resolve(TRANSFER_CACHE_DIRECTORY_NAME);
    }

    public static List<CarPack> discoverCarPacks() {
        Path root = getRootDirectory();
        List<CarPack> packs = new ArrayList<>();
        try {
            Files.createDirectories(root);
            Files.createDirectories(root.resolve(DISABLED_DIRECTORY_NAME));
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(root)) {
                for (Path entry : entries) {
                    if (entry.getFileName().toString().equalsIgnoreCase(DISABLED_DIRECTORY_NAME)
                            || entry.getFileName().toString().equalsIgnoreCase(CACHE_DIRECTORY_NAME)) {
                        continue;
                    }

                    if (!isRiautoArchive(entry) || !Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                        continue;
                    }
                    Pack.ResourcesSupplier resources = detectPackResources(entry);
                    try {
                        CarPackArchiveStore.validateRiautoArchive(entry);
                    } catch (IOException exception) {
                        LOGGER.warn("Ignoring invalid RIAutomobility car pack {}", entry, exception);
                        continue;
                    }

                    String name = entry.getFileName().toString();
                    try {
                        packs.add(new CarPack(carPackId(name), name, entry, resources, digest(entry)));
                    } catch (IOException exception) {
                        LOGGER.warn("Failed to calculate digest for RIAutomobility car pack {}", entry, exception);
                    }
                }
            }
        } catch (IOException exception) {
            LOGGER.warn("Failed to scan RIAutomobility car packs in {}", root, exception);
        }

        packs.sort(Comparator.comparing(CarPack::id).thenComparing(CarPack::displayName));
        Map<String, CarPack> uniquePacks = new LinkedHashMap<>();
        for (CarPack pack : packs) {
            CarPack duplicate = uniquePacks.putIfAbsent(pack.id(), pack);
            if (duplicate != null) {
                LOGGER.warn("Ignoring duplicate RIAutomobility car pack id {} from {}; already provided by {}",
                        pack.id(), pack.path(), duplicate.path());
            }
        }
        return List.copyOf(uniquePacks.values());
    }

    public static List<CarPack> discoverClientResourcePacks() {
        List<CarPack> packs = clientResourcePacks;
        List<CarPack> resolved = packs == null ? discoverCarPacks() : packs;
        CarPack preview = clientPreviewPack;
        if (preview == null) {
            return resolved;
        }
        List<CarPack> withPreview = new ArrayList<>(resolved);
        withPreview.add(preview);
        return List.copyOf(withPreview);
    }

    public static void setClientResourcePacks(List<CarPack> packs) {
        clientResourcePacks = List.copyOf(packs);
    }

    public static void clearClientResourcePacks() {
        clientResourcePacks = null;
    }

    public static void setClientPreviewPack(Path archive) throws IOException {
        Pack.ResourcesSupplier resources = detectPackResources(archive);
        if (resources == null) {
            throw new IOException("Editor preview is not a valid RIAuto archive");
        }
        clientPreviewPack = new CarPack(
                PACK_ID_PREFIX + "editor-preview", "Vehicle editor preview", archive, resources, digest(archive));
    }

    public static void clearClientPreviewPack() {
        clientPreviewPack = null;
    }

    public static CarPack cachedCarPack(CarPackManifestEntry manifest, Path archive) throws IOException {
        Pack.ResourcesSupplier resources = detectPackResources(archive);
        if (resources == null) {
            throw new IOException("Downloaded car pack is not a readable RIAuto archive: " + manifest.id());
        }
        return new CarPack(manifest.id(), manifest.displayName(), archive, resources, manifest.contentDigest());
    }

    public static Map<String, String> getDigests() {
        Map<String, String> digests = new LinkedHashMap<>();
        for (CarPack pack : discoverCarPacks()) {
            digests.put(pack.id(), pack.digest());
        }
        return Map.copyOf(digests);
    }

    public static void refreshAllServerLevels() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (var level : server.getAllLevels()) {
            refreshLevel(level);
        }
    }

    public static void refreshLevel(Level level) {
        for (RIAutomobileEntity automobile : level.getEntitiesOfClass(RIAutomobileEntity.class, WORLD_BOUNDS)) {
            automobile.reloadRIAutomobilityComponents();
        }
    }

    private static String digest(Path path) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }

        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            digestDirectory(path, digest);
        } else {
            digestZip(path, digest);
        }
        return toHex(digest.digest());
    }

    private static String carPackId(String fileName) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        String id;
        if (lowerName.endsWith(CAR_PACK_EXTENSION)) {
            id = fileName.substring(0, fileName.length() - CAR_PACK_EXTENSION.length());
        } else if (lowerName.endsWith(".zip")) {
            id = fileName.substring(0, fileName.length() - 4);
        } else {
            id = fileName;
        }
        return PACK_ID_PREFIX + id;
    }

    static Pack.ResourcesSupplier detectPackResources(Path path) {
        if (isRiautoArchive(path) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            var file = path.toFile();
            return id -> new FilePackResources(id, file, false);
        }
        return null;
    }

    private static boolean isRiautoArchive(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(CAR_PACK_EXTENSION);
    }

    private static void digestDirectory(Path root, MessageDigest digest) throws IOException {
        List<Path> files;
        try (var paths = Files.walk(root)) {
            files = paths
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .sorted(Comparator.comparing(path -> normalize(root.relativize(path))))
                    .toList();
        }

        byte[] buffer = new byte[BUFFER_SIZE];
        for (Path file : files) {
            updateEntryName(digest, normalize(root.relativize(file)));
            try (InputStream input = Files.newInputStream(file)) {
                updateContents(digest, input, buffer);
            }
        }
    }

    private static void digestZip(Path path, MessageDigest digest) throws IOException {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            List<? extends ZipEntry> entries = zipEntries(zip);
            byte[] buffer = new byte[BUFFER_SIZE];
            for (ZipEntry entry : entries) {
                updateEntryName(digest, entry.getName());
                try (InputStream input = zip.getInputStream(entry)) {
                    updateContents(digest, input, buffer);
                }
            }
        }
    }

    private static List<? extends ZipEntry> zipEntries(ZipFile zip) {
        List<ZipEntry> entries = new ArrayList<>();
        Enumeration<? extends ZipEntry> enumeration = zip.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry entry = enumeration.nextElement();
            if (!entry.isDirectory()) {
                entries.add(entry);
            }
        }
        entries.sort(Comparator.comparing(ZipEntry::getName));
        return entries;
    }

    private static void updateEntryName(MessageDigest digest, String name) {
        digest.update(name.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    private static void updateContents(MessageDigest digest, InputStream input, byte[] buffer) throws IOException {
        int read;
        while ((read = input.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        digest.update((byte) 0);
    }

    private static String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit((value >>> 4) & 0xF, 16));
            result.append(Character.forDigit(value & 0xF, 16));
        }
        return result.toString();
    }

    public record CarPack(String id, String displayName, Path path, Pack.ResourcesSupplier resources, String digest) {
    }
}
