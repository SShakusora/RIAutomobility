package com.sshakusora.riautomobility.util;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RIAutomobileCameraTransformUtil {
    private static final float HALF_TURN_DEGREES = 180.0F;

    private RIAutomobileCameraTransformUtil() {
    }

    /**
     * Composes the vehicle's rendered pitch/roll with Forge's Z-X-Y camera view rotation.
     * Directly adding Euler angles is only correct when the player faces the vehicle's
     * forward direction and the tilt is small.
     */
    public static CameraAngles applyVehicleTilt(float cameraYaw, float cameraPitch, float cameraRoll,
                                                float vehiclePitch, float vehicleRoll) {
        Quaternionf viewRotation = cameraViewRotation(cameraYaw, cameraPitch, cameraRoll);
        Quaternionf inverseVehicleTilt = vehicleTilt(vehiclePitch, vehicleRoll).conjugate();
        viewRotation.mul(inverseVehicleTilt);

        Vector3f euler = viewRotation.getEulerAnglesZXY(new Vector3f());
        return new CameraAngles(
                wrapDegrees((float) Math.toDegrees(euler.y) - HALF_TURN_DEGREES),
                (float) Math.toDegrees(euler.x),
                (float) Math.toDegrees(euler.z)
        );
    }

    /**
     * Rotates the normally world-up eye-height vector by the same tilt quaternion used
     * by the automobile renderer. Vehicle yaw is intentionally absent because it does
     * not change a local vertical vector.
     */
    public static Vector3f rotateEyeOffset(float eyeHeight, float vehiclePitch, float vehicleRoll) {
        return vehicleTilt(vehiclePitch, vehicleRoll).transform(new Vector3f(0.0F, eyeHeight, 0.0F));
    }

    static Quaternionf cameraViewRotation(float yaw, float pitch, float roll) {
        return new Quaternionf()
                .rotationZ((float) Math.toRadians(roll))
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw + HALF_TURN_DEGREES));
    }

    static Quaternionf vehicleTilt(float pitch, float roll) {
        return new Quaternionf().rotationXYZ(
                (float) Math.toRadians(pitch),
                0.0F,
                (float) Math.toRadians(roll)
        );
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= HALF_TURN_DEGREES) {
            wrapped -= 360.0F;
        }
        if (wrapped < -HALF_TURN_DEGREES) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    public record CameraAngles(float yaw, float pitch, float roll) {
    }
}
