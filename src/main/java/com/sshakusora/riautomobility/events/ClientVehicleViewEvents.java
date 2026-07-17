package com.sshakusora.riautomobility.events;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.util.RIAutomobileCameraRegistry;
import com.sshakusora.riautomobility.util.RIAutomobileSeatRegistry;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID, value = Dist.CLIENT)
public final class ClientVehicleViewEvents {
    private ClientVehicleViewEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof LocalPlayer player)) {
            return;
        }
        if (!(player.getVehicle() instanceof RIAutomobileEntity automobile)) {
            return;
        }
        if (!RIAutomobileFrame.isRIAutomobileFrame(automobile.getFrame())) {
            return;
        }

        automobile.rotateLocalPassengerWithVehicle(player);
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Camera camera = event.getCamera();
        if (!(camera.getEntity() instanceof LocalPlayer player)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            return;
        }
        if (!(player.getVehicle() instanceof AutomobileEntity automobile)
                || !RIAutomobileFrame.isRIAutomobileFrame(automobile.getFrame())) {
            return;
        }

        RIAutomobileSeatRegistry.SeatPos seat = RIAutomobileSeatRegistry.getSeat(automobile, player);
        Vec3 cameraOffset = RIAutomobileCameraRegistry.getCameraPos(automobile, player);
        camera.move(cameraOffset.x, cameraOffset.y, cameraOffset.z - seat.pos.x);

        Vec3 targetPosition = camera.getPosition();
        Vec3 eyePosition = player.getEyePosition((float) event.getPartialTick());
        Vec3 offset = targetPosition.subtract(eyePosition);
        if (offset.lengthSqr() <= 1.0E-6) {
            return;
        }

        HitResult hit = player.level().clip(new ClipContext(
                eyePosition,
                targetPosition,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                player
        ));
        if (hit.getType() == HitResult.Type.BLOCK) {
            targetPosition = hit.getLocation().subtract(offset.normalize().scale(0.2));
        }

        camera.setPosition(targetPosition);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof AutomobileEntity automobile)) {
            return;
        }
        if (!RIAutomobileFrame.isRIAutomobileFrame(automobile.getFrame())
                || player == automobile.getControllingPassenger()) {
            return;
        }

        double boostFov = Math.sqrt(automobile.getBoostSpeed((float) event.getPartialTick()))
                * 18.0D * minecraft.options.fovEffectScale().get();
        event.setFOV(event.getFOV() - boostFov);
    }
}
