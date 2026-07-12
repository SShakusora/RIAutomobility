package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.carpack.CarPackManifestEntry;
import com.sshakusora.riautomobility.network.packet.client.ClientCarPackSynchronizer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CarPackTransferCompletePacket(String archiveDigest) {
    public CarPackTransferCompletePacket {
        CarPackManifestEntry.validateDigest(archiveDigest, "archive");
    }

    public static void encode(CarPackTransferCompletePacket message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.archiveDigest, 64);
    }

    public static CarPackTransferCompletePacket decode(FriendlyByteBuf buffer) {
        return new CarPackTransferCompletePacket(buffer.readUtf(64));
    }

    public static void handle(CarPackTransferCompletePacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCarPackSynchronizer.completeTransfer(message.archiveDigest));
        contextSupplier.get().setPacketHandled(true);
    }
}
