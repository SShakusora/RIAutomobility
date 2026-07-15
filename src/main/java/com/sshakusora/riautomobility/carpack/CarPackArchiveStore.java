package com.sshakusora.riautomobility.carpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class CarPackArchiveStore {
    public static final String RIAUTO_METADATA_FILE = "riauto.json";
    public static final int RIAUTO_FORMAT_VERSION = 1;
    public static final int MAX_ENTRIES = 8192;
    public static final long MAX_UNCOMPRESSED_SIZE = 1024L * 1024L * 1024L;
    public static final long MAX_ENTRY_SIZE = 256L * 1024L * 1024L;
    private static final int BUFFER_SIZE = 8192;
    private static volatile Map<String, TransferPack> transferPacks = Map.of();

    private CarPackArchiveStore() {
    }

    public static synchronized List<CarPackManifestEntry> prepareManifest() {
        Map<String, TransferPack> prepared = new LinkedHashMap<>();
        Map<ResourceLocation, String> componentOwners = new HashMap<>();
        for (CarPackManager.CarPack pack : CarPackManager.discoverCarPacks()) {
            try {
                TransferPack transferPack = prepare(pack);
                for (ResourceLocation component : transferPack.manifest().components()) {
                    String previous = componentOwners.putIfAbsent(component, pack.id());
                    if (previous != null) {
                        throw new IOException("Component " + component + " is declared by both " + previous + " and " + pack.id());
                    }
                }
                prepared.put(pack.id(), transferPack);
            } catch (IOException exception) {
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
                new CarPackManifestEntry(pack.id(), pack.displayName(), pack.digest(), archiveDigest, size,
                        readDeclaredComponentIds(archive)),
                archive
        );
    }

    static List<ResourceLocation> readDeclaredComponentIds(Path archive) throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            ZipEntry metadataEntry = zip.getEntry(RIAUTO_METADATA_FILE);
            if (metadataEntry == null || metadataEntry.isDirectory()) {
                throw new IOException("RIAuto archive does not contain a root " + RIAUTO_METADATA_FILE);
            }
            JsonObject metadata;
            try (var reader = new InputStreamReader(zip.getInputStream(metadataEntry), StandardCharsets.UTF_8)) {
                metadata = JsonParser.parseReader(reader).getAsJsonObject();
            }
            LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
            JsonObject components = metadata.getAsJsonObject("components");
            for (String kind : List.of("frames", "wheels", "engines")) {
                if (!components.has(kind)) continue;
                for (JsonElement element : components.getAsJsonArray(kind)) {
                    ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                    if (id == null || !ids.add(id)) {
                        throw new IOException("RIAuto metadata contains an invalid or duplicate component id");
                    }
                }
            }
            return List.copyOf(ids);
        } catch (RuntimeException exception) {
            throw new IOException("Invalid " + RIAUTO_METADATA_FILE + ": " + exception.getMessage(), exception);
        }
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
        Set<String> entryNames = new HashSet<>();
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            Enumeration<? extends ZipEntry> enumeration = zip.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                validateEntryName(entry.getName());
                if (!entryNames.add(entry.getName())) {
                    throw new IOException("Car pack archive contains duplicate entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    continue;
                }
                if (!isAllowedRuntimeEntry(entry.getName())) {
                    throw new IOException("RIAuto archive contains unsupported vanilla resource/data content: " + entry.getName());
                }
                entries++;
                if (entries > MAX_ENTRIES) {
                    throw new IOException("Car pack archive contains too many entries");
                }
                long size;
                try (InputStream input = zip.getInputStream(entry)) {
                    size = countBytes(input, Math.min(MAX_ENTRY_SIZE, MAX_UNCOMPRESSED_SIZE - uncompressedSize));
                }
                uncompressedSize = Math.addExact(uncompressedSize, size);
                if (uncompressedSize > MAX_UNCOMPRESSED_SIZE) {
                    throw new IOException("Car pack archive expands beyond the safety limit");
                }
                hasMetadata |= RIAUTO_METADATA_FILE.equals(entry.getName());
            }
        } catch (ArithmeticException exception) {
            throw new IOException("Car pack archive size overflow", exception);
        }
        if (!hasMetadata) {
            throw new IOException("Car pack archive does not contain a root " + RIAUTO_METADATA_FILE);
        }
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            ZipEntry metadataEntry = zip.getEntry(RIAUTO_METADATA_FILE);
            if (metadataEntry == null || metadataEntry.isDirectory()) {
                throw new IOException("RIAuto archive does not contain a root " + RIAUTO_METADATA_FILE);
            }
            JsonObject metadata;
            try (var reader = new InputStreamReader(zip.getInputStream(metadataEntry), StandardCharsets.UTF_8)) {
                metadata = JsonParser.parseReader(reader).getAsJsonObject();
            }
            validateRiautoMetadata(metadata);
            validateDeclaredComponentFiles(metadata, entryNames);
        } catch (RuntimeException exception) {
            throw new IOException("Invalid " + RIAUTO_METADATA_FILE + ": " + exception.getMessage(), exception);
        }
    }

    public static void validateRiautoArchive(Path archive) throws IOException {
        validateArchive(archive);
    }

    private static void validateRiautoMetadata(JsonObject metadata) throws IOException {
        if (!metadata.has("format") || !metadata.get("format").isJsonPrimitive()
                || metadata.get("format").getAsInt() != RIAUTO_FORMAT_VERSION) {
            throw new IOException("Unsupported RIAuto format version");
        }
        validateResourceLocation(requireString(metadata, "id"), "id");
        String name = requireString(metadata, "name");
        if (name.isBlank() || name.length() > 80) {
            throw new IOException("RIAuto name must contain 1-80 characters");
        }
        JsonObject components = metadata.has("components") && metadata.get("components").isJsonObject()
                ? metadata.getAsJsonObject("components") : null;
        if (components == null) {
            throw new IOException("RIAuto metadata is missing components");
        }
        int componentCount = validateComponentIds(components, "frames") + validateComponentIds(components, "wheels")
                + (components.has("engines") ? validateComponentIds(components, "engines") : 0);
        if (componentCount == 0) {
            throw new IOException("RIAuto metadata must declare at least one frame, wheel, or engine");
        }
    }

    private static int validateComponentIds(JsonObject components, String member) throws IOException {
        if (!components.has(member) || !components.get(member).isJsonArray()) {
            throw new IOException("RIAuto components." + member + " must be an array");
        }
        JsonArray values = components.getAsJsonArray(member);
        Set<String> ids = new HashSet<>();
        for (JsonElement value : values) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new IOException("RIAuto components." + member + " contains a non-string id");
            }
            String id = value.getAsString();
            validateResourceLocation(id, "components." + member);
            if (!ids.add(id)) {
                throw new IOException("RIAuto components." + member + " contains duplicate id " + id);
            }
        }
        return values.size();
    }

    private static String requireString(JsonObject object, String member) throws IOException {
        if (!object.has(member) || !object.get(member).isJsonPrimitive()
                || !object.getAsJsonPrimitive(member).isString()) {
            throw new IOException("RIAuto metadata field '" + member + "' must be a string");
        }
        return object.get(member).getAsString();
    }

    private static void validateResourceLocation(String value, String member) throws IOException {
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9/._-]+") || value.contains("..")) {
            throw new IOException("RIAuto metadata field '" + member + "' contains invalid id " + value);
        }
    }

    private static void validateDeclaredComponentFiles(JsonObject metadata, Set<String> entries) throws IOException {
        JsonObject components = metadata.getAsJsonObject("components");
        for (String kind : List.of("frames", "wheels", "engines")) {
            if (!components.has(kind)) continue;
            for (JsonElement element : components.getAsJsonArray(kind)) {
                String[] id = element.getAsString().split(":", 2);
                String expected = "data/" + id[0] + "/riautomobility/" + kind + "/" + id[1] + ".json";
                if (!entries.contains(expected)) {
                    throw new IOException("RIAuto metadata declares missing component file " + expected);
                }
            }
        }
    }

    private static boolean isAllowedRuntimeEntry(String name) {
        if (RIAUTO_METADATA_FILE.equals(name)) return true;
        if (name.matches("data/[a-z0-9_.-]+/riautomobility/(frames|wheels|engines)/[a-z0-9/._-]+\\.json")) {
            return true;
        }
        if (name.matches("assets/[a-z0-9_.-]+/models/entity/automobile/(frame|wheel|engine)/[a-z0-9/._-]+\\.(json|bbmodel)")) {
            return true;
        }
        if (name.matches("assets/[a-z0-9_.-]+/(geo|animations)/[a-z0-9/._-]+\\.json")) {
            return true;
        }
        return name.matches("assets/[a-z0-9_.-]+/textures/[a-z0-9/._-]+\\.png(\\.mcmeta)?");
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
        } catch (AtomicMoveNotSupportedException exception) {
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

    public record TransferPack(CarPackManifestEntry manifest, Path archive) {
    }
}
