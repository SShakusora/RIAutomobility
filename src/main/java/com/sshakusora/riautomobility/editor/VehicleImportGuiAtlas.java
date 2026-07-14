package com.sshakusora.riautomobility.editor;

public final class VehicleImportGuiAtlas {
    public static final int SIZE = 256;

    private VehicleImportGuiAtlas() {
    }

    public enum Sprite {
        SCREEN(0, 0, 16, 16, 4),
        SIDEBAR(16, 0, 16, 16, 4),
        CONTROLS(32, 0, 16, 16, 4),
        PREVIEW(48, 0, 16, 16, 4),
        SELECTION(64, 0, 16, 16, 4),
        INVENTORY(80, 0, 16, 16, 4),
        ATTACHMENT_LIST(96, 0, 16, 16, 2),
        DROPDOWN(112, 0, 16, 16, 2),
        ROW_NORMAL(128, 0, 16, 16, 2),
        ROW_HOVERED(144, 0, 16, 16, 2),
        ROW_SELECTED(160, 0, 16, 16, 2),
        ROW_SELECTED_HOVERED(176, 0, 16, 16, 2),

        BUTTON_NORMAL(0, 20, 20, 20, 4),
        BUTTON_HOVERED(20, 20, 20, 20, 4),
        BUTTON_DISABLED(40, 20, 20, 20, 4),
        BUTTON_FINE(60, 20, 20, 20, 4),

        INPUT_NORMAL(0, 40, 20, 20, 3),
        INPUT_FOCUSED(20, 40, 20, 20, 3),
        INPUT_DISABLED(40, 40, 20, 20, 3),

        SLOT_NORMAL(0, 60, 18, 18, 0),
        SLOT_OUTPUT(18, 60, 18, 18, 0),

        ICON_NORMAL(0, 80, 24, 24, 0),
        ICON_HOVERED(24, 80, 24, 24, 0),
        ICON_SELECTED(48, 80, 24, 24, 0),
        ICON_DISABLED(72, 80, 24, 24, 0),

        TOGGLE_OFF(0, 104, 38, 16, 0),
        TOGGLE_ON(38, 104, 38, 16, 0),
        TOGGLE_DISABLED(76, 104, 38, 16, 0),

        SCROLL_TRACK_VERTICAL(0, 120, 7, 16, 0),
        SCROLL_THUMB_VERTICAL(8, 120, 7, 16, 0),
        SCROLL_TRACK_HORIZONTAL(16, 120, 16, 4, 0),
        SCROLL_THUMB_HORIZONTAL(16, 125, 16, 4, 0);

        private final int u;
        private final int v;
        private final int width;
        private final int height;
        private final int border;

        Sprite(int u, int v, int width, int height, int border) {
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
            this.border = border;
        }

        public int u() { return u; }
        public int v() { return v; }
        public int width() { return width; }
        public int height() { return height; }
        public int border() { return border; }
    }
}
