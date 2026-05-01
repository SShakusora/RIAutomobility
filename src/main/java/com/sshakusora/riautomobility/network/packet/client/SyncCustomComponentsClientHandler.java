package com.sshakusora.riautomobility.network.packet.client;

import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.RIAutomobilityComponentManager;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.model.DynamicJsonModelLoader;
import com.sshakusora.riautomobility.model.RIAutomobileModels;
import com.sshakusora.riautomobility.reload.RIAutomobilityReloadManager;
import com.sshakusora.riautomobility.wheel.RIAutomobileWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public final class SyncCustomComponentsClientHandler {
    private SyncCustomComponentsClientHandler() {}

    public static void handle(Map<ResourceLocation, FrameSpec> frames, Map<ResourceLocation, WheelSpec> wheels) {
        RIAutomobileFrame.reload();
        RIAutomobileWheel.reload();
        RIAutomobilityComponentManager.applyCustomComponents(frames, wheels);
        RIAutomobileModels.registerDynamicModels(frames.values(), wheels.values());

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getEntityModels() != null) {
            DynamicJsonModelLoader.loadIntoEntityModelSet(minecraft.getEntityModels(), minecraft.getResourceManager());
        }

        RIAutomobileModels.rebuildDynamicModelsNow();

        if (minecraft.level != null) {
            RIAutomobilityReloadManager.refreshLevel(minecraft.level);
        }
    }
}
