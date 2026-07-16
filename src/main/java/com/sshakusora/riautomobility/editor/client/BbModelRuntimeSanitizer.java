package com.sshakusora.riautomobility.editor.client;

import com.google.gson.*;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

final class BbModelRuntimeSanitizer {
    private static final Gson GSON = new Gson();
    private static final String EMBEDDED_PNG_PREFIX = "data:image/png;base64,";
    private static final long MAX_TEXTURE_PIXELS = 16L * 1024L * 1024L;
    private static final Set<String> EDITOR_ONLY_ROOT_FIELDS = Set.of(
            "reference_images",
            "backgrounds",
            "editor_state",
            "history",
            "history_index",
            "export_options",
            "collections",
            "texture_groups"
    );
    private static final Set<String> EDITOR_ONLY_ELEMENT_FIELDS = Set.of(
            "allow_mirror_modeling",
            "autouv",
            "box_uv",
            "color",
            "export",
            "locked",
            "render_order",
            "scope"
    );
    private static final Set<String> EDITOR_ONLY_GROUP_FIELDS = Set.of(
            "_static",
            "autouv",
            "bedrock_binding",
            "color",
            "export",
            "isOpen",
            "locked",
            "mirror_uv",
            "primary_selected",
            "reset",
            "scope",
            "selected",
            "shade"
    );
    private static final Set<String> EDITOR_ONLY_TEXTURE_FIELDS = Set.of(
            "file_format",
            "folder",
            "fps",
            "frame_interpolate",
            "frame_order",
            "frame_order_type",
            "frame_time",
            "group",
            "height",
            "layers_enabled",
            "namespace",
            "particle",
            "pbr_channel",
            "render_sides",
            "saved",
            "scope",
            "sync_to_project",
            "visible",
            "width",
            "wrap_mode"
    );

    private BbModelRuntimeSanitizer() {
    }

    static ExportedModel externalize(byte[] source, String namespace, String textureBasePath) throws IOException {
        JsonObject project;
        try {
            project = JsonParser.parseString(new String(source, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Model is not a valid JSON object", exception);
        }

        JsonObject runtimeProject = normalizeProject(project);
        return externalizeTextures(runtimeProject, namespace, textureBasePath);
    }

    private static JsonObject normalizeProject(JsonObject source) throws IOException {
        JsonObject project = source.deepCopy();
        EDITOR_ONLY_ROOT_FIELDS.forEach(project::remove);

        Map<String, String> nodeIds = createNodeIdMap(project);
        JsonArray elements = project.has("elements") && project.get("elements").isJsonArray()
                ? project.getAsJsonArray("elements") : new JsonArray();
        for (JsonElement elementEntry : elements) {
            if (!elementEntry.isJsonObject()) continue;
            JsonObject element = elementEntry.getAsJsonObject();
            removeFields(element, EDITOR_ONLY_ELEMENT_FIELDS);
            remapUuid(element, nodeIds);
        }

        Map<String, JsonObject> groupDefinitions = groupDefinitions(source);
        if (project.has("outliner") && project.get("outliner").isJsonArray()) {
            JsonArray outliner = new JsonArray();
            for (JsonElement entry : project.getAsJsonArray("outliner")) {
                JsonElement normalized = normalizeOutlinerEntry(
                        entry, nodeIds, groupDefinitions, new HashSet<>());
                if (normalized != null) outliner.add(normalized);
            }
            project.add("outliner", outliner);
        }
        project.remove("groups");
        remapAnimationTargets(project, nodeIds);
        return project;
    }

    private static Map<String, String> createNodeIdMap(JsonObject project) throws IOException {
        Map<String, String> ids = new LinkedHashMap<>();
        collectArrayNodeIds(project, "elements", ids);
        collectArrayNodeIds(project, "groups", ids);
        if (project.has("outliner") && project.get("outliner").isJsonArray()) {
            collectInlineGroupIds(project.getAsJsonArray("outliner"), ids);
        }
        return ids;
    }

    private static void collectArrayNodeIds(JsonObject project, String member,
                                            Map<String, String> ids) throws IOException {
        if (!project.has(member) || !project.get(member).isJsonArray()) return;
        for (JsonElement entry : project.getAsJsonArray(member)) {
            if (!entry.isJsonObject()) continue;
            registerNodeId(entry.getAsJsonObject(), ids);
        }
    }

    private static void collectInlineGroupIds(JsonArray entries, Map<String, String> ids) throws IOException {
        for (JsonElement entry : entries) {
            if (!entry.isJsonObject()) continue;
            JsonObject group = entry.getAsJsonObject();
            registerNodeId(group, ids);
            if (group.has("children") && group.get("children").isJsonArray()) {
                collectInlineGroupIds(group.getAsJsonArray("children"), ids);
            }
        }
    }

    private static void registerNodeId(JsonObject node, Map<String, String> ids) throws IOException {
        if (!node.has("uuid") || !node.get("uuid").isJsonPrimitive()
                || !node.getAsJsonPrimitive("uuid").isString()
                || node.get("uuid").getAsString().isBlank()) {
            throw new IOException("BBModel node is missing uuid");
        }
        String original = node.get("uuid").getAsString();
        ids.computeIfAbsent(original, ignored -> Integer.toString(ids.size(), Character.MAX_RADIX));
    }

    private static Map<String, JsonObject> groupDefinitions(JsonObject project) {
        Map<String, JsonObject> definitions = new LinkedHashMap<>();
        if (!project.has("groups") || !project.get("groups").isJsonArray()) return definitions;
        for (JsonElement entry : project.getAsJsonArray("groups")) {
            if (!entry.isJsonObject()) continue;
            JsonObject group = entry.getAsJsonObject();
            if (group.has("uuid") && group.get("uuid").isJsonPrimitive()) {
                definitions.put(group.get("uuid").getAsString(), group.deepCopy());
            }
        }
        return definitions;
    }

    private static JsonElement normalizeOutlinerEntry(JsonElement entry, Map<String, String> nodeIds,
                                                       Map<String, JsonObject> groupDefinitions,
                                                       Set<String> groupStack) throws IOException {
        if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
            String uuid = entry.getAsString();
            JsonObject definition = groupDefinitions.get(uuid);
            if (definition != null) {
                return normalizeGroup(definition, nodeIds, groupDefinitions, groupStack);
            }
            String mapped = nodeIds.get(uuid);
            return mapped == null ? null : new JsonPrimitive(mapped);
        }
        if (!entry.isJsonObject()) return null;

        JsonObject inline = entry.getAsJsonObject();
        String uuid = inline.has("uuid") ? inline.get("uuid").getAsString() : "";
        JsonObject definition = groupDefinitions.get(uuid);
        JsonObject merged = definition == null ? inline.deepCopy() : definition.deepCopy();
        for (Map.Entry<String, JsonElement> property : inline.entrySet()) {
            merged.add(property.getKey(), property.getValue().deepCopy());
        }
        return normalizeGroup(merged, nodeIds, groupDefinitions, groupStack);
    }

    private static JsonObject normalizeGroup(JsonObject source, Map<String, String> nodeIds,
                                             Map<String, JsonObject> groupDefinitions,
                                             Set<String> groupStack) throws IOException {
        if (!source.has("uuid") || source.get("uuid").getAsString().isBlank()) {
            throw new IOException("BBModel group is missing uuid");
        }
        String originalUuid = source.get("uuid").getAsString();
        if (!groupStack.add(originalUuid)) {
            throw new IOException("Cyclic BBModel group hierarchy at " + originalUuid);
        }
        try {
            JsonObject group = source.deepCopy();
            removeFields(group, EDITOR_ONLY_GROUP_FIELDS);
            group.addProperty("uuid", nodeIds.getOrDefault(originalUuid, originalUuid));
            if (source.has("children") && source.get("children").isJsonArray()) {
                JsonArray children = new JsonArray();
                for (JsonElement child : source.getAsJsonArray("children")) {
                    JsonElement normalized = normalizeOutlinerEntry(
                            child, nodeIds, groupDefinitions, groupStack);
                    if (normalized != null) children.add(normalized);
                }
                group.add("children", children);
            }
            return group;
        } finally {
            groupStack.remove(originalUuid);
        }
    }

    private static void remapAnimationTargets(JsonObject project, Map<String, String> nodeIds) {
        if (!project.has("animations") || !project.get("animations").isJsonArray()) return;
        for (JsonElement animationEntry : project.getAsJsonArray("animations")) {
            if (!animationEntry.isJsonObject()) continue;
            JsonObject animation = animationEntry.getAsJsonObject();
            if (!animation.has("animators") || !animation.get("animators").isJsonObject()) continue;
            JsonObject remapped = new JsonObject();
            for (Map.Entry<String, JsonElement> animator : animation.getAsJsonObject("animators").entrySet()) {
                remapped.add(nodeIds.getOrDefault(animator.getKey(), animator.getKey()), animator.getValue());
            }
            animation.add("animators", remapped);
        }
    }

    private static void remapUuid(JsonObject node, Map<String, String> nodeIds) {
        if (!node.has("uuid") || !node.get("uuid").isJsonPrimitive()) return;
        String uuid = node.get("uuid").getAsString();
        node.addProperty("uuid", nodeIds.getOrDefault(uuid, uuid));
    }

    private static void removeFields(JsonObject object, Set<String> fields) {
        fields.forEach(object::remove);
    }

    private static ExportedModel externalizeTextures(JsonObject project, String namespace,
                                                      String textureBasePath) throws IOException {
        JsonElement texturesElement = project.get("textures");
        if (texturesElement == null || !texturesElement.isJsonArray()) {
            throw new IOException("BBModel must contain embedded PNG textures");
        }

        JsonArray textures = texturesElement.getAsJsonArray();
        if (textures.isEmpty()) {
            throw new IOException("BBModel must contain embedded PNG textures");
        }
        int defaultIndex = 0;
        for (int index = 0; index < textures.size(); index++) {
            JsonObject texture = textures.get(index).getAsJsonObject();
            if (texture.has("use_as_default") && texture.get("use_as_default").getAsBoolean()) {
                defaultIndex = index;
                break;
            }
        }

        Map<String, byte[]> entries = new LinkedHashMap<>();
        Map<String, String> resourcesByDigest = new HashMap<>();
        String defaultTexture = null;
        for (int index = 0; index < textures.size(); index++) {
            JsonElement textureElement = textures.get(index);
            if (!textureElement.isJsonObject()) {
                throw new IOException("BBModel texture entry is not an object");
            }
            JsonObject texture = textureElement.getAsJsonObject();
            JsonElement source = texture.get("source");
            if (source == null || !source.isJsonPrimitive() || !source.getAsJsonPrimitive().isString()
                    || !source.getAsString().startsWith(EMBEDDED_PNG_PREFIX)) {
                throw new IOException("BBModel texture must contain embedded PNG data");
            }
            byte[] png;
            try {
                png = Base64.getDecoder().decode(source.getAsString().substring(EMBEDDED_PNG_PREFIX.length()));
            } catch (IllegalArgumentException exception) {
                throw new IOException("BBModel texture contains invalid Base64 data", exception);
            }
            png = optimizePng(png);
            String digest = sha256(png);
            String resource = resourcesByDigest.get(digest);
            if (resource == null) {
                String resourcePath = textureBasePath + "/texture-" + entries.size() + ".png";
                resource = namespace + ":" + resourcePath;
                resourcesByDigest.put(digest, resource);
                entries.put("assets/" + namespace + "/" + resourcePath, png);
            }
            removeFields(texture, EDITOR_ONLY_TEXTURE_FIELDS);
            texture.remove("source");
            texture.remove("path");
            texture.remove("internal");
            texture.addProperty("relative_path", resource);
            if (index == defaultIndex) defaultTexture = resource;
        }
        byte[] modelBytes = GSON.toJson(project).getBytes(StandardCharsets.UTF_8);
        return new ExportedModel(modelBytes,
                Collections.unmodifiableMap(new LinkedHashMap<>(entries)), defaultTexture);
    }

    private static byte[] optimizePng(byte[] png) throws IOException {
        BufferedImage image = readPng(png);
        byte[] optimized;
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) throw new IOException("PNG encoder is unavailable");
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            if (imageOutput == null) throw new IOException("PNG output stream is unavailable");
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameters.setCompressionQuality(0.0F);
            }
            writer.setOutput(imageOutput);
            writer.write(null, new IIOImage(image, null, null), parameters);
            imageOutput.flush();
            optimized = output.toByteArray();
        } finally {
            writer.dispose();
        }

        BufferedImage decoded = readPng(optimized);
        requireEqualPixels(image, decoded);
        return optimized.length < png.length ? optimized : png;
    }

    private static BufferedImage readPng(byte[] png) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(png))) {
            if (input == null) throw new IOException("PNG input stream is unavailable");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Texture is not a supported PNG image");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_TEXTURE_PIXELS) {
                    throw new IOException("PNG texture dimensions exceed the safety limit");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    private static void requireEqualPixels(BufferedImage expected, BufferedImage actual) throws IOException {
        if (expected.getWidth() != actual.getWidth() || expected.getHeight() != actual.getHeight()) {
            throw new IOException("PNG optimization changed texture dimensions");
        }
        int width = expected.getWidth();
        int[] expectedRow = new int[width];
        int[] actualRow = new int[width];
        for (int y = 0; y < expected.getHeight(); y++) {
            expected.getRGB(0, y, width, 1, expectedRow, 0, width);
            actual.getRGB(0, y, width, 1, actualRow, 0, width);
            if (!Arrays.equals(expectedRow, actualRow)) {
                throw new IOException("PNG optimization changed texture pixels");
            }
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    record ExportedModel(byte[] modelBytes, Map<String, byte[]> textureEntries, String defaultTexture) {
    }
}
