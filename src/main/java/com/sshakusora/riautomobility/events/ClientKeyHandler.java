package com.sshakusora.riautomobility.events;

import com.sshakusora.riautomobility.client.RIAutomobilityKeyBindings;
import com.sshakusora.riautomobility.entity.DriverSeatEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.BoardingAsPassengerPacket;
import com.sshakusora.riautomobility.network.packet.PassengerDriverSwitchPacket;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientKeyHandler {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        if (RIAutomobilityKeyBindings.BOARDING_AS_PASSENGER.consumeClick()) {
            Entity vehicle = player.getVehicle();
            if(vehicle instanceof AutomobileEntity auto && RIAutomobileFrame.isRIAutomobileFrame(auto.getFrame()) || vehicle instanceof DriverSeatEntity) {
                RIAutomobilityNetwork.CHANNEL.sendToServer(new PassengerDriverSwitchPacket());
                return;
            }

            double reach = player.getEntityReach();
            Entity target = getLookAtEntity(player, reach);
            if(!(target instanceof AutomobileEntity)) return;
            if(!RIAutomobileFrame.isRIAutomobileFrame(((AutomobileEntity) target).getFrame())) return;
            RIAutomobilityNetwork.CHANNEL.sendToServer(new BoardingAsPassengerPacket(target.getId()));
        }
    }

    public static Entity getLookAtEntity(Player player, double distance) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 end = eyePos.add(lookVec.scale(distance));

        AABB aabb = player.getBoundingBox()
                .expandTowards(lookVec.scale(distance))
                .inflate(1.0D);

        EntityHitResult result = ProjectileUtil.getEntityHitResult(
                player,
                eyePos,
                end,
                aabb,
                e -> !e.isSpectator() && e.isPickable(),
                distance * distance
        );

        return result != null ? result.getEntity() : null;
    }

}
