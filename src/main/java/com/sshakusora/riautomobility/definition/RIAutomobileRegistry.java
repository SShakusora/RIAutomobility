package com.sshakusora.riautomobility.definition;

import io.github.foundationgames.automobility.automobile.AutomobileFrame;

import java.util.HashMap;
import java.util.Map;

public final class RIAutomobileRegistry {
    private static final Map<AutomobileFrame, RIAutomobileDefinition> DEFINITIONS = new HashMap<>();

    private RIAutomobileRegistry() {}

    public static AutomobileFrame register(AutomobileFrame frame, RIAutomobileDefinition definition) {
        DEFINITIONS.put(frame, definition);
        return AutomobileFrame.REGISTRY.register(frame);
    }

    public static RIAutomobileDefinition get(AutomobileFrame frame) {
        return DEFINITIONS.getOrDefault(frame, RIAutomobileDefinition.DEFAULT);
    }

    public static boolean isRegistered(AutomobileFrame frame) {
        return DEFINITIONS.containsKey(frame);
    }
}
