package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.entity.DriverSeatEntity;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import io.github.foundationgames.automobility.platform.Platform;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "rideTick", at = @At("TAIL"))
    public void rideTick(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if(self.getVehicle() instanceof DriverSeatEntity driverSeat && driverSeat.getVehicle() instanceof AutomobileEntity vehicle) {
            if (Platform.get().inControllerMode()) {
                vehicle.provideClientInput(Platform.get().controllerAccel(), Platform.get().controllerBrake(), self.input.left, self.input.right, Platform.get().controllerDrift());
            } else {
                vehicle.provideClientInput(self.input.up, self.input.down, self.input.left, self.input.right, self.input.jumping);
            }
        }
    }
}
