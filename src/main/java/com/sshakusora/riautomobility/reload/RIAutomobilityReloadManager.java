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
import net.minecraftforge.server.ServerLifecycleHooks;

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

    public static void refreshAllServerLevels() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (var level : server.getAllLevels()) {
            refreshLevel(level);
        }
    }

    public static void refreshLevel(Level level) {
        for (RIAutomobileEntity automobile : level.getEntitiesOfClass(RIAutomobileEntity.class, WORLD_BOUNDS)) {
            automobile.reloadRIAutomobilityComponents();
        }
    }

    @Mod.EventBusSubscriber(modid = RIAutomobility.MODID)
    public static final class CommonEvents {
        @SubscribeEvent
        public static void addReloadListener(AddReloadListenerEvent event) {
            event.addListener(new RIAutomobilityComponentDataLoader());
        }
    }
}
