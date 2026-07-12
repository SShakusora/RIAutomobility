package com.sshakusora.riautomobility.carpack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class CarPackArchiveStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsAValidUnifiedCarPack() throws IOException {
        Path archive = createZip(Map.of(
                "pack.mcmeta", "{\"pack\":{\"pack_format\":15,\"description\":\"test\"}}",
                "assets/test/textures/example.txt", "asset",
                "data/test/riautomobility/frames/example.json", "{}"
        ));

        assertDoesNotThrow(() -> CarPackArchiveStore.validateArchive(archive));
    }

    @Test
    void rejectsPathTraversalEntries() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("pack.mcmeta", "{}");
        entries.put("../outside.txt", "unsafe");
        Path archive = createZip(entries);

        assertThrows(IOException.class, () -> CarPackArchiveStore.validateArchive(archive));
    }

    @Test
    void rejectsPacksWithoutRootMetadata() throws IOException {
        Path archive = createZip(Map.of("assets/test/example.txt", "asset"));

        assertThrows(IOException.class, () -> CarPackArchiveStore.validateArchive(archive));
    }

    @Test
    void calculatesTheRawArchiveDigest() throws Exception {
        Path file = temporaryDirectory.resolve("bytes.bin");
        byte[] bytes = "RIAutomobility cache".getBytes(StandardCharsets.UTF_8);
        Files.write(file, bytes);
        String expected = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));

        assertEquals(expected, CarPackArchiveStore.sha256(file));
    }

    @Test
    void validatesManifestBoundsAndDigests() {
        String digest = "a".repeat(64);
        assertDoesNotThrow(() -> new CarPackManifestEntry(
                "riautomobility/test", "test", digest, digest, 1024
        ));
        assertThrows(IllegalArgumentException.class, () -> new CarPackManifestEntry(
                "other/test", "test", digest, digest, 1024
        ));
        assertThrows(IllegalArgumentException.class, () -> new CarPackManifestEntry(
                "riautomobility/test", "test", "invalid", digest, 1024
        ));
        assertThrows(IllegalArgumentException.class, () -> new CarPackManifestEntry(
                "riautomobility/test", "test", digest, digest, CarPackManifestEntry.MAX_ARCHIVE_SIZE + 1
        ));
    }

    private Path createZip(Map<String, String> entries) throws IOException {
        Path archive = temporaryDirectory.resolve("pack-" + System.nanoTime() + ".zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        return archive;
    }
}
