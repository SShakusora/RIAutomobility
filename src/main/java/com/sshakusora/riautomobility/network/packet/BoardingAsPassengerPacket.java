package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.definition.RIAutomobileRegistry;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

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
            int seatCount = RIAutomobileRegistry.get(((AutomobileEntity) entity).getFrame()).seats().size();
            if(entity.getPassengers().size() < seatCount) {
                player.startRiding(entity, true);
            } else {
                for(Entity e : entity.getPassengers()){
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
