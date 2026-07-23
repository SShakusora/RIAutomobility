package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.carpack.CarPackManager;
import com.sshakusora.riautomobility.carpack.client.ClientCarPackResources;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.model.RIAutomobileModels;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class ClientVehiclePreviewSession {
    private final Map<VehicleEditorDraft.Target, Registration> registrations = new EnumMap<>(VehicleEditorDraft.Target.class);
    private Path archive;
    private long generation;

    CompletableFuture<Void> load(VehicleEditorDraft draft) throws IOException {
        long requestGeneration = ++this.generation;
        VehicleEditorDraft.Target importedTarget = draft.target;
        Path oldArchive = this.archive;
        unregisterModels();
        for (VehicleEditorDraft.Target target : VehicleEditorDraft.Target.values())
            draft.setPreviewReady(target, false);

        Path directory = CarPackManager.getRootDirectory().resolve("cache").resolve("editor");
        Files.createDirectories(directory);
        this.archive = directory.resolve("preview-combined-" + UUID.randomUUID().toString().replace("-", "")
                + CarPackManager.CAR_PACK_EXTENSION);
        VehiclePackBuilder.buildPreview(draft, this.archive);

        for (VehicleEditorDraft.Target target : VehicleEditorDraft.Target.values()) {
            if (draft.modelFile(target) == null) continue;
            ResourceLocation componentId = new ResourceLocation(VehicleEditorDraft.PREVIEW_NAMESPACE, draft.previewKey(target));
            FrameSpec.ModelSpec modelSpec = draft.modelSpec(target, true);
            RIAutomobileModels.registerTemporaryDynamicModel(componentId, modelSpec);
            this.registrations.put(target, new Registration(componentId, modelSpec));
        }

        CarPackManager.setClientPreviewPack(this.archive);
        ClientCarPackResources.refreshDiscoveredPacks();
        if (requestGeneration == this.generation) {
            for (VehicleEditorDraft.Target target : this.registrations.keySet()) {
                draft.setPreviewReady(target, draft.usesImportedModelPreview(target));
            }
            draft.showPart(importedTarget);
        }
        deleteQuietly(oldArchive);
        return CompletableFuture.completedFuture(null);
    }

    void close() {
        ++this.generation;
        CarPackManager.clearClientPreviewPack();
        Path oldArchive = this.archive;
        this.archive = null;
        unregisterModels();
        ClientCarPackResources.refreshDiscoveredPacks();
        deleteQuietly(oldArchive);
    }

    private void unregisterModels() {
        for (Registration registration : this.registrations.values()) {
            RIAutomobileModels.unregisterTemporaryDynamicModel(registration.componentId(), registration.modelSpec());
        }
        this.registrations.clear();
    }

    private static void deleteQuietly(Path archive) {
        if (archive == null) return;
        try {
            Files.deleteIfExists(archive);
        } catch (IOException ignored) {
        }
    }

    private record Registration(ResourceLocation componentId, FrameSpec.ModelSpec modelSpec) {
    }
}
