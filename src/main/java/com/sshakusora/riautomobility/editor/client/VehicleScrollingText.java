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

    static void renderCentered(GuiGraphics graphics, Font font, Component text,
                               int x, int y, int width, int height, int color, float scale) {
        renderCentered(graphics, font, text, x, y, width, height, color, scale, false);
    }

    static void renderCentered(GuiGraphics graphics, Font font, Component text,
                               int x, int y, int width, int height, int color, float scale,
                               boolean shadow) {
        int availableWidth = Math.max(1, (int) (width / scale));
        int textWidth = font.width(text);
        int overflow = Math.max(0, textWidth - availableWidth);
        int offset = 0;
        if (overflow > 0) {
            double time = Util.getMillis() / 1000.0D;
            double duration = Math.max(overflow * scale * 0.5D, 3.0D);
            double progress = Math.sin((Math.PI / 2.0D)
                    * Math.cos((Math.PI * 2.0D) * time / duration)) / 2.0D + 0.5D;
            offset = (int) Mth.lerp(progress, 0.0D, overflow);
        }

        int textX = overflow == 0 ? (availableWidth - textWidth) / 2 : -offset;
        float textY = (height - font.lineHeight * scale) / 2.0F;
        graphics.enableScissor(x, y, x + width, y + height);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y + textY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, textX, 0, color, shadow);
        graphics.pose().popPose();
        graphics.disableScissor();
    }
}
