package com.sshakusora.riautomobility.interaction;

import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.OptionalDouble;
import java.util.regex.Pattern;

public record VehicleInteractionBox(
        String id,
        Vec3 center,
        Vec3 size,
        Vec3 rotation,
        List<VehicleInteractionAction> actions
) {
    public static final int MAX_BOXES = 64;
    public static final int MAX_ACTIONS = 16;
    public static final double MAX_COORDINATE = 32.0D;
    private static final double EPSILON = 1.0E-9D;
    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_.-]{1,64}");

    public VehicleInteractionBox {
        if (id == null || !VALID_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Interaction box id must match " + VALID_ID.pattern());
        }
        requireFinite(center, "center");
        requireFinite(size, "size");
        requireFinite(rotation, "rotation");
        if (size.x <= 0.0D || size.y <= 0.0D || size.z <= 0.0D
                || size.x > MAX_COORDINATE || size.y > MAX_COORDINATE || size.z > MAX_COORDINATE) {
            throw new IllegalArgumentException("Interaction box size must be positive and at most " + MAX_COORDINATE);
        }
        if (Math.abs(center.x) > MAX_COORDINATE || Math.abs(center.y) > MAX_COORDINATE
                || Math.abs(center.z) > MAX_COORDINATE) {
            throw new IllegalArgumentException("Interaction box center must be within " + MAX_COORDINATE + " blocks");
        }
        actions = List.copyOf(actions);
        if (actions.isEmpty() || actions.size() > MAX_ACTIONS) {
            throw new IllegalArgumentException("Interaction box must have between 1 and " + MAX_ACTIONS + " actions");
        }
    }

    public OptionalDouble raycast(AutomobileEntity automobile, Vec3 rayStart, Vec3 rayEnd, float partialTick) {
        Vec3 localStart = worldToVehicleLocal(automobile, rayStart, partialTick);
        Vec3 localEnd = worldToVehicleLocal(automobile, rayEnd, partialTick);
        return raycastVehicleLocal(localStart, localEnd);
    }

    public OptionalDouble raycastVehicleLocal(Vec3 rayStart, Vec3 rayEnd) {
        Vec3 start = inverseBoxRotation(rayStart.subtract(center));
        Vec3 end = inverseBoxRotation(rayEnd.subtract(center));
        Vec3 delta = end.subtract(start);
        Vec3 half = size.scale(0.5D);

        double tMin = 0.0D;
        double tMax = 1.0D;
        double[] starts = {start.x, start.y, start.z};
        double[] deltas = {delta.x, delta.y, delta.z};
        double[] halves = {half.x, half.y, half.z};
        for (int axis = 0; axis < 3; axis++) {
            double component = deltas[axis];
            if (Math.abs(component) < EPSILON) {
                if (starts[axis] < -halves[axis] || starts[axis] > halves[axis]) {
                    return OptionalDouble.empty();
                }
                continue;
            }
            double first = (-halves[axis] - starts[axis]) / component;
            double second = (halves[axis] - starts[axis]) / component;
            if (first > second) {
                double swap = first;
                first = second;
                second = swap;
            }
            tMin = Math.max(tMin, first);
            tMax = Math.min(tMax, second);
            if (tMin > tMax) {
                return OptionalDouble.empty();
            }
        }
        return OptionalDouble.of(rayStart.distanceTo(rayEnd) * tMin);
    }

    public double enclosingRadius() {
        return center.length() + size.length() * 0.5D;
    }

    public Vec3[] worldCorners(AutomobileEntity automobile, float partialTick) {
        FrameTransform transform = frameTransform(automobile, partialTick);
        Vec3[] corners = vehicleLocalCorners();
        for (int index = 0; index < corners.length; index++) {
            corners[index] = vehicleLocalToWorld(
                    corners[index], transform.origin, transform.yaw, transform.pitch, transform.roll);
        }
        return corners;
    }

    public Vec3[] vehicleLocalCorners() {
        return vehicleLocalCorners(center, size, rotation);
    }

    public static Vec3[] vehicleLocalCorners(Vec3 center, Vec3 size, Vec3 rotation) {
        Vec3 half = size.scale(0.5D);
        Vec3[] corners = new Vec3[8];
        for (int index = 0; index < corners.length; index++) {
            Vec3 corner = new Vec3(
                    (index & 1) == 0 ? -half.x : half.x,
                    (index & 2) == 0 ? -half.y : half.y,
                    (index & 4) == 0 ? -half.z : half.z
            );
            corners[index] = corner
                    .yRot((float) Math.toRadians(-rotation.y))
                    .zRot((float) Math.toRadians(-rotation.z))
                    .xRot((float) Math.toRadians(-rotation.x))
                    .add(center);
        }
        return corners;
    }

    private Vec3 inverseBoxRotation(Vec3 value) {
        return value
                .xRot((float) Math.toRadians(rotation.x))
                .zRot((float) Math.toRadians(rotation.z))
                .yRot((float) Math.toRadians(rotation.y));
    }

    private static Vec3 worldToVehicleLocal(AutomobileEntity automobile, Vec3 position, float partialTick) {
        FrameTransform transform = frameTransform(automobile, partialTick);
        return worldToVehicleLocal(
                position, transform.origin, transform.yaw, transform.pitch, transform.roll);
    }

    static Vec3 worldToVehicleLocal(Vec3 position, Vec3 origin,
                                    float yaw, float pitch, float roll) {
        return position.subtract(origin)
                .xRot(pitch * Mth.DEG_TO_RAD)
                .zRot(roll * Mth.DEG_TO_RAD)
                .yRot(yaw * Mth.DEG_TO_RAD);
    }

    static Vec3 vehicleLocalToWorld(Vec3 position, Vec3 origin,
                                    float yaw, float pitch, float roll) {
        return origin.add(position
                .yRot(-yaw * Mth.DEG_TO_RAD)
                .zRot(-roll * Mth.DEG_TO_RAD)
                .xRot(-pitch * Mth.DEG_TO_RAD));
    }

    static Vec3 frameOrigin(Vec3 automobileOrigin, float wheelRadiusPixels,
                            float yaw, float pitch, float roll) {
        return vehicleLocalToWorld(
                new Vec3(0.0D, wheelRadiusPixels / 16.0D, 0.0D),
                automobileOrigin, yaw, pitch, roll);
    }

    private static FrameTransform frameTransform(AutomobileEntity automobile, float partialTick) {
        float pitch = automobile.getDisplacement().getAngularX(partialTick);
        float roll = automobile.getDisplacement().getAngularZ(partialTick);
        float vertical = automobile.getDisplacement().getVertical(partialTick);
        float yaw = automobile.getAutomobileYaw(partialTick);
        Vec3 automobileOrigin = new Vec3(
                Mth.lerp(partialTick, automobile.xOld, automobile.getX()),
                Mth.lerp(partialTick, automobile.yOld, automobile.getY()) + vertical,
                Mth.lerp(partialTick, automobile.zOld, automobile.getZ())
        );
        Vec3 origin = frameOrigin(
                automobileOrigin, automobile.getWheels().model().radius(),
                yaw, pitch, roll);
        return new FrameTransform(origin, yaw, pitch, roll);
    }

    private static void requireFinite(Vec3 value, String field) {
        if (value == null || !Double.isFinite(value.x) || !Double.isFinite(value.y) || !Double.isFinite(value.z)) {
            throw new IllegalArgumentException("Interaction box " + field + " must be finite");
        }
    }

    private record FrameTransform(Vec3 origin, float yaw, float pitch, float roll) {
    }
}
