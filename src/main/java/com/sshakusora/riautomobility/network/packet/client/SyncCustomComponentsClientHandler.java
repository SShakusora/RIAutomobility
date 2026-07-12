package com.sshakusora.riautomobility.network.packet.client;

import com.mojang.logging.LogUtils;
import com.sshakusora.riautomobility.carpack.CarPackManager;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.RIAutomobilityComponentManager;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.model.DynamicJsonModelLoader;
import com.sshakusora.riautomobility.model.RIAutomobileModels;
import com.sshakusora.riautomobility.model.bbmodel.BbModelRepository;
import com.sshakusora.riautomobility.wheel.RIAutomobileWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SyncCustomComponentsClientHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private SyncCustomComponentsClientHandler() {}

    public static void handle(Map<ResourceLocation, FrameSpec> frames, Map<ResourceLocation, WheelSpec> wheels, Map<String, String> serverDigests) {
        Minecraft minecraft = Minecraft.getInstance();
        Map<String, String> clientDigests = CarPackManager.getDigests();
        boolean hasSelectedCarPack = minecraft.getResourcePackRepository().getSelectedIds().stream()
                .anyMatch(id -> id.startsWith("riautomobility/"));
        if (serverDigests.isEmpty() && clientDigests.isEmpty() && !hasSelectedCarPack) {
            applyComponents(minecraft, frames, wheels);
            return;
        }

        minecraft.reloadResourcePacks().whenComplete((unused, error) -> minecraft.execute(() -> {
            if (error != null) {
                LOGGER.error("Failed to reload RIAutomobility car pack resources", error);
            }
            reportPackDifferences(minecraft, serverDigests, CarPackManager.getDigests());
            applyComponents(minecraft, frames, wheels);
        }));
    }

    private static void applyComponents(Minecraft minecraft, Map<ResourceLocation, FrameSpec> frames, Map<ResourceLocation, WheelSpec> wheels) {
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

    private static void reportPackDifferences(Minecraft minecraft, Map<String, String> serverDigests, Map<String, String> clientDigests) {
        List<String> missing = new ArrayList<>();
        List<String> different = new ArrayList<>();
        List<String> clientOnly = new ArrayList<>();
        for (Map.Entry<String, String> entry : serverDigests.entrySet()) {
            String clientDigest = clientDigests.get(entry.getKey());
            if (clientDigest == null) {
                missing.add(entry.getKey());
            } else if (!clientDigest.equals(entry.getValue())) {
                different.add(entry.getKey());
            }
        }
        for (String clientPack : clientDigests.keySet()) {
            if (!serverDigests.containsKey(clientPack)) {
                clientOnly.add(clientPack);
            }
        }
        missing.sort(String::compareTo);
        different.sort(String::compareTo);
        clientOnly.sort(String::compareTo);

        if (minecraft.player == null || missing.isEmpty() && different.isEmpty() && clientOnly.isEmpty()) {
            return;
        }

        MutableComponent message = Component.translatable("message.riautomobility.carpacks.mismatch");
        appendDifference(message, "message.riautomobility.carpacks.missing", missing);
        appendDifference(message, "message.riautomobility.carpacks.different", different);
        appendDifference(message, "message.riautomobility.carpacks.client_only", clientOnly);
        minecraft.player.displayClientMessage(message, false);
    }

    private static void appendDifference(MutableComponent message, String translationKey, List<String> packs) {
        if (!packs.isEmpty()) {
            message.append(" ").append(Component.translatable(translationKey, String.join(", ", packs)));
        }
    }
}
