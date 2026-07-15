package com.sshakusora.riautomobility.editor.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class BbModelRuntimeSanitizer {
    private static final Gson GSON = new Gson();
    private static final String EMBEDDED_PNG_PREFIX = "data:image/png;base64,";
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

    private BbModelRuntimeSanitizer() {
    }

    static ExportedModel externalize(byte[] source, String namespace, String textureBasePath) throws IOException {
        JsonObject project;
        try {
            project = JsonParser.parseString(new String(source, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Model is not a valid JSON object", exception);
        }

        JsonObject runtimeProject = project.deepCopy();
        EDITOR_ONLY_ROOT_FIELDS.forEach(runtimeProject::remove);
        return externalizeTextures(runtimeProject, namespace, textureBasePath);
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
            String resourcePath = textureBasePath + "/" + sha256(png) + ".png";
            String resource = namespace + ":" + resourcePath;
            entries.putIfAbsent("assets/" + namespace + "/" + resourcePath, png);
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

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    record ExportedModel(byte[] modelBytes, Map<String, byte[]> textureEntries, String defaultTexture) {}
}
