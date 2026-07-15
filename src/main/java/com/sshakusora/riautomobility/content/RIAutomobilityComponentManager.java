package com.sshakusora.riautomobility.content;

import com.sshakusora.riautomobility.definition.RIAutomobileDefinition;
import com.sshakusora.riautomobility.definition.RIAutomobileRegistry;
import com.sshakusora.riautomobility.util.RIAutomobilityRegistryUtil;
import com.sshakusora.riautomobility.wheel.RIAutomobileWheel;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import io.github.foundationgames.automobility.util.SimpleMapContentRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RIAutomobilityComponentManager {
    private static final Map<ResourceLocation, FrameSpec> CUSTOM_FRAMES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, WheelSpec> CUSTOM_WHEELS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, EngineSpec> CUSTOM_ENGINES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, OriginalFrame> ORIGINAL_FRAMES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, AutomobileWheel> ORIGINAL_WHEELS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, AutomobileEngine> ORIGINAL_ENGINES = new LinkedHashMap<>();

    private RIAutomobilityComponentManager() {
    }

    public static void clearCustomComponents() {
        for (ResourceLocation id : CUSTOM_FRAMES.keySet()) {
            RIAutomobileRegistry.remove(id);
            OriginalFrame original = ORIGINAL_FRAMES.remove(id);
            if (original != null) {
                if (original.definition() != null) {
                    RIAutomobileRegistry.register(original.frame(), original.definition());
                } else {
                    RIAutomobilityRegistryUtil.registerOrReplace(AutomobileFrame.REGISTRY, original.frame());
                }
            }
        }
        for (ResourceLocation id : CUSTOM_WHEELS.keySet()) {
            RIAutomobilityRegistryUtil.remove(AutomobileWheel.REGISTRY, id);
            AutomobileWheel original = ORIGINAL_WHEELS.remove(id);
            if (original != null) {
                RIAutomobilityRegistryUtil.registerOrReplace(AutomobileWheel.REGISTRY, original);
            }
        }
        for (ResourceLocation id : CUSTOM_ENGINES.keySet()) {
            RIAutomobilityRegistryUtil.remove(AutomobileEngine.REGISTRY, id);
            AutomobileEngine original = ORIGINAL_ENGINES.remove(id);
            if (original != null) {
                RIAutomobilityRegistryUtil.registerOrReplace(AutomobileEngine.REGISTRY, original);
            }
        }
        CUSTOM_FRAMES.clear();
        CUSTOM_WHEELS.clear();
        CUSTOM_ENGINES.clear();
    }

    public static void applyCustomComponents(Map<ResourceLocation, FrameSpec> frames, Map<ResourceLocation, WheelSpec> wheels,
                                             Map<ResourceLocation, EngineSpec> engines) {
        frames.keySet().stream().filter(id -> !CUSTOM_FRAMES.containsKey(id))
                .forEach(RIAutomobilityComponentManager::rememberOriginalFrame);
        wheels.keySet().stream().filter(id -> !CUSTOM_WHEELS.containsKey(id))
                .forEach(id -> rememberOriginal(AutomobileWheel.REGISTRY, id, ORIGINAL_WHEELS));
        engines.keySet().stream().filter(id -> !CUSTOM_ENGINES.containsKey(id))
                .forEach(id -> rememberOriginal(AutomobileEngine.REGISTRY, id, ORIGINAL_ENGINES));
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

    private static void rememberOriginalFrame(ResourceLocation id) {
        AutomobileFrame frame = AutomobileFrame.REGISTRY.get(id);
        if (frame == null) {
            return;
        }
        RIAutomobileDefinition definition = RIAutomobileRegistry.isRegistered(frame)
                ? RIAutomobileRegistry.get(frame) : null;
        ORIGINAL_FRAMES.put(id, new OriginalFrame(frame, definition));
    }

    private static <V extends SimpleMapContentRegistry.Identifiable>
    void rememberOriginal(SimpleMapContentRegistry<V> registry,
                          ResourceLocation id, Map<ResourceLocation, V> originals) {
        V original = registry.get(id);
        if (original != null) {
            originals.put(id, original);
        }
    }

    private record OriginalFrame(AutomobileFrame frame, RIAutomobileDefinition definition) {
    }
}
