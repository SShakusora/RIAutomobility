package com.sshakusora.riautomobility.editor.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sshakusora.riautomobility.model.bbmodel.BbModelParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BbModelRuntimeSanitizerTest {
    private static final String EMBEDDED_PNG = "data:image/png;base64,"
            + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    @Test
    void removesEditorDataAndLocalPathsButPreservesAnimationAndExtensions() throws Exception {
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
                "animations", "animation_controllers", "animation_variable_placeholders",
                "elements", "groups", "outliner", "plugin_runtime_data", "display", "overrides"
        }) {
            assertEquals(original.get(field), sanitized.get(field), field + " should be preserved");
        }

        JsonObject texture = sanitized.getAsJsonArray("textures").get(0).getAsJsonObject();
        assertFalse(texture.has("source"));
        assertEquals("texture-uuid", texture.get("uuid").getAsString());
        assertEquals(32, texture.get("uv_width").getAsInt());
        assertFalse(texture.has("path"));
        assertTrue(texture.get("relative_path").getAsString().matches(
                "test:textures/entity/automobile/frame/car/[0-9a-f]{64}\\.png"));
        assertEquals(texture.get("relative_path").getAsString(), exported.defaultTexture());
        assertEquals(1, exported.textureEntries().size());

        assertTrue(original.has("reference_images"));
        assertTrue(original.getAsJsonArray("textures").get(0).getAsJsonObject().has("path"));
        assertDoesNotThrow(() -> BbModelParser.requireExternalPngTextures(BbModelParser.parse(sanitized)));
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
                    "faces": {}
                  }],
                  "groups": [{"uuid": "bone-uuid", "name": "body", "children": ["cube-uuid"]}],
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
                          "data_points": [{"x": 0, "y": 180, "z": 0}],
                          "bezier_left_time": [0.1, 0.1, 0.1],
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
