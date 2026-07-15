package com.sshakusora.riautomobility.editor.client;

import com.google.gson.*;
import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.model.bbmodel.BbModelBounds;
import com.sshakusora.riautomobility.model.bbmodel.BbModelData;
import com.sshakusora.riautomobility.model.bbmodel.BbModelParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class VehiclePackBuilder {
    public static final long MAX_SOURCE_FILE_SIZE = 32L * 1024L * 1024L;
    private static final String EMBEDDED_PNG_PREFIX = "data:image/png;base64,";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private VehiclePackBuilder() {
    }

    public static Path build(VehicleEditorDraft draft, Path destination, boolean preview) throws IOException {
        BbModelData.Document document = validateSources(draft);
        String validation = draft.validationError();
        if (!validation.isBlank()) {
            throw new IOException(validation);
        }
        Files.createDirectories(destination.getParent());

        String namespace = preview ? VehicleEditorDraft.PREVIEW_NAMESPACE : draft.namespace();
        String componentPath = preview ? draft.previewKey(draft.target) : draft.componentPath();
        String kind = draft.target.path;
        Map<String, byte[]> entries = new LinkedHashMap<>();
        JsonObject metadata = new JsonObject();
        metadata.addProperty("format", CarPackArchiveStore.RIAUTO_FORMAT_VERSION);
        String declaredComponentId = namespace + ":" + componentPath;
        metadata.addProperty("id", declaredComponentId);
        metadata.addProperty("name", draft.displayName());
        JsonObject components = new JsonObject();
        var frames = new JsonArray();
        var wheels = new JsonArray();
        var engines = new JsonArray();
        switch (draft.target) {
            case FRAME -> frames.add(declaredComponentId);
            case WHEEL -> wheels.add(declaredComponentId);
            case ENGINE -> engines.add(declaredComponentId);
        }
        components.add("frames", frames);
        components.add("wheels", wheels);
        components.add("engines", engines);
        metadata.add("components", components);
        entries.put(CarPackArchiveStore.RIAUTO_METADATA_FILE, GSON.toJson(metadata).getBytes(StandardCharsets.UTF_8));

        JsonObject component = switch (draft.target) {
            case FRAME -> draft.frameSpec(preview).toJson();
            case WHEEL -> draft.wheelSpec(preview).toJson();
            case ENGINE -> draft.engineSpec(preview).toJson();
        };
        entries.put("data/" + namespace + "/riautomobility/" + draft.target.path + "s/"
                + componentPath + ".json", GSON.toJson(component).getBytes(StandardCharsets.UTF_8));

        entries.put("assets/" + namespace + "/models/entity/automobile/" + kind + "/" + componentPath + ".bbmodel",
                readLimited(draft.modelFile()));
        entries.put("assets/" + namespace + "/textures/entity/automobile/" + kind + "/" + componentPath + ".png",
                defaultEmbeddedTexture(document));

        writeArchive(destination, entries);
        return destination;
    }

    static Path buildPreview(VehicleEditorDraft draft, Path destination) throws IOException {
        Map<VehicleEditorDraft.Target, Path> modelFiles = new EnumMap<>(VehicleEditorDraft.Target.class);
        Map<VehicleEditorDraft.Target, String> previewKeys = new EnumMap<>(VehicleEditorDraft.Target.class);
        for (VehicleEditorDraft.Target target : VehicleEditorDraft.Target.values()) {
            if (draft.modelFile(target) != null) modelFiles.put(target, draft.modelFile(target));
            previewKeys.put(target, draft.previewKey(target));
        }
        return buildPreview(modelFiles, previewKeys, destination, (target, document) -> {
            if (target == VehicleEditorDraft.Target.FRAME) {
                draft.applyAutomaticFrameModelSize(BbModelBounds.measure(document));
            } else if (target == VehicleEditorDraft.Target.WHEEL) {
                draft.applyAutomaticWheelModelSize(BbModelBounds.measure(document));
            }
        });
    }

    static Path buildPreview(Map<VehicleEditorDraft.Target, Path> modelFiles,
                              Map<VehicleEditorDraft.Target, String> previewKeys,
                              Path destination) throws IOException {
        return buildPreview(modelFiles, previewKeys, destination, (target, document) -> {});
    }

    private static Path buildPreview(Map<VehicleEditorDraft.Target, Path> modelFiles,
                                     Map<VehicleEditorDraft.Target, String> previewKeys,
                                     Path destination,
                                     BiConsumer<VehicleEditorDraft.Target, BbModelData.Document> modelConsumer) throws IOException {
        Files.createDirectories(destination.getParent());
        Map<String, byte[]> entries = new LinkedHashMap<>();
        int modelCount = 0;
        for (VehicleEditorDraft.Target target : VehicleEditorDraft.Target.values()) {
            Path modelFile = modelFiles.get(target);
            if (modelFile == null) continue;
            BbModelData.Document document = validateSource(modelFile);
            modelConsumer.accept(target, document);
            String componentPath = previewKeys.get(target);
            if (componentPath == null || !componentPath.matches("[a-z0-9/._-]+")) {
                throw new IOException("Invalid preview component path for " + target.path);
            }
            entries.put("assets/" + VehicleEditorDraft.PREVIEW_NAMESPACE + "/models/entity/automobile/"
                    + target.path + "/" + componentPath + ".bbmodel", readLimited(modelFile));
            entries.put("assets/" + VehicleEditorDraft.PREVIEW_NAMESPACE + "/textures/entity/automobile/"
                    + target.path + "/" + componentPath + ".png", defaultEmbeddedTexture(document));
            modelCount++;
        }
        if (modelCount == 0) throw new IOException("Choose at least one BBModel file");
        writeArchive(destination, entries);
        return destination;
    }

    private static BbModelData.Document validateSources(VehicleEditorDraft draft) throws IOException {
        BbModelData.Document document = validateSource(draft.modelFile());
        if (draft.target == VehicleEditorDraft.Target.FRAME) {
            draft.applyAutomaticFrameModelSize(BbModelBounds.measure(document));
        } else if (draft.target == VehicleEditorDraft.Target.WHEEL) {
            draft.applyAutomaticWheelModelSize(BbModelBounds.measure(document));
        }
        return document;
    }

    static BbModelData.Document validateSource(Path source) throws IOException {
        byte[] model = readLimited(source);
        JsonObject json;
        try {
            json = JsonParser.parseString(new String(model, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Model is not a valid JSON object", exception);
        }
        try {
            BbModelData.Document document = BbModelParser.parse(json);
            validateEmbeddedTextures(document);
            return document;
        } catch (RuntimeException exception) {
            throw new IOException("Invalid BBModel: " + exception.getMessage(), exception);
        }
    }

    static void validateEmbeddedTextures(BbModelData.Document document) throws IOException {
        try {
            BbModelParser.requireEmbeddedPngTextures(document);
        } catch (RuntimeException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
    }

    static byte[] defaultEmbeddedTexture(BbModelData.Document document) throws IOException {
        BbModelData.Texture texture = document.textures().stream()
                .filter(BbModelData.Texture::useAsDefault)
                .findFirst()
                .orElseGet(() -> document.textures().isEmpty() ? null : document.textures().get(0));
        if (texture == null || !texture.source().startsWith(EMBEDDED_PNG_PREFIX)) {
            throw new IOException("BBModel does not contain a default embedded PNG texture");
        }
        try {
            return Base64.getDecoder().decode(texture.source().substring(EMBEDDED_PNG_PREFIX.length()));
        } catch (IllegalArgumentException exception) {
            throw new IOException("BBModel default texture contains invalid Base64 data", exception);
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

    private static void writeArchive(Path destination, Map<String, byte[]> entries) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destination), StandardCharsets.UTF_8)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipEntry.setTime(0L);
                zip.putNextEntry(zipEntry);
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
    }
}
