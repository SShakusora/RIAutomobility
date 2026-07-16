package com.sshakusora.riautomobility.editor.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.content.EngineSpec;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.model.bbmodel.BbModelData;
import com.sshakusora.riautomobility.model.bbmodel.BbModelParser;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class VehiclePackImporter {
    private static final Gson GSON = new Gson();
    private static final String EMBEDDED_PNG_PREFIX = "data:image/png;base64,";

    private VehiclePackImporter() {
    }

    static ImportedComponent importComponent(Path archive, Path extractionDirectory) throws IOException {
        CarPackArchiveStore.validateRiautoArchive(archive);
        CarPackArchiveStore.DeclaredComponent declared = CarPackArchiveStore.readDeclaredComponent(archive);
        Map<String, String> files = CarPackArchiveStore.readFileMappings(archive);
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            JsonObject metadata = readJson(zip, CarPackArchiveStore.RIAUTO_METADATA_FILE);
            Candidate candidate = readCandidate(zip, files, declared);
            FrameSpec.ModelSpec model = candidate.model();
            if (!model.isBbModel() || model.bbModel() == null) {
                throw new IOException("RIAuto component does not use an editable BBModel");
            }
            String modelEntryName = findFile(files,
                    "assets/" + model.bbModel().getNamespace() + "/" + model.bbModel().getPath());
            ZipEntry modelEntry = zip.getEntry(modelEntryName);
            if (modelEntry == null || modelEntry.isDirectory()) {
                throw new IOException("RIAuto file is missing BBModel asset " + modelEntryName);
            }

            Files.createDirectories(extractionDirectory);
            Path extracted = extractionDirectory.resolve("import-"
                    + UUID.randomUUID().toString().replace("-", "") + ".bbmodel");
            try {
                byte[] modelBytes;
                try (var input = zip.getInputStream(modelEntry)) {
                    modelBytes = input.readNBytes((int) VehiclePackBuilder.MAX_SOURCE_FILE_SIZE + 1);
                }
                if (modelBytes.length > VehiclePackBuilder.MAX_SOURCE_FILE_SIZE) {
                    throw new IOException("BBModel exceeds " + VehiclePackBuilder.MAX_SOURCE_FILE_SIZE + " bytes");
                }
                modelBytes = embedExternalTextures(zip, files, modelBytes);
                if (modelBytes.length > VehiclePackBuilder.MAX_SOURCE_FILE_SIZE) {
                    throw new IOException("Editable BBModel exceeds "
                            + VehiclePackBuilder.MAX_SOURCE_FILE_SIZE + " bytes after restoring textures");
                }
                Files.write(extracted, modelBytes);
                VehiclePackBuilder.validateSource(extracted);
                String author = metadata.has("author") ? metadata.get("author").getAsString().strip() : "";
                return candidate.imported(metadata.get("name").getAsString(), author, extracted);
            } catch (IOException | RuntimeException exception) {
                Files.deleteIfExists(extracted);
                if (exception instanceof IOException ioException) throw ioException;
                throw new IOException("Invalid RIAuto component: " + exception.getMessage(), exception);
            }
        } catch (RuntimeException exception) {
            throw new IOException("Invalid RIAuto file: " + exception.getMessage(), exception);
        }
    }

    private static byte[] embedExternalTextures(ZipFile zip, Map<String, String> files, byte[] modelBytes)
            throws IOException {
        JsonObject model;
        BbModelData.Document document;
        try {
            model = JsonParser.parseString(new String(modelBytes, StandardCharsets.UTF_8)).getAsJsonObject();
            document = BbModelParser.parse(model);
        } catch (RuntimeException exception) {
            throw new IOException("Invalid RIAuto v1 BBModel: " + exception.getMessage(), exception);
        }

        List<BbModelParser.ExternalTexture> textures;
        try {
            textures = BbModelParser.requireExternalPngTextures(document);
        } catch (RuntimeException exception) {
            throw new IOException("Invalid RIAuto v1 textures: " + exception.getMessage(), exception);
        }
        var textureJson = model.getAsJsonArray("textures");
        long restoredSizeEstimate = modelBytes.length;
        for (int index = 0; index < textures.size(); index++) {
            BbModelParser.ExternalTexture texture = textures.get(index);
            String entryName = findFile(files,
                    "assets/" + texture.resource().getNamespace() + "/" + texture.resource().getPath());
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null || entry.isDirectory()) {
                throw new IOException("RIAuto v1 is missing external texture " + texture.resource());
            }
            byte[] png;
            try (var input = zip.getInputStream(entry)) {
                png = input.readNBytes((int) VehiclePackBuilder.MAX_SOURCE_FILE_SIZE + 1);
            }
            if (png.length > VehiclePackBuilder.MAX_SOURCE_FILE_SIZE) {
                throw new IOException("RIAuto v1 texture exceeds " + VehiclePackBuilder.MAX_SOURCE_FILE_SIZE + " bytes");
            }
            restoredSizeEstimate += 4L * ((png.length + 2L) / 3L);
            if (restoredSizeEstimate > VehiclePackBuilder.MAX_SOURCE_FILE_SIZE) {
                throw new IOException("Editable BBModel exceeds "
                        + VehiclePackBuilder.MAX_SOURCE_FILE_SIZE + " bytes after restoring textures");
            }
            try {
                BbModelParser.requirePngTextureBytes(texture.name(), png);
            } catch (IllegalArgumentException exception) {
                throw new IOException(exception.getMessage(), exception);
            }
            JsonObject textureObject = textureJson.get(index).getAsJsonObject();
            textureObject.remove("path");
            textureObject.remove("relative_path");
            textureObject.addProperty("source", EMBEDDED_PNG_PREFIX + Base64.getEncoder().encodeToString(png));
            textureObject.addProperty("internal", true);
        }
        return GSON.toJson(model).getBytes(StandardCharsets.UTF_8);
    }

    private static Candidate readCandidate(ZipFile zip, Map<String, String> files,
                                           CarPackArchiveStore.DeclaredComponent declared) throws IOException {
        ResourceLocation id = declared.id();
        String logicalPath = "data/" + id.getNamespace() + "/riautomobility/"
                + declared.kind().collection() + "/" + id.getPath() + ".json";
        JsonObject component = readJson(zip, findFile(files, logicalPath));
        try {
            return switch (declared.kind()) {
                case FRAME -> Candidate.frame(FrameSpec.fromJson(id, component));
                case WHEEL -> Candidate.wheel(WheelSpec.fromJson(id, component));
                case ENGINE -> Candidate.engine(EngineSpec.fromJson(id, component));
            };
        } catch (RuntimeException exception) {
            throw new IOException("Invalid " + declared.kind().path() + " component " + id
                    + ": " + exception.getMessage(), exception);
        }
    }

    private static String findFile(Map<String, String> files, String logicalPath) throws IOException {
        return files.entrySet().stream()
                .filter(entry -> entry.getValue().equals(logicalPath))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IOException("RIAuto file is missing mapped resource " + logicalPath));
    }

    private static JsonObject readJson(ZipFile zip, String entryName) throws IOException {
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null || entry.isDirectory()) throw new IOException("RIAuto file is missing " + entryName);
        try (var reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid JSON in " + entryName + ": " + exception.getMessage(), exception);
        }
    }

    record ImportedComponent(VehicleEditorDraft.Target target, String displayName, String author, Path modelFile,
                             FrameSpec frame, WheelSpec wheel, EngineSpec engine) {
        void applyTo(VehicleEditorDraft draft) {
            switch (target) {
                case FRAME -> draft.importFrame(frame, displayName, modelFile);
                case WHEEL -> draft.importWheel(wheel, displayName, modelFile);
                case ENGINE -> draft.importEngine(engine, displayName, modelFile);
            }
            draft.setImportedAuthor(target, author);
        }
    }

    private record Candidate(FrameSpec frame, WheelSpec wheel, EngineSpec engine) {
        static Candidate frame(FrameSpec spec) { return new Candidate(spec, null, null); }
        static Candidate wheel(WheelSpec spec) { return new Candidate(null, spec, null); }
        static Candidate engine(EngineSpec spec) { return new Candidate(null, null, spec); }

        FrameSpec.ModelSpec model() {
            if (frame != null) return frame.model();
            if (wheel != null) return wheel.model();
            return engine.model();
        }

        ImportedComponent imported(String displayName, String author, Path modelFile) {
            VehicleEditorDraft.Target target = frame != null ? VehicleEditorDraft.Target.FRAME
                    : wheel != null ? VehicleEditorDraft.Target.WHEEL : VehicleEditorDraft.Target.ENGINE;
            return new ImportedComponent(target, displayName, author, modelFile, frame, wheel, engine);
        }
    }
}
