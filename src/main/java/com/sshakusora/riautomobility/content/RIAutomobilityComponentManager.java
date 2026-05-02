package com.sshakusora.riautomobility.content;

import com.sshakusora.riautomobility.definition.RIAutomobileRegistry;
import com.sshakusora.riautomobility.wheel.RIAutomobileWheel;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RIAutomobilityComponentManager {
    private static final Map<ResourceLocation, FrameSpec> CUSTOM_FRAMES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, WheelSpec> CUSTOM_WHEELS = new LinkedHashMap<>();

    private RIAutomobilityComponentManager() {}

    public static void applyCustomComponents(Map<ResourceLocation, FrameSpec> frames, Map<ResourceLocation, WheelSpec> wheels) {
        CUSTOM_FRAMES.clear();
        CUSTOM_FRAMES.putAll(frames);
        CUSTOM_WHEELS.clear();
        CUSTOM_WHEELS.putAll(wheels);

        for (FrameSpec spec : CUSTOM_FRAMES.values()) {
            RIAutomobileRegistry.register(spec.toFrame(), spec.toDefinition());
        }
        for (WheelSpec spec : CUSTOM_WHEELS.values()) {
            RIAutomobileWheel.register(spec.toWheel());
        }
    }

    public static Map<ResourceLocation, FrameSpec> getCustomFrames() {
        return Map.copyOf(CUSTOM_FRAMES);
    }

    public static Map<ResourceLocation, WheelSpec> getCustomWheels() {
        return Map.copyOf(CUSTOM_WHEELS);
    }

    public static Collection<FrameSpec> getCustomFrameSpecs() {
        return CUSTOM_FRAMES.values();
    }

    public static Collection<WheelSpec> getCustomWheelSpecs() {
        return CUSTOM_WHEELS.values();
    }

    public static boolean isManagedFrame(AutomobileFrame frame) {
        return frame != null && CUSTOM_FRAMES.containsKey(frame.getId());
    }

    public static boolean isManagedWheel(AutomobileWheel wheel) {
        return wheel != null && CUSTOM_WHEELS.containsKey(wheel.getId());
    }
}
