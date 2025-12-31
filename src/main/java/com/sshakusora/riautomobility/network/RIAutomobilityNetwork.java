package com.sshakusora.riautomobility.network;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.network.packet.PassengerSwitchPacket;
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
                PassengerSwitchPacket.class,
                PassengerSwitchPacket::encode,
                PassengerSwitchPacket::decode,
                PassengerSwitchPacket::handle
        );
    }
}
