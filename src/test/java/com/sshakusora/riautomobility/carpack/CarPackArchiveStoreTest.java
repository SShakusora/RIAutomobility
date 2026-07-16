package com.sshakusora.riautomobility.carpack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
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
                "riauto.json", "{\"format\":1,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[\"test:car\"],\"wheels\":[],\"engines\":[]}}",
                "assets/test/textures/entity/car.png", "png",
                "data/test/riautomobility/frames/car.json", "{}"
        ));

        assertDoesNotThrow(() -> CarPackArchiveStore.validateArchive(archive));
    }

    @Test
    void acceptsAValidRiautoCarPack() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":1,\"id\":\"test:car\",\"name\":\"Test Car\",\"author\":\"Test Author\","
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
                "riauto.json", "{\"format\":1,\"id\":\"test:car\",\"name\":\"Test Car\",\"author\":\"bad\\nauthor\","
                        + "\"components\":{\"frames\":[\"test:car\"],\"wheels\":[],\"engines\":[]}}",
                "data/test/riautomobility/frames/car.json", "{}"
        ));

        assertThrows(IOException.class, () -> CarPackArchiveStore.validateRiautoArchive(archive));
    }

    @Test
    void acceptsAnEngineOnlyRiautoCarPack() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":1,\"id\":\"test:engine\",\"name\":\"Test Engine\","
                        + "\"components\":{\"frames\":[],\"wheels\":[],\"engines\":[\"test:engine\"]}}",
                "data/test/riautomobility/engines/engine.json", "{}"
        ));

        assertDoesNotThrow(() -> CarPackArchiveStore.validateRiautoArchive(archive));
    }

    @Test
    void readsTheComponentOwnershipIndexFromMetadata() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":1,\"id\":\"test:car\",\"name\":\"Test Car\","
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
                "riauto.json", "{\"format\":1,\"id\":\"test:car\",\"name\":\"Test Car\","
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
                "riauto.json", "{\"format\":1,\"id\":\"test:empty\",\"name\":\"Empty\","
                        + "\"components\":{\"frames\":[],\"wheels\":[],\"engines\":[]}}"
        ));

        IOException error = assertThrows(IOException.class,
                () -> CarPackArchiveStore.validateRiautoArchive(archive));
        assertTrue(error.getMessage().contains("exactly one"));
    }

    @Test
    void rejectsUndeclaredComponentFiles() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":1,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[\"test:frame\"],\"wheels\":[],\"engines\":[]}}",
                "data/test/riautomobility/frames/frame.json", "{}",
                "data/test/riautomobility/wheels/hidden.json", "{}"
        ));

        IOException error = assertThrows(IOException.class,
                () -> CarPackArchiveStore.validateRiautoArchive(archive));
        assertTrue(error.getMessage().contains("undeclared component resource"));
    }

    @Test
    void opensRiautoWithMinecraftFilePackResources() throws IOException {
        Path archive = Files.move(createZip(Map.of(
                "riauto.json", "{\"format\":1,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[],\"wheels\":[\"test:wheel\"],\"engines\":[]}}",
                "data/test/riautomobility/wheels/wheel.json", "{}"
        )), temporaryDirectory.resolve("test.riauto"));

        var supplier = CarPackManager.detectPackResources(archive);
        assertNotNull(supplier);
        try (PackResources resources = supplier.open("test")) {
            assertNotNull(resources.getRootResource("riauto.json"));
            var component = resources.getResource(PackType.SERVER_DATA,
                    new ResourceLocation("test", "riautomobility/wheels/wheel.json"));
            assertNotNull(component);
            try (var input = component.get()) {
                assertEquals("{}", new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
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
    void rejectsDiscardedV2Format() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":2,\"id\":\"test:car\",\"name\":\"Test Car\","
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
                "riauto.json", "{\"format\":1,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[\"test:car\"],\"wheels\":[],\"engines\":[]}}",
                "data/test/riautomobility/frames/car.json", "{}",
                "data/test/recipes/car.json", "{}"
        ));

        IOException error = assertThrows(IOException.class, () -> CarPackArchiveStore.validateArchive(archive));
        assertTrue(error.getMessage().contains("unsupported resource"));
    }

    @Test
    void rejectsManuallyPackagedLegacyModels() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":1,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[\"test:car\"],\"wheels\":[],\"engines\":[]}}",
                "data/test/riautomobility/frames/car.json", "{}",
                "assets/test/models/entity/automobile/frame/car/main.json", "{}",
                "assets/test/models/legacy/car.model.json", "{}"
        ));

        IOException error = assertThrows(IOException.class, () -> CarPackArchiveStore.validateArchive(archive));
        assertTrue(error.getMessage().contains("unsupported resource"));
    }

    @Test
    void rejectsMinecraftPackMetadata() throws IOException {
        Path archive = createZip(Map.of(
                "riauto.json", "{\"format\":1,\"id\":\"test:car\",\"name\":\"Test Car\","
                        + "\"components\":{\"frames\":[\"test:car\"],\"wheels\":[],\"engines\":[]}}",
                "data/test/riautomobility/frames/car.json", "{}",
                "pack.mcmeta", "{}"
        ));

        assertThrows(IOException.class, () -> CarPackArchiveStore.validateArchive(archive));
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
        Map<String, String> flattened = new LinkedHashMap<>();
        JsonObject files = new JsonObject();
        int index = 0;
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("assets/") || name.startsWith("data/")) {
                String extension = name.substring(name.lastIndexOf('.'));
                String physicalName = "file-" + index++ + extension;
                flattened.put(physicalName, entry.getValue());
                files.addProperty(physicalName, name);
            } else {
                flattened.put(name, entry.getValue());
            }
        }
        if (flattened.containsKey("riauto.json")) {
            try {
                JsonObject metadata = JsonParser.parseString(flattened.get("riauto.json")).getAsJsonObject();
                if (metadata.has("format") && metadata.get("format").getAsInt() == 1) {
                    metadata.add("files", files);
                    flattened.put("riauto.json", metadata.toString());
                }
            } catch (RuntimeException ignored) {
            }
        }
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, String> entry : flattened.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        return archive;
    }
}
