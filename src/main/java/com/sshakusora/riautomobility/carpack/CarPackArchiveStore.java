package com.sshakusora.riautomobility.carpack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class CarPackArchiveStore {
    public static final int MAX_ENTRIES = 8192;
    public static final long MAX_UNCOMPRESSED_SIZE = 1024L * 1024L * 1024L;
    private static final int BUFFER_SIZE = 8192;
    private static volatile Map<String, TransferPack> transferPacks = Map.of();

    private CarPackArchiveStore() {}

    public static synchronized List<CarPackManifestEntry> prepareManifest() {
        Map<String, TransferPack> prepared = new LinkedHashMap<>();
        long totalSize = 0;
        for (CarPackManager.CarPack pack : CarPackManager.discoverCarPacks()) {
            try {
                TransferPack transferPack = prepare(pack);
                totalSize = Math.addExact(totalSize, transferPack.manifest().archiveSize());
                if (totalSize > CarPackManifestEntry.MAX_TOTAL_ARCHIVE_SIZE) {
                    throw new IOException("Total car pack transfer size exceeds the configured safety limit");
                }
                prepared.put(pack.id(), transferPack);
            } catch (IOException | ArithmeticException exception) {
                throw new IllegalStateException("Unable to prepare car pack " + pack.id() + " for network transfer", exception);
            }
        }
        if (prepared.size() > CarPackManifestEntry.MAX_PACKS) {
            throw new IllegalStateException("Too many car packs for network synchronization: " + prepared.size());
        }
        transferPacks = Map.copyOf(prepared);
        return prepared.values().stream().map(TransferPack::manifest).toList();
    }

    public static TransferPack find(String id, String archiveDigest) {
        TransferPack pack = transferPacks.get(id);
        return pack != null && pack.manifest().archiveDigest().equals(archiveDigest) ? pack : null;
    }

    private static TransferPack prepare(CarPackManager.CarPack pack) throws IOException {
        Path source = pack.path();
        Path archive;
        if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
            Path cache = CarPackManager.getTransferCacheDirectory();
            Files.createDirectories(cache);
            archive = cache.resolve(pack.digest() + ".zip");
            if (!Files.isRegularFile(archive)) {
                createArchive(source, archive);
            }
        } else {
            archive = source;
        }

        validateArchive(archive);
        long size = Files.size(archive);
        if (size > CarPackManifestEntry.MAX_ARCHIVE_SIZE) {
            throw new IOException("Car pack archive exceeds " + CarPackManifestEntry.MAX_ARCHIVE_SIZE + " bytes");
        }
        String archiveDigest = sha256(archive);
        return new TransferPack(
                new CarPackManifestEntry(pack.id(), pack.displayName(), pack.digest(), archiveDigest, size),
                archive
        );
    }

    private static void createArchive(Path root, Path target) throws IOException {
        List<Path> files;
        try (var paths = Files.walk(root)) {
            files = paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .sorted(Comparator.comparing(path -> normalize(root.relativize(path))))
                    .toList();
        }
        if (files.size() > MAX_ENTRIES) {
            throw new IOException("Car pack contains too many files: " + files.size());
        }

        long uncompressedSize = 0;
        for (Path file : files) {
            uncompressedSize = Math.addExact(uncompressedSize, Files.size(file));
            if (uncompressedSize > MAX_UNCOMPRESSED_SIZE) {
                throw new IOException("Car pack contains too much uncompressed data");
            }
        }

        Path temporary = target.resolveSibling(target.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary);
                 ZipOutputStream zip = new ZipOutputStream(output)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                for (Path file : files) {
                    String name = normalize(root.relativize(file));
                    validateEntryName(name);
                    ZipEntry entry = new ZipEntry(name);
                    entry.setTime(0L);
                    zip.putNextEntry(entry);
                    try (InputStream input = Files.newInputStream(file)) {
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            zip.write(buffer, 0, read);
                        }
                    }
                    zip.closeEntry();
                }
            }
            moveAtomically(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static void validateArchive(Path archive) throws IOException {
        int entries = 0;
        long uncompressedSize = 0;
        boolean hasMetadata = false;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            Enumeration<? extends ZipEntry> enumeration = zip.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                validateEntryName(entry.getName());
                if (entry.isDirectory()) {
                    continue;
                }
                entries++;
                if (entries > MAX_ENTRIES) {
                    throw new IOException("Car pack archive contains too many entries");
                }
                long size;
                try (InputStream input = zip.getInputStream(entry)) {
                    size = countBytes(input, MAX_UNCOMPRESSED_SIZE - uncompressedSize);
                }
                uncompressedSize = Math.addExact(uncompressedSize, size);
                if (uncompressedSize > MAX_UNCOMPRESSED_SIZE) {
                    throw new IOException("Car pack archive expands beyond the safety limit");
                }
                hasMetadata |= "pack.mcmeta".equals(entry.getName());
            }
        } catch (ArithmeticException exception) {
            throw new IOException("Car pack archive size overflow", exception);
        }
        if (!hasMetadata) {
            throw new IOException("Car pack archive does not contain a root pack.mcmeta");
        }
    }

    public static String sha256(Path path) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream input = Files.newInputStream(path)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return toHex(digest.digest());
    }

    private static long countBytes(InputStream input, long maximum) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maximum) {
                throw new IOException("Car pack archive expands beyond the safety limit");
            }
        }
        return total;
    }

    private static void validateEntryName(String name) throws IOException {
        if (name == null || name.isBlank() || name.indexOf('\\') >= 0 || name.startsWith("/") || name.matches("^[A-Za-z]:.*")) {
            throw new IOException("Unsafe car pack archive entry: " + name);
        }
        for (String part : name.split("/")) {
            if ("..".equals(part)) {
                throw new IOException("Unsafe car pack archive entry: " + name);
            }
        }
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
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

    public record TransferPack(CarPackManifestEntry manifest, Path archive) {}
}
