package com.sshakusora.riautomobility.editor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

final class VehiclePageButton extends Button {
    VehiclePageButton(int x, int y, Component message, OnPress onPress) {
        super(x, y, 48, 25, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean selected = !active;
        VehicleGuiTextures.blitPageButton(graphics, getX(), getY(), selected);
        int color = (selected ? 0xFFFFFF : 0xA0A0A0) | Mth.ceil(alpha * 255.0F) << 24;
        renderString(graphics, Minecraft.getInstance().font, color);
    }
}
