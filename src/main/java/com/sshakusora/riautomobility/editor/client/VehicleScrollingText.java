package com.sshakusora.riautomobility.editor.client;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

final class VehicleScrollingText {
    private VehicleScrollingText() {
    }

    static void renderLeftAligned(GuiGraphics graphics, Font font, Component text,
                                  int x, int y, int width, int color) {
        int textWidth = font.width(text);
        if (textWidth <= width) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }

        int overflow = textWidth - width;
        double time = Util.getMillis() / 1000.0D;
        double duration = Math.max(overflow * 0.5D, 3.0D);
        double progress = Math.sin((Math.PI / 2.0D)
                * Math.cos((Math.PI * 2.0D) * time / duration)) / 2.0D + 0.5D;
        int offset = (int)Mth.lerp(progress, 0.0D, overflow);
        graphics.enableScissor(x, y - 1, x + width, y + font.lineHeight + 1);
        graphics.drawString(font, text, x - offset, y, color, false);
        graphics.disableScissor();
    }
}
