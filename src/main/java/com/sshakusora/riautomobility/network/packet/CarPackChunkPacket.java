package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.carpack.CarPackManifestEntry;
import com.sshakusora.riautomobility.network.packet.client.ClientCarPackSynchronizer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CarPackChunkPacket(String archiveDigest, int index, byte[] data) {
    public static final int MAX_CHUNK_SIZE = 256 * 1024;

    public CarPackChunkPacket {
        CarPackManifestEntry.validateDigest(archiveDigest, "archive");
        if (index < 0) {
            throw new IllegalArgumentException("Invalid car pack chunk index: " + index);
        }
        if (data == null || data.length == 0 || data.length > MAX_CHUNK_SIZE) {
            throw new IllegalArgumentException("Invalid car pack chunk size");
        }
    }

    public static void encode(CarPackChunkPacket message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.archiveDigest, 64);
        buffer.writeVarInt(message.index);
        buffer.writeByteArray(message.data);
    }

    public static CarPackChunkPacket decode(FriendlyByteBuf buffer) {
        return new CarPackChunkPacket(buffer.readUtf(64), buffer.readVarInt(), buffer.readByteArray(MAX_CHUNK_SIZE));
    }

    public static void handle(CarPackChunkPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientCarPackSynchronizer.acceptChunk(message.archiveDigest, message.index, message.data));
        contextSupplier.get().setPacketHandled(true);
    }
}
