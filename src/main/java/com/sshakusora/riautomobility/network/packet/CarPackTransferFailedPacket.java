package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.network.packet.client.ClientCarPackSynchronizer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CarPackTransferFailedPacket(String reason) {
    public CarPackTransferFailedPacket {
        reason = reason == null || reason.isBlank() ? "Unknown car pack transfer error" : reason;
        if (reason.length() > 512) {
            reason = reason.substring(0, 512);
        }
    }

    public static void encode(CarPackTransferFailedPacket message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.reason, 512);
    }

    public static CarPackTransferFailedPacket decode(FriendlyByteBuf buffer) {
        return new CarPackTransferFailedPacket(buffer.readUtf(512));
    }

    public static void handle(CarPackTransferFailedPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCarPackSynchronizer.failFromServer(message.reason));
        contextSupplier.get().setPacketHandled(true);
    }
}
