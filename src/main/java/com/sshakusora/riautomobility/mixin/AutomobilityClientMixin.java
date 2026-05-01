package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import io.github.foundationgames.automobility.AutomobilityClient;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AutomobilityClient.class)
public class AutomobilityClientMixin {
    @Inject(method = "modifyBoostFov", at = @At("HEAD"), cancellable = true, remap = false)
    private static void riautomobility$disablePassengerBoostFov(Minecraft client, double old, float tickDelta, CallbackInfoReturnable<Double> cir) {
        if (client.player == null) {
            return;
        }
        var player = client.player;

        if (!(player.getVehicle() instanceof AutomobileEntity auto)) {
            return;
        }

        if (!RIAutomobileFrame.isRIAutomobileFrame(auto.getFrame())) {
            return;
        }

        if (player != auto.getControllingPassenger()) {
            cir.setReturnValue(old);
        }
    }
}
