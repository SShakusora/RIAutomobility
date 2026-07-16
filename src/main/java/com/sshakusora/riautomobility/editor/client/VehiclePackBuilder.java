package com.sshakusora.riautomobility.editor.client;

import com.google.gson.*;
import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.model.bbmodel.BbModelBounds;
import com.sshakusora.riautomobility.model.bbmodel.BbModelData;
import com.sshakusora.riautomobility.model.bbmodel.BbModelParser;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class VehiclePackBuilder {
    public static final long MAX_SOURCE_FILE_SIZE = 32L * 1024L * 1024L;
    private static final String EMBEDDED_PNG_PREFIX = "data:image/png;base64,";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ExecutorService BUILD_IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "RIAutomobility vehicle pack builder");
        thread.setDaemon(true);
        return thread;
    });

    private VehiclePackBuilder() {
    }

    public static Path build(VehicleEditorDraft draft, Path destination, boolean preview, String author) throws IOException {
        author = validateAuthor(author);
        String namespace = preview ? VehicleEditorDraft.PREVIEW_NAMESPACE : draft.namespace();
        String componentPath = preview ? draft.previewKey(draft.target) : draft.componentPath();
        ValidatedModel model = validateSources(draft, namespace, componentPath);
        String validation = draft.validationError();
        if (!validation.isBlank()) {
            throw new IOException(validation);
        }
        Files.createDirectories(destination.getParent());

        String kind = draft.target.path;
        Map<String, byte[]> entries = new LinkedHashMap<>();
        JsonObject metadata = new JsonObject();
        metadata.addProperty("format", CarPackArchiveStore.RIAUTO_FORMAT_VERSION);
        String declaredComponentId = namespace + ":" + componentPath;
        metadata.addProperty("id", declaredComponentId);
        metadata.addProperty("name", draft.displayName());
        metadata.addProperty("author", author);
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
        addV2ModelEntries(entries, component, model.exported(), namespace, kind, componentPath);
        entries.put("data/" + namespace + "/riautomobility/" + draft.target.path + "s/"
                + componentPath + ".json", GSON.toJson(component).getBytes(StandardCharsets.UTF_8));

        writeArchive(destination, entries);
        return destination;
    }

    public static ExportRequest capture(VehicleEditorDraft draft, String author) throws IOException {
        author = validateAuthor(author);
        String validation = draft.validationError();
        if (!validation.isBlank()) throw new IOException(validation);
        Path modelFile = draft.modelFile();
        if (modelFile == null) throw new IOException("Source model is unavailable");
        JsonObject component = switch (draft.target) {
            case FRAME -> draft.frameSpec(false).toJson();
            case WHEEL -> draft.wheelSpec(false).toJson();
            case ENGINE -> draft.engineSpec(false).toJson();
        };
        return new ExportRequest(draft.target, modelFile, draft.namespace(), draft.componentPath(),
                draft.displayName(), author, component.deepCopy(), draft.packName(), draft.overwrite,
                draft.usesAutomaticFrameModelSize(), draft.usesAutomaticWheelModelSize());
    }

    public static CompletableFuture<Path> buildAsync(ExportRequest request, Path destination) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return build(request, destination);
            } catch (IOException exception) {
                throw new CompletionException(exception);
            }
        }, BUILD_IO);
    }

    static Path build(ExportRequest request, Path destination) throws IOException {
        byte[] sourceBytes = readLimited(request.modelFile());
        BbModelData.Document sourceDocument = validateSource(sourceBytes);
        String textureBasePath = "textures/entity/automobile/" + request.target().path + "/" + request.componentPath();
        BbModelRuntimeSanitizer.ExportedModel exported = BbModelRuntimeSanitizer.externalize(
                sourceBytes, request.namespace(), textureBasePath);
        BbModelParser.requireExternalPngTextures(parseSource(exported.modelBytes()));
        Files.createDirectories(destination.getParent());

        String declaredComponentId = request.namespace() + ":" + request.componentPath();
        JsonObject metadata = new JsonObject();
        metadata.addProperty("format", CarPackArchiveStore.RIAUTO_FORMAT_VERSION);
        metadata.addProperty("id", declaredComponentId);
        metadata.addProperty("name", request.displayName());
        metadata.addProperty("author", request.author());
        JsonObject components = new JsonObject();
        components.add("frames", componentArray(request.target() == VehicleEditorDraft.Target.FRAME, declaredComponentId));
        components.add("wheels", componentArray(request.target() == VehicleEditorDraft.Target.WHEEL, declaredComponentId));
        components.add("engines", componentArray(request.target() == VehicleEditorDraft.Target.ENGINE, declaredComponentId));
        metadata.add("components", components);

        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put(CarPackArchiveStore.RIAUTO_METADATA_FILE,
                GSON.toJson(metadata).getBytes(StandardCharsets.UTF_8));
        JsonObject component = request.component().deepCopy();
        applyAutomaticModelSize(request, component, sourceDocument);
        addV2ModelEntries(entries, component, exported, request.namespace(), request.target().path, request.componentPath());
        entries.put("data/" + request.namespace() + "/riautomobility/" + request.target().path + "s/"
                + request.componentPath() + ".json", GSON.toJson(component).getBytes(StandardCharsets.UTF_8));
        writeArchive(destination, entries);
        return destination;
    }

    private static void applyAutomaticModelSize(ExportRequest request, JsonObject component,
                                                BbModelData.Document sourceDocument) {
        if (request.target() == VehicleEditorDraft.Target.FRAME && request.automaticFrameModelSize()) {
            BbModelBounds.Measurement measurement = BbModelBounds.measure(sourceDocument);
            component.addProperty("length_px", measurement.frameItemLengthPx());
            BbModelBounds.Size size = measurement.size();
            Vec3 offset = VehicleEditorDraft.automaticThirdPersonCameraOffset(
                    size.widthPx(), size.heightPx(), size.depthPx());
            JsonArray seats = component.getAsJsonArray("seats");
            JsonArray cameras = new JsonArray();
            int cameraCount = Math.max(1, seats == null ? 0 : seats.size());
            for (int index = 0; index < cameraCount; index++) {
                JsonObject camera = new JsonObject();
                camera.addProperty("x", offset.x);
                camera.addProperty("y", offset.y);
                camera.addProperty("z", offset.z);
                cameras.add(camera);
            }
            component.add("camera_positions", cameras);
        } else if (request.target() == VehicleEditorDraft.Target.WHEEL && request.automaticWheelModelSize()) {
            BbModelBounds.Measurement measurement = BbModelBounds.measure(sourceDocument);
            VehicleEditorDraft.AutomaticWheelModelSize size =
                    VehicleEditorDraft.automaticWheelModelSize(measurement.size());
            component.addProperty("radius", size.radiusPx());
            component.addProperty("width", size.widthPx());
            component.getAsJsonObject("model").addProperty("rotation_y", size.rotationY());
        }
    }

    private static JsonArray componentArray(boolean include, String componentId) {
        JsonArray values = new JsonArray();
        if (include) values.add(componentId);
        return values;
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
        return buildPreview(modelFiles, previewKeys, destination, (target, document) -> {
        });
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

    private static ValidatedModel validateSources(VehicleEditorDraft draft, String namespace,
                                                  String componentPath) throws IOException {
        byte[] sourceBytes = readLimited(draft.modelFile());
        BbModelData.Document document = validateSource(sourceBytes);
        if (draft.target == VehicleEditorDraft.Target.FRAME) {
            draft.applyAutomaticFrameModelSize(BbModelBounds.measure(document));
        } else if (draft.target == VehicleEditorDraft.Target.WHEEL) {
            draft.applyAutomaticWheelModelSize(BbModelBounds.measure(document));
        }
        String textureBasePath = "textures/entity/automobile/" + draft.target.path + "/" + componentPath;
        BbModelRuntimeSanitizer.ExportedModel exported = BbModelRuntimeSanitizer.externalize(
                sourceBytes, namespace, textureBasePath);
        BbModelData.Document runtimeDocument = parseSource(exported.modelBytes());
        BbModelParser.requireExternalPngTextures(runtimeDocument);
        return new ValidatedModel(exported);
    }

    static BbModelData.Document validateSource(Path source) throws IOException {
        return validateSource(readLimited(source));
    }

    static BbModelData.Document validateSource(byte[] model) throws IOException {
        BbModelData.Document document = parseSource(model);
        validateEmbeddedTextures(document);
        return document;
    }

    private static BbModelData.Document parseSource(byte[] model) throws IOException {
        JsonObject json;
        try {
            json = JsonParser.parseString(new String(model, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Model is not a valid JSON object", exception);
        }
        try {
            return BbModelParser.parse(json);
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

    static void addV2ModelEntries(Map<String, byte[]> entries, JsonObject component,
                                  BbModelRuntimeSanitizer.ExportedModel model, String namespace,
                                  String kind, String componentPath) {
        component.getAsJsonObject("model").addProperty("texture", model.defaultTexture());
        entries.put("assets/" + namespace + "/models/entity/automobile/" + kind + "/"
                + componentPath + ".bbmodel", model.modelBytes());
        entries.putAll(model.textureEntries());
    }

    private static String validateAuthor(String author) throws IOException {
        if (author == null) throw new IOException("Exporting player name is unavailable");
        author = author.strip();
        if (author.isBlank() || author.length() > CarPackArchiveStore.MAX_AUTHOR_LENGTH
                || author.chars().anyMatch(Character::isISOControl)) {
            throw new IOException("Invalid exporting player name");
        }
        return author;
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

    static void writeArchive(Path destination, Map<String, byte[]> entries) throws IOException {
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

    private record ValidatedModel(BbModelRuntimeSanitizer.ExportedModel exported) {
    }

    public record ExportRequest(VehicleEditorDraft.Target target, Path modelFile, String namespace,
                                String componentPath, String displayName, String author,
                                JsonObject component, String packName, boolean overwrite,
                                boolean automaticFrameModelSize, boolean automaticWheelModelSize) {
        public ExportRequest(VehicleEditorDraft.Target target, Path modelFile, String namespace,
                             String componentPath, String displayName, String author,
                             JsonObject component, String packName, boolean overwrite) {
            this(target, modelFile, namespace, componentPath, displayName, author, component, packName,
                    overwrite, false, false);
        }
    }
}
