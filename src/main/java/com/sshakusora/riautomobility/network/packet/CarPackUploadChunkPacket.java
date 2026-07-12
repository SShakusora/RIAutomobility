package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.editor.upload.CarPackUploadService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record CarPackUploadChunkPacket(UUID uploadId, int index, byte[] data) {
    public static final int MAX_CHUNK_SIZE = 256 * 1024;

    public CarPackUploadChunkPacket {
        if (index < 0 || data == null || data.length == 0 || data.length > MAX_CHUNK_SIZE) {
            throw new IllegalArgumentException("Invalid upload chunk");
        }
    }

    public static void encode(CarPackUploadChunkPacket message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.uploadId);
        buffer.writeVarInt(message.index);
        buffer.writeByteArray(message.data);
    }

    public static CarPackUploadChunkPacket decode(FriendlyByteBuf buffer) {
        return new CarPackUploadChunkPacket(buffer.readUUID(), buffer.readVarInt(), buffer.readByteArray(MAX_CHUNK_SIZE));
    }

    public static void handle(CarPackUploadChunkPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) context.enqueueWork(() -> CarPackUploadService.chunk(player, message));
        context.setPacketHandled(true);
    }
}
