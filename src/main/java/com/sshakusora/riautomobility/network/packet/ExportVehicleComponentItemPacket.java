package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.editor.VehicleImportMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ExportVehicleComponentItemPacket(int containerId, String target, ResourceLocation componentId,
                                               String displayName, String author) {
    private static final int MAX_DISPLAY_NAME_LENGTH = 80;

    public static void encode(ExportVehicleComponentItemPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.containerId);
        buffer.writeUtf(message.target, 16);
        buffer.writeResourceLocation(message.componentId);
        buffer.writeUtf(message.displayName, MAX_DISPLAY_NAME_LENGTH);
        buffer.writeUtf(message.author, CarPackArchiveStore.MAX_AUTHOR_LENGTH);
    }

    public static ExportVehicleComponentItemPacket decode(FriendlyByteBuf buffer) {
        return new ExportVehicleComponentItemPacket(
                buffer.readVarInt(), buffer.readUtf(16), buffer.readResourceLocation(),
                buffer.readUtf(MAX_DISPLAY_NAME_LENGTH), buffer.readUtf(CarPackArchiveStore.MAX_AUTHOR_LENGTH));
    }

    public static void handle(ExportVehicleComponentItemPacket message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> {
                if (player.containerMenu instanceof VehicleImportMenu menu
                        && menu.containerId == message.containerId) {
                    menu.exportItem(message.target, message.componentId, message.displayName, message.author);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
