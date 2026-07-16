package com.sshakusora.riautomobility.network.packet.client;

import com.sshakusora.riautomobility.carpack.CarPackManager;
import com.sshakusora.riautomobility.carpack.CarPackManifestEntry;
import com.sshakusora.riautomobility.content.EngineSpec;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.RIAutomobilityComponentManager;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.model.RIAutomobileModels;
import com.sshakusora.riautomobility.model.bbmodel.BbModelRepository;
import com.sshakusora.riautomobility.wheel.RIAutomobileWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public final class SyncCustomComponentsClientHandler {
    private SyncCustomComponentsClientHandler() {
    }

    public static void handle(Map<ResourceLocation, FrameSpec> frames, Map<ResourceLocation, WheelSpec> wheels,
                              Map<ResourceLocation, EngineSpec> engines,
                              List<CarPackManifestEntry> manifest) {
        ClientCarPackSynchronizer.begin(frames, wheels, engines, manifest);
    }

    static void applyComponents(Minecraft minecraft, Map<ResourceLocation, FrameSpec> frames, Map<ResourceLocation, WheelSpec> wheels,
                                Map<ResourceLocation, EngineSpec> engines) {
        RIAutomobilityComponentManager.clearCustomComponents();
        RIAutomobileFrame.reload();
        RIAutomobileWheel.reload();
        RIAutomobilityComponentManager.applyCustomComponents(frames, wheels, engines);
        RIAutomobileModels.registerDynamicModels(frames.values(), wheels.values(), engines.values());
        BbModelRepository.reload(minecraft.getResourceManager());

        RIAutomobileModels.rebuildDynamicModelsNow();

        if (minecraft.level != null) {
            CarPackManager.refreshLevel(minecraft.level);
        }
    }

    static void refreshMountedComponents(Minecraft minecraft, Collection<ResourceLocation> componentIds,
                                         Map<ResourceLocation, FrameSpec> frames,
                                         Map<ResourceLocation, WheelSpec> wheels,
                                         Map<ResourceLocation, EngineSpec> engines) {
        if (componentIds.isEmpty()) return;
        Set<ResourceLocation> bbModels = new HashSet<>();
        Set<ResourceLocation> affectedComponents = new HashSet<>();

        for (ResourceLocation componentId : componentIds) {
            FrameSpec.ModelSpec model = modelSpec(componentId, frames, wheels, engines);
            if (model == null) continue;
            affectedComponents.add(componentId);
            if (model.bbModel() != null) bbModels.add(model.bbModel());
        }

        BbModelRepository.reload(minecraft.getResourceManager(), bbModels);
        RIAutomobileModels.rebuildDynamicModelsNow(affectedComponents);
    }

    private static FrameSpec.ModelSpec modelSpec(ResourceLocation id,
                                                 Map<ResourceLocation, FrameSpec> frames,
                                                 Map<ResourceLocation, WheelSpec> wheels,
                                                 Map<ResourceLocation, EngineSpec> engines) {
        FrameSpec frame = frames.get(id);
        if (frame != null) return frame.model();
        WheelSpec wheel = wheels.get(id);
        if (wheel != null) return wheel.model();
        EngineSpec engine = engines.get(id);
        return engine == null ? null : engine.model();
    }

}
