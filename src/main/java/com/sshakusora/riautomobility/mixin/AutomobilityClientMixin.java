package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.entity.SeatEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import io.github.foundationgames.automobility.AutomobilityClient;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AutomobilityClient.class)
public class AutomobilityClientMixin {
    @Unique private static double scale = 256.0F;

    @Inject(method = "modifyBoostFov", at = @At("HEAD"), remap = false, cancellable = true)
    private static void modifyBoostFovFix(Minecraft client, double old, float tickDelta, CallbackInfoReturnable<Double> cir) {
        LocalPlayer player = client.player;
        Entity var6 = null;
        if (player != null) {
            var6 = player.getVehicle();
        }
        if (var6 instanceof AutomobileEntity auto && RIAutomobileFrame.isRIAutomobileFrame(auto.getFrame())) {
            cir.setReturnValue(old);
        } else if (var6 instanceof SeatEntity seat) {
            AutomobileEntity auto = (AutomobileEntity) seat.getVehicle();
            if (auto != null) {
                cir.setReturnValue(old + Math.sqrt((double)auto.getBoostSpeed(tickDelta) * scale * (Double)client.options.fovEffectScale().get()));
            }
        }
    }
}
