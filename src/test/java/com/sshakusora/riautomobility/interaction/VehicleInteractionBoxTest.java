package com.sshakusora.riautomobility.interaction;

import com.sshakusora.riautomobility.util.RIAutomobileTransformUtil;
import org.junit.jupiter.api.Test;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VehicleInteractionBoxTest {
    private static final List<VehicleInteractionAction> ACTIONS =
            List.of(new VehicleInteractionAction.OpenContainer(true));

    @Test
    void raycastsAxisAlignedBox() {
        VehicleInteractionBox box = box(new Vec3(2.0D, 2.0D, 2.0D), Vec3.ZERO);

        assertEquals(2.0D, box.raycastVehicleLocal(
                new Vec3(-3.0D, 0.0D, 0.0D),
                new Vec3(3.0D, 0.0D, 0.0D)).orElseThrow(), 1.0E-8D);
    }

    @Test
    void raycastsRotatedBoxInLocalSpace() {
        VehicleInteractionBox box = box(new Vec3(4.0D, 1.0D, 1.0D),
                new Vec3(0.0D, 90.0D, 0.0D));

        assertEquals(1.0D, box.raycastVehicleLocal(
                new Vec3(0.0D, 0.0D, -3.0D),
                new Vec3(0.0D, 0.0D, 3.0D)).orElseThrow(), 1.0E-8D);
    }

    @Test
    void returnsZeroWhenRayStartsInside() {
        VehicleInteractionBox box = box(new Vec3(2.0D, 2.0D, 2.0D), Vec3.ZERO);

        assertEquals(0.0D, box.raycastVehicleLocal(Vec3.ZERO, new Vec3(4.0D, 0.0D, 0.0D))
                .orElseThrow(), 1.0E-8D);
    }

    @Test
    void rejectsParallelRayOutsideBox() {
        VehicleInteractionBox box = box(new Vec3(2.0D, 2.0D, 2.0D), Vec3.ZERO);

        assertTrue(box.raycastVehicleLocal(
                new Vec3(-3.0D, 2.0D, 0.0D),
                new Vec3(3.0D, 2.0D, 0.0D)).isEmpty());
    }

    @Test
    void validatesIdsAndGeometry() {
        assertThrows(IllegalArgumentException.class, () ->
                new VehicleInteractionBox("Invalid ID", Vec3.ZERO,
                        new Vec3(1.0D, 1.0D, 1.0D), Vec3.ZERO, ACTIONS));
        assertThrows(IllegalArgumentException.class, () ->
                new VehicleInteractionBox("valid", Vec3.ZERO,
                        new Vec3(0.0D, 1.0D, 1.0D), Vec3.ZERO, ACTIONS));
        assertThrows(IllegalArgumentException.class, () ->
                new VehicleInteractionBox("valid", Vec3.ZERO,
                        new Vec3(1.0D, 1.0D, 1.0D), Vec3.ZERO, List.of()));
    }

    @Test
    void invertsTheVehiclePhysicalTransform() {
        Vec3 origin = new Vec3(10.0D, 4.0D, -3.0D);
        Vec3 local = new Vec3(1.25D, 0.5D, -2.0D);
        float yaw = 37.0F;
        float pitch = -8.0F;
        float roll = 11.0F;
        Vec3 world = origin.add(RIAutomobileTransformUtil.rotateLocalOffset(
                local, yaw, pitch, roll));
        Vec3 interactionWorld = VehicleInteractionBox.vehicleLocalToWorld(
                local, origin, yaw, pitch, roll);

        Vec3 restored = VehicleInteractionBox.worldToVehicleLocal(
                world, origin, yaw, pitch, roll);

        assertEquals(world.x, interactionWorld.x, 1.0E-8D);
        assertEquals(world.y, interactionWorld.y, 1.0E-8D);
        assertEquals(world.z, interactionWorld.z, 1.0E-8D);
        assertEquals(local.x, restored.x, 3.0E-4D);
        assertEquals(local.y, restored.y, 3.0E-4D);
        assertEquals(local.z, restored.z, 3.0E-4D);
    }

    @Test
    void buildsCornersForRotatedObb() {
        VehicleInteractionBox box = new VehicleInteractionBox(
                "rotated", new Vec3(1.0D, 2.0D, 3.0D),
                new Vec3(4.0D, 2.0D, 2.0D),
                new Vec3(0.0D, 90.0D, 0.0D), ACTIONS);

        Vec3[] corners = box.vehicleLocalCorners();
        assertEquals(8, corners.length);
        assertEquals(0.0D, min(corners, 0), 1.0E-6D);
        assertEquals(2.0D, max(corners, 0), 1.0E-6D);
        assertEquals(1.0D, min(corners, 1), 1.0E-6D);
        assertEquals(3.0D, max(corners, 1), 1.0E-6D);
        assertEquals(1.0D, min(corners, 2), 1.0E-6D);
        assertEquals(5.0D, max(corners, 2), 1.0E-6D);
    }

    @Test
    void usesWheelRaisedFrameOrigin() {
        Vec3 automobileOrigin = new Vec3(4.0D, 2.0D, -1.0D);
        float yaw = 31.0F;
        float pitch = -9.0F;
        float roll = 14.0F;
        float wheelRadiusPixels = 8.0F;

        Vec3 expected = VehicleInteractionBox.vehicleLocalToWorld(
                new Vec3(0.0D, 0.5D, 0.0D),
                automobileOrigin, yaw, pitch, roll);
        Vec3 actual = VehicleInteractionBox.frameOrigin(
                automobileOrigin, wheelRadiusPixels, yaw, pitch, roll);

        assertEquals(expected.x, actual.x, 1.0E-8D);
        assertEquals(expected.y, actual.y, 1.0E-8D);
        assertEquals(expected.z, actual.z, 1.0E-8D);
        Vec3 local = VehicleInteractionBox.worldToVehicleLocal(
                actual, actual, yaw, pitch, roll);
        assertEquals(Vec3.ZERO, local);
    }

    private static VehicleInteractionBox box(Vec3 size, Vec3 rotation) {
        return new VehicleInteractionBox("test", Vec3.ZERO, size, rotation, ACTIONS);
    }

    private static double min(Vec3[] corners, int axis) {
        return java.util.Arrays.stream(corners)
                .mapToDouble(corner -> component(corner, axis))
                .min()
                .orElseThrow();
    }

    private static double max(Vec3[] corners, int axis) {
        return java.util.Arrays.stream(corners)
                .mapToDouble(corner -> component(corner, axis))
                .max()
                .orElseThrow();
    }

    private static double component(Vec3 corner, int axis) {
        return switch (axis) {
            case 0 -> corner.x;
            case 1 -> corner.y;
            case 2 -> corner.z;
            default -> throw new IllegalArgumentException("Invalid axis " + axis);
        };
    }
}
