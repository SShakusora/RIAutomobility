package com.sshakusora.riautomobility.model.bbmodel;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.sshakusora.riautomobility.content.FrameSpec;
import io.github.foundationgames.automobility.automobile.render.RenderableAutomobile;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class BbModelParserTest {
    @Test
    void parsesConvertedBuiltinVehicleModels() throws IOException {
        Map<String, BuiltinModelExpectation> models = Map.of(
                "dmc12_frame.bbmodel", new BuiltinModelExpectation(501,
                        "riautomobility:textures/entity/automobile/frame/dmc12.png"),
                "standard_formula_frame.bbmodel", new BuiltinModelExpectation(211,
                        "riautomobility:textures/entity/automobile/frame/standard_formula.png"),
                "lorry_frame.bbmodel", new BuiltinModelExpectation(204,
                        "riautomobility:textures/entity/automobile/frame/lorry.png"),
                "dmc12_wheel.bbmodel", new BuiltinModelExpectation(82,
                        "riautomobility:textures/entity/automobile/wheel/dmc12.png"),
                "standard_formula_wheel.bbmodel", new BuiltinModelExpectation(43,
                        "riautomobility:textures/entity/automobile/wheel/standard_formula.png")
        );

        for (Map.Entry<String, BuiltinModelExpectation> entry : models.entrySet()) {
            BbModelData.Document document = parseBuiltinModel(entry.getKey());
            BuiltinModelExpectation expected = entry.getValue();

            assertFalse(document.roots().isEmpty(), entry.getKey());
            assertEquals(expected.elements(), countElements(document.roots()), entry.getKey());
            assertEquals(1, document.textures().size(), entry.getKey());
            BbModelData.Texture texture = document.textures().get(0);
            assertTrue(expected.texture().equals(texture.relativePath())
                            || texture.source().startsWith("data:image/png;base64,"),
                    entry.getKey());
            float maxDimension = BbModelBounds.maxDimensionPx(document);
            assertTrue(Float.isFinite(maxDimension) && maxDimension > 0.0F, entry.getKey());
        }
    }

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
    void rejectsNonEditorModelDefinitions() {
        assertThrows(IllegalArgumentException.class,
                () -> FrameSpec.ModelSpec.fromComponentJson(null));
        assertThrows(IllegalArgumentException.class,
                () -> FrameSpec.ModelSpec.fromComponentJson(
                        new JsonPrimitive("example:models/car.bbmodel")));
        assertThrows(IllegalArgumentException.class,
                () -> FrameSpec.ModelSpec.fromComponentJson(JsonParser.parseString("""
                        {"type":"jsonem","model_id":"example:car","texture":"example:textures/car.png"}
                        """)));
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
    void onlyTreatsSelectedAnimationsWithKeyframesAsDynamic() {
        BbModelData.Document effective = animationDocument("1");
        BbModelData.Document empty = BbModelParser.parse(JsonParser.parseString("""
                {
                  "meta":{"format_version":"5.0","model_format":"modded_entity"},
                  "elements":[],
                  "animations":[{"name":"empty","animators":{"bone":{"type":"bone","keyframes":[]}}}]
                }
                """).getAsJsonObject());

        assertTrue(BbAnimationPlayer.hasEffectiveAnimation(effective, "cached"));
        assertFalse(BbAnimationPlayer.hasEffectiveAnimation(effective, "missing"));
        assertFalse(BbAnimationPlayer.hasEffectiveAnimation(empty, "empty"));
        assertFalse(BbAnimationPlayer.hasEffectiveAnimation(new BbModelData.Document(
                "5.0", "modded_entity", 16, 16, List.of(), List.of(), List.of()), ""));
    }

    @Test
    void clearCacheReleasesCompiledMolangExpressions() throws ReflectiveOperationException {
        BbAnimationPlayer.clearCache();
        BbModelData.Document document = animationDocument("math.sin(90)");

        BbAnimationPlayer.sample(document, "cached", null);

        assertFalse(expressionCache().isEmpty());
        BbAnimationPlayer.clearCache();
        assertTrue(expressionCache().isEmpty());
    }

    @Test
    void animationSamplingDoesNotRetainAutomobileQueryContext() throws ReflectiveOperationException {
        RenderableAutomobile automobile = (RenderableAutomobile) Proxy.newProxyInstance(
                RenderableAutomobile.class.getClassLoader(),
                new Class<?>[]{RenderableAutomobile.class},
                (proxy, method, arguments) -> defaultValue(method.getReturnType()));
        BbRenderContext.begin(null, automobile, 0.0F);
        try {
            BbAnimationPlayer.sample(animationDocument("query.life_time"), "cached", BbRenderContext.current());
        } finally {
            BbRenderContext.end();
        }

        Field queryStateField = BbAnimationPlayer.class.getDeclaredField("QUERY_STATE");
        queryStateField.setAccessible(true);
        Object queryState = ((ThreadLocal<?>) queryStateField.get(null)).get();
        Field automobileField = queryState.getClass().getDeclaredField("automobile");
        automobileField.setAccessible(true);
        assertNull(automobileField.get(queryState));
    }

    @Test
    void evaluatesMolangQueriesOperatorsFunctionsAndConditionals() {
        MolangExpression.Expression expression = MolangExpression.compile(
                "query.anim_time > 1 ? math.lerp(2, math.clamp(10, 0, 6), 0.5) + 2 * 3 : 0");

        assertEquals(10.0D, expression.evaluate(name -> name.equals("query.anim_time") ? 2.0D : 0.0D), 0.0001D);
        assertEquals(0.0D, expression.evaluate(name -> 0.5D), 0.0001D);
    }

    private static BbModelData.Document animationDocument(String expression) {
        return BbModelParser.parse(JsonParser.parseString("""
                {
                  "meta":{"format_version":"5.0","model_format":"modded_entity"},
                  "elements":[],
                  "animations":[{"name":"cached","animators":{"bone":{"type":"bone","keyframes":[
                    {"channel":"position","time":0,"data_points":[{"x":"%s","y":0,"z":0}]}
                  ]}}}]
                }
                """.formatted(expression)).getAsJsonObject());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, MolangExpression.Expression> expressionCache() throws ReflectiveOperationException {
        Field field = BbAnimationPlayer.class.getDeclaredField("EXPRESSIONS");
        field.setAccessible(true);
        return (Map<String, MolangExpression.Expression>) field.get(null);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0F;
        if (type == double.class) return 0.0D;
        if (type == char.class) return '\0';
        return null;
    }

    @Test
    void rejectsInvalidMolangExpressions() {
        assertThrows(IllegalArgumentException.class, () -> MolangExpression.compile("math.unknown(1)"));
        assertThrows(IllegalArgumentException.class, () -> MolangExpression.compile("1 +"));
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

        assertEquals(new Vector3f(-2, -5, 7), position);
        assertEquals(new Vector3f(-10, -20, 30), rotation);
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
        assertEquals(0.0F, converted.vertices()[0].x, 0.0F);
        assertEquals(0.0F, converted.vertices()[0].y, 0.0F);
        assertEquals(0.0F, converted.vertices()[0].z, 0.0F);
        assertEquals(new Vector2f(0, 0), converted.uvs()[0]);
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

    private static BbModelData.Document parseBuiltinModel(String name) throws IOException {
        String path = "assets/riautomobility/models/entity/automobile/builtin/" + name;
        try (InputStream stream = BbModelParserTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return BbModelParser.parse(JsonParser.parseReader(new InputStreamReader(stream, UTF_8)).getAsJsonObject());
        }
    }

    private static int countElements(List<BbModelData.Node> nodes) {
        int count = 0;
        for (BbModelData.Node node : nodes) {
            if (node instanceof BbModelData.ElementNode) count++;
            if (node instanceof BbModelData.GroupNode group) count += countElements(group.children());
        }
        return count;
    }

    private record BuiltinModelExpectation(int elements, String texture) {}

}
