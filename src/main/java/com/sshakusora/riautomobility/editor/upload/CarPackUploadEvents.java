package com.sshakusora.riautomobility.editor.upload;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID)
public final class CarPackUploadEvents {
    private CarPackUploadEvents() {}

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CarPackUploadService.abortPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer().getTickCount() % 200 == 0) {
            CarPackUploadService.expireStaleUploads();
        }
    }
}
