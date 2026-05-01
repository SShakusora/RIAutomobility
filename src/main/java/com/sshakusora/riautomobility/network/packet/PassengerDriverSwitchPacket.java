package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PassengerDriverSwitchPacket {
    public PassengerDriverSwitchPacket() {}

    public static void encode(PassengerDriverSwitchPacket msg, FriendlyByteBuf buf) {}

    public static PassengerDriverSwitchPacket decode(FriendlyByteBuf buf) {return new PassengerDriverSwitchPacket();}

    public static void handle(PassengerDriverSwitchPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Entity entity = player.getVehicle();
            if (entity == null) return;

            if(entity instanceof RIAutomobileEntity auto && RIAutomobileFrame.isRIAutomobileFrame(auto.getFrame())) {
                auto.cycleSeat(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
