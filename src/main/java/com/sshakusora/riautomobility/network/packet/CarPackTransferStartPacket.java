package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.carpack.CarPackManifestEntry;
import com.sshakusora.riautomobility.network.packet.client.ClientCarPackSynchronizer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CarPackTransferStartPacket(CarPackManifestEntry manifest) {
    public static void encode(CarPackTransferStartPacket message, FriendlyByteBuf buffer) {
        message.manifest.write(buffer);
    }

    public static CarPackTransferStartPacket decode(FriendlyByteBuf buffer) {
        return new CarPackTransferStartPacket(CarPackManifestEntry.read(buffer));
    }

    public static void handle(CarPackTransferStartPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCarPackSynchronizer.startTransfer(message.manifest));
        contextSupplier.get().setPacketHandled(true);
    }
}
