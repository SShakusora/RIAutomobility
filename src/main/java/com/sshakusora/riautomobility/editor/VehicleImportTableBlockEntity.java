package com.sshakusora.riautomobility.editor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class VehicleImportTableBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final String EDITOR_STATE_TAG = "EditorState";
    private static final int MAX_EDITOR_STATE_TEXT_LENGTH = 32_768;
    private static final Component TITLE = Component.translatable("container.riautomobility.vehicle_import");

    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private CompoundTag editorState = new CompoundTag();

    public VehicleImportTableBlockEntity(BlockPos pos, BlockState state) {
        super(VehicleImportRegistries.VEHICLE_IMPORT_TABLE_BLOCK_ENTITY.get(), pos, state);
    }

    public CompoundTag getEditorState() {
        return this.editorState.copy();
    }

    public void setEditorState(CompoundTag state) {
        if (state == null || state.toString().length() > MAX_EDITOR_STATE_TEXT_LENGTH) return;
        this.editorState = state.copy();
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
        if (!this.editorState.isEmpty()) tag.put(EDITOR_STATE_TAG, this.editorState.copy());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items);
        CompoundTag loadedState = tag.getCompound(EDITOR_STATE_TAG);
        this.editorState = loadedState.toString().length() <= MAX_EDITOR_STATE_TEXT_LENGTH
                ? loadedState.copy() : new CompoundTag();
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new VehicleImportMenu(containerId, inventory, this, player.hasPermissions(2));
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.items.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) this.setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(this.items, slot);
        if (!result.isEmpty()) this.setChanged();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) stack.setCount(this.getMaxStackSize());
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) return false;
        return player.distanceToSqr(
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }
}
