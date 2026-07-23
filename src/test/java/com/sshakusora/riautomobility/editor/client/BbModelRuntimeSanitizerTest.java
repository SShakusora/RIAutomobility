package com.sshakusora.riautomobility.editor.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sshakusora.riautomobility.model.bbmodel.BbModelData;
import com.sshakusora.riautomobility.model.bbmodel.BbModelParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BbModelRuntimeSanitizerTest {
    private static final String EMBEDDED_PNG = "data:image/png;base64,"
            + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    @Test
    void compactsRuntimeDataAndPreservesAnimationAndExtensions() throws Exception {
        JsonObject original = project();

        BbModelRuntimeSanitizer.ExportedModel exported = BbModelRuntimeSanitizer.externalize(
                original.toString().getBytes(StandardCharsets.UTF_8), "test",
                "textures/entity/automobile/frame/car");
        byte[] sanitizedBytes = exported.modelBytes();
        JsonObject sanitized = JsonParser.parseString(
                new String(sanitizedBytes, StandardCharsets.UTF_8)).getAsJsonObject();

        for (String field : new String[]{
                "reference_images", "backgrounds", "editor_state", "history", "history_index",
                "export_options", "collections", "texture_groups"
        }) {
            assertFalse(sanitized.has(field), field + " should not be exported");
        }
        for (String field : new String[]{
                "animation_controllers", "animation_variable_placeholders",
                "plugin_runtime_data", "display", "overrides"
        }) {
            assertEquals(original.get(field), sanitized.get(field), field + " should be preserved");
        }

        JsonObject element = sanitized.getAsJsonArray("elements").get(0).getAsJsonObject();
        assertEquals("0", element.get("uuid").getAsString());
        assertFalse(element.has("locked"));
        assertFalse(element.has("color"));
        assertFalse(sanitized.has("groups"));
        JsonObject group = sanitized.getAsJsonArray("outliner").get(0).getAsJsonObject();
        assertEquals("1", group.get("uuid").getAsString());
        assertEquals("0", group.getAsJsonArray("children").get(0).getAsString());
        assertFalse(group.has("selected"));

        JsonObject animation = sanitized.getAsJsonArray("animations").get(0).getAsJsonObject();
        assertEquals("animation-uuid", animation.get("uuid").getAsString());
        assertEquals("wheel_spin", animation.get("name").getAsString());
        assertTrue(animation.getAsJsonObject("animators").has("1"));
        JsonObject keyframe = animation.getAsJsonObject("animators").getAsJsonObject("1")
                .getAsJsonArray("keyframes").get(0).getAsJsonObject();
        assertEquals("math.sin(query.anim_time * 180)", keyframe.getAsJsonArray("data_points")
                .get(0).getAsJsonObject().get("x").getAsString());
        assertEquals(2, keyframe.getAsJsonArray("data_points").size());
        assertEquals(3, keyframe.getAsJsonArray("bezier_left_time").size());
        assertEquals(3, keyframe.getAsJsonArray("bezier_left_value").size());
        assertEquals(3, keyframe.getAsJsonArray("bezier_right_time").size());
        assertEquals(3, keyframe.getAsJsonArray("bezier_right_value").size());

        JsonObject texture = sanitized.getAsJsonArray("textures").get(0).getAsJsonObject();
        assertFalse(texture.has("source"));
        assertEquals("texture-uuid", texture.get("uuid").getAsString());
        assertEquals(32, texture.get("uv_width").getAsInt());
        assertFalse(texture.has("path"));
        assertFalse(texture.has("saved"));
        assertEquals("test:textures/entity/automobile/frame/car/texture-0.png",
                texture.get("relative_path").getAsString());
        assertEquals(texture.get("relative_path").getAsString(), exported.defaultTexture());
        assertEquals(1, exported.textureEntries().size());

        assertTrue(original.has("reference_images"));
        assertTrue(original.getAsJsonArray("textures").get(0).getAsJsonObject().has("path"));
        BbModelData.Document document = assertDoesNotThrow(() -> BbModelParser.parse(sanitized));
        assertDoesNotThrow(() -> BbModelParser.requireExternalPngTextures(document));
        assertEquals("1", document.animations().get(0).animators().keySet().iterator().next());
        assertEquals(Map.of("variable.speed", "0"), document.variablePlaceholders());
    }

    @Test
    void storesIdenticalTextureDataOnlyOnce() throws Exception {
        BbModelRuntimeSanitizer.ExportedModel exported = BbModelRuntimeSanitizer.externalize(
                project().toString().getBytes(StandardCharsets.UTF_8), "test",
                "textures/entity/automobile/frame/car");
        JsonObject model = JsonParser.parseString(
                new String(exported.modelBytes(), StandardCharsets.UTF_8)).getAsJsonObject();

        assertEquals(1, exported.textureEntries().size());
        assertEquals(model.getAsJsonArray("textures").get(0).getAsJsonObject().get("relative_path"),
                model.getAsJsonArray("textures").get(1).getAsJsonObject().get("relative_path"));
    }

    @Test
    void producesDeterministicRuntimeModelAndTextureBytes() throws Exception {
        byte[] source = project().toString().getBytes(StandardCharsets.UTF_8);
        BbModelRuntimeSanitizer.ExportedModel first = BbModelRuntimeSanitizer.externalize(
                source, "test", "textures/entity/automobile/frame/car");
        BbModelRuntimeSanitizer.ExportedModel second = BbModelRuntimeSanitizer.externalize(
                source, "test", "textures/entity/automobile/frame/car");

        assertArrayEquals(first.modelBytes(), second.modelBytes());
        assertEquals(first.textureEntries().keySet(), second.textureEntries().keySet());
        for (String texture : first.textureEntries().keySet()) {
            assertArrayEquals(first.textureEntries().get(texture), second.textureEntries().get(texture));
        }
        assertEquals(first.defaultTexture(), second.defaultTexture());
    }

    @Test
    void optimizesPngWithoutChangingDecodedPixels() throws Exception {
        BbModelRuntimeSanitizer.ExportedModel exported = BbModelRuntimeSanitizer.externalize(
                project().toString().getBytes(StandardCharsets.UTF_8), "test",
                "textures/entity/automobile/frame/car");
        byte[] optimized = exported.textureEntries().values().iterator().next();
        byte[] original = Base64.getDecoder().decode(
                EMBEDDED_PNG.substring("data:image/png;base64,".length()));

        BufferedImage expected = ImageIO.read(new ByteArrayInputStream(original));
        BufferedImage actual = ImageIO.read(new ByteArrayInputStream(optimized));
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertEquals(expected.getRGB(x, y), actual.getRGB(x, y));
            }
        }
    }

    @Test
    void substantiallyCompactsAProductionSizedModel() throws Exception {
        String resource = "assets/riautomobility/models/entity/automobile/builtin/lorry_frame.bbmodel";
        byte[] source;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            source = input.readAllBytes();
        }
        JsonObject original = JsonParser.parseString(new String(source, StandardCharsets.UTF_8)).getAsJsonObject();
        byte[] originalPng = Base64.getDecoder().decode(original.getAsJsonArray("textures").get(0)
                .getAsJsonObject().get("source").getAsString().substring("data:image/png;base64,".length()));
        BbModelData.Document originalDocument = BbModelParser.parse(original);

        BbModelRuntimeSanitizer.ExportedModel exported = BbModelRuntimeSanitizer.externalize(
                source, "test", "textures/entity/automobile/frame/lorry");
        JsonObject compact = JsonParser.parseString(
                new String(exported.modelBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        BbModelData.Document compactDocument = BbModelParser.parse(compact);

        assertTrue(exported.modelBytes().length < 110_000,
                () -> "Expected compact runtime model, got " + exported.modelBytes().length + " bytes");
        assertTrue(exported.textureEntries().values().iterator().next().length <= originalPng.length);
        assertEquals(countNodes(originalDocument.roots()), countNodes(compactDocument.roots()));
        assertEquals(originalDocument.animations().size(), compactDocument.animations().size());
    }

    private static int countNodes(List<BbModelData.Node> nodes) {
        int count = 0;
        for (BbModelData.Node node : nodes) {
            count++;
            if (node instanceof BbModelData.GroupNode group) count += countNodes(group.children());
        }
        return count;
    }

    private static JsonObject project() {
        return JsonParser.parseString(("""
                {
                  "meta": {"format_version": "5.0", "model_format": "modded_entity"},
                  "credit": "  Test   Author  ",
                  "resolution": {"width": 32, "height": 32},
                  "textures": [{
                    "uuid": "texture-uuid",
                    "id": "0",
                    "name": "body.png",
                    "path": "C:/projects/car/body.png",
                    "relative_path": "body.png",
                    "source": "%s",
                    "render_mode": "default",
                    "saved": true,
                    "uv_width": 32,
                    "uv_height": 32
                  }, {
                    "uuid": "duplicate-texture-uuid",
                    "id": "1",
                    "name": "duplicate.png",
                    "source": "%s",
                    "uv_width": 32,
                    "uv_height": 32
                  }],
                  "elements": [{
                    "uuid": "cube-uuid",
                    "name": "body",
                    "type": "cube",
                    "from": [0, 0, 0],
                    "to": [16, 16, 16],
                    "locked": true,
                    "color": 3,
                    "faces": {}
                  }],
                  "groups": [{"uuid": "bone-uuid", "name": "body", "selected": true,
                    "children": ["cube-uuid"]}],
                  "outliner": ["bone-uuid"],
                  "animations": [{
                    "uuid": "animation-uuid",
                    "name": "wheel_spin",
                    "length": 1.0,
                    "loop": "loop",
                    "animators": {
                      "bone-uuid": {
                        "name": "body",
                        "type": "bone",
                        "keyframes": [{
                          "channel": "rotation",
                          "time": 0.5,
                          "interpolation": "bezier",
                          "data_points": [
                            {"x": "math.sin(query.anim_time * 180)", "y": 180, "z": 0},
                            {"x": 0, "y": 200, "z": 0}
                          ],
                          "bezier_left_time": [0.1, 0.1, 0.1],
                          "bezier_left_value": [0, 160, 0],
                          "bezier_right_time": [0.2, 0.2, 0.2],
                          "bezier_right_value": [0, 200, 0]
                        }]
                      }
                    }
                  }],
                  "animation_controllers": [{"name": "controller.animation.car"}],
                  "animation_variable_placeholders": "variable.speed = 0;",
                  "display": {"gui": {"rotation": [0, 0, 0]}},
                  "overrides": [{"predicate": {"custom_model_data": 1}}],
                  "plugin_runtime_data": {"future_animation_extension": true},
                  "reference_images": [{"name": "blueprint", "source": "data:image/png;base64,large"}],
                  "backgrounds": {"front": "data:image/png;base64,large"},
                  "editor_state": {"save_path": "C:/projects/car.bbmodel"},
                  "history": [{"before": {}, "post": {}}],
                  "history_index": 1,
                  "export_options": {"codec": "project"},
                  "collections": [{"name": "selection"}],
                  "texture_groups": [{"name": "paint"}]
                }
                """).formatted(EMBEDDED_PNG, EMBEDDED_PNG)).getAsJsonObject();
    }
}
