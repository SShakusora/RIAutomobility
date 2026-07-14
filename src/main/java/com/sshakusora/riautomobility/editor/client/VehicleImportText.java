package com.sshakusora.riautomobility.editor.client;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

final class VehicleImportText {
    private static final String PREFIX = "editor.riautomobility.vehicle_import.";

    private VehicleImportText() {
    }

    static String key(String path) {
        return PREFIX + path;
    }

    static Component component(String path, Object... args) {
        return Component.translatable(key(path), args);
    }

    static String string(String path, Object... args) {
        return I18n.get(key(path), args);
    }
}
