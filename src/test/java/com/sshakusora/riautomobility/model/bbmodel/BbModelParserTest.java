package com.sshakusora.riautomobility.model.bbmodel;

import com.google.gson.JsonParser;
import com.sshakusora.riautomobility.content.FrameSpec;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BbModelParserTest {
    @Test
    void parsesNativeGeometryHierarchyTexturesAndAnimation() {
        BbModelData.Document document = BbModelParser.parse(JsonParser.parseString("""
                {
                  "meta": {"format_version": "5.0", "model_format": "modded_entity", "box_uv": false},
                  "resolution": {"width": 64, "height": 32},
                  "textures": [
                    {"uuid": "body-texture", "name": "body.png", "uv_width": 64, "uv_height": 32},
                    {"uuid": "trim-texture", "name": "trim.png", "uv_width": 16, "uv_height": 16}
                  ],
                  "elements": [
                    {
                      "type": "cube", "uuid": "body", "name": "Body", "origin": [0, 8, 0],
                      "from": [-8, 0, -12], "to": [8, 8, 12],
                      "faces": {"north": {"uv": [0, 0, 16, 8], "texture": 0}}
                    },
                    {
                      "type": "mesh", "uuid": "spoiler", "name": "Spoiler", "origin": [0, 8, 0],
                      "vertices": {"a": [-6, 0, 0], "b": [6, 0, 0], "c": [0, 3, 0]},
                      "faces": {"front": {"vertices": ["a", "b", "c"], "uv": {"a": [0, 0], "b": [16, 0], "c": [8, 8]}, "texture": 1}}
                    }
                  ],
                  "groups": [{"uuid": "root-bone", "name": "Root", "origin": [0, 8, 0]}],
                  "outliner": [{"uuid": "root-bone", "name": "Root", "origin": [0, 8, 0], "children": ["body", "spoiler"]}],
                  "animations": [{
                    "uuid": "idle", "name": "idle", "length": 1.0, "loop": "loop",
                    "animators": {"root-bone": {"name": "Root", "type": "bone", "keyframes": [
                      {"channel": "position", "time": 0, "interpolation": "linear", "data_points": [{"x": 0, "y": 0, "z": 0}]},
                      {"channel": "position", "time": 1, "interpolation": "bezier", "data_points": [{"x": 0, "y": "math.sin(query.anim_time * 180)", "z": 0}]}
                    ]}}
                  }]
                }
                """).getAsJsonObject());

        assertEquals("5.0", document.formatVersion());
        assertEquals(2, document.textures().size());
        assertEquals(1, document.roots().size());
        BbModelData.GroupNode root = assertInstanceOf(BbModelData.GroupNode.class, document.roots().get(0));
        assertInstanceOf(BbModelData.Cube.class, ((BbModelData.ElementNode) root.children().get(0)).geometry());
        assertInstanceOf(BbModelData.Mesh.class, ((BbModelData.ElementNode) root.children().get(1)).geometry());
        assertEquals(1, document.animations().size());
        assertEquals(2, document.animations().get(0).animators().get("root-bone").keyframes().size());
    }

    @Test
    void rejectsFutureProjectVersions() {
        BbModelFormatException error = assertThrows(BbModelFormatException.class, () -> BbModelParser.parse(JsonParser.parseString("""
                {"meta":{"format_version":"5.1","model_format":"free"},"elements":[]}
                """).getAsJsonObject()));
        assertEquals("Blockbench project version 5.1 is newer than supported version 5.0", error.getMessage());
    }

    @Test
    void rejectsPluginFormatsWithoutAnAdapter() {
        assertThrows(BbModelFormatException.class, () -> BbModelParser.parse(JsonParser.parseString("""
                {"meta":{"format_version":"5.0","model_format":"plugin_vehicle"},"elements":[]}
                """).getAsJsonObject()));
    }

    @Test
    void comparesSemanticVersionsNumerically() {
        assertEquals(1, BbModelParser.compareVersions("4.10", "4.9"));
        assertEquals(0, BbModelParser.compareVersions("5.0.0", "5.0"));
        assertEquals(-1, BbModelParser.compareVersions("4.10", "5.0"));
    }

    @Test
    void migratesPreFiveAnimationAxes() {
        BbModelData.Document document = BbModelParser.parse(JsonParser.parseString("""
                {
                  "meta":{"format_version":"4.10","model_format":"modded_entity"},
                  "elements":[],
                  "animations":[{"name":"old","animators":{"bone":{"type":"bone","keyframes":[
                    {"channel":"rotation","time":0,"data_points":[{"x":10,"y":"query.anim_time","z":5}]}
                  ]}}}]
                }
                """).getAsJsonObject());
        BbModelData.DataPoint point = document.animations().get(0).animators().get("bone").keyframes().get(0).dataPoints().get(0);
        assertEquals(-10, point.x().getAsInt());
        assertEquals("-(query.anim_time)", point.y().getAsString());
        assertEquals(5, point.z().getAsInt());
    }

    @Test
    void parsesShippedFrameAndWheelExamples() throws IOException {
        BbModelData.Document frame = BbModelParser.parse(JsonParser.parseString(Files.readString(Path.of(
                "examples/examplepack/assets/examplepack/models/entity/automobile/frame/example_buggy.bbmodel"
        ))).getAsJsonObject());
        BbModelData.Document wheel = BbModelParser.parse(JsonParser.parseString(Files.readString(Path.of(
                "examples/examplepack/assets/examplepack/models/entity/automobile/wheel/example_buggy.bbmodel"
        ))).getAsJsonObject());

        assertEquals(3, ((BbModelData.GroupNode) frame.roots().get(0)).children().size());
        assertEquals(1, wheel.roots().size());
    }

    @Test
    void modelSpecPreservesBbModelFieldsAcrossJsonSync() {
        FrameSpec.ModelSpec original = FrameSpec.ModelSpec.fromJson(JsonParser.parseString("""
                {
                  "type":"bbmodel",
                  "texture":"example:textures/car.png",
                  "model_id":"example:car",
                  "bbmodel":"example:models/car.bbmodel",
                  "bb_animation":"drive",
                  "textures":{"body":"example:textures/body.png"}
                }
                """).getAsJsonObject());
        FrameSpec.ModelSpec restored = FrameSpec.ModelSpec.fromJson(original.toJson());

        assertEquals(original, restored);
    }

    @Test
    void bbModelSpecAllowsEmbeddedTexturesWithoutExternalFallback() {
        FrameSpec.ModelSpec spec = FrameSpec.ModelSpec.fromJson(JsonParser.parseString("""
                {"type":"bbmodel","model_id":"example:embedded","bbmodel":"example:models/embedded.bbmodel"}
                """).getAsJsonObject());

        assertEquals("minecraft:textures/item/barrier.png", spec.texture().toString());
    }

    @Test
    void componentBbModelStringDerivesRuntimeModelId() {
        FrameSpec.ModelSpec spec = FrameSpec.ModelSpec.fromComponentJson(
                new com.google.gson.JsonPrimitive("example:models/vehicles/car.bbmodel"),
                new net.minecraft.resources.ResourceLocation("example", "car"),
                "frame"
        );

        assertEquals("bbmodel", spec.type());
        assertEquals("example:models/vehicles/car.bbmodel", spec.bbModel().toString());
        assertEquals("example:riautomobility/frame/car", spec.modelId().toString());
    }

    @Test
    void omittedComponentModelUsesConventionalBbModelPath() {
        FrameSpec.ModelSpec spec = FrameSpec.ModelSpec.fromComponentJson(
                null,
                new net.minecraft.resources.ResourceLocation("example", "sports/red_car"),
                "wheel"
        );

        assertEquals("example:models/entity/automobile/wheel/sports/red_car.bbmodel", spec.bbModel().toString());
        assertEquals("example:riautomobility/wheel/sports/red_car", spec.modelId().toString());
        assertEquals("", spec.bbAnimation());
        assertTrue(spec.textureOverrides().isEmpty());
    }

    @Test
    void parsesShorthandModelsInShippedComponentDefinitions() throws IOException {
        net.minecraft.resources.ResourceLocation id = new net.minecraft.resources.ResourceLocation("examplepack", "example_buggy_bbmodel");
        FrameSpec frame = FrameSpec.fromJson(id, JsonParser.parseString(Files.readString(Path.of(
                "examples/examplepack/data/examplepack/riautomobility/frames/example_buggy_bbmodel.json"
        ))).getAsJsonObject());
        com.sshakusora.riautomobility.content.WheelSpec wheel = com.sshakusora.riautomobility.content.WheelSpec.fromJson(
                id,
                JsonParser.parseString(Files.readString(Path.of(
                        "examples/examplepack/data/examplepack/riautomobility/wheels/example_buggy_bbmodel.json"
                ))).getAsJsonObject()
        );

        assertEquals("examplepack:models/entity/automobile/frame/example_buggy.bbmodel", frame.model().bbModel().toString());
        assertEquals("examplepack:riautomobility/frame/example_buggy_bbmodel", frame.model().modelId().toString());
        assertEquals("examplepack:models/entity/automobile/wheel/example_buggy.bbmodel", wheel.model().bbModel().toString());
        assertEquals("examplepack:riautomobility/wheel/example_buggy_bbmodel", wheel.model().modelId().toString());
    }

    @Test
    void evaluatesMolangAnimationValues() {
        BbModelData.Document document = BbModelParser.parse(JsonParser.parseString("""
                {
                  "meta":{"format_version":"5.0","model_format":"modded_entity"},
                  "elements":[],
                  "animations":[{"name":"molang","animators":{"bone":{"type":"bone","keyframes":[
                    {"channel":"position","time":0,"data_points":[{"x":"math.sin(90)","y":0,"z":0}]}
                  ]}}}]
                }
                """).getAsJsonObject());

        BbAnimationPlayer.Transform transform = BbAnimationPlayer.sample(document, "molang", null).get("bone");
        assertEquals(1.0F, transform.position().x, 0.0001F);
    }

    @Test
    void bezierInterpolationUsesRelativeValueAndTimeHandles() {
        BbModelData.Keyframe before = keyframe(0.0F, new float[]{0.8F, 0.8F, 0.8F}, new float[]{1, 1, 1}, new float[0], new float[0]);
        BbModelData.Keyframe after = keyframe(1.0F, new float[0], new float[0], new float[]{-0.1F, -0.1F, -0.1F}, new float[]{1, 1, 1});

        float value = BbAnimationPlayer.bezierAxis(before, after, 0.0F, 0.0F, 0.5F, 0);

        assertEquals(0.5828F, value, 0.001F);
    }

    @Test
    void preservesIncomingAndOutgoingKeyframeDataPoints() {
        BbModelData.Keyframe before = new BbModelData.Keyframe(
                "position", 0, "step",
                java.util.List.of(
                        new BbModelData.DataPoint(new com.google.gson.JsonPrimitive(1), new com.google.gson.JsonPrimitive(0), new com.google.gson.JsonPrimitive(0)),
                        new BbModelData.DataPoint(new com.google.gson.JsonPrimitive(2), new com.google.gson.JsonPrimitive(0), new com.google.gson.JsonPrimitive(0))
                ), new float[0], new float[0], new float[0], new float[0]
        );
        BbModelData.Keyframe after = keyframe(1.0F, new float[0], new float[0], new float[0], new float[0]);
        org.joml.Vector3f value = BbAnimationPlayer.sampleChannel(java.util.List.of(before, after), 0.5F, new org.joml.Vector3f());

        assertEquals(2.0F, value.x, 0.0001F);
    }

    @Test
    void convertsBlockbenchCoordinatesToAutomobilityModelSpace() {
        org.joml.Vector3f position = BbCoordinateSystem.position(new org.joml.Vector3f(2, 5, 7));
        org.joml.Vector3f rotation = BbCoordinateSystem.rotation(new org.joml.Vector3f(10, 20, 30));

        assertEquals(new org.joml.Vector3f(2, -5, 7), position);
        assertEquals(new org.joml.Vector3f(-10, 20, -30), rotation);
    }

    @Test
    void coordinateReflectionPreservesFaceWindingAndUvPairing() {
        org.joml.Vector3f[] vertices = {
                new org.joml.Vector3f(0, 0, 0),
                new org.joml.Vector3f(1, 0, 0),
                new org.joml.Vector3f(1, 1, 0),
                new org.joml.Vector3f(0, 1, 0)
        };
        org.joml.Vector2f[] uvs = {
                new org.joml.Vector2f(0, 0),
                new org.joml.Vector2f(1, 0),
                new org.joml.Vector2f(1, 1),
                new org.joml.Vector2f(0, 1)
        };

        BbCoordinateSystem.ConvertedQuad converted = BbCoordinateSystem.quad(vertices, uvs);
        org.joml.Vector3f normal = new org.joml.Vector3f(converted.vertices()[1])
                .sub(converted.vertices()[0])
                .cross(new org.joml.Vector3f(converted.vertices()[2]).sub(converted.vertices()[0]));

        assertTrue(normal.z > 0);
        assertEquals(new org.joml.Vector3f(1, -1, 0), converted.vertices()[0]);
        assertEquals(new org.joml.Vector2f(1, 1), converted.uvs()[0]);
    }

    @Test
    void coordinateReflectionKeepsDegenerateTriangleNormalValid() {
        org.joml.Vector3f[] vertices = {
                new org.joml.Vector3f(0, 0, 0),
                new org.joml.Vector3f(1, 0, 0),
                new org.joml.Vector3f(0, 1, 0),
                new org.joml.Vector3f(0, 1, 0)
        };
        org.joml.Vector2f[] uvs = {
                new org.joml.Vector2f(0, 0),
                new org.joml.Vector2f(1, 0),
                new org.joml.Vector2f(0, 1),
                new org.joml.Vector2f(0, 1)
        };

        BbCoordinateSystem.ConvertedQuad converted = BbCoordinateSystem.quad(vertices, uvs);
        org.joml.Vector3f normal = new org.joml.Vector3f(converted.vertices()[1])
                .sub(converted.vertices()[0])
                .cross(new org.joml.Vector3f(converted.vertices()[2]).sub(converted.vertices()[0]));

        assertTrue(normal.lengthSquared() > 0);
        assertTrue(normal.z > 0);
    }

    private static BbModelData.Keyframe keyframe(float time, float[] rightTime, float[] rightValue, float[] leftTime, float[] leftValue) {
        return new BbModelData.Keyframe(
                "position", time, "bezier",
                java.util.List.of(new BbModelData.DataPoint(new com.google.gson.JsonPrimitive(0), new com.google.gson.JsonPrimitive(0), new com.google.gson.JsonPrimitive(0))),
                leftTime, leftValue, rightTime, rightValue
        );
    }

}
