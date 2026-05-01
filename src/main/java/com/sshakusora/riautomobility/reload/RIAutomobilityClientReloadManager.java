package com.sshakusora.riautomobility.reload;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RIAutomobilityClientReloadManager {
    private RIAutomobilityClientReloadManager() {}

    @SubscribeEvent
    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ClientReloadListener());
    }

    private static final class ClientReloadListener implements PreparableReloadListener {
        @Override
        public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
            return stage.wait(CompletableFuture.completedFuture(null)).thenRunAsync(() -> {
                RIAutomobilityReloadManager.reloadDefinitions();

                if (Minecraft.getInstance().level != null) {
                    RIAutomobilityReloadManager.refreshLevel(Minecraft.getInstance().level);
                }
            }, gameExecutor);
        }
    }
}
