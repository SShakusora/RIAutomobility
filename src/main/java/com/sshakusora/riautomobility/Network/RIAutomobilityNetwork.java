package com.sshakusora.riautomobility.Network;

import com.sshakusora.riautomobility.Network.packet.BoardingAsPassengerPacket;
import com.sshakusora.riautomobility.Network.packet.PassengerDriverSwitchPacket;
import com.sshakusora.riautomobility.RIAutomobility;
import io.github.foundationgames.automobility.automobile.attachment.rear.PassengerSeatRearAttachment;
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
