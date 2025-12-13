package com.sshakusora.riautomobility.util;

import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import net.minecraft.world.entity.EntityDimensions;

import java.util.HashMap;
import java.util.Map;

public class RIAutomobileEntityDimensionsRegistry {
    private static final Map<AutomobileFrame, EntityDimensions> customEntityDimensions = new HashMap<>();

    public static void register(AutomobileFrame frame, EntityDimensions dimensions) {
        customEntityDimensions.put(frame, dimensions);
    }

    public static EntityDimensions getEntityDimensions(AutomobileFrame frame){
        return customEntityDimensions.getOrDefault(frame, EntityDimensions.scalable(1.0F, 0.66F));
    }
}
