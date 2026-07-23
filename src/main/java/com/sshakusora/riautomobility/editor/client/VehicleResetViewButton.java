package com.sshakusora.riautomobility.editor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

final class VehicleResetViewButton extends Button {
    VehicleResetViewButton(int x, int y, Component message, OnPress onPress) {
        super(x, y, 74, 20, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        VehicleGuiTextures.blitResetViewButton(graphics, getX(), getY(), active && isHoveredOrFocused());
        int color = getFGColor() | Mth.ceil(alpha * 255.0F) << 24;
        renderString(graphics, Minecraft.getInstance().font, color);
    }
}
