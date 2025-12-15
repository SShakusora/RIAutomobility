package com.sshakusora.riautomobility.Network.packet;

import com.sshakusora.riautomobility.entity.DriverSeatEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.util.RIAutomobileSeatRegistry;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.sql.Driver;
import java.util.List;
import java.util.function.Supplier;

public class BoardingAsPassengerPacket {
    private final int entityId;

    public BoardingAsPassengerPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(BoardingAsPassengerPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
    }

    public static BoardingAsPassengerPacket decode(FriendlyByteBuf buf) {
        return new BoardingAsPassengerPacket(buf.readInt());
    }

    public static void handle(BoardingAsPassengerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Entity entity = player.level().getEntity(msg.entityId);
            if (entity == null) return;
            if (entity == player) return;
            if (player.getVehicle() == entity) return;
            List<RIAutomobileSeatRegistry.SeatPos> seats = RIAutomobileSeatRegistry.getSeats(((AutomobileEntity) entity).getFrame());
            if(entity.getPassengers().size() < seats.size()) {
                player.startRiding(entity, true);
            } else {
                for(Entity e : entity.getPassengers()){
                    if(e instanceof DriverSeatEntity) continue;
                    if(e instanceof Player) continue;

                    e.stopRiding();
                    player.startRiding(entity, true);
                    break;
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
