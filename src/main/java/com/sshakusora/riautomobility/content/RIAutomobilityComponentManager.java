package com.sshakusora.riautomobility.content;

import com.sshakusora.riautomobility.definition.RIAutomobileRegistry;
import com.sshakusora.riautomobility.util.RIAutomobilityRegistryUtil;
import com.sshakusora.riautomobility.wheel.RIAutomobileWheel;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RIAutomobilityComponentManager {
    private static final Map<ResourceLocation, FrameSpec> CUSTOM_FRAMES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, WheelSpec> CUSTOM_WHEELS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, EngineSpec> CUSTOM_ENGINES = new LinkedHashMap<>();

    private RIAutomobilityComponentManager() {
    }

    public static void clearCustomComponents() {
        CUSTOM_FRAMES.keySet().forEach(RIAutomobileRegistry::remove);
        CUSTOM_WHEELS.keySet().forEach(id -> RIAutomobilityRegistryUtil.remove(AutomobileWheel.REGISTRY, id));
        CUSTOM_ENGINES.keySet().forEach(id -> RIAutomobilityRegistryUtil.remove(AutomobileEngine.REGISTRY, id));
        CUSTOM_FRAMES.clear();
        CUSTOM_WHEELS.clear();
        CUSTOM_ENGINES.clear();
    }

    public static void applyCustomComponents(Map<ResourceLocation, FrameSpec> frames, Map<ResourceLocation, WheelSpec> wheels,
                                             Map<ResourceLocation, EngineSpec> engines) {
        CUSTOM_FRAMES.putAll(frames);
        CUSTOM_WHEELS.putAll(wheels);
        CUSTOM_ENGINES.putAll(engines);

        for (FrameSpec spec : CUSTOM_FRAMES.values()) {
            RIAutomobileRegistry.register(spec.toFrame(), spec.toDefinition());
        }
        for (WheelSpec spec : CUSTOM_WHEELS.values()) {
            RIAutomobileWheel.register(spec.toWheel());
        }
        for (EngineSpec spec : CUSTOM_ENGINES.values()) {
            RIAutomobilityRegistryUtil.registerOrReplace(AutomobileEngine.REGISTRY, spec.toEngine());
        }
    }

    public static Map<ResourceLocation, FrameSpec> getCustomFrames() {
        return Map.copyOf(CUSTOM_FRAMES);
    }

    public static Map<ResourceLocation, WheelSpec> getCustomWheels() {
        return Map.copyOf(CUSTOM_WHEELS);
    }

    public static Map<ResourceLocation, EngineSpec> getCustomEngines() {
        return Map.copyOf(CUSTOM_ENGINES);
    }

    public static Collection<FrameSpec> getCustomFrameSpecs() {
        return CUSTOM_FRAMES.values();
    }

    public static Collection<WheelSpec> getCustomWheelSpecs() {
        return CUSTOM_WHEELS.values();
    }

    public static Collection<EngineSpec> getCustomEngineSpecs() {
        return CUSTOM_ENGINES.values();
    }

    public static boolean isManagedFrame(AutomobileFrame frame) {
        return frame != null && CUSTOM_FRAMES.containsKey(frame.getId());
    }

    public static boolean isManagedWheel(AutomobileWheel wheel) {
        return wheel != null && CUSTOM_WHEELS.containsKey(wheel.getId());
    }

    public static boolean isManagedEngine(AutomobileEngine engine) {
        return engine != null && CUSTOM_ENGINES.containsKey(engine.getId());
    }
}
