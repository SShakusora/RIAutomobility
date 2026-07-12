package com.sshakusora.riautomobility.network.packet.client;

import com.mojang.logging.LogUtils;
import com.sshakusora.riautomobility.carpack.CarPackManager;
import com.sshakusora.riautomobility.carpack.CarPackManifestEntry;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.RIAutomobilityComponentManager;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.model.DynamicJsonModelLoader;
import com.sshakusora.riautomobility.model.RIAutomobileModels;
import com.sshakusora.riautomobility.model.bbmodel.BbModelRepository;
import com.sshakusora.riautomobility.wheel.RIAutomobileWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public final class SyncCustomComponentsClientHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private SyncCustomComponentsClientHandler() {}

    public static void handle(Map<ResourceLocation, FrameSpec> frames, Map<ResourceLocation, WheelSpec> wheels,
                              List<CarPackManifestEntry> manifest) {
        ClientCarPackSynchronizer.begin(frames, wheels, manifest);
    }

    static void applyComponents(Minecraft minecraft, Map<ResourceLocation, FrameSpec> frames, Map<ResourceLocation, WheelSpec> wheels) {
        RIAutomobileFrame.reload();
        RIAutomobileWheel.reload();
        RIAutomobilityComponentManager.applyCustomComponents(frames, wheels);
        RIAutomobileModels.registerDynamicModels(frames.values(), wheels.values());
        BbModelRepository.reload(minecraft.getResourceManager());

        if (minecraft.getEntityModels() != null) {
            DynamicJsonModelLoader.loadIntoEntityModelSet(minecraft.getEntityModels(), minecraft.getResourceManager());
        }

        RIAutomobileModels.rebuildDynamicModelsNow();

        if (minecraft.level != null) {
            CarPackManager.refreshLevel(minecraft.level);
        }
    }

}
