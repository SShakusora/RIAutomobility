package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.editor.VehicleImportGuiAtlas;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

final class VehicleEditBox extends EditBox {
    VehicleEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        VehicleImportGuiAtlas.Sprite sprite = !active
                ? VehicleImportGuiAtlas.Sprite.INPUT_DISABLED
                : isFocused()
                ? VehicleImportGuiAtlas.Sprite.INPUT_FOCUSED
                : VehicleImportGuiAtlas.Sprite.INPUT_NORMAL;
        VehicleGuiTextures.blitNineSliced(graphics, sprite, getX(), getY(), width, height);

        int originalX = getX();
        int originalY = getY();
        int originalWidth = width;
        int originalHeight = height;
        setBordered(false);
        setX(originalX + 4);
        setY(originalY + (originalHeight - 8) / 2);
        width = Math.max(1, originalWidth - 8);
        height = 8;
        try {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        } finally {
            setX(originalX);
            setY(originalY);
            width = originalWidth;
            height = originalHeight;
            setBordered(true);
        }
    }
}
