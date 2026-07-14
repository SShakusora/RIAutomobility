package com.sshakusora.riautomobility.editor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

final class VehicleToggleSliderButton extends AbstractButton {
    private static final int TRACK_WIDTH = 38;
    private static final int TRACK_HEIGHT = 16;

    private final String label;
    private final String falseLabel;
    private final String trueLabel;
    private final BooleanSupplier getter;
    private final Runnable toggle;

    VehicleToggleSliderButton(int x, int y, int width, int height, String label,
                              String falseLabel, String trueLabel,
                              BooleanSupplier getter, Runnable toggle) {
        super(x, y, width, height, Component.empty());
        this.label = label;
        this.falseLabel = falseLabel;
        this.trueLabel = trueLabel;
        this.getter = getter;
        this.toggle = toggle;
        updateMessage();
    }

    @Override public void onPress() {
        toggle.run();
        updateMessage();
    }

    private void updateMessage() {
        setMessage(VehicleImportText.component(label + "." + (getter.getAsBoolean() ? trueLabel : falseLabel)));
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean enabled = getter.getAsBoolean();
        var font = Minecraft.getInstance().font;
        int textColor = active ? 0xFFE6E9ED : 0xFF9A9EA4;
        int trackX = getX() + width - TRACK_WIDTH;
        renderScrollingString(graphics, font,
                VehicleImportText.component(label + "." + (enabled ? trueLabel : falseLabel)),
                getX(), getY(), trackX - 4, getY() + height, textColor);

        int trackY = getY() + (height - TRACK_HEIGHT) / 2;
        int border = isHoveredOrFocused() ? 0xFFAEB7C2 : 0xFF737A84;
        int background = enabled ? 0xFF3E8E5A : 0xFF30343A;
        graphics.fill(trackX, trackY, trackX + TRACK_WIDTH, trackY + TRACK_HEIGHT, border);
        graphics.fill(trackX + 1, trackY + 1, trackX + TRACK_WIDTH - 1,
                trackY + TRACK_HEIGHT - 1, background);

        int knobSize = TRACK_HEIGHT - 4;
        int knobX = enabled ? trackX + TRACK_WIDTH - knobSize - 2 : trackX + 2;
        int knobY = trackY + 2;
        graphics.fill(knobX, knobY, knobX + knobSize, knobY + knobSize,
                active ? 0xFFF1F3F5 : 0xFFB0B3B7);
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
