package com.sshakusora.riautomobility.util;

import com.sshakusora.riautomobility.entity.HitboxEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class RIAutomobileHitboxRegistry {
    private static final Map<AutomobileFrame, List<RIAutomobileFrame.Hitbox>> HITBOXES = new HashMap<>();
    private static final Map<AutomobileEntity, List<HitboxEntity>> HITBOXES_ENTITIES = new WeakHashMap<>();

    public static void register(AutomobileFrame frame, List<RIAutomobileFrame.Hitbox> hitboxes) {
        HITBOXES.put(frame, hitboxes);
    }

    public static void addHitbox(AutomobileEntity automobile, HitboxEntity hitbox) {
        HITBOXES_ENTITIES
                .computeIfAbsent(automobile, a -> new ArrayList<>())
                .add(hitbox);
    }


    public static List<RIAutomobileFrame.Hitbox> getHitboxes(final AutomobileFrame frame) {
        return HITBOXES.getOrDefault(frame, List.of());
    }

    public static List<HitboxEntity> getHitboxEntities(AutomobileEntity entity) {
        return HITBOXES_ENTITIES.getOrDefault(entity, List.of());
    }

    public record IncomingCollision(Vec3 depth, Vec3 velocity, Vec3 origin, float inertia) {
    }

    public static void removeAll(AutomobileEntity automobile) {
        var list = HITBOXES_ENTITIES.remove(automobile);
        if (list != null) {
            for (HitboxEntity hb : list) {
                if (!hb.isRemoved()) {
                    hb.discard();
                }
            }
        }
    }
}
