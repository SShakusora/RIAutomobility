package com.sshakusora.riautomobility.editor;

import com.sshakusora.riautomobility.editor.client.VehicleEditorDraft;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import io.github.foundationgames.automobility.item.AutomobilityItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class VehicleImportMenu extends AbstractContainerMenu {
    private static final int MAX_EXPORTED_NAME_LENGTH = 80;
    public static final int OUTPUT_SLOT_X = 259;
    public static final int OUTPUT_SLOT_Y = 256;
    public static final int INVENTORY_X = 70;
    public static final int INVENTORY_Y = 218;
    private static final int OUTPUT_SLOT = 0;
    private static final int PLAYER_INVENTORY_START = 1;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_SLOTS_END = PLAYER_HOTBAR_START + 9;

    private final ContainerLevelAccess access;
    private final boolean canPublish;
    private final SimpleContainer output = new SimpleContainer(1);
    private boolean slotsActive = true;

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
        this.addSlot(new Slot(this.output, 0, OUTPUT_SLOT_X, OUTPUT_SLOT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean isActive() { return VehicleImportMenu.this.slotsActive; }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(this.playerSlot(inventory, column + row * 9 + 9,
                        INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(this.playerSlot(inventory, column, INVENTORY_X + column * 18, INVENTORY_Y + 58));
        }
    }

    private Slot playerSlot(Inventory inventory, int index, int x, int y) {
        return new Slot(inventory, index, x, y) {
            @Override public boolean isActive() { return VehicleImportMenu.this.slotsActive; }
        };
    }

    public void setSlotsActive(boolean active) {
        this.slotsActive = active;
    }

    public boolean canPublish() {
        return this.canPublish;
    }

    public boolean hasOutputItem() {
        return !this.output.getItem(0).isEmpty();
    }

    public void exportItem(String target, ResourceLocation componentId, String displayName) {
        if (!this.canPublish || this.hasOutputItem()
                || !VehicleEditorDraft.GENERATED_NAMESPACE.equals(componentId.getNamespace())
                || !componentId.getPath().matches(VehicleEditorDraft.GENERATED_COMPONENT_PREFIX + "[0-9a-f]{32}")
                || displayName == null || displayName.isBlank()
                || displayName.length() > MAX_EXPORTED_NAME_LENGTH
                || displayName.chars().anyMatch(Character::isISOControl)) {
            return;
        }
        ItemStack stack = switch (target) {
            case "frame" -> {
                AutomobileFrame frame = AutomobileFrame.REGISTRY.getOrDefault(componentId);
                yield frame.isEmpty() || !frame.getId().equals(componentId)
                        ? ItemStack.EMPTY : AutomobilityItems.AUTOMOBILE_FRAME.require().createStack(frame);
            }
            case "wheel" -> {
                AutomobileWheel wheel = AutomobileWheel.REGISTRY.getOrDefault(componentId);
                yield wheel.isEmpty() || !wheel.getId().equals(componentId)
                        ? ItemStack.EMPTY : AutomobilityItems.AUTOMOBILE_WHEEL.require().createStack(wheel);
            }
            case "engine" -> {
                AutomobileEngine engine = AutomobileEngine.REGISTRY.getOrDefault(componentId);
                yield engine.isEmpty() || !engine.getId().equals(componentId)
                        ? ItemStack.EMPTY : AutomobilityItems.AUTOMOBILE_ENGINE.require().createStack(engine);
            }
            default -> ItemStack.EMPTY;
        };
        if (!stack.isEmpty()) {
            stack.setCount(1);
            stack.setHoverName(Component.literal(displayName).withStyle(style -> style.withItalic(false)));
            this.output.setItem(0, stack);
            this.broadcastChanges();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, VehicleImportRegistries.VEHICLE_IMPORT_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == OUTPUT_SLOT) {
            if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_SLOTS_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
        } else if (index < PLAYER_HOTBAR_START) {
            if (!this.moveItemStackTo(stack, PLAYER_HOTBAR_START, PLAYER_SLOTS_END, false)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) this.clearContainer(player, this.output);
    }
}
