package com.sshakusora.riautomobility.editor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public final class VehicleImportMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final boolean canPublish;

    public VehicleImportMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(
                containerId,
                inventory,
                ContainerLevelAccess.create(inventory.player.level(), buffer.readBlockPos()),
                buffer.readBoolean()
        );
    }

    public VehicleImportMenu(int containerId, Inventory inventory, ContainerLevelAccess access, boolean canPublish) {
        super(VehicleImportRegistries.VEHICLE_IMPORT_MENU.get(), containerId);
        this.access = access;
        this.canPublish = canPublish;
    }

    public boolean canPublish() {
        return this.canPublish;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, VehicleImportRegistries.VEHICLE_IMPORT_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
