package com.sshakusora.riautomobility.editor.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class VehiclePackImporterTest {
    private static final String EMBEDDED_PNG = "data:image/png;base64,"
            + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    @TempDir
    Path temporaryDirectory;

    @Test
    void importsTheMatchingBbModelComponentAndItsSavedValues() throws IOException {
        Path archive = archive(Map.of(
                "sample:frame", frameJson("sample:models/entity/automobile/frame/frame.bbmodel", 1.25F)),
                Map.of("sample:frame", bbModel()));

        var imported = VehiclePackImporter.importComponent(
                archive, temporaryDirectory.resolve("imports"));

        assertEquals(VehicleEditorDraft.Target.FRAME, imported.target());
        assertEquals("Imported Vehicle", imported.displayName());
        assertEquals("OriginalPlayer", imported.author());
        assertEquals(1.25F, imported.frame().weight());
        assertTrue(Files.isRegularFile(imported.modelFile()));
        JsonObject importedModel = JsonParser.parseString(Files.readString(imported.modelFile())).getAsJsonObject();
        assertEquals(EMBEDDED_PNG, importedModel.getAsJsonArray("textures").get(0)
                .getAsJsonObject().get("source").getAsString());
    }

    @Test
    void detectsTheSoleComponentTypeFromMetadata() throws IOException {
        Path archive = archive(Map.of(
                "sample:frame", frameJson("sample:models/entity/automobile/frame/frame.bbmodel", 1.25F)),
                Map.of("sample:frame", bbModel()));

        var imported = VehiclePackImporter.importComponent(
                archive, temporaryDirectory.resolve("imports"));

        assertEquals(VehicleEditorDraft.Target.FRAME, imported.target());
        assertNotNull(imported.frame());
        assertNull(imported.wheel());
    }

    @Test
    void rejectsASoleComponentThatIsNotEditableAsBbModel() throws IOException {
        Path archive = archive(Map.of("sample:legacy", frameJson("jsonem", 0.5F)), Map.of());

        IOException exception = assertThrows(IOException.class, () -> VehiclePackImporter.importComponent(
                archive, temporaryDirectory.resolve("imports")));

        assertTrue(exception.getMessage().contains("does not use an editable BBModel"));
    }

    @Test
    void rejectsPacksWithMultipleEditableComponentsForTheCurrentPage() throws IOException {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("sample:first", frameJson(
                "sample:models/entity/automobile/frame/first.bbmodel", 0.5F));
        components.put("sample:second", frameJson(
                "sample:models/entity/automobile/frame/second.bbmodel", 0.9F));
        Path archive = archive(components, Map.of(
                "sample:first", bbModel(),
                "sample:second", bbModel()));

        IOException exception = assertThrows(IOException.class, () -> VehiclePackImporter.importComponent(
                archive, temporaryDirectory.resolve("imports")));

        assertTrue(exception.getMessage().contains("exactly one"));
    }

    @Test
    void restoresExternalV2TexturesForEditing() throws IOException {
        Path archive = v2Archive(true);

        var imported = VehiclePackImporter.importComponent(
                archive, temporaryDirectory.resolve("v2-imports"));

        String importedModel = Files.readString(imported.modelFile());
        var texture = com.google.gson.JsonParser.parseString(importedModel).getAsJsonObject()
                .getAsJsonArray("textures").get(0).getAsJsonObject();
        assertEquals(EMBEDDED_PNG, texture.get("source").getAsString());
        assertTrue(texture.get("internal").getAsBoolean());
        assertFalse(texture.has("relative_path"));
    }

    @Test
    void rejectsV2ModelsWhoseExternalTextureIsMissing() throws IOException {
        Path archive = v2Archive(false);

        IOException exception = assertThrows(IOException.class, () -> VehiclePackImporter.importComponent(
                archive, temporaryDirectory.resolve("missing-v2-imports")));

        assertTrue(exception.getMessage().contains("missing external texture"));
    }

    private Path archive(Map<String, String> frameComponents, Map<String, String> bbModels) throws IOException {
        String frameIds = frameComponents.keySet().stream().map(id -> "\"" + id + "\"")
                .reduce((left, right) -> left + "," + right).orElse("");
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("riauto.json", ("{\"format\":2,\"id\":\"sample:pack\",\"name\":\"Imported Vehicle\","
                + "\"author\":\"OriginalPlayer\","
                + "\"components\":{\"frames\":[" + frameIds + "],\"wheels\":[],\"engines\":[]}}")
                .getBytes(StandardCharsets.UTF_8));
        frameComponents.forEach((id, json) -> entries.put(componentEntry(id), json.getBytes(StandardCharsets.UTF_8)));
        bbModels.forEach((id, modelJson) -> addExternalModel(entries, id, modelJson));

        Path archive = temporaryDirectory.resolve("test-" + frameComponents.size() + "-" + bbModels.size() + ".riauto");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive), StandardCharsets.UTF_8)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return archive;
    }

    private static void addExternalModel(Map<String, byte[]> entries, String id, String modelJson) {
        String[] parts = id.split(":", 2);
        JsonObject model = JsonParser.parseString(modelJson).getAsJsonObject();
        var textures = model.getAsJsonArray("textures");
        for (int index = 0; index < textures.size(); index++) {
            JsonObject texture = textures.get(index).getAsJsonObject();
            byte[] png = Base64.getDecoder().decode(
                    texture.remove("source").getAsString().substring("data:image/png;base64,".length()));
            String texturePath = "textures/entity/automobile/frame/" + parts[1] + "/texture-" + index + ".png";
            texture.addProperty("relative_path", parts[0] + ":" + texturePath);
            entries.put("assets/" + parts[0] + "/" + texturePath, png);
        }
        entries.put(modelEntry(id), model.toString().getBytes(StandardCharsets.UTF_8));
    }

    private Path v2Archive(boolean includeTexture) throws IOException {
        String textureResource = "sample:textures/entity/automobile/frame/frame/texture.png";
        String model = "{\"meta\":{\"format_version\":\"5.0\",\"model_format\":\"modded_entity\"},"
                + "\"textures\":[{\"uuid\":\"body\",\"name\":\"body.png\",\"relative_path\":\""
                + textureResource + "\"}],\"elements\":[],\"outliner\":[]}";
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("riauto.json", ("{\"format\":2,\"id\":\"sample:pack\",\"name\":\"Imported Vehicle\","
                + "\"author\":\"OriginalPlayer\","
                + "\"components\":{\"frames\":[\"sample:frame\"],\"wheels\":[],\"engines\":[]}}")
                .getBytes(StandardCharsets.UTF_8));
        entries.put(componentEntry("sample:frame"),
                frameJson("sample:models/entity/automobile/frame/frame.bbmodel", 1.25F)
                        .getBytes(StandardCharsets.UTF_8));
        entries.put(modelEntry("sample:frame"), model.getBytes(StandardCharsets.UTF_8));
        if (includeTexture) {
            entries.put("assets/sample/textures/entity/automobile/frame/frame/texture.png",
                    Base64.getDecoder().decode(EMBEDDED_PNG.substring("data:image/png;base64,".length())));
        }

        Path archive = temporaryDirectory.resolve("v2-" + includeTexture + ".riauto");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive), StandardCharsets.UTF_8)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return archive;
    }

    private static String componentEntry(String id) {
        String[] parts = id.split(":", 2);
        return "data/" + parts[0] + "/riautomobility/frames/" + parts[1] + ".json";
    }

    private static String modelEntry(String id) {
        String[] parts = id.split(":", 2);
        return "assets/" + parts[0] + "/models/entity/automobile/frame/" + parts[1] + ".bbmodel";
    }

    private static String frameJson(String model, float weight) {
        String modelJson = "jsonem".equals(model)
                ? "{\"type\":\"jsonem\",\"texture\":\"minecraft:textures/item/barrier.png\",\"model_id\":\"automobility:empty\"}"
                : "\"" + model + "\"";
        return "{\"weight\":" + weight + ",\"model\":" + modelJson
                + ",\"wheel_base\":{\"forward_separation\":16,\"side_separation\":10},"
                + "\"length_px\":24,\"engine_pos_back\":8,\"engine_pos_up\":2,"
                + "\"rear_attachment_pos\":12,\"front_attachment_pos\":12,"
                + "\"dimensions\":{\"width\":1.5,\"height\":1},\"seats\":[],"
                + "\"camera_positions\":[],\"hitboxes\":[],\"show_in_creative_tab\":true}";
    }

    private static String bbModel() {
        return "{\"meta\":{\"format_version\":\"5.0\",\"model_format\":\"modded_entity\"},"
                + "\"textures\":[{\"name\":\"body.png\",\"source\":\"" + EMBEDDED_PNG + "\"}],"
                + "\"elements\":[],\"outliner\":[]}";
    }
}
