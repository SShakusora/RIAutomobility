package com.sshakusora.riautomobility.carpack;

import com.mojang.logging.LogUtils;
import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.CarPackTransferFailedPacket;
import com.sshakusora.riautomobility.network.packet.SyncCustomComponentsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CarPackEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CarPackEvents() {
    }

    @Mod.EventBusSubscriber(modid = RIAutomobility.MODID)
    public static final class CommonEvents {
        private CommonEvents() {
        }

        @SubscribeEvent
        public static void loadCarPacks(ServerAboutToStartEvent event) {
            CarPackSharedDirectoryMonitor.DirectoryState initialState =
                    CarPackRuntime.reloadServerAndCaptureState();
            CarPackSharedDirectoryMonitor.start(event.getServer(), initialState);
        }

        @SubscribeEvent
        public static void pollSharedCarPacks(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                CarPackSharedDirectoryMonitor.tick(event.getServer());
            }
        }

        @SubscribeEvent
        public static void stopSharedCarPackMonitor(ServerStoppedEvent event) {
            CarPackSharedDirectoryMonitor.stop(event.getServer());
        }

        @SubscribeEvent
        public static void syncCarPacks(OnDatapackSyncEvent event) {
            SyncCustomComponentsPacket packet;
            try {
                packet = SyncCustomComponentsPacket.create();
            } catch (RuntimeException exception) {
                LOGGER.error("Unable to prepare RIAutomobility car packs for synchronization", exception);
                String detail = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
                CarPackTransferFailedPacket failure = new CarPackTransferFailedPacket(
                        "The server could not prepare its required car packs: " + detail
                );
                if (event.getPlayer() != null) {
                    send(failure, event.getPlayer());
                } else {
                    event.getPlayerList().getPlayers().forEach(player -> send(failure, player));
                }
                return;
            }
            if (event.getPlayer() != null) {
                send(packet, event.getPlayer());
                return;
            }

            for (var player : event.getPlayerList().getPlayers()) {
                send(packet, player);
            }
        }

        public static void syncAll(MinecraftServer server) {
            SyncCustomComponentsPacket packet = SyncCustomComponentsPacket.create();
            server.getPlayerList().getPlayers().forEach(player -> send(packet, player));
        }

        private static void send(Object packet, ServerPlayer player) {
            RIAutomobilityNetwork.CHANNEL.sendTo(
                    packet,
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT
            );
        }
    }
}
