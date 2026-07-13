package com.sshakusora.riautomobility.carpack;

import net.minecraft.server.packs.PackResources;
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
    void acceptsAValidRiautoCarPack() throws IOException {
        Path archive = createZip(Map.of(
                "pack.mcmeta", "{\"pack\":{\"pack_format\":15,\"description\":\"test\"}}",
                "riauto.json", "{\"format\":1,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[\"test:car\"],\"wheels\":[]}}",
                "data/test/riautomobility/frames/car.json", "{}"
        ));

        assertDoesNotThrow(() -> CarPackArchiveStore.validateRiautoArchive(archive));
    }

    @Test
    void acceptsAnEngineOnlyRiautoCarPack() throws IOException {
        Path archive = createZip(Map.of(
                "pack.mcmeta", "{\"pack\":{\"pack_format\":15,\"description\":\"test\"}}",
                "riauto.json", "{\"format\":1,\"id\":\"test:engine\",\"name\":\"Test Engine\","
                        + "\"components\":{\"frames\":[],\"wheels\":[],\"engines\":[\"test:engine\"]}}",
                "data/test/riautomobility/engines/engine.json", "{}"
        ));

        assertDoesNotThrow(() -> CarPackArchiveStore.validateRiautoArchive(archive));
    }

    @Test
    void opensRiautoWithMinecraftFilePackResources() throws IOException {
        Path archive = Files.move(createZip(Map.of(
                        "pack.mcmeta", "{\"pack\":{\"pack_format\":15,\"description\":\"test\"}}",
                        "riauto.json", "{\"format\":1,\"id\":\"test:car\",\"name\":\"Test Car\","
                                + "\"components\":{\"frames\":[\"test:car\"],\"wheels\":[]}}"
                )), temporaryDirectory.resolve("test.riauto"));

        var supplier = CarPackManager.detectPackResources(archive);
        assertNotNull(supplier);
        try (PackResources resources = supplier.open("test")) {
            assertNotNull(resources.getRootResource("pack.mcmeta"));
        }
    }

    @Test
    void rejectsRiautoWithoutFormatMetadata() throws IOException {
        Path archive = createZip(Map.of("pack.mcmeta", "{}"));

        IOException error = assertThrows(IOException.class,
                () -> CarPackArchiveStore.validateRiautoArchive(archive));
        assertTrue(error.getMessage().contains("riauto.json"));
    }

    @Test
    void rejectsUnsupportedRiautoFormatVersion() throws IOException {
        Path archive = createZip(Map.of(
                "pack.mcmeta", "{}",
                "riauto.json", "{\"format\":2,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[\"test:car\"],\"wheels\":[]}}"
        ));

        assertThrows(IOException.class, () -> CarPackArchiveStore.validateRiautoArchive(archive));
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
