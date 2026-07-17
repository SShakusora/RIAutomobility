package com.sshakusora.riautomobility.network.packet.client;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.Util;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID, value = Dist.CLIENT)
public final class VehicleHighlightClientHandler {
    private static final Map<UUID, Long> HIGHLIGHT_UNTIL = new HashMap<>();
    private static final int CLEANUP_INTERVAL_TICKS = 20;
    private static int cleanupTicker;

    private VehicleHighlightClientHandler() {
    }

    public static void handle(UUID automobileId, int durationTicks) {
        handle(automobileId, durationTicks, Util.getMillis());
    }

    static void handle(UUID automobileId, int durationTicks, long now) {
        HIGHLIGHT_UNTIL.put(automobileId, now + durationTicks * 50L);
    }

    public static boolean shouldHighlight(Entity entity) {
        return shouldHighlight(entity.getUUID(), Util.getMillis());
    }

    static boolean shouldHighlight(UUID automobileId, long now) {
        Long expiresAt = HIGHLIGHT_UNTIL.get(automobileId);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt <= now) {
            HIGHLIGHT_UNTIL.remove(automobileId);
            return false;
        }
        return true;
    }

    static void purgeExpired(long now) {
        HIGHLIGHT_UNTIL.values().removeIf(expiresAt -> expiresAt <= now);
    }

    static void clear() {
        HIGHLIGHT_UNTIL.clear();
        cleanupTicker = 0;
    }

    static int trackedHighlightCount() {
        return HIGHLIGHT_UNTIL.size();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++cleanupTicker < CLEANUP_INTERVAL_TICKS) {
            return;
        }
        cleanupTicker = 0;
        purgeExpired(Util.getMillis());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
