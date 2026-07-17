package com.sshakusora.riautomobility.util;

import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RIAutomobileTransformUtil {
    private RIAutomobileTransformUtil() {}

    public static Vec3 localPosToWorldSpace(AutomobileEntity auto, Vec3 position) {
        float pitch = auto.getDisplacement().getAngularX(1.0F);
        float roll = auto.getDisplacement().getAngularZ(1.0F);
        float vert = auto.getDisplacement().getVertical(1.0F);

        return auto.position()
                .add(0.0F, vert, 0.0F)
                .add(rotateLocalOffset(position, auto.getYRot(), pitch, roll));
    }

    /**
     * Applies the same physical rotation matrix as the renderer: the vehicle yaw is
     * inside the renderer's {@code rotationXYZ(pitch, 0, roll)} tilt.
     */
    public static Vec3 rotateLocalOffset(Vec3 position, float yaw, float pitch, float roll) {
        return position
                .yRot(-yaw * Mth.DEG_TO_RAD)
                .zRot(-roll * Mth.DEG_TO_RAD)
                .xRot(-pitch * Mth.DEG_TO_RAD);
    }
}
