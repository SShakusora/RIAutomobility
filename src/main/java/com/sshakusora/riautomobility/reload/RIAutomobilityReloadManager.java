package com.sshakusora.riautomobility.reload;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.wheel.RIAutomobileWheel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class RIAutomobilityReloadManager {
    private static final AABB WORLD_BOUNDS = new AABB(-3.0E7D, -2048.0D, -3.0E7D, 3.0E7D, 2048.0D, 3.0E7D);

    private RIAutomobilityReloadManager() {}

    public static void reloadDefinitions() {
        RIAutomobileFrame.reload();
        RIAutomobileWheel.reload();

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (var level : server.getAllLevels()) {
                refreshLevel(level);
            }
        }
    }

    public static void refreshLevel(Level level) {
        for (RIAutomobileEntity automobile : level.getEntitiesOfClass(RIAutomobileEntity.class, WORLD_BOUNDS)) {
            automobile.reloadRIAutomobilityComponents();
        }
    }

    public static final class ReloadListener implements PreparableReloadListener {
        @Override
        public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
            return stage.wait(CompletableFuture.completedFuture(null)).thenRunAsync(RIAutomobilityReloadManager::reloadDefinitions, gameExecutor);
        }
    }

    @Mod.EventBusSubscriber(modid = RIAutomobility.MODID)
    public static final class CommonEvents {
        @SubscribeEvent
        public static void addReloadListener(AddReloadListenerEvent event) {
            event.addListener(new ReloadListener());
        }
    }
}
