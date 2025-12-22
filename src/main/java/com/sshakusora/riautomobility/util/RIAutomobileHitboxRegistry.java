package com.sshakusora.riautomobility.util;

import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RIAutomobileHitboxRegistry {
    private static final Map<AutomobileFrame, List<RIAutomobileFrame.Hitbox>> HITBOXES = new HashMap<>();

    public static void register(AutomobileFrame frame, List<RIAutomobileFrame.Hitbox> hitboxes) {
        HITBOXES.put(frame, hitboxes);
    }

    public static List<RIAutomobileFrame.Hitbox> getHitboxes(final AutomobileFrame frame) {
        return HITBOXES.getOrDefault(frame, List.of());
    }

    public record IncomingCollision(Vec3 depth, Vec3 velocity, Vec3 origin, float inertia) {
    }
}
