package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.editor.VehicleImportMenu;
import com.sshakusora.riautomobility.editor.VehicleImportTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpdateVehicleImportDraftPacket(BlockPos pos, CompoundTag editorState) {
    public static void encode(UpdateVehicleImportDraftPacket message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeNbt(message.editorState);
    }

    public static UpdateVehicleImportDraftPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        CompoundTag state = buffer.readNbt();
        return new UpdateVehicleImportDraftPacket(pos, state == null ? new CompoundTag() : state);
    }

    public static void handle(UpdateVehicleImportDraftPacket message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> {
                if (!(player.containerMenu instanceof VehicleImportMenu menu)
                        || !menu.blockPos().equals(message.pos)
                        || !(player.level().getBlockEntity(message.pos)
                        instanceof VehicleImportTableBlockEntity blockEntity)) {
                    return;
                }
                blockEntity.setEditorState(message.editorState);
            });
        }
        context.setPacketHandled(true);
    }
}
