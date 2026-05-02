package com.sshakusora.riautomobility.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sshakusora.riautomobility.RIAutomobility;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = RIAutomobility.MODID)
public class PassengerRenderHandler {
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.getVehicle() instanceof AutomobileEntity auto)) {
            return;
        }

        float partialTick = event.getPartialTick();
        float pitch = auto.getDisplacement().getAngularX(partialTick);
        float roll = auto.getDisplacement().getAngularZ(partialTick);

        PoseStack poseStack = event.getPoseStack();
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
    }
}
