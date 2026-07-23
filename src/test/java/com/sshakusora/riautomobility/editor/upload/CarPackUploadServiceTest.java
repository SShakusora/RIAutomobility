package com.sshakusora.riautomobility.editor.upload;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.network.packet.BeginCarPackUploadPacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
        assertTrue(Files.isRegularFile(temporaryDirectory.resolve(".riautomobility.revision")));
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
    void acceptsSamePackContentWithoutReloadingWhenOverwriteIsDisabled() {
        assertDoesNotThrow(() -> {
            Path source = sameContentArchive("upload.riauto", "uploaded copy");
            Path target = sameContentArchive("installed.riauto", "installed copy");
            byte[] installedBytes = Files.readAllBytes(target);
            AtomicBoolean applied = new AtomicBoolean();
            AtomicBoolean rolledBack = new AtomicBoolean();

            assertNotEquals(-1L, Files.mismatch(source, target));
            CarPackUploadService.installAtomically(
                    source, target, false,
                    () -> applied.set(true),
                    () -> rolledBack.set(true));

            assertArrayEquals(installedBytes, Files.readAllBytes(target));
            assertFalse(Files.exists(source));
            assertFalse(applied.get());
            assertFalse(rolledBack.get());
            assertFalse(Files.exists(temporaryDirectory.resolve(".riautomobility.revision")));
            assertNoBackupFiles();
        });
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

    private Path sameContentArchive(String name, String comment) throws IOException {
        Path archive = temporaryDirectory.resolve(name);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive), StandardCharsets.UTF_8)) {
            zip.setComment(comment);
            ZipEntry entry = new ZipEntry("component.json");
            entry.setTime(0L);
            zip.putNextEntry(entry);
            zip.write("same component".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return archive;
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
            assertTrue(files.noneMatch(path -> {
                String name = path.getFileName().toString();
                return name.endsWith(".backup") || name.endsWith(".part");
            }));
        }
    }
}
