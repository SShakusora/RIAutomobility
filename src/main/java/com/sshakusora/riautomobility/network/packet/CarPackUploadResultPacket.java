package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.network.packet.client.ClientCarPackUploader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record CarPackUploadResultPacket(UUID uploadId, boolean successful, String detail) {
    public CarPackUploadResultPacket {
        detail = detail == null ? "" : detail.substring(0, Math.min(detail.length(), 512));
    }
    public static void encode(CarPackUploadResultPacket message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.uploadId); buffer.writeBoolean(message.successful); buffer.writeUtf(message.detail, 512);
    }
    public static CarPackUploadResultPacket decode(FriendlyByteBuf buffer) {
        return new CarPackUploadResultPacket(buffer.readUUID(), buffer.readBoolean(), buffer.readUtf(512));
    }
    public static void handle(CarPackUploadResultPacket message, Supplier<NetworkEvent.Context> supplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCarPackUploader.acceptResult(message));
        supplier.get().setPacketHandled(true);
    }
}
