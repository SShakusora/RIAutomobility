package com.sshakusora.riautomobility.util;

import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import net.minecraft.world.entity.EntityDimensions;

import java.util.HashMap;
import java.util.Map;

public class RIAutomobileEntityDimensionsRegistry {
    private static final Map<AutomobileFrame, EntityDimensions> CUSTOM_ENTITY_DIMENSIONS = new HashMap<>();

    public static void register(AutomobileFrame frame, EntityDimensions dimensions) {
        CUSTOM_ENTITY_DIMENSIONS.put(frame, dimensions);
    }

    public static EntityDimensions getEntityDimensions(AutomobileFrame frame){
        return CUSTOM_ENTITY_DIMENSIONS.getOrDefault(frame, EntityDimensions.scalable(1.0F, 0.66F));
    }
}
