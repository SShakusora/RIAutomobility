package com.sshakusora.riautomobility.editor.client;

import net.minecraft.network.chat.Component;

final class VehicleImportTooltips {
    private VehicleImportTooltips() {
    }

    static Component text(String label) {
        return VehicleImportText.component("tooltip." + label);
    }

    static Component number(String label, boolean wheelPage, boolean seatSection) {
        String context = switch (label) {
            case "width" -> wheelPage ? "wheel" : "hitbox";
            case "rotation_y" -> wheelPage ? "wheel" : "engine";
            case "x", "y", "z" -> seatSection ? "seat" : "hitbox";
            default -> "";
        };
        return VehicleImportText.component("tooltip." + label + (context.isEmpty() ? "" : "." + context));
    }

    static Component toggle(String label) {
        return VehicleImportText.component("tooltip." + label.substring("label.".length()));
    }

    record Area(int x, int y, int width, int height, Component description) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
