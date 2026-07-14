package com.sshakusora.riautomobility.editor.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.IntFunction;

final class VehiclePositionDropdown {
    enum Type { WHEEL, SEAT, COLLISION }

    private static final int MAX_VISIBLE_ROWS = 5;
    private static final int ROW_HEIGHT = 20;
    private static final int SCROLLBAR_WIDTH = 7;
    private static final int MIN_THUMB_HEIGHT = 10;
    private static final double OPEN_ANIMATION_MS = 150.0D;
    private static final double CLOSE_ANIMATION_MS = 120.0D;
    private static final double SCROLL_ANIMATION_MS = 140.0D;

    private final Type type;
    private final int headerX;
    private final int headerY;
    private final int width;
    private final int size;
    private final int selectedIndex;
    private final IntFunction<Component> optionLabel;
    private final Consumer<Integer> select;
    private final Consumer<Integer> scrollSink;
    private int scrollIndex;
    private double displayedScroll;
    private double scrollAnimationFrom;
    private long scrollAnimationStartedAt;
    private double openAmount;
    private long transitionUpdatedAt;
    private boolean closing;
    private boolean draggingScrollbar;
    private double thumbDragOffset;
    private Runnable afterClose;

    VehiclePositionDropdown(Type type, int headerX, int headerY, int width,
                            int size, int selectedIndex, IntFunction<Component> optionLabel,
                            Consumer<Integer> select, int initialScroll,
                            Consumer<Integer> scrollSink) {
        this.type = type;
        this.headerX = headerX;
        this.headerY = headerY;
        this.width = width;
        this.size = size;
        this.selectedIndex = selectedIndex;
        this.optionLabel = optionLabel;
        this.select = select;
        this.scrollSink = scrollSink;
        scrollIndex = Math.max(0, Math.min(maxScrollIndex(), initialScroll));
        if (selectedIndex < scrollIndex) scrollIndex = selectedIndex;
        else if (selectedIndex >= scrollIndex + visibleRows()) {
            scrollIndex = selectedIndex - visibleRows() + 1;
        }
        displayedScroll = scrollIndex;
        scrollAnimationFrom = displayedScroll;
        long now = Util.getMillis();
        scrollAnimationStartedAt = now;
        transitionUpdatedAt = now;
        scrollSink.accept(scrollIndex);
    }

    Type type() {
        return type;
    }

    void render(GuiGraphics graphics, int mouseX, int mouseY) {
        long now = Util.getMillis();
        updateTransition(now);
        updateDisplayedScroll(now);
        int animatedHeight = (int)Math.ceil(popupHeight() * smoothStep(openAmount));
        if (animatedHeight <= 0) return;

        int x = headerX;
        int y = popupY();
        int height = popupHeight();
        int contentRight = x + width - (hasOverflow() ? SCROLLBAR_WIDTH : 1);
        int contentBottom = y + 1 + visibleRows() * ROW_HEIGHT;
        var font = Minecraft.getInstance().font;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        graphics.enableScissor(x, y, x + width, y + animatedHeight);
        graphics.fill(x, y, x + width, y + height, 0xFF69717C);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF15191E);

        graphics.enableScissor(x + 1, y + 1, contentRight, contentBottom);
        int firstIndex = (int)Math.floor(displayedScroll);
        double fractionalScroll = displayedScroll - firstIndex;
        for (int row = 0; row <= visibleRows(); row++) {
            int index = firstIndex + row;
            if (index < 0 || index >= size) continue;
            int rowY = y + 1 + row * ROW_HEIGHT - (int)Math.round(fractionalScroll * ROW_HEIGHT);
            boolean hovered = mouseX >= x + 1 && mouseX < contentRight
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            int color = index == selectedIndex
                    ? (hovered ? 0xFF4B9C68 : 0xFF34754D)
                    : (hovered ? 0xFF353B43 : 0xFF20252B);
            graphics.fill(x + 1, rowY, contentRight, rowY + ROW_HEIGHT, color);
            graphics.fill(x + 1, rowY, contentRight, rowY + 1, 0xFF454C55);
            Component label = optionLabel.apply(index);
            graphics.drawString(font, font.plainSubstrByWidth(label.getString(), contentRight - x - 8),
                    x + 4, rowY + 6, 0xFFE6E9ED, false);
        }
        graphics.disableScissor();

        if (hasOverflow()) renderScrollbar(graphics);
        graphics.disableScissor();
        graphics.pose().popPose();
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int trackX = headerX + width - SCROLLBAR_WIDTH;
        int trackY = popupY() + 1;
        int trackHeight = visibleRows() * ROW_HEIGHT;
        int thumbHeight = thumbHeight(trackHeight);
        int thumbY = trackY + (int)Math.round(displayedScroll / maxScrollIndex()
                * (trackHeight - thumbHeight));
        graphics.fill(trackX, trackY, headerX + width - 1, trackY + trackHeight, 0xFF30353B);
        graphics.fill(trackX + 1, thumbY, headerX + width - 2, thumbY + thumbHeight, 0xFFAEB7C2);
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isOverPopup(mouseX, mouseY)) return false;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        if (hasOverflow() && mouseX >= headerX + width - SCROLLBAR_WIDTH) {
            int trackY = popupY() + 1;
            int trackHeight = visibleRows() * ROW_HEIGHT;
            int thumbHeight = thumbHeight(trackHeight);
            int thumbY = trackY + (int)Math.round(displayedScroll / maxScrollIndex()
                    * (trackHeight - thumbHeight));
            if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
                thumbDragOffset = mouseY - thumbY;
            } else {
                thumbDragOffset = thumbHeight * 0.5D;
                updateScrollFromThumb(mouseY, trackY, trackHeight, thumbHeight);
            }
            draggingScrollbar = true;
            return true;
        }
        int index = (int)Math.floor(displayedScroll + (mouseY - popupY() - 1) / ROW_HEIGHT);
        if (index >= 0 && index < size) select.accept(index);
        return true;
    }

    boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!draggingScrollbar || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        int trackY = popupY() + 1;
        int trackHeight = visibleRows() * ROW_HEIGHT;
        updateScrollFromThumb(mouseY, trackY, trackHeight, thumbHeight(trackHeight));
        return true;
    }

    boolean mouseReleased(int button) {
        if (!draggingScrollbar || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        draggingScrollbar = false;
        return true;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isOverPopup(mouseX, mouseY)) return false;
        if (hasOverflow() && amount != 0.0D) {
            setScrollIndex(scrollIndex - (amount > 0.0D ? 1 : -1));
        }
        return true;
    }

    private void updateScrollFromThumb(double mouseY, int trackY, int trackHeight, int thumbHeight) {
        double thumbPosition = mouseY - thumbDragOffset - trackY;
        double ratio = thumbPosition / Math.max(1, trackHeight - thumbHeight);
        setScrollIndex((int)Math.round(ratio * maxScrollIndex()));
    }

    private void setScrollIndex(int value) {
        int clamped = Math.max(0, Math.min(maxScrollIndex(), value));
        if (clamped == scrollIndex) return;
        long now = Util.getMillis();
        updateDisplayedScroll(now);
        scrollAnimationFrom = displayedScroll;
        scrollAnimationStartedAt = now;
        scrollIndex = clamped;
        scrollSink.accept(scrollIndex);
    }

    private void updateDisplayedScroll(long now) {
        double progress = Math.max(0.0D, Math.min(1.0D,
                (now - scrollAnimationStartedAt) / SCROLL_ANIMATION_MS));
        double eased = smoothStep(progress);
        displayedScroll = scrollAnimationFrom + (scrollIndex - scrollAnimationFrom) * eased;
    }

    private void updateTransition(long now) {
        long elapsed = Math.max(0L, now - transitionUpdatedAt);
        transitionUpdatedAt = now;
        double duration = closing ? CLOSE_ANIMATION_MS : OPEN_ANIMATION_MS;
        openAmount = Math.max(0.0D, Math.min(1.0D,
                openAmount + (closing ? -1.0D : 1.0D) * elapsed / duration));
    }

    void close(Runnable callback) {
        if (callback != null) {
            Runnable previous = afterClose;
            afterClose = previous == null ? callback : () -> {
                previous.run();
                callback.run();
            };
        }
        closing = true;
        draggingScrollbar = false;
        transitionUpdatedAt = Util.getMillis();
    }

    boolean isClosing() {
        return closing;
    }

    boolean isClosed() {
        updateTransition(Util.getMillis());
        return closing && openAmount <= 0.0D;
    }

    void finishClose() {
        Runnable callback = afterClose;
        afterClose = null;
        if (callback != null) callback.run();
    }

    private int popupY() {
        return headerY + ROW_HEIGHT;
    }

    private int popupHeight() {
        return visibleRows() * ROW_HEIGHT + 2;
    }

    private int visibleRows() {
        return Math.max(1, Math.min(MAX_VISIBLE_ROWS, size));
    }

    private int maxScrollIndex() {
        return Math.max(0, size - visibleRows());
    }

    private boolean hasOverflow() {
        return maxScrollIndex() > 0;
    }

    private int thumbHeight(int trackHeight) {
        return Math.max(MIN_THUMB_HEIGHT,
                Math.round((float)visibleRows() / size * trackHeight));
    }

    boolean isOverHeader(double mouseX, double mouseY) {
        return mouseX >= headerX && mouseX < headerX + width
                && mouseY >= headerY && mouseY < headerY + ROW_HEIGHT;
    }

    private boolean isOverPopup(double mouseX, double mouseY) {
        updateTransition(Util.getMillis());
        int visibleHeight = (int)Math.ceil(popupHeight() * smoothStep(openAmount));
        return mouseX >= headerX && mouseX < headerX + width
                && mouseY >= popupY() && mouseY < popupY() + visibleHeight;
    }

    private static double smoothStep(double value) {
        return value * value * (3.0D - 2.0D * value);
    }
}
