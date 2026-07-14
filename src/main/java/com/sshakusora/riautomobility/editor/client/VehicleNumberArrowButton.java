package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.editor.VehicleImportGuiAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class VehicleNumberArrowButton extends AbstractButton {
    private final Runnable action;

    VehicleNumberArrowButton(int x, int y, int width, int height, Component message, Runnable action) {
        super(x, y, width, height, message);
        this.action = action;
    }

    @Override public void onPress() {
        action.run();
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean fine = Screen.hasShiftDown();
        VehicleImportGuiAtlas.Sprite sprite = !active
                ? VehicleImportGuiAtlas.Sprite.BUTTON_DISABLED
                : fine
                ? VehicleImportGuiAtlas.Sprite.BUTTON_FINE
                : isHoveredOrFocused()
                ? VehicleImportGuiAtlas.Sprite.BUTTON_HOVERED
                : VehicleImportGuiAtlas.Sprite.BUTTON_NORMAL;
        VehicleGuiTextures.blitNineSliced(graphics, sprite, getX(), getY(), width, height);
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                getX() + width / 2, getY() + (height - 8) / 2, active ? 0xFFFFFF : 0xA0A0A0);
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
