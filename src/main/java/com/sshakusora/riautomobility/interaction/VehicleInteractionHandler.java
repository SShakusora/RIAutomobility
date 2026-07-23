package com.sshakusora.riautomobility.interaction;

import com.sshakusora.riautomobility.definition.RIAutomobileRegistry;
import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import com.sshakusora.riautomobility.item.VehicleKeyAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalDouble;

public final class VehicleInteractionHandler {
    private static final double HIT_EPSILON = 1.0E-4D;

    private VehicleInteractionHandler() {
    }

    public static Optional<Hit> findNearest(Level level, Vec3 rayStart, Vec3 rayEnd, float partialTick) {
        return findNearest(level, rayStart, rayEnd, partialTick,
                VehicleInteractionAction.Trigger.RIGHT_CLICK);
    }

    public static Optional<Hit> findNearest(Level level, Vec3 rayStart, Vec3 rayEnd, float partialTick,
                                            VehicleInteractionAction.Trigger trigger) {
        double expansion = RIAutomobileRegistry.maxInteractionRadius() + 1.0D;
        AABB search = new AABB(rayStart, rayEnd).inflate(expansion);
        return level.getEntitiesOfClass(
                        RIAutomobileEntity.class,
                        search,
                        automobile -> automobile.isAlive() && !automobile.getInteractionBoxes().isEmpty()
                ).stream()
                .flatMap(automobile -> automobile.getInteractionBoxes().stream()
                        .filter(box -> box.actions().stream().anyMatch(action -> action.trigger() == trigger))
                        .map(box -> hit(automobile, box, rayStart, rayEnd, partialTick))
                        .flatMap(Optional::stream))
                .min(Comparator.comparingDouble(Hit::distance));
    }

    public static Optional<Hit> hit(RIAutomobileEntity automobile, VehicleInteractionBox box,
                                    Vec3 rayStart, Vec3 rayEnd, float partialTick) {
        OptionalDouble distance = box.raycast(automobile, rayStart, rayEnd, partialTick);
        return distance.isPresent()
                ? Optional.of(new Hit(automobile, box, distance.getAsDouble()))
                : Optional.empty();
    }

    public static boolean interactionBoxTakesPriority(boolean sameVehicleTarget,
                                                      double vanillaTargetDistance,
                                                      double interactionBoxDistance) {
        return sameVehicleTarget
                || vanillaTargetDistance + HIT_EPSILON >= interactionBoxDistance;
    }

    public static void execute(ServerPlayer player, RIAutomobileEntity automobile,
                               VehicleInteractionBox box) {
        execute(player, automobile, box, VehicleInteractionAction.Trigger.RIGHT_CLICK);
    }

    public static void execute(ServerPlayer player, RIAutomobileEntity automobile,
                               VehicleInteractionBox box, VehicleInteractionAction.Trigger trigger) {
        var actions = box.actions().stream()
                .filter(action -> action.trigger() == trigger)
                .toList();
        boolean authorized = automobile.canPlayerAccess(player);
        if (!authorized && actions.stream().anyMatch(action ->
                action.requiresAccess() || action instanceof VehicleInteractionAction.OpenContainer)) {
            VehicleKeyAccess.deny(player);
            return;
        }

        for (VehicleInteractionAction action : actions) {
            if (action instanceof VehicleInteractionAction.OpenContainer) {
                automobile.openInteractionInventory(player);
            } else if (action instanceof VehicleInteractionAction.Mount mount) {
                automobile.boardAtInteractionSeat(player, mount.seat());
            } else if (action instanceof VehicleInteractionAction.Molang molang) {
                automobile.applyInteractionMolang(molang);
            }
        }
    }

    public record Hit(RIAutomobileEntity automobile, VehicleInteractionBox box, double distance) {
    }
}
