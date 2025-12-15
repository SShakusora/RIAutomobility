package com.sshakusora.riautomobility.Network.packet;

import com.sshakusora.riautomobility.entity.DriverSeatEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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

            if(entity instanceof DriverSeatEntity) {
                AutomobileEntity auto = (AutomobileEntity) entity.getVehicle();
                auto.interact(player, InteractionHand.MAIN_HAND);
            }

            if(entity instanceof AutomobileEntity auto && RIAutomobileFrame.isRIAutomobileFrame(auto.getFrame())) {
                Entity seat = auto.getFirstPassenger();
                Entity driver = seat.getFirstPassenger();
                if(driver == null) {
                    player.startRiding(seat, true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
