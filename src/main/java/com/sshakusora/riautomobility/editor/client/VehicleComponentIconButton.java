package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.editor.VehicleImportGuiAtlas;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.world.item.ItemStack;

final class VehicleComponentIconButton extends AbstractButton {
    static final int SIZE = 24;

    private final ItemStack stack;
    private final boolean selected;
    private final Runnable action;

    VehicleComponentIconButton(int x, int y, ItemStack stack, boolean selected, Runnable action) {
        super(x, y, SIZE, SIZE, stack.getHoverName());
        this.stack = stack;
        this.selected = selected;
        this.action = action;
    }

    ItemStack stack() {
        return stack;
    }

    @Override public void onPress() {
        action.run();
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        VehicleImportGuiAtlas.Sprite sprite = !active
                ? VehicleImportGuiAtlas.Sprite.ICON_DISABLED
                : selected
                ? VehicleImportGuiAtlas.Sprite.ICON_SELECTED
                : isHoveredOrFocused()
                ? VehicleImportGuiAtlas.Sprite.ICON_HOVERED
                : VehicleImportGuiAtlas.Sprite.ICON_NORMAL;
        VehicleGuiTextures.blit(graphics, sprite, getX(), getY(), width, height);
        graphics.renderItem(stack, getX() + 4, getY() + 4);
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
