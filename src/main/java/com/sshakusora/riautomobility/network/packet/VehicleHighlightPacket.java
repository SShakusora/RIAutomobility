package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.network.packet.client.VehicleHighlightClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class VehicleHighlightPacket {
    private final UUID automobileId;
    private final int durationTicks;

    public VehicleHighlightPacket(UUID automobileId, int durationTicks) {
        this.automobileId = automobileId;
        this.durationTicks = Math.max(1, Math.min(durationTicks, 1200));
    }

    public static void encode(VehicleHighlightPacket message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.automobileId);
        buffer.writeVarInt(message.durationTicks);
    }

    public static VehicleHighlightPacket decode(FriendlyByteBuf buffer) {
        return new VehicleHighlightPacket(buffer.readUUID(), buffer.readVarInt());
    }

    public static void handle(VehicleHighlightPacket message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                VehicleHighlightClientHandler.handle(message.automobileId, message.durationTicks)));
        context.get().setPacketHandled(true);
    }
}
