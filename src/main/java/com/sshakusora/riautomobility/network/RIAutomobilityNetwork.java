package com.sshakusora.riautomobility.network;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.network.packet.BoardingAsPassengerPacket;
import com.sshakusora.riautomobility.network.packet.PassengerDriverSwitchPacket;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class RIAutomobilityNetwork {
    private static final String PROTOCOL_VERSION = "1";

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
                BoardingAsPassengerPacket.class,
                BoardingAsPassengerPacket::encode,
                BoardingAsPassengerPacket::decode,
                BoardingAsPassengerPacket::handle
        );

        CHANNEL.registerMessage(
                id++,
                PassengerDriverSwitchPacket.class,
                PassengerDriverSwitchPacket::encode,
                PassengerDriverSwitchPacket::decode,
                PassengerDriverSwitchPacket::handle
        );
    }
}
