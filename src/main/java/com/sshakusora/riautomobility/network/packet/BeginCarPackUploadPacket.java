package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.editor.upload.CarPackUploadService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record BeginCarPackUploadPacket(UUID uploadId, String packName, String namespace, String componentPath,
                                      String target, boolean overwrite, long archiveSize, String sha256) {
    public static void encode(BeginCarPackUploadPacket message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.uploadId);
        buffer.writeUtf(message.packName, 96);
        buffer.writeUtf(message.namespace, 64);
        buffer.writeUtf(message.componentPath, 192);
        buffer.writeUtf(message.target, 16);
        buffer.writeBoolean(message.overwrite);
        buffer.writeLong(message.archiveSize);
        buffer.writeUtf(message.sha256, 64);
    }

    public static BeginCarPackUploadPacket decode(FriendlyByteBuf buffer) {
        return new BeginCarPackUploadPacket(buffer.readUUID(), buffer.readUtf(96), buffer.readUtf(64),
                buffer.readUtf(192), buffer.readUtf(16), buffer.readBoolean(), buffer.readLong(), buffer.readUtf(64));
    }

    public static void handle(BeginCarPackUploadPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) context.enqueueWork(() -> CarPackUploadService.begin(player, message));
        context.setPacketHandled(true);
    }
}
