package com.sshakusora.riautomobility.editor.client;

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
        int border = selected ? 0xFF62C778 : (isHoveredOrFocused() ? 0xFFAEB7C2 : 0xFF5A6068);
        int background = isHoveredOrFocused() ? 0xFF454B53 : 0xFF30343A;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, background);
        graphics.renderItem(stack, getX() + 4, getY() + 4);
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
