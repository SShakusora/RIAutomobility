package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.entity.SeatEntity;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PassengerSwitchPacket {
    public PassengerSwitchPacket() {}

    public static void encode(PassengerSwitchPacket msg, FriendlyByteBuf buf) {}

    public static PassengerSwitchPacket decode(FriendlyByteBuf buf) {return new PassengerSwitchPacket();}

    public static void handle(PassengerSwitchPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Entity entity = player.getVehicle();
            if (entity == null) return;
            if(!(entity instanceof SeatEntity seat)) return;

            AutomobileEntity auto = seat.getAutomobile();
            if(auto == null) return;

            int index = auto.getPassengers().indexOf(seat);
            if (index == -1) return;

            int seatCount = auto.getPassengers().size();
            SeatEntity targetSeat = null;
            for (int i = 1; i <= seatCount; i++) {
                int nextIndex = (index + i) % seatCount;
                SeatEntity nextSeat = (SeatEntity) auto.getPassengers().get(nextIndex);

                if (nextSeat.getPassengers().isEmpty()) {
                    targetSeat = nextSeat;
                    break;
                }
            }

            if (targetSeat == null) return;
            player.startRiding(targetSeat, true);
        });
        ctx.get().setPacketHandled(true);
    }
}
