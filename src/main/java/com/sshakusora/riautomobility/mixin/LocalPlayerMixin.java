package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "rideTick", at = @At("TAIL"))
    private void riautomobility$rotatePassengerWithAutomobile(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        Entity vehicle = self.getVehicle();
        if (!(vehicle instanceof RIAutomobileEntity auto)) {
            return;
        }
        if (!RIAutomobileFrame.isRIAutomobileFrame(auto.getFrame())) {
            return;
        }

        auto.rotateLocalPassengerWithVehicle(self);
    }
}
