package com.sshakusora.riautomobility.model.bbmodel;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.WheelSpec;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                "examples/components/frame-bbmodel/assets/examplepack/models/entity/automobile/frame/example_buggy.bbmodel"
        ))).getAsJsonObject());
        BbModelData.Document wheel = BbModelParser.parse(JsonParser.parseString(Files.readString(Path.of(
                "examples/components/wheel-bbmodel/assets/examplepack/models/entity/automobile/wheel/example_buggy.bbmodel"
        ))).getAsJsonObject());

        assertEquals(3, ((BbModelData.GroupNode) frame.roots().get(0)).children().size());
        assertEquals(1, wheel.roots().size());
        assertEquals(44.0F, BbModelBounds.maxDimensionPx(frame), 0.001F);
    }

    @Test
    void modelBoundsApplyHierarchyScaleAndIgnoreHiddenGeometry() {
        BbModelData.CubeFace face = new BbModelData.CubeFace(
                new float[]{0, 0, 1, 1}, 0, BbModelData.TextureReference.none(), true);
        BbModelData.ElementNode visible = new BbModelData.ElementNode(
                "visible", "Visible", new Vector3f(), new Vector3f(), new Vector3f(1, 1, 1), true,
                new BbModelData.Cube(
                        new Vector3f(-5, 0, -20), new Vector3f(5, 8, 20),
                        0.0F, false, Map.of("north", face, "south", face)));
        BbModelData.ElementNode hidden = new BbModelData.ElementNode(
                "hidden", "Hidden", new Vector3f(), new Vector3f(), new Vector3f(1, 1, 1), false,
                new BbModelData.Cube(
                        new Vector3f(-100, -100, -100), new Vector3f(100, 100, 100),
                        0.0F, false, Map.of("north", face)));
        BbModelData.GroupNode root = new BbModelData.GroupNode(
                "root", "Root", new Vector3f(), new Vector3f(), new Vector3f(1, 1, 1.5F), true,
                List.of(visible, hidden));
        BbModelData.Document document = new BbModelData.Document(
                "5.0", "modded_entity", 16, 16, List.of(), List.of(root), List.of());

        BbModelBounds.Measurement measurement = BbModelBounds.measure(document);
        assertEquals(60.0F, measurement.size().maxDimensionPx(), 0.001F);
        assertEquals(measurement.frameItemProjectedSpanPx() * 0.44F / 0.77F,
                measurement.frameItemLengthPx(), 0.001F);
        assertTrue(measurement.frameItemLengthPx() < measurement.size().maxDimensionPx());
    }

    @Test
    void modelBoundsOnlyIncludeRenderedCubeFaces() {
        BbModelData.CubeFace face = new BbModelData.CubeFace(
                new float[]{0, 0, 1, 1}, 0, BbModelData.TextureReference.none(), true);
        BbModelData.ElementNode surface = new BbModelData.ElementNode(
                "surface", "Surface", new Vector3f(), new Vector3f(), new Vector3f(1, 1, 1), true,
                new BbModelData.Cube(
                        new Vector3f(-5, 0, -100), new Vector3f(5, 8, 100),
                        0.0F, false, Map.of("north", face)));
        BbModelData.Document document = new BbModelData.Document(
                "5.0", "modded_entity", 16, 16, List.of(), List.of(surface), List.of());

        assertEquals(10.0F, BbModelBounds.maxDimensionPx(document), 0.001F);
    }

    @Test
    void customModelRefreshRetainsTemporaryPreviewModels() {
        FrameSpec.ModelSpec persistent = FrameSpec.ModelSpec.fromJson(JsonParser.parseString("""
                {"type":"bbmodel","model_id":"test:persistent","bbmodel":"test:models/persistent.bbmodel"}
                """).getAsJsonObject());
        FrameSpec.ModelSpec preview = FrameSpec.ModelSpec.fromJson(JsonParser.parseString("""
                {"type":"bbmodel","model_id":"test:preview","bbmodel":"test:models/preview.bbmodel"}
                """).getAsJsonObject());
        BbModelRepository.register(persistent);
        BbModelRepository.registerTemporary(preview);
        try {
            BbModelRepository.retain(Set.of(persistent.bbModel()));

            assertTrue(BbModelRepository.isRegistered(persistent));
            assertTrue(BbModelRepository.isRegistered(preview));
        } finally {
            BbModelRepository.unregister(persistent);
            BbModelRepository.unregisterTemporary(preview);
        }
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
                new JsonPrimitive("example:models/vehicles/car.bbmodel"),
                new ResourceLocation("example", "car"),
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
                new ResourceLocation("example", "sports/red_car"),
                "wheel"
        );

        assertEquals("example:models/entity/automobile/wheel/sports/red_car.bbmodel", spec.bbModel().toString());
        assertEquals("example:riautomobility/wheel/sports/red_car", spec.modelId().toString());
        assertEquals("", spec.bbAnimation());
        assertTrue(spec.textureOverrides().isEmpty());
    }

    @Test
    void parsesShorthandModelsInShippedComponentDefinitions() throws IOException {
        ResourceLocation id = new ResourceLocation("examplepack", "example_buggy_bbmodel");
        FrameSpec frame = FrameSpec.fromJson(id, JsonParser.parseString(Files.readString(Path.of(
                "examples/components/frame-bbmodel/data/examplepack/riautomobility/frames/example_buggy_bbmodel.json"
        ))).getAsJsonObject());
        WheelSpec wheel = WheelSpec.fromJson(
                new ResourceLocation("examplepack", "example_buggy_bbmodel_wheel"),
                JsonParser.parseString(Files.readString(Path.of(
                        "examples/components/wheel-bbmodel/data/examplepack/riautomobility/wheels/example_buggy_bbmodel_wheel.json"
                ))).getAsJsonObject()
        );

        assertEquals("examplepack:models/entity/automobile/frame/example_buggy.bbmodel", frame.model().bbModel().toString());
        assertEquals("examplepack:riautomobility/frame/example_buggy_bbmodel", frame.model().modelId().toString());
        assertEquals("examplepack:models/entity/automobile/wheel/example_buggy.bbmodel", wheel.model().bbModel().toString());
        assertEquals("examplepack:riautomobility/wheel/example_buggy_bbmodel_wheel", wheel.model().modelId().toString());
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
    void reusesAnimationSampleStorage() {
        BbModelData.Document document = BbModelParser.parse(JsonParser.parseString("""
                {
                  "meta":{"format_version":"5.0","model_format":"modded_entity"},
                  "elements":[],
                  "animations":[{"name":"cached","animators":{"bone":{"type":"bone","keyframes":[
                    {"channel":"position","time":0,"data_points":[{"x":1,"y":2,"z":3}]}
                  ]}}}]
                }
                """).getAsJsonObject());

        Map<String, BbAnimationPlayer.Transform> first = BbAnimationPlayer.sample(document, "cached", null);
        Map<String, BbAnimationPlayer.Transform> second = BbAnimationPlayer.sample(document, "cached", null);

        assertSame(first, second);
        assertSame(first.get("bone"), second.get("bone"));
        assertSame(first.get("bone").position(), second.get("bone").position());
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
                List.of(
                        new BbModelData.DataPoint(new JsonPrimitive(1), new JsonPrimitive(0), new JsonPrimitive(0)),
                        new BbModelData.DataPoint(new JsonPrimitive(2), new JsonPrimitive(0), new JsonPrimitive(0))
                ), new float[0], new float[0], new float[0], new float[0]
        );
        BbModelData.Keyframe after = keyframe(1.0F, new float[0], new float[0], new float[0], new float[0]);
        Vector3f value = BbAnimationPlayer.sampleChannel(List.of(before, after), 0.5F, new Vector3f());

        assertEquals(2.0F, value.x, 0.0001F);
    }

    @Test
    void convertsBlockbenchCoordinatesToAutomobilityModelSpace() {
        Vector3f position = BbCoordinateSystem.position(new Vector3f(2, 5, 7));
        Vector3f rotation = BbCoordinateSystem.rotation(new Vector3f(10, 20, 30));

        assertEquals(new Vector3f(2, -5, 7), position);
        assertEquals(new Vector3f(-10, 20, -30), rotation);
    }

    @Test
    void coordinateReflectionPreservesFaceWindingAndUvPairing() {
        Vector3f[] vertices = {
                new Vector3f(0, 0, 0),
                new Vector3f(1, 0, 0),
                new Vector3f(1, 1, 0),
                new Vector3f(0, 1, 0)
        };
        Vector2f[] uvs = {
                new Vector2f(0, 0),
                new Vector2f(1, 0),
                new Vector2f(1, 1),
                new Vector2f(0, 1)
        };

        BbCoordinateSystem.ConvertedQuad converted = BbCoordinateSystem.quad(vertices, uvs);
        Vector3f normal = new Vector3f(converted.vertices()[1])
                .sub(converted.vertices()[0])
                .cross(new Vector3f(converted.vertices()[2]).sub(converted.vertices()[0]));

        assertTrue(normal.z > 0);
        assertEquals(new Vector3f(1, -1, 0), converted.vertices()[0]);
        assertEquals(new Vector2f(1, 1), converted.uvs()[0]);
    }

    @Test
    void coordinateReflectionKeepsDegenerateTriangleNormalValid() {
        Vector3f[] vertices = {
                new Vector3f(0, 0, 0),
                new Vector3f(1, 0, 0),
                new Vector3f(0, 1, 0),
                new Vector3f(0, 1, 0)
        };
        Vector2f[] uvs = {
                new Vector2f(0, 0),
                new Vector2f(1, 0),
                new Vector2f(0, 1),
                new Vector2f(0, 1)
        };

        BbCoordinateSystem.ConvertedQuad converted = BbCoordinateSystem.quad(vertices, uvs);
        Vector3f normal = new Vector3f(converted.vertices()[1])
                .sub(converted.vertices()[0])
                .cross(new Vector3f(converted.vertices()[2]).sub(converted.vertices()[0]));

        assertTrue(normal.lengthSquared() > 0);
        assertTrue(normal.z > 0);
    }

    private static BbModelData.Keyframe keyframe(float time, float[] rightTime, float[] rightValue, float[] leftTime, float[] leftValue) {
        return new BbModelData.Keyframe(
                "position", time, "bezier",
                List.of(new BbModelData.DataPoint(new JsonPrimitive(0), new JsonPrimitive(0), new JsonPrimitive(0))),
                leftTime, leftValue, rightTime, rightValue
        );
    }

}
