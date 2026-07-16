package com.sshakusora.riautomobility.carpack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class CarPackArchiveStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsAValidSingleComponentCarPack() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":2,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[\"test:car\"],\"wheels\":[],\"engines\":[]}}",
                "assets/test/textures/entity/car.png", "png",
                "data/test/riautomobility/frames/car.json", "{}"
        ));

        assertDoesNotThrow(() -> CarPackArchiveStore.validateArchive(archive));
    }

    @Test
    void acceptsAValidRiautoCarPack() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":2,\"id\":\"test:car\",\"name\":\"Test Car\",\"author\":\"Test Author\","
                        + "\"components\":{\"frames\":[\"test:car\"],\"wheels\":[],\"engines\":[]}}",
                "data/test/riautomobility/frames/car.json", "{}"
        ));

        assertDoesNotThrow(() -> CarPackArchiveStore.validateRiautoArchive(archive));
        assertEquals("Test Author", CarPackArchiveStore.readAuthor(archive));
        CarPackArchiveStore.ComponentMetadata metadata = CarPackArchiveStore.readComponentMetadata(archive);
        assertEquals("Test Car", metadata.displayName());
        assertEquals("Test Author", metadata.author());
        assertEquals(new ResourceLocation("test", "car"), metadata.component().id());
    }

    @Test
    void rejectsInvalidAuthorMetadata() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":2,\"id\":\"test:car\",\"name\":\"Test Car\",\"author\":\"bad\\nauthor\","
                        + "\"components\":{\"frames\":[\"test:car\"],\"wheels\":[],\"engines\":[]}}",
                "data/test/riautomobility/frames/car.json", "{}"
        ));

        assertThrows(IOException.class, () -> CarPackArchiveStore.validateRiautoArchive(archive));
    }

    @Test
    void acceptsAnEngineOnlyRiautoCarPack() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":2,\"id\":\"test:engine\",\"name\":\"Test Engine\","
                        + "\"components\":{\"frames\":[],\"wheels\":[],\"engines\":[\"test:engine\"]}}",
                "data/test/riautomobility/engines/engine.json", "{}"
        ));

        assertDoesNotThrow(() -> CarPackArchiveStore.validateRiautoArchive(archive));
    }

    @Test
    void readsTheComponentOwnershipIndexFromMetadata() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":2,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[\"test:frame\"],\"wheels\":[],\"engines\":[]}}",
                "data/test/riautomobility/frames/frame.json", "{}"
        ));

        CarPackArchiveStore.validateRiautoArchive(archive);
        assertEquals(new CarPackArchiveStore.DeclaredComponent(
                        CarPackArchiveStore.ComponentKind.FRAME, new ResourceLocation("test", "frame")),
                CarPackArchiveStore.readDeclaredComponent(archive));
    }

    @Test
    void rejectsRiautoDeclaringMoreThanOneComponent() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":2,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[\"test:frame\"],\"wheels\":[\"test:wheel\"],\"engines\":[]}}",
                "data/test/riautomobility/frames/frame.json", "{}",
                "data/test/riautomobility/wheels/wheel.json", "{}"
        ));

        IOException error = assertThrows(IOException.class,
                () -> CarPackArchiveStore.validateRiautoArchive(archive));
        assertTrue(error.getMessage().contains("exactly one"));
    }

    @Test
    void rejectsRiautoDeclaringNoComponents() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":2,\"id\":\"test:empty\",\"name\":\"Empty\","
                        + "\"components\":{\"frames\":[],\"wheels\":[],\"engines\":[]}}"
        ));

        IOException error = assertThrows(IOException.class,
                () -> CarPackArchiveStore.validateRiautoArchive(archive));
        assertTrue(error.getMessage().contains("exactly one"));
    }

    @Test
    void rejectsUndeclaredComponentFiles() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":2,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[\"test:frame\"],\"wheels\":[],\"engines\":[]}}",
                "data/test/riautomobility/frames/frame.json", "{}",
                "data/test/riautomobility/wheels/hidden.json", "{}"
        ));

        IOException error = assertThrows(IOException.class,
                () -> CarPackArchiveStore.validateRiautoArchive(archive));
        assertTrue(error.getMessage().contains("undeclared component file"));
    }

    @Test
    void opensRiautoWithMinecraftFilePackResources() throws IOException {
        Path archive = Files.move(createZip(Map.of(
                "riauto.json", "{\"format\":2,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[],\"wheels\":[\"test:wheel\"],\"engines\":[]}}",
                "data/test/riautomobility/wheels/wheel.json", "{}"
        )), temporaryDirectory.resolve("test.riauto"));

        var supplier = CarPackManager.detectPackResources(archive);
        assertNotNull(supplier);
        try (PackResources resources = supplier.open("test")) {
            assertNotNull(resources.getRootResource("riauto.json"));
        }
    }

    @Test
    void rejectsRiautoWithoutFormatMetadata() throws IOException {
        Path archive = createZip(Map.of("assets/test/textures/car.png", "png"));

        IOException error = assertThrows(IOException.class,
                () -> CarPackArchiveStore.validateRiautoArchive(archive));
        assertTrue(error.getMessage().contains("riauto.json"));
    }

    @Test
    void rejectsUnsupportedRiautoFormatVersion() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":3,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[\"test:car\"],\"wheels\":[]}}"
        ));

        assertThrows(IOException.class, () -> CarPackArchiveStore.validateRiautoArchive(archive));
    }

    @Test
    void rejectsDiscardedV1Format() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":1,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[\"test:car\"],\"wheels\":[],\"engines\":[]}}",
                "data/test/riautomobility/frames/car.json", "{}"
        ));

        assertThrows(IOException.class, () -> CarPackArchiveStore.validateRiautoArchive(archive));
    }

    @Test
    void rejectsPathTraversalEntries() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("riauto.json", "{}");
        entries.put("../outside.txt", "unsafe");
        Path archive = createZip(entries);

        assertThrows(IOException.class, () -> CarPackArchiveStore.validateArchive(archive));
    }

    @Test
    void rejectsPacksWithoutRootMetadata() throws IOException {
        Path archive = createZip(Map.of("assets/test/textures/example.png", "asset"));

        assertThrows(IOException.class, () -> CarPackArchiveStore.validateArchive(archive));
    }

    @Test
    void rejectsVanillaDatapackContent() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":2,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[\"test:car\"],\"wheels\":[],\"engines\":[]}}",
                "data/test/riautomobility/frames/car.json", "{}",
                "data/test/recipes/car.json", "{}"
        ));

        IOException error = assertThrows(IOException.class, () -> CarPackArchiveStore.validateArchive(archive));
        assertTrue(error.getMessage().contains("unsupported vanilla"));
    }

    @Test
    void rejectsMinecraftPackMetadata() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":2,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[\"test:car\"],\"wheels\":[],\"engines\":[]}}",
                "data/test/riautomobility/frames/car.json", "{}",
                "pack.mcmeta", "{}"
        ));

        assertThrows(IOException.class, () -> CarPackArchiveStore.validateArchive(archive));
    }

    @Test
    void repositoryExamplesConformToSingleComponentContract() throws IOException {
        Path examples = Path.of("examples", "components");
        try (var directories = Files.list(examples)) {
            for (Path source : directories.filter(Files::isDirectory).sorted().toList()) {
                Path archive = temporaryDirectory.resolve(source.getFileName() + ".riauto");
                try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive));
                     var paths = Files.walk(source)) {
                    for (Path file : paths.filter(Files::isRegularFile).sorted().toList()) {
                        output.putNextEntry(new ZipEntry(source.relativize(file).toString().replace('\\', '/')));
                        Files.copy(file, output);
                        output.closeEntry();
                    }
                }
                assertDoesNotThrow(() -> CarPackArchiveStore.validateRiautoArchive(archive),
                        source.getFileName().toString());
            }
        }
    }

    @Test
    void calculatesTheRawArchiveDigest() throws Exception {
        Path file = temporaryDirectory.resolve("bytes.bin");
        byte[] bytes = "RIAutomobility cache".getBytes(StandardCharsets.UTF_8);
        Files.write(file, bytes);
        String expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));

        assertEquals(expected, CarPackArchiveStore.sha256(file));
    }

    @Test
    void validatesManifestBoundsAndDigests() {
        String digest = "a".repeat(64);
        assertDoesNotThrow(() -> new CarPackManifestEntry(
                "riautomobility/test", "test", "author", digest, digest, 1024,
                new ResourceLocation("test", "frame")
        ));
        assertThrows(IllegalArgumentException.class, () -> new CarPackManifestEntry(
                "other/test", "test", "author", digest, digest, 1024,
                new ResourceLocation("test", "frame")
        ));
        assertThrows(IllegalArgumentException.class, () -> new CarPackManifestEntry(
                "riautomobility/test", "test", "author", "invalid", digest, 1024,
                new ResourceLocation("test", "frame")
        ));
        assertThrows(IllegalArgumentException.class, () -> new CarPackManifestEntry(
                "riautomobility/test", "test", "author", digest, digest,
                CarPackManifestEntry.MAX_ARCHIVE_SIZE + 1,
                new ResourceLocation("test", "frame")
        ));
        assertThrows(NullPointerException.class, () -> new CarPackManifestEntry(
                "riautomobility/test", "test", "author", digest, digest, 1024, null
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
