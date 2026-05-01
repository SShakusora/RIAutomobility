package com.sshakusora.riautomobility.events;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.SyncCustomComponentsPacket;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID)
public final class CustomComponentSyncHandler {
    private CustomComponentSyncHandler() {}

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        SyncCustomComponentsPacket packet = SyncCustomComponentsPacket.create();
        if (event.getPlayer() != null) {
            RIAutomobilityNetwork.CHANNEL.sendTo(packet, event.getPlayer().connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
            return;
        }

        for (var player : event.getPlayerList().getPlayers()) {
            RIAutomobilityNetwork.CHANNEL.sendTo(packet, player.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
        }
    }
}
