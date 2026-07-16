package com.sshakusora.riautomobility.editor.upload;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.network.packet.BeginCarPackUploadPacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class CarPackUploadServiceTest {
    private static final String COMPONENT_PATH = "auto_" + "a".repeat(32);
    private static final String EXPORTING_PLAYER_NAME = "TestPlayer";
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    @TempDir
    Path temporaryDirectory;

    @Test
    void commitsNewPackAndRemovesBackupAfterApplySucceeds() throws Exception {
        Path source = write("upload.riauto", "new");
        Path target = write("installed.riauto", "old");

        CarPackUploadService.installAtomically(source, target, true, () -> {}, () -> {
            throw new AssertionError("rollback must not run");
        });

        assertEquals("new", Files.readString(target));
        assertFalse(Files.exists(source));
        assertNoBackupFiles();
    }

    @Test
    void restoresOverwrittenPackWhenApplyFails() throws Exception {
        Path source = write("upload.riauto", "new");
        Path target = write("installed.riauto", "old");
        AtomicBoolean rolledBack = new AtomicBoolean();

        IOException error = assertThrows(IOException.class, () -> CarPackUploadService.installAtomically(
                source, target, true,
                () -> { throw new IOException("reload failed"); },
                () -> rolledBack.set(true)
        ));

        assertEquals("reload failed", error.getMessage());
        assertEquals("old", Files.readString(target));
        assertFalse(Files.exists(source));
        assertTrue(rolledBack.get());
        assertNoBackupFiles();
    }

    @Test
    void removesNewPackWhenApplyFailsWithoutPreviousPack() throws Exception {
        Path source = write("upload.riauto", "new");
        Path target = temporaryDirectory.resolve("installed.riauto");

        assertThrows(IOException.class, () -> CarPackUploadService.installAtomically(
                source, target, false,
                () -> { throw new IOException("reload failed"); },
                () -> {}
        ));

        assertFalse(Files.exists(target));
        assertNoBackupFiles();
    }

    @Test
    void leavesExistingPackUntouchedWhenOverwriteIsDisabled() throws Exception {
        Path source = write("upload.riauto", "new");
        Path target = write("installed.riauto", "old");

        assertThrows(IOException.class, () -> CarPackUploadService.installAtomically(
                source, target, false, () -> {}, () -> {}
        ));

        assertEquals("old", Files.readString(target));
        assertEquals("new", Files.readString(source));
    }

    @Test
    void acceptsV1EditorArchiveWithExternalTexture() throws IOException {
        Path archive = editorArchive(true);

        CarPackArchiveStore.validateRiautoArchive(archive);
        assertDoesNotThrow(() -> CarPackUploadService.validateEditorArchive(archive, uploadRequest(archive)));
    }

    @Test
    void rejectsV1EditorArchiveWithMissingExternalTexture() throws IOException {
        Path archive = editorArchive(false);

        CarPackArchiveStore.validateRiautoArchive(archive);
        IOException error = assertThrows(IOException.class,
                () -> CarPackUploadService.validateEditorArchive(archive, uploadRequest(archive)));
        assertTrue(error.getMessage().contains("missing mapped resource"));
    }

    private Path write(String name, String contents) throws IOException {
        return Files.writeString(temporaryDirectory.resolve(name), contents);
    }

    private Path editorArchive(boolean includeTexture) throws IOException {
        String id = "riautomobility:" + COMPONENT_PATH;
        String modelResource = "riautomobility:models/entity/automobile/wheel/" + COMPONENT_PATH + ".bbmodel";
        String textureResource = "riautomobility:textures/entity/automobile/wheel/" + COMPONENT_PATH + "/texture.png";
        Map<String, byte[]> content = new LinkedHashMap<>();
        JsonObject files = new JsonObject();
        content.put("component.json",
                ("{\"size\":0.6,\"grip\":0.5,\"radius\":3,\"width\":3,\"model\":{"
                        + "\"type\":\"bbmodel\",\"texture\":\"" + textureResource + "\","
                        + "\"model_id\":\"riautomobility:riautomobility/wheel/" + COMPONENT_PATH + "\","
                        + "\"bbmodel\":\"" + modelResource + "\"}}")
                        .getBytes(StandardCharsets.UTF_8));
        files.addProperty("component.json",
                "data/riautomobility/riautomobility/wheels/" + COMPONENT_PATH + ".json");
        content.put("model.bbmodel",
                ("{\"meta\":{\"format_version\":\"5.0\",\"model_format\":\"modded_entity\"},"
                        + "\"textures\":[{\"name\":\"wheel.png\",\"relative_path\":\""
                        + textureResource + "\"}],\"elements\":[],\"outliner\":[]}")
                        .getBytes(StandardCharsets.UTF_8));
        files.addProperty("model.bbmodel",
                "assets/riautomobility/models/entity/automobile/wheel/" + COMPONENT_PATH + ".bbmodel");
        if (includeTexture) {
            content.put("texture.png", PNG);
            files.addProperty("texture.png", "assets/riautomobility/textures/entity/automobile/wheel/"
                    + COMPONENT_PATH + "/texture.png");
        }
        JsonObject metadata = JsonParser.parseString("{\"format\":1,\"id\":\"" + id + "\",\"name\":\"Wheel\","
                + "\"author\":\"" + EXPORTING_PLAYER_NAME + "\","
                + "\"components\":{\"frames\":[],\"wheels\":[\"" + id + "\"],\"engines\":[]}}").getAsJsonObject();
        metadata.add("files", files);
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("riauto.json", metadata.toString().getBytes(StandardCharsets.UTF_8));
        entries.putAll(content);
        Path archive = temporaryDirectory.resolve("editor-" + includeTexture + ".riauto");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return archive;
    }

    private static BeginCarPackUploadPacket uploadRequest(Path archive) throws IOException {
        return new BeginCarPackUploadPacket(UUID.randomUUID(), "test", "riautomobility", COMPONENT_PATH,
                "wheel", false, Files.size(archive), "0".repeat(64));
    }

    private void assertNoBackupFiles() throws IOException {
        try (var files = Files.list(temporaryDirectory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".backup")));
        }
    }
}
