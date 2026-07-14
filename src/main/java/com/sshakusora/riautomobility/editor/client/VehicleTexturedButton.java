package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.editor.VehicleImportGuiAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

final class VehicleTexturedButton extends Button {
    VehicleTexturedButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        VehicleImportGuiAtlas.Sprite sprite = !active
                ? VehicleImportGuiAtlas.Sprite.BUTTON_DISABLED
                : isHoveredOrFocused()
                ? VehicleImportGuiAtlas.Sprite.BUTTON_HOVERED
                : VehicleImportGuiAtlas.Sprite.BUTTON_NORMAL;
        VehicleGuiTextures.blitNineSliced(graphics, sprite, getX(), getY(), width, height);
        int color = getFGColor() | Mth.ceil(alpha * 255.0F) << 24;
        renderString(graphics, Minecraft.getInstance().font, color);
    }
}
