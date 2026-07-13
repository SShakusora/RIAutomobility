package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.editor.VehicleImportMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ExportVehicleComponentItemPacket(int containerId, String target, ResourceLocation componentId) {
    public static void encode(ExportVehicleComponentItemPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.containerId);
        buffer.writeUtf(message.target, 16);
        buffer.writeResourceLocation(message.componentId);
    }

    public static ExportVehicleComponentItemPacket decode(FriendlyByteBuf buffer) {
        return new ExportVehicleComponentItemPacket(
                buffer.readVarInt(), buffer.readUtf(16), buffer.readResourceLocation());
    }

    public static void handle(ExportVehicleComponentItemPacket message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> {
                if (player.containerMenu instanceof VehicleImportMenu menu
                        && menu.containerId == message.containerId) {
                    menu.exportItem(message.target, message.componentId);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
