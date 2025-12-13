package com.sshakusora.riautomobility.events;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.entity.DriverSeatEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.mixin.CameraAccessor;
import com.sshakusora.riautomobility.util.RIAutomobileSeatRegistry;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID, value = Dist.CLIENT)
public class ModifyCamera {
    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        Entity vehicle = player.getVehicle();
        if ((vehicle instanceof AutomobileEntity auto && RIAutomobileFrame.isRIAutomobileFrame(auto.getFrame())) || (vehicle instanceof DriverSeatEntity)) {
            if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) return;
            Camera camera = event.getCamera();
            CameraAccessor accessor = (CameraAccessor) camera;
            RIAutomobileSeatRegistry.SeatPos pos = RIAutomobileSeatRegistry.getSeat(vehicle, player);

            /*
            x:forward and back
            y:up and down
            z:left and right
             */
            //TODO: add camera position feature and fix sound
            accessor.invokeMove(0.0, 0.0, -pos.x);
            Vec3 targetPos = camera.getPosition();
            Vec3 eyePos = player.getEyePosition((float) event.getPartialTick());
            Vec3 dir1 = targetPos.subtract(eyePos).normalize();
            Vec3 detectPos = targetPos.add(dir1.scale(0.3));

            ClipContext context = new ClipContext(
                    eyePos,
                    detectPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            );
            if(mc.level == null) return;
            HitResult hit = mc.level.clip(context);

            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 hitPos = hit.getLocation();
                Vec3 dir2 = eyePos.subtract(hitPos).normalize();
                targetPos = hitPos.add(dir2.scale(0.3));
            }

            accessor.invokeSetPosition(targetPos);
        }
    }
}
