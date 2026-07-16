package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.editor.VehicleImportGuiAtlas;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

final class VehicleVerticalScrollBar extends AbstractWidget {
    private static final int MIN_THUMB_HEIGHT = 12;
    private static final double SCROLL_ANIMATION_MS = 140.0D;

    private final int contentHeight;
    private final int scrollStep;
    private final IntConsumer scrollSink;
    private final DoubleConsumer displayedScrollSink;
    private int scroll;
    private double displayedScroll;
    private double scrollAnimationFrom;
    private long scrollAnimationStartedAt;
    private boolean dragging;
    private double thumbDragOffset;

    VehicleVerticalScrollBar(int x, int y, int width, int height,
                             int contentHeight, int scrollStep, int initialScroll,
                             IntConsumer scrollSink, DoubleConsumer displayedScrollSink) {
        super(x, y, width, height, Component.translatable("gui.riautomobility.scrollbar"));
        this.contentHeight = contentHeight;
        this.scrollStep = scrollStep;
        this.scrollSink = scrollSink;
        this.displayedScrollSink = displayedScrollSink;
        scroll = steppedScroll(initialScroll);
        displayedScroll = scroll;
        scrollAnimationFrom = displayedScroll;
        scrollAnimationStartedAt = Util.getMillis();
        scrollSink.accept(scroll);
        displayedScrollSink.accept(displayedScroll);
        visible = maxScroll() > 0;
    }

    int scroll() {
        return scroll;
    }

    boolean scrollBy(double amount) {
        if (!visible || amount == 0.0D) return false;
        setScroll(scroll - (amount > 0.0D ? scrollStep : -scrollStep));
        return true;
    }

    double displayedScroll() {
        return displayedScroll;
    }

    void updateAnimation() {
        double progress = Math.max(0.0D, Math.min(1.0D,
                (Util.getMillis() - scrollAnimationStartedAt) / SCROLL_ANIMATION_MS));
        double updated = interpolate(scrollAnimationFrom, scroll, progress);
        if (updated == displayedScroll) return;
        displayedScroll = updated;
        displayedScrollSink.accept(displayedScroll);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateAnimation();
        int thumbHeight = thumbHeight();
        int thumbY = getY() + (int) Math.round(displayedScroll / maxScroll() * (height - thumbHeight));
        VehicleGuiTextures.blit(graphics, VehicleImportGuiAtlas.Sprite.SCROLL_TRACK_VERTICAL,
                getX(), getY(), width - 1, height);
        VehicleGuiTextures.blit(graphics, VehicleImportGuiAtlas.Sprite.SCROLL_THUMB_VERTICAL,
                getX() + 1, thumbY, width - 3, thumbHeight);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        int thumbHeight = thumbHeight();
        updateAnimation();
        int thumbY = getY() + (int) Math.round(displayedScroll / maxScroll() * (height - thumbHeight));
        if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
            thumbDragOffset = mouseY - thumbY;
        } else {
            thumbDragOffset = thumbHeight * 0.5D;
            updateFromThumb(mouseY);
        }
        dragging = true;
        setFocused(true);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!active || !visible || !dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        updateFromThumb(mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        dragging = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return isMouseOver(mouseX, mouseY) && scrollBy(amount);
    }

    private void updateFromThumb(double mouseY) {
        int thumbHeight = thumbHeight();
        double ratio = (mouseY - thumbDragOffset - getY()) / Math.max(1, height - thumbHeight);
        int rawScroll = (int) Math.round(ratio * maxScroll() / scrollStep) * scrollStep;
        setScroll(rawScroll);
    }

    private void setScroll(int value) {
        int stepped = steppedScroll(value);
        if (stepped == scroll) return;
        updateAnimation();
        scrollAnimationFrom = displayedScroll;
        scrollAnimationStartedAt = Util.getMillis();
        scroll = stepped;
        scrollSink.accept(scroll);
    }

    private int steppedScroll(int value) {
        int clamped = Math.max(0, Math.min(maxScroll(), value));
        return Math.min(maxScroll(), Math.round((float) clamped / scrollStep) * scrollStep);
    }

    private int maxScroll() {
        int overflow = Math.max(0, contentHeight - height);
        return overflow == 0 ? 0 : ((overflow + scrollStep - 1) / scrollStep) * scrollStep;
    }

    private int thumbHeight() {
        return Math.max(MIN_THUMB_HEIGHT,
                Math.min(height, Math.round((float) height / contentHeight * height)));
    }

    static double interpolate(double from, double to, double progress) {
        double clamped = Math.max(0.0D, Math.min(1.0D, progress));
        double eased = clamped * clamped * (3.0D - 2.0D * clamped);
        return from + (to - from) * eased;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
