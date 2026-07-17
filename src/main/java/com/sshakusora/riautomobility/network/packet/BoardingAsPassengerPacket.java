package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
            if (!(entity instanceof RIAutomobileEntity automobile)) return;
            if (!player.isAlive() || player.isSpectator() || !automobile.isAlive()) return;
            if (player.getVehicle() == automobile) return;
            if (!RIAutomobileFrame.isRIAutomobileFrame(automobile.getFrame())) return;

            double reach = player.getEntityReach() + 1.0D;
            if (player.distanceToSqr(automobile) > reach * reach) return;
            automobile.boardAsPassenger(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
