package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.editor.VehicleImportGuiAtlas;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;

final class VehicleAttachmentIconList extends AbstractWidget {
    private static final int ICON_SIZE = 16;
    private static final int ICON_STEP = 18;
    private static final int CONTENT_PADDING = 2;
    private static final int SCROLLBAR_HEIGHT = 3;
    private static final int MIN_THUMB_WIDTH = 8;

    private final List<ItemStack> stacks;
    private final Consumer<Double> scrollSink;
    private double scroll;
    private boolean draggingScrollbar;
    private double thumbDragOffset;

    VehicleAttachmentIconList(int x, int y, int width, int height, Component message,
                              List<ItemStack> stacks, double initialScroll,
                              Consumer<Double> scrollSink) {
        super(x, y, width, height, message);
        this.stacks = List.copyOf(stacks);
        this.scrollSink = scrollSink;
        setScroll(initialScroll);
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        VehicleGuiTextures.blitNineSliced(graphics, VehicleImportGuiAtlas.Sprite.ATTACHMENT_LIST,
                getX(), getY(), width, height);

        int iconBottom = getY() + (hasOverflow() ? height - SCROLLBAR_HEIGHT : height - 1);
        graphics.enableScissor(getX() + 1, getY() + 1, getX() + width - 1, iconBottom);
        for (int index = 0; index < stacks.size(); index++) {
            int iconX = getX() + CONTENT_PADDING + index * ICON_STEP - (int)Math.round(scroll);
            if (iconX + ICON_SIZE > getX() + 1 && iconX < getX() + width - 1) {
                graphics.renderItem(stacks.get(index), iconX, getY() + 1);
            }
        }
        graphics.disableScissor();

        if (hasOverflow()) {
            int trackX = getX() + CONTENT_PADDING;
            int trackY = getY() + height - SCROLLBAR_HEIGHT;
            int trackWidth = width - CONTENT_PADDING * 2;
            int thumbWidth = thumbWidth(trackWidth);
            int thumbX = trackX + (int)Math.round(scroll / maxScroll() * (trackWidth - thumbWidth));
            VehicleGuiTextures.blit(graphics, VehicleImportGuiAtlas.Sprite.SCROLL_TRACK_HORIZONTAL,
                    trackX, trackY, trackWidth, SCROLLBAR_HEIGHT - 1);
            VehicleGuiTextures.blit(graphics, VehicleImportGuiAtlas.Sprite.SCROLL_THUMB_HORIZONTAL,
                    thumbX, trackY, thumbWidth, SCROLLBAR_HEIGHT - 1);
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !isOverScrollbar(mouseX, mouseY)) return false;
        int trackX = getX() + CONTENT_PADDING;
        int trackWidth = width - CONTENT_PADDING * 2;
        int thumbWidth = thumbWidth(trackWidth);
        int thumbX = trackX + (int)Math.round(scroll / maxScroll() * (trackWidth - thumbWidth));
        if (mouseX >= thumbX && mouseX < thumbX + thumbWidth) {
            thumbDragOffset = mouseX - thumbX;
        } else {
            thumbDragOffset = thumbWidth * 0.5D;
            updateScrollFromThumb(mouseX, trackX, trackWidth, thumbWidth);
        }
        draggingScrollbar = true;
        setFocused(true);
        return true;
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button,
                                          double dragX, double dragY) {
        if (!active || !draggingScrollbar || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        int trackX = getX() + CONTENT_PADDING;
        int trackWidth = width - CONTENT_PADDING * 2;
        updateScrollFromThumb(mouseX, trackX, trackWidth, thumbWidth(trackWidth));
        return true;
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !draggingScrollbar) return false;
        draggingScrollbar = false;
        return true;
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!active || !isMouseOver(mouseX, mouseY) || !hasOverflow() || amount == 0.0D) return false;
        setScroll(scroll - amount * ICON_STEP);
        return true;
    }

    private void updateScrollFromThumb(double mouseX, int trackX, int trackWidth, int thumbWidth) {
        double thumbPosition = mouseX - thumbDragOffset - trackX;
        setScroll(thumbPosition / Math.max(1, trackWidth - thumbWidth) * maxScroll());
    }

    private void setScroll(double value) {
        scroll = Math.max(0.0D, Math.min(maxScroll(), value));
        scrollSink.accept(scroll);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        return hasOverflow() && mouseX >= getX() && mouseX < getX() + width
                && mouseY >= getY() + height - SCROLLBAR_HEIGHT && mouseY < getY() + height;
    }

    private int contentWidth() {
        return stacks.isEmpty() ? 0 : stacks.size() * ICON_STEP - (ICON_STEP - ICON_SIZE);
    }

    private int viewportWidth() {
        return width - CONTENT_PADDING * 2;
    }

    private double maxScroll() {
        return Math.max(0, contentWidth() - viewportWidth());
    }

    private boolean hasOverflow() {
        return maxScroll() > 0.0D;
    }

    private int thumbWidth(int trackWidth) {
        return Math.max(MIN_THUMB_WIDTH,
                Math.min(trackWidth, Math.round((float)viewportWidth() / contentWidth() * trackWidth)));
    }

    ItemStack hoveredStack(double mouseX, double mouseY) {
        if (!active || mouseX < getX() + 1 || mouseX >= getX() + width - 1
                || mouseY < getY() + 1
                || mouseY >= getY() + (hasOverflow() ? height - SCROLLBAR_HEIGHT : height - 1)) {
            return ItemStack.EMPTY;
        }
        double contentX = mouseX - getX() - CONTENT_PADDING + scroll;
        if (contentX < 0.0D) return ItemStack.EMPTY;
        int index = (int)(contentX / ICON_STEP);
        if (index < 0 || index >= stacks.size() || contentX - index * ICON_STEP >= ICON_SIZE) {
            return ItemStack.EMPTY;
        }
        return stacks.get(index);
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
