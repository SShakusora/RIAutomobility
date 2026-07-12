package com.sshakusora.riautomobility.network.packet;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public record CarPackSyncStatusPacket(boolean successful, String detail) {
    private static final Logger LOGGER = LogUtils.getLogger();

    public CarPackSyncStatusPacket {
        detail = detail == null ? "" : detail;
        if (detail.length() > 512) {
            detail = detail.substring(0, 512);
        }
    }

    public static void encode(CarPackSyncStatusPacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.successful);
        buffer.writeUtf(message.detail, 512);
    }

    public static CarPackSyncStatusPacket decode(FriendlyByteBuf buffer) {
        return new CarPackSyncStatusPacket(buffer.readBoolean(), buffer.readUtf(512));
    }

    public static void handle(CarPackSyncStatusPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> {
                if (message.successful) {
                    LOGGER.info("Synchronized RIAutomobility car packs with {}: {}", player.getGameProfile().getName(), message.detail);
                } else {
                    LOGGER.warn("RIAutomobility car pack synchronization failed for {}: {}", player.getGameProfile().getName(), message.detail);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
