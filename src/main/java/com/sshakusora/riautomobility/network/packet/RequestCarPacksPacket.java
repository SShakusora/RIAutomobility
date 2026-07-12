package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.carpack.CarPackManifestEntry;
import com.sshakusora.riautomobility.carpack.CarPackTransferSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record RequestCarPacksPacket(List<Request> requests) {
    public RequestCarPacksPacket {
        requests = List.copyOf(requests);
        if (requests.size() > CarPackManifestEntry.MAX_PACKS) {
            throw new IllegalArgumentException("Too many requested car packs: " + requests.size());
        }
    }

    public static void encode(RequestCarPacksPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.requests.size());
        for (Request request : message.requests) {
            buffer.writeUtf(request.id(), CarPackManifestEntry.MAX_ID_LENGTH);
            buffer.writeUtf(request.archiveDigest(), 64);
        }
    }

    public static RequestCarPacksPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > CarPackManifestEntry.MAX_PACKS) {
            throw new IllegalArgumentException("Invalid requested car pack count: " + count);
        }
        List<Request> requests = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            requests.add(new Request(buffer.readUtf(CarPackManifestEntry.MAX_ID_LENGTH), buffer.readUtf(64)));
        }
        return new RequestCarPacksPacket(requests);
    }

    public static void handle(RequestCarPacksPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> CarPackTransferSender.send(player, message.requests));
        }
        context.setPacketHandled(true);
    }

    public record Request(String id, String archiveDigest) {
        public Request {
            if (id == null || !id.startsWith("riautomobility/") || id.length() > CarPackManifestEntry.MAX_ID_LENGTH) {
                throw new IllegalArgumentException("Invalid requested car pack id");
            }
            CarPackManifestEntry.validateDigest(archiveDigest, "archive");
        }
    }
}
