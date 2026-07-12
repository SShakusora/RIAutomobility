package com.sshakusora.riautomobility.editor.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sshakusora.riautomobility.model.bbmodel.BbModelParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class VehiclePackBuilder {
    public static final long MAX_SOURCE_FILE_SIZE = 32L * 1024L * 1024L;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private VehiclePackBuilder() {}

    public static Path build(VehicleEditorDraft draft, Path destination, boolean preview) throws IOException {
        String validation = draft.validationError();
        if (!validation.isBlank()) {
            throw new IOException(validation);
        }
        validateSources(draft);
        Files.createDirectories(destination.getParent());

        String namespace = preview ? VehicleEditorDraft.PREVIEW_NAMESPACE : draft.namespace;
        String componentPath = preview ? draft.previewKey : draft.componentPath;
        String kind = draft.target.path;
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("pack.mcmeta", ("{\n  \"pack\": {\n    \"pack_format\": 15,\n    \"description\": \"RIAutomobility vehicle editor pack\"\n  }\n}\n")
                .getBytes(StandardCharsets.UTF_8));

        JsonObject component = draft.target == VehicleEditorDraft.Target.FRAME
                ? draft.frameSpec(preview).toJson() : draft.wheelSpec(preview).toJson();
        entries.put("data/" + namespace + "/riautomobility/" + (draft.target == VehicleEditorDraft.Target.FRAME ? "frames/" : "wheels/")
                + componentPath + ".json", GSON.toJson(component).getBytes(StandardCharsets.UTF_8));

        switch (draft.modelFormat) {
            case BBMODEL -> entries.put("assets/" + namespace + "/models/entity/automobile/" + kind + "/" + componentPath + ".bbmodel",
                    readLimited(draft.modelFile));
            case GECKOLIB -> {
                entries.put("assets/" + namespace + "/geo/" + kind + "/" + componentPath + ".geo.json", readLimited(draft.modelFile));
                byte[] animation = draft.animationFile == null
                        ? "{\"format_version\":\"1.8.0\",\"animations\":{}}".getBytes(StandardCharsets.UTF_8)
                        : readLimited(draft.animationFile);
                entries.put("assets/" + namespace + "/animations/" + kind + "/" + componentPath + ".animation.json", animation);
            }
            case JSONEM -> entries.put("assets/" + namespace + "/models/entity/automobile/" + kind + "/" + componentPath + "/main.json",
                    readLimited(draft.modelFile));
        }
        if (draft.textureFile != null) {
            entries.put("assets/" + namespace + "/textures/entity/automobile/" + kind + "/" + componentPath + ".png",
                    readLimited(draft.textureFile));
        }

        JsonObject language = new JsonObject();
        language.addProperty(draft.target.path + "." + namespace + "." + componentPath, draft.displayName);
        entries.put("assets/" + namespace + "/lang/en_us.json", GSON.toJson(language).getBytes(StandardCharsets.UTF_8));
        entries.put("assets/" + namespace + "/lang/zh_cn.json", GSON.toJson(language).getBytes(StandardCharsets.UTF_8));

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destination), StandardCharsets.UTF_8)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipEntry.setTime(0L);
                zip.putNextEntry(zipEntry);
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return destination;
    }

    private static void validateSources(VehicleEditorDraft draft) throws IOException {
        byte[] model = readLimited(draft.modelFile);
        JsonObject json;
        try {
            json = JsonParser.parseString(new String(model, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Model is not a valid JSON object", exception);
        }
        if (draft.modelFormat == VehicleEditorDraft.ModelFormat.BBMODEL) {
            try {
                BbModelParser.parse(json);
            } catch (RuntimeException exception) {
                throw new IOException("Invalid BBModel: " + exception.getMessage(), exception);
            }
        }
        if (draft.animationFile != null) {
            parseJsonFile(draft.animationFile, "animation");
        }
        if (draft.textureFile != null) {
            byte[] png = readLimited(draft.textureFile);
            if (png.length < 8 || png[0] != (byte) 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') {
                throw new IOException("Texture is not a PNG file");
            }
        }
    }

    private static void parseJsonFile(Path path, String label) throws IOException {
        try {
            JsonParser.parseString(new String(readLimited(path), StandardCharsets.UTF_8));
        } catch (RuntimeException exception) {
            throw new IOException("Invalid " + label + " JSON", exception);
        }
    }

    private static byte[] readLimited(Path path) throws IOException {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IOException("Source file does not exist: " + path);
        }
        long size = Files.size(path);
        if (size > MAX_SOURCE_FILE_SIZE) {
            throw new IOException("Source file exceeds " + MAX_SOURCE_FILE_SIZE + " bytes");
        }
        return Files.readAllBytes(path);
    }
}
