package com.sshakusora.riautomobility.carpack;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.SyncCustomComponentsPacket;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CarPackEvents {
    private CarPackEvents() {}

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA || event.getPackType() == PackType.CLIENT_RESOURCES) {
            event.addRepositorySource(new CarPackRepositorySource(event.getPackType()));
        }
    }

    @Mod.EventBusSubscriber(modid = RIAutomobility.MODID)
    public static final class CommonEvents {
        private CommonEvents() {}

        @SubscribeEvent
        public static void addReloadListener(AddReloadListenerEvent event) {
            event.addListener(new CarPackComponentDataLoader());
        }

        @SubscribeEvent
        public static void syncCarPacks(OnDatapackSyncEvent event) {
            SyncCustomComponentsPacket packet = SyncCustomComponentsPacket.create();
            if (event.getPlayer() != null) {
                RIAutomobilityNetwork.CHANNEL.sendTo(
                        packet,
                        event.getPlayer().connection.connection,
                        NetworkDirection.PLAY_TO_CLIENT
                );
                return;
            }

            for (var player : event.getPlayerList().getPlayers()) {
                RIAutomobilityNetwork.CHANNEL.sendTo(
                        packet,
                        player.connection.connection,
                        NetworkDirection.PLAY_TO_CLIENT
                );
            }
        }
    }
}
