package com.sshakusora.riautomobility.interaction.client;

import com.sshakusora.riautomobility.entity.HitboxEntity;
import com.sshakusora.riautomobility.interaction.VehicleInteractionAction;
import com.sshakusora.riautomobility.interaction.VehicleInteractionHandler;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.VehicleInteractionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class ClientVehicleInteractionHandler {
    private static long handledGameTime = Long.MIN_VALUE;

    private ClientVehicleInteractionHandler() {
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() && !event.isAttack()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        long gameTime = minecraft.level.getGameTime();
        if (event.getHand() == InteractionHand.OFF_HAND && handledGameTime == gameTime) {
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        VehicleInteractionAction.Trigger trigger = VehicleInteractionAction.Trigger.fromInput(
                event.isAttack(), player.isShiftKeyDown());
        double reach = player.getEntityReach();
        float partialTick = minecraft.getFrameTime();
        Vec3 start = player.getEyePosition(partialTick);
        Vec3 end = start.add(player.getViewVector(partialTick).scale(reach));
        VehicleInteractionHandler.Hit hit = VehicleInteractionHandler
                .findNearest(minecraft.level, start, end, partialTick, trigger)
                .orElse(null);
        if (hit == null || isBlockedByVanillaTarget(minecraft.hitResult, start, hit)) {
            return;
        }

        handledGameTime = gameTime;
        RIAutomobilityNetwork.CHANNEL.sendToServer(new VehicleInteractionPacket(
                hit.automobile().getId(), hit.box().id(), event.getHand(), trigger));
        event.setCanceled(true);
        event.setSwingHand(true);
    }

    private static boolean isBlockedByVanillaTarget(HitResult vanillaHit, Vec3 rayStart,
                                                     VehicleInteractionHandler.Hit interactionHit) {
        if (vanillaHit == null || vanillaHit.getType() == HitResult.Type.MISS) {
            return false;
        }
        boolean sameVehicleTarget = vanillaHit instanceof EntityHitResult entityHit
                && belongsToVehicle(entityHit.getEntity(), interactionHit);
        return !VehicleInteractionHandler.interactionBoxTakesPriority(
                sameVehicleTarget,
                rayStart.distanceTo(vanillaHit.getLocation()),
                interactionHit.distance());
    }

    private static boolean belongsToVehicle(Entity entity, VehicleInteractionHandler.Hit hit) {
        if (entity == hit.automobile()) {
            return true;
        }
        return entity instanceof HitboxEntity hitbox && hitbox.belongsTo(hit.automobile());
    }
}
