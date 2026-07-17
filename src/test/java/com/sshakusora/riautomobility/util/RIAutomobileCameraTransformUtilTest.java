package com.sshakusora.riautomobility.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RIAutomobileCameraTransformUtilTest {
    private static final float EPSILON = 1.0E-4F;
    private static final float MINECRAFT_VECTOR_EPSILON = 5.0E-4F;

    @Test
    void zeroVehicleTiltPreservesTheCameraRotation() {
        RIAutomobileCameraTransformUtil.CameraAngles angles =
                RIAutomobileCameraTransformUtil.applyVehicleTilt(37.0F, -24.0F, 8.0F, 0.0F, 0.0F);

        assertEquals(37.0F, angles.yaw(), EPSILON);
        assertEquals(-24.0F, angles.pitch(), EPSILON);
        assertEquals(8.0F, angles.roll(), EPSILON);
    }

    @Test
    void pureVehicleTiltMatchesForgeCameraPitchAndRoll() {
        RIAutomobileCameraTransformUtil.CameraAngles pitch =
                RIAutomobileCameraTransformUtil.applyVehicleTilt(0.0F, 0.0F, 0.0F, 31.0F, 0.0F);
        RIAutomobileCameraTransformUtil.CameraAngles roll =
                RIAutomobileCameraTransformUtil.applyVehicleTilt(0.0F, 0.0F, 0.0F, 0.0F, -73.0F);

        assertEquals(31.0F, pitch.pitch(), EPSILON);
        assertEquals(-73.0F, roll.roll(), EPSILON);
    }

    @Test
    void compoundTiltProducesTheExactExpectedViewRotation() {
        float cameraYaw = 68.0F;
        float cameraPitch = -17.0F;
        float cameraRoll = 4.0F;
        float vehiclePitch = 29.0F;
        float vehicleRoll = 82.0F;

        Quaternionf expected = RIAutomobileCameraTransformUtil
                .cameraViewRotation(cameraYaw, cameraPitch, cameraRoll)
                .mul(RIAutomobileCameraTransformUtil.vehicleTilt(vehiclePitch, vehicleRoll).conjugate())
                .normalize();

        RIAutomobileCameraTransformUtil.CameraAngles angles =
                RIAutomobileCameraTransformUtil.applyVehicleTilt(
                        cameraYaw, cameraPitch, cameraRoll, vehiclePitch, vehicleRoll);
        Quaternionf actual = RIAutomobileCameraTransformUtil
                .cameraViewRotation(angles.yaw(), angles.pitch(), angles.roll())
                .normalize();

        assertTrue(Math.abs(expected.dot(actual)) > 1.0F - EPSILON,
                "decomposed Forge camera angles must preserve the composed rotation");
    }

    @Test
    void eyeHeightUsesTheVehicleLocalUpDirection() {
        float eyeHeight = 1.6F;
        Vector3f offset = RIAutomobileCameraTransformUtil.rotateEyeOffset(eyeHeight, 0.0F, 90.0F);

        assertEquals(-eyeHeight, offset.x, EPSILON);
        assertEquals(0.0F, offset.y, EPSILON);
        assertEquals(0.0F, offset.z, EPSILON);
    }

    @Test
    void passengerOffsetUsesTheSameCompoundRotationAsTheRenderer() {
        Vec3 localOffset = new Vec3(0.7D, 1.4D, -0.9D);
        float yaw = 53.0F;
        float pitch = 28.0F;
        float roll = 76.0F;

        Vec3 actual = RIAutomobileTransformUtil.rotateLocalOffset(localOffset, yaw, pitch, roll);
        Vector3f expected = RIAutomobileCameraTransformUtil.vehicleTilt(pitch, roll)
                .rotateY((float) Math.toRadians(-yaw))
                .transform(new Vector3f((float) localOffset.x, (float) localOffset.y, (float) localOffset.z));

        assertEquals(expected.x, actual.x, MINECRAFT_VECTOR_EPSILON);
        assertEquals(expected.y, actual.y, MINECRAFT_VECTOR_EPSILON);
        assertEquals(expected.z, actual.z, MINECRAFT_VECTOR_EPSILON);
    }
}
