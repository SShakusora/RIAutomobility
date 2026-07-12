package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.editor.upload.CarPackUploadService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record CompleteCarPackUploadPacket(UUID uploadId) {
    public static void encode(CompleteCarPackUploadPacket message, FriendlyByteBuf buffer) { buffer.writeUUID(message.uploadId); }
    public static CompleteCarPackUploadPacket decode(FriendlyByteBuf buffer) { return new CompleteCarPackUploadPacket(buffer.readUUID()); }
    public static void handle(CompleteCarPackUploadPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) context.enqueueWork(() -> CarPackUploadService.complete(player, message.uploadId));
        context.setPacketHandled(true);
    }
}
