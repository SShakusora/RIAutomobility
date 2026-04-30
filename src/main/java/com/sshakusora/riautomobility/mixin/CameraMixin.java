package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.util.RIAutomobileCameraRegistry;
import com.sshakusora.riautomobility.util.RIAutomobileSeatRegistry;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {
    @Inject(method = "setup", at = @At("TAIL"))
    private void RIAutomobileCameraSetup(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
        if (!detached || mirrored) return;
        if (!(entity instanceof LocalPlayer)) return;

        Camera camera = (Camera) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) return;
        Entity vehicle = entity.getVehicle();
        if (vehicle instanceof AutomobileEntity auto && RIAutomobileFrame.isRIAutomobileFrame(auto.getFrame())) {
            RIAutomobileSeatRegistry.SeatPos pos = RIAutomobileSeatRegistry.getSeat(vehicle, entity);
            Vec3 cameraPos = RIAutomobileCameraRegistry.getCameraPos(vehicle, entity);
            /*
            x:forward and back
            y:up and down
            z:left and right
             */
            //TODO: add camera position feature and fix sound
            camera.move(cameraPos.x, cameraPos.y, cameraPos.z - pos.pos.x);
            Vec3 targetPos = camera.getPosition();
            Vec3 eyePos = entity.getEyePosition(partialTick);
            Vec3 offset = targetPos.subtract(eyePos);

            if (offset.lengthSqr() <= 1.0E-6) {
                return;
            }

            ClipContext context = new ClipContext(
                    eyePos,
                    targetPos,
                    ClipContext.Block.VISUAL,
                    ClipContext.Fluid.NONE,
                    entity
            );
            if(mc.level == null) return;
            HitResult hit = mc.level.clip(context);

            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 hitPos = hit.getLocation();
                Vec3 pullBack = offset.normalize().scale(0.2);
                targetPos = hitPos.subtract(pullBack);
            }

            camera.setPosition(targetPos);
        }
    }
}
