package com.sshakusora.riautomobility.network;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.network.packet.*;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class RIAutomobilityNetwork {
    private static final String PROTOCOL_VERSION = "4";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            RIAutomobility.rl("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(
                id++,
                PassengerDriverSwitchPacket.class,
                PassengerDriverSwitchPacket::encode,
                PassengerDriverSwitchPacket::decode,
                PassengerDriverSwitchPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                id++,
                BoardingAsPassengerPacket.class,
                BoardingAsPassengerPacket::encode,
                BoardingAsPassengerPacket::decode,
                BoardingAsPassengerPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                id++,
                SyncCustomComponentsPacket.class,
                SyncCustomComponentsPacket::encode,
                SyncCustomComponentsPacket::decode,
                SyncCustomComponentsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                RequestCarPacksPacket.class,
                RequestCarPacksPacket::encode,
                RequestCarPacksPacket::decode,
                RequestCarPacksPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                id++,
                CarPackTransferStartPacket.class,
                CarPackTransferStartPacket::encode,
                CarPackTransferStartPacket::decode,
                CarPackTransferStartPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                CarPackChunkPacket.class,
                CarPackChunkPacket::encode,
                CarPackChunkPacket::decode,
                CarPackChunkPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                CarPackTransferCompletePacket.class,
                CarPackTransferCompletePacket::encode,
                CarPackTransferCompletePacket::decode,
                CarPackTransferCompletePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                CarPackTransferFailedPacket.class,
                CarPackTransferFailedPacket::encode,
                CarPackTransferFailedPacket::decode,
                CarPackTransferFailedPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                CarPackSyncStatusPacket.class,
                CarPackSyncStatusPacket::encode,
                CarPackSyncStatusPacket::decode,
                CarPackSyncStatusPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(id++, BeginCarPackUploadPacket.class, BeginCarPackUploadPacket::encode,
                BeginCarPackUploadPacket::decode, BeginCarPackUploadPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, CarPackUploadChunkPacket.class, CarPackUploadChunkPacket::encode,
                CarPackUploadChunkPacket::decode, CarPackUploadChunkPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, CompleteCarPackUploadPacket.class, CompleteCarPackUploadPacket::encode,
                CompleteCarPackUploadPacket::decode, CompleteCarPackUploadPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, CarPackUploadResultPacket.class, CarPackUploadResultPacket::encode,
                CarPackUploadResultPacket::decode, CarPackUploadResultPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
