package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import com.sshakusora.riautomobility.entity.HitboxEntity;
import com.sshakusora.riautomobility.interaction.VehicleInteractionBox;
import com.sshakusora.riautomobility.interaction.VehicleInteractionAction;
import com.sshakusora.riautomobility.interaction.VehicleInteractionHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public record VehicleInteractionPacket(int vehicleId, String boxId, InteractionHand hand,
                                       VehicleInteractionAction.Trigger trigger) {
    private static final double VALIDATION_EPSILON = 0.15D;

    public static void encode(VehicleInteractionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.vehicleId);
        buffer.writeUtf(packet.boxId, 64);
        buffer.writeEnum(packet.hand);
        buffer.writeEnum(packet.trigger);
    }

    public static VehicleInteractionPacket decode(FriendlyByteBuf buffer) {
        return new VehicleInteractionPacket(buffer.readVarInt(), buffer.readUtf(64),
                buffer.readEnum(InteractionHand.class),
                buffer.readEnum(VehicleInteractionAction.Trigger.class));
    }

    public static void handle(VehicleInteractionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> validateAndExecute(packet, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void validateAndExecute(VehicleInteractionPacket packet, ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator()) {
            return;
        }
        Entity entity = player.level().getEntity(packet.vehicleId);
        if (!(entity instanceof RIAutomobileEntity requestedVehicle) || !requestedVehicle.isAlive()) {
            return;
        }
        Optional<VehicleInteractionBox> requestedBox = requestedVehicle.getInteractionBoxes().stream()
                .filter(box -> box.id().equals(packet.boxId))
                .filter(box -> box.actions().stream().anyMatch(action -> action.trigger() == packet.trigger))
                .findFirst();
        if (requestedBox.isEmpty()) {
            return;
        }

        double reach = player.getEntityReach();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(reach));
        Optional<VehicleInteractionHandler.Hit> nearest =
                VehicleInteractionHandler.findNearest(
                        player.level(), start, end, 1.0F, packet.trigger);
        if (nearest.isEmpty()
                || nearest.get().automobile() != requestedVehicle
                || nearest.get().box() != requestedBox.get()
                || nearest.get().distance() > reach + VALIDATION_EPSILON) {
            return;
        }

        BlockHitResult blockHit = player.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS
                && start.distanceTo(blockHit.getLocation()) + VALIDATION_EPSILON < nearest.get().distance()) {
            return;
        }
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                player.getBoundingBox().expandTowards(player.getLookAngle().scale(reach)).inflate(1.0D),
                candidate -> !candidate.isSpectator() && candidate.isPickable()
                        && !belongsToVehicle(candidate, requestedVehicle),
                reach * reach
        );
        if (entityHit != null
                && start.distanceTo(entityHit.getLocation()) + VALIDATION_EPSILON < nearest.get().distance()) {
            return;
        }
        VehicleInteractionHandler.execute(player, requestedVehicle, requestedBox.get(), packet.trigger);
    }

    private static boolean belongsToVehicle(Entity entity, RIAutomobileEntity vehicle) {
        return entity == vehicle
                || entity instanceof HitboxEntity hitbox && hitbox.belongsTo(vehicle);
    }
}
