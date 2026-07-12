package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.carpack.CarPackManager;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.model.RIAutomobileModels;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

final class ClientVehiclePreviewSession {
    private Path archive;
    private net.minecraft.resources.ResourceLocation componentId;
    private FrameSpec.ModelSpec modelSpec;

    CompletableFuture<Void> load(VehicleEditorDraft draft) throws IOException {
        if (this.componentId != null && this.modelSpec != null) {
            RIAutomobileModels.unregisterTemporaryDynamicModel(this.componentId, this.modelSpec);
        }
        draft.previewReady = false;
        Path directory = CarPackManager.getRootDirectory().resolve("cache").resolve("editor");
        Files.createDirectories(directory);
        this.archive = directory.resolve("preview-" + draft.previewKey + ".zip");
        VehiclePackBuilder.build(draft, this.archive, true);

        this.componentId = new net.minecraft.resources.ResourceLocation(VehicleEditorDraft.PREVIEW_NAMESPACE, draft.previewKey);
        this.modelSpec = draft.modelSpec(true);
        RIAutomobileModels.registerTemporaryDynamicModel(this.componentId, this.modelSpec);
        CarPackManager.setClientPreviewPack(this.archive);
        return Minecraft.getInstance().reloadResourcePacks().thenRun(() -> {
            draft.previewReady = true;
            RIAutomobileModels.rebuildDynamicModelsNow();
        });
    }

    void close() {
        CarPackManager.clearClientPreviewPack();
        Path oldArchive = this.archive;
        this.archive = null;
        if (this.componentId != null && this.modelSpec != null) {
            RIAutomobileModels.unregisterTemporaryDynamicModel(this.componentId, this.modelSpec);
            this.componentId = null;
            this.modelSpec = null;
        }
        if (oldArchive != null) Minecraft.getInstance().reloadResourcePacks().whenComplete((unused, error) -> {
            try { Files.deleteIfExists(oldArchive); } catch (IOException ignored) {}
        });
    }
}
