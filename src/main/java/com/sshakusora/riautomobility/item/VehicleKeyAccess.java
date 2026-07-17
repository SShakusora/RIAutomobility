package com.sshakusora.riautomobility.item;

import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.UUID;

public final class VehicleKeyAccess {
    private VehicleKeyAccess() {
    }

    public static boolean canAccess(Player player, RIAutomobileEntity automobile) {
        return !automobile.isKeyed() || canBypass(player) || hasMatchingKey(player, automobile);
    }

    public static boolean canBypass(Player player) {
        return player.isCreative()
                || player instanceof ServerPlayer serverPlayer
                && serverPlayer.createCommandSourceStack().hasPermission(2);
    }

    public static boolean hasMatchingKey(Player player, RIAutomobileEntity automobile) {
        return findMatchingKey(player.getInventory(), automobile) != null;
    }

    @Nullable
    public static ItemStack findMatchingKey(Inventory inventory, RIAutomobileEntity automobile) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (VehicleKeyItem.isBoundTo(stack, automobile.getUUID())) {
                return stack;
            }
        }
        return null;
    }

    public static boolean tryBindOffhandKey(@Nullable Player player, RIAutomobileEntity automobile) {
        if (player == null) {
            return false;
        }
        ItemStack offhand = player.getOffhandItem();
        if (!VehicleKeyItem.isBlankKey(offhand)) {
            return false;
        }
        VehicleKeyItem.bind(offhand, automobile.getUUID(), automobile.getCustomName());
        automobile.setKeyed(true);
        player.getInventory().setChanged();
        return true;
    }

    public static void updateVehicleName(MinecraftServer server, RIAutomobileEntity automobile) {
        UUID automobileId = automobile.getUUID();
        Component automobileName = automobile.getCustomName();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (updateMatchingKeys(player.getInventory(), automobileId, automobileName)) {
                player.getInventory().setChanged();
                player.inventoryMenu.broadcastChanges();
            }
        }

        for (var level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity itemEntity) {
                    ItemStack stack = itemEntity.getItem();
                    if (VehicleKeyItem.isBoundTo(stack, automobileId)) {
                        ItemStack updated = stack.copy();
                        VehicleKeyItem.updateVehicleName(updated, automobileName);
                        itemEntity.setItem(updated);
                    }
                } else if (entity instanceof RIAutomobileEntity loadedAutomobile) {
                    if (updateMatchingKeys(loadedAutomobile, automobileId, automobileName)) {
                        loadedAutomobile.setChanged();
                    }
                }
            }
        }
    }

    private static boolean updateMatchingKeys(Container container, UUID automobileId,
                                              @Nullable Component automobileName) {
        boolean changed = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (VehicleKeyItem.isBoundTo(stack, automobileId)) {
                VehicleKeyItem.updateVehicleName(stack, automobileName);
                changed = true;
            }
        }
        return changed;
    }

    public static boolean resetOneMatchingKey(Player player, RIAutomobileEntity automobile) {
        ItemStack matching = findMatchingKey(player.getInventory(), automobile);
        if (matching == null) {
            return false;
        }
        VehicleKeyItem.clearBinding(matching);
        player.getInventory().setChanged();
        return true;
    }

    public static void deny(Player player) {
        if (!player.level().isClientSide()) {
            player.displayClientMessage(Component.translatable("message.riautomobility.vehicle_key.denied"), true);
        }
    }
}
