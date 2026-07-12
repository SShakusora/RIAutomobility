package com.sshakusora.riautomobility.model.bbmodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.*;

public final class BbModelParser {
    public static final String MIN_VERSION = "4.10";
    public static final String MAX_VERSION = "5.0";

    static {
        BbElementDecoderRegistry.register("cube", BbModelParser::decodeCube);
        BbElementDecoderRegistry.register("mesh", BbModelParser::decodeMesh);
        BbElementDecoderRegistry.register("texture_mesh", BbModelParser::decodeTextureMesh);
        BbElementDecoderRegistry.register("locator", BbModelParser::decodeEmpty);
        BbElementDecoderRegistry.register("null_object", BbModelParser::decodeEmpty);
        BbElementDecoderRegistry.register("bounding_box", BbModelParser::decodeEmpty);
    }

    private BbModelParser() {}

    public static BbModelData.Document parse(JsonObject source) {
        if (!source.has("meta")) {
            throw new BbModelFormatException("Blockbench project is missing meta");
        }

        JsonObject model = source.deepCopy();
        JsonObject meta = GsonHelper.getAsJsonObject(model, "meta");
        String version = GsonHelper.getAsString(meta, "format_version", GsonHelper.getAsString(meta, "format", ""));
        if (version.isBlank()) {
            throw new BbModelFormatException("Blockbench project is missing meta.format_version");
        }
        if (compareVersions(version, MIN_VERSION) < 0) {
            throw new BbModelFormatException("Blockbench project version " + version + " is older than supported version " + MIN_VERSION + "; resave it with a current Blockbench release");
        }
        if (compareVersions(version, MAX_VERSION) > 0) {
            throw new BbModelFormatException("Blockbench project version " + version + " is newer than supported version " + MAX_VERSION);
        }

        String modelFormat = GsonHelper.getAsString(meta, "model_format", GsonHelper.getAsBoolean(meta, "bone_rig", false) ? "bedrock_old" : "java_block");
        model = BbFormatAdapterRegistry.adapt(modelFormat, model);
        if (model.has("cubes") && !model.has("elements")) {
            model.add("elements", model.get("cubes"));
        }
        migrateCompatibility(model, version);

        JsonObject resolution = model.has("resolution") ? GsonHelper.getAsJsonObject(model, "resolution") : new JsonObject();
        int textureWidth = GsonHelper.getAsInt(resolution, "width", 16);
        int textureHeight = GsonHelper.getAsInt(resolution, "height", 16);
        if (textureWidth <= 0 || textureHeight <= 0) {
            throw new BbModelFormatException("Blockbench texture resolution must be positive");
        }

        List<BbModelData.Texture> textures = parseTextures(model, textureWidth, textureHeight);
        Context context = new Context(textureWidth, textureHeight, textures);
        Map<String, BbModelData.ElementNode> elements = parseElements(model, context);
        Map<String, JsonObject> groups = parseGroups(model);
        List<BbModelData.Node> roots = parseOutliner(model, elements, groups);
        return new BbModelData.Document(version, modelFormat, textureWidth, textureHeight, List.copyOf(textures), roots, parseAnimations(model));
    }

    private static List<BbModelData.Texture> parseTextures(JsonObject model, int defaultWidth, int defaultHeight) {
        List<BbModelData.Texture> textures = new ArrayList<>();
        if (!model.has("textures")) {
            return textures;
        }
        int index = 0;
        for (JsonElement entry : GsonHelper.getAsJsonArray(model, "textures")) {
            JsonObject texture = entry.getAsJsonObject();
            textures.add(new BbModelData.Texture(
                    index++,
                    GsonHelper.getAsString(texture, "uuid", ""),
                    GsonHelper.getAsString(texture, "id", ""),
                    GsonHelper.getAsString(texture, "name", "texture"),
                    GsonHelper.getAsString(texture, "relative_path", ""),
                    GsonHelper.getAsString(texture, "path", ""),
                    GsonHelper.getAsString(texture, "source", ""),
                    GsonHelper.getAsString(texture, "render_mode", "default"),
                    GsonHelper.getAsBoolean(texture, "use_as_default", false),
                    positiveInt(texture, "uv_width", defaultWidth),
                    positiveInt(texture, "uv_height", defaultHeight)
            ));
        }
        return textures;
    }

    private static int positiveInt(JsonObject object, String key, int fallback) {
        int value = GsonHelper.getAsInt(object, key, fallback);
        return value > 0 ? value : fallback;
    }

    private static Map<String, BbModelData.ElementNode> parseElements(JsonObject model, Context context) {
        Map<String, BbModelData.ElementNode> elements = new LinkedHashMap<>();
        if (!model.has("elements")) {
            return elements;
        }
        for (JsonElement entry : GsonHelper.getAsJsonArray(model, "elements")) {
            JsonObject element = entry.getAsJsonObject();
            String uuid = requiredString(element, "uuid", "Blockbench element");
            String type = GsonHelper.getAsString(element, "type", "cube");
            BbElementDecoderRegistry.Decoder decoder = BbElementDecoderRegistry.get(type);
            if (decoder == null) {
                throw new BbModelFormatException("Unsupported Blockbench element type '" + type + "' on " + GsonHelper.getAsString(element, "name", uuid));
            }
            BbModelData.ElementNode previous = elements.put(uuid, new BbModelData.ElementNode(
                    uuid,
                    GsonHelper.getAsString(element, "name", type),
                    vector3(element, "origin"),
                    vector3(element, "rotation"),
                    vector3(element, "scale", new Vector3f(1, 1, 1)),
                    GsonHelper.getAsBoolean(element, "visibility", true),
                    decoder.decode(element, context)
            ));
            if (previous != null) {
                throw new BbModelFormatException("Duplicate Blockbench element UUID " + uuid);
            }
        }
        return elements;
    }

    private static Map<String, JsonObject> parseGroups(JsonObject model) {
        Map<String, JsonObject> groups = new LinkedHashMap<>();
        if (!model.has("groups")) {
            return groups;
        }
        for (JsonElement entry : GsonHelper.getAsJsonArray(model, "groups")) {
            JsonObject group = entry.getAsJsonObject();
            groups.put(requiredString(group, "uuid", "Blockbench group"), group);
        }
        return groups;
    }

    private static List<BbModelData.Node> parseOutliner(JsonObject model, Map<String, BbModelData.ElementNode> elements, Map<String, JsonObject> groupDefinitions) {
        List<BbModelData.Node> roots = new ArrayList<>();
        Set<String> usedElements = new HashSet<>();
        Set<String> groupStack = new HashSet<>();
        if (model.has("outliner")) {
            for (JsonElement entry : GsonHelper.getAsJsonArray(model, "outliner")) {
                BbModelData.Node node = parseOutlinerEntry(entry, elements, groupDefinitions, usedElements, groupStack);
                if (node != null) {
                    roots.add(node);
                }
            }
        }
        for (BbModelData.ElementNode element : elements.values()) {
            if (usedElements.add(element.uuid())) {
                roots.add(element);
            }
        }
        return List.copyOf(roots);
    }

    private static BbModelData.Node parseOutlinerEntry(JsonElement entry, Map<String, BbModelData.ElementNode> elements, Map<String, JsonObject> groupDefinitions, Set<String> usedElements, Set<String> groupStack) {
        if (entry.isJsonPrimitive()) {
            String uuid = entry.getAsString();
            BbModelData.ElementNode element = elements.get(uuid);
            if (element != null) {
                usedElements.add(uuid);
                return element;
            }
            JsonObject group = groupDefinitions.get(uuid);
            return group == null ? null : parseGroup(group, elements, groupDefinitions, usedElements, groupStack);
        }
        if (!entry.isJsonObject()) {
            return null;
        }
        JsonObject inline = entry.getAsJsonObject();
        String uuid = GsonHelper.getAsString(inline, "uuid", "");
        JsonObject definition = groupDefinitions.get(uuid);
        JsonObject merged = definition == null ? inline.deepCopy() : definition.deepCopy();
        for (Map.Entry<String, JsonElement> property : inline.entrySet()) {
            merged.add(property.getKey(), property.getValue());
        }
        return parseGroup(merged, elements, groupDefinitions, usedElements, groupStack);
    }

    private static BbModelData.GroupNode parseGroup(JsonObject group, Map<String, BbModelData.ElementNode> elements, Map<String, JsonObject> groupDefinitions, Set<String> usedElements, Set<String> groupStack) {
        String uuid = requiredString(group, "uuid", "Blockbench group");
        if (!groupStack.add(uuid)) {
            throw new BbModelFormatException("Cyclic Blockbench group hierarchy at " + uuid);
        }
        List<BbModelData.Node> children = new ArrayList<>();
        if (group.has("children")) {
            for (JsonElement child : GsonHelper.getAsJsonArray(group, "children")) {
                BbModelData.Node node = parseOutlinerEntry(child, elements, groupDefinitions, usedElements, groupStack);
                if (node != null) {
                    children.add(node);
                }
            }
        }
        groupStack.remove(uuid);
        return new BbModelData.GroupNode(
                uuid,
                GsonHelper.getAsString(group, "name", "group"),
                vector3(group, "origin"),
                vector3(group, "rotation"),
                vector3(group, "scale", new Vector3f(1, 1, 1)),
                GsonHelper.getAsBoolean(group, "visibility", true),
                List.copyOf(children)
        );
    }

    private static BbModelData.Geometry decodeCube(JsonObject element, Context context) {
        Map<String, BbModelData.CubeFace> faces = new LinkedHashMap<>();
        JsonObject faceObject = element.has("faces") ? GsonHelper.getAsJsonObject(element, "faces") : new JsonObject();
        for (String direction : List.of("north", "south", "east", "west", "up", "down")) {
            if (!faceObject.has(direction)) {
                continue;
            }
            JsonObject face = GsonHelper.getAsJsonObject(faceObject, direction);
            BbModelData.TextureReference texture = textureReference(face.get("texture"));
            faces.put(direction, new BbModelData.CubeFace(
                    floatArray(face.get("uv"), 4, new float[]{0, 0, context.textureWidth, context.textureHeight}),
                    GsonHelper.getAsInt(face, "rotation", 0),
                    texture,
                    GsonHelper.getAsBoolean(face, "enabled", true) && !texture.disabled()
            ));
        }
        return new BbModelData.Cube(
                vector3Required(element, "from"),
                vector3Required(element, "to"),
                GsonHelper.getAsFloat(element, "inflate", 0.0F),
                GsonHelper.getAsBoolean(element, "mirror_uv", false),
                Map.copyOf(faces)
        );
    }

    private static BbModelData.Geometry decodeMesh(JsonObject element, Context context) {
        Map<String, Vector3f> vertices = new LinkedHashMap<>();
        JsonObject vertexObject = GsonHelper.getAsJsonObject(element, "vertices");
        vertexObject.entrySet().forEach(entry -> vertices.put(entry.getKey(), vector3(entry.getValue())));
        List<BbModelData.MeshFace> faces = new ArrayList<>();
        JsonObject faceObject = GsonHelper.getAsJsonObject(element, "faces");
        for (Map.Entry<String, JsonElement> entry : faceObject.entrySet()) {
            JsonObject face = entry.getValue().getAsJsonObject();
            List<String> vertexIds = stringList(GsonHelper.getAsJsonArray(face, "vertices"));
            Map<String, Vector2f> uvs = new LinkedHashMap<>();
            if (face.has("uv")) {
                GsonHelper.getAsJsonObject(face, "uv").entrySet().forEach(uv -> uvs.put(uv.getKey(), vector2(uv.getValue())));
            }
            faces.add(new BbModelData.MeshFace(vertexIds, Map.copyOf(uvs), textureReference(face.get("texture"))));
        }
        return new BbModelData.Mesh(Map.copyOf(vertices), List.copyOf(faces));
    }

    private static BbModelData.Geometry decodeTextureMesh(JsonObject element, Context context) {
        return new BbModelData.TextureMesh(
                GsonHelper.getAsString(element, "texture_name", ""),
                vector3(element, "local_pivot")
        );
    }

    private static BbModelData.Geometry decodeEmpty(JsonObject element, Context context) {
        return new BbModelData.EmptyGeometry(GsonHelper.getAsString(element, "type", "unknown"), element.deepCopy());
    }

    private static List<BbModelData.Animation> parseAnimations(JsonObject model) {
        List<BbModelData.Animation> animations = new ArrayList<>();
        if (!model.has("animations")) {
            return animations;
        }
        for (JsonElement entry : GsonHelper.getAsJsonArray(model, "animations")) {
            JsonObject animation = entry.getAsJsonObject();
            Map<String, BbModelData.Animator> animators = new LinkedHashMap<>();
            if (animation.has("animators")) {
                for (Map.Entry<String, JsonElement> animatorEntry : GsonHelper.getAsJsonObject(animation, "animators").entrySet()) {
                    JsonObject animator = animatorEntry.getValue().getAsJsonObject();
                    List<BbModelData.Keyframe> keyframes = new ArrayList<>();
                    if (animator.has("keyframes")) {
                        for (JsonElement keyframeEntry : GsonHelper.getAsJsonArray(animator, "keyframes")) {
                            JsonObject keyframe = keyframeEntry.getAsJsonObject();
                            List<BbModelData.DataPoint> points = new ArrayList<>();
                            if (keyframe.has("data_points")) {
                                for (JsonElement pointEntry : GsonHelper.getAsJsonArray(keyframe, "data_points")) {
                                    JsonObject point = pointEntry.getAsJsonObject();
                                    points.add(new BbModelData.DataPoint(value(point, "x"), value(point, "y"), value(point, "z")));
                                }
                            }
                            if (points.isEmpty()) {
                                points.add(new BbModelData.DataPoint(value(keyframe, "x"), value(keyframe, "y"), value(keyframe, "z")));
                            }
                            keyframes.add(new BbModelData.Keyframe(
                                    GsonHelper.getAsString(keyframe, "channel", "rotation"),
                                    GsonHelper.getAsFloat(keyframe, "time", 0.0F),
                                    GsonHelper.getAsString(keyframe, "interpolation", "linear"),
                                    List.copyOf(points),
                                    optionalFloatArray(keyframe, "bezier_left_time"),
                                    optionalFloatArray(keyframe, "bezier_left_value"),
                                    optionalFloatArray(keyframe, "bezier_right_time"),
                                    optionalFloatArray(keyframe, "bezier_right_value")
                            ));
                        }
                    }
                    animators.put(animatorEntry.getKey(), new BbModelData.Animator(
                            animatorEntry.getKey(),
                            GsonHelper.getAsString(animator, "name", ""),
                            GsonHelper.getAsString(animator, "type", "bone"),
                            List.copyOf(keyframes)
                    ));
                }
            }
            animations.add(new BbModelData.Animation(
                    GsonHelper.getAsString(animation, "uuid", ""),
                    GsonHelper.getAsString(animation, "name", "animation"),
                    GsonHelper.getAsFloat(animation, "length", 0.0F),
                    GsonHelper.getAsString(animation, "loop", "once"),
                    Map.copyOf(animators)
            ));
        }
        return List.copyOf(animations);
    }

    private static void migrateCompatibility(JsonObject model, String version) {
        if (compareVersions(version, "5.0") >= 0 || !model.has("animations")) {
            return;
        }
        for (JsonElement animationEntry : GsonHelper.getAsJsonArray(model, "animations")) {
            JsonObject animation = animationEntry.getAsJsonObject();
            if (!animation.has("animators")) {
                continue;
            }
            for (JsonElement animatorEntry : GsonHelper.getAsJsonObject(animation, "animators").asMap().values()) {
                JsonObject animator = animatorEntry.getAsJsonObject();
                if (!animator.has("keyframes")) {
                    continue;
                }
                for (JsonElement keyframeEntry : GsonHelper.getAsJsonArray(animator, "keyframes")) {
                    JsonObject keyframe = keyframeEntry.getAsJsonObject();
                    String channel = GsonHelper.getAsString(keyframe, "channel", "");
                    if (!"position".equals(channel) && !"rotation".equals(channel)) {
                        continue;
                    }
                    if (keyframe.has("data_points")) {
                        for (JsonElement pointEntry : GsonHelper.getAsJsonArray(keyframe, "data_points")) {
                            invert(pointEntry.getAsJsonObject(), "x");
                            if ("rotation".equals(channel)) {
                                invert(pointEntry.getAsJsonObject(), "y");
                            }
                        }
                    }
                    invertHandle(keyframe, "bezier_left_value", "rotation".equals(channel));
                    invertHandle(keyframe, "bezier_right_value", "rotation".equals(channel));
                }
            }
        }
    }

    private static void invert(JsonObject object, String key) {
        if (!object.has(key)) {
            return;
        }
        JsonElement value = object.get(key);
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            object.addProperty(key, -value.getAsDouble());
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            object.addProperty(key, "-(" + value.getAsString() + ")");
        }
    }

    private static void invertHandle(JsonObject keyframe, String key, boolean invertY) {
        if (!keyframe.has(key) || !keyframe.get(key).isJsonArray()) {
            return;
        }
        JsonArray values = keyframe.getAsJsonArray(key);
        if (!values.isEmpty()) {
            values.set(0, new com.google.gson.JsonPrimitive(-values.get(0).getAsFloat()));
        }
        if (invertY && values.size() > 1) {
            values.set(1, new com.google.gson.JsonPrimitive(-values.get(1).getAsFloat()));
        }
    }

    private static JsonElement value(JsonObject object, String key) {
        return object.has(key) ? object.get(key).deepCopy() : new com.google.gson.JsonPrimitive(0);
    }

    private static float[] optionalFloatArray(JsonObject object, String key) {
        return object.has(key) ? floatArray(object.get(key), -1, new float[0]) : new float[0];
    }

    private static BbModelData.TextureReference textureReference(JsonElement element) {
        if (element == null || element.isJsonNull() || element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean() && !element.getAsBoolean()) {
            return BbModelData.TextureReference.disabledReference();
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return new BbModelData.TextureReference(element.getAsInt(), null, false);
        }
        return new BbModelData.TextureReference(null, element.getAsString(), false);
    }

    private static Vector3f vector3(JsonObject object, String key) {
        return object.has(key) ? vector3(object.get(key)) : new Vector3f();
    }

    private static Vector3f vector3(JsonObject object, String key, Vector3f fallback) {
        return object.has(key) ? vector3(object.get(key)) : new Vector3f(fallback);
    }

    private static Vector3f vector3Required(JsonObject object, String key) {
        if (!object.has(key)) {
            throw new BbModelFormatException("Blockbench element is missing " + key);
        }
        return vector3(object.get(key));
    }

    private static Vector3f vector3(JsonElement element) {
        float[] values = floatArray(element, 3, new float[]{0, 0, 0});
        return new Vector3f(values[0], values[1], values[2]);
    }

    private static Vector2f vector2(JsonElement element) {
        float[] values = floatArray(element, 2, new float[]{0, 0});
        return new Vector2f(values[0], values[1]);
    }

    private static float[] floatArray(JsonElement element, int expected, float[] fallback) {
        if (element == null || !element.isJsonArray()) {
            return fallback.clone();
        }
        JsonArray array = element.getAsJsonArray();
        int length = expected < 0 ? array.size() : expected;
        if (expected >= 0 && array.size() < expected) {
            return fallback.clone();
        }
        float[] values = new float[length];
        for (int index = 0; index < length; index++) {
            values[index] = array.get(index).getAsFloat();
        }
        return values;
    }

    private static List<String> stringList(JsonArray array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.getAsString()));
        return List.copyOf(values);
    }

    private static String requiredString(JsonObject object, String key, String owner) {
        String value = GsonHelper.getAsString(object, key, "");
        if (value.isBlank()) {
            throw new BbModelFormatException(owner + " is missing " + key);
        }
        return value;
    }

    static int compareVersions(String left, String right) {
        String[] a = left.split("\\.");
        String[] b = right.split("\\.");
        int length = Math.max(a.length, b.length);
        for (int index = 0; index < length; index++) {
            int av = index < a.length ? parseVersionPart(a[index]) : 0;
            int bv = index < b.length ? parseVersionPart(b[index]) : 0;
            if (av != bv) {
                return Integer.compare(av, bv);
            }
        }
        return 0;
    }

    private static int parseVersionPart(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9].*$", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public record Context(int textureWidth, int textureHeight, List<BbModelData.Texture> textures) {}
}
