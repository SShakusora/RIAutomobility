package com.sshakusora.riautomobility.definition;

import com.sshakusora.riautomobility.util.RIAutomobilityRegistryUtil;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class RIAutomobileRegistry {
    private static final Map<ResourceLocation, RIAutomobileDefinition> DEFINITIONS = new HashMap<>();

    private RIAutomobileRegistry() {
    }

    public static AutomobileFrame register(AutomobileFrame frame, RIAutomobileDefinition definition) {
        DEFINITIONS.put(frame.getId(), definition);
        return RIAutomobilityRegistryUtil.registerOrReplace(AutomobileFrame.REGISTRY, frame);
    }

    public static RIAutomobileDefinition get(AutomobileFrame frame) {
        return frame == null ? RIAutomobileDefinition.DEFAULT : get(frame.getId());
    }

    public static RIAutomobileDefinition get(ResourceLocation id) {
        return DEFINITIONS.getOrDefault(id, RIAutomobileDefinition.DEFAULT);
    }

    public static boolean isRegistered(AutomobileFrame frame) {
        return frame != null && DEFINITIONS.containsKey(frame.getId());
    }

    public static boolean hidesEngine(AutomobileFrame frame) {
        return frame != null && get(frame).hideEngine();
    }

    public static void remove(ResourceLocation id) {
        DEFINITIONS.remove(id);
        RIAutomobilityRegistryUtil.remove(AutomobileFrame.REGISTRY, id);
    }
}
