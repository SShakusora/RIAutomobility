package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.entity.DriverSeatEntity;
import io.github.foundationgames.automobility.sound.AutomobileSoundInstance;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AutomobileSoundInstance.class)
public class AutomobileSoundInstanceMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getVehicle()Lnet/minecraft/world/entity/Entity;"))
    private Entity getVehicle(LocalPlayer player) {
        //TODO: Fix sound
        if (player.getVehicle() instanceof DriverSeatEntity seat) {
            return seat.getVehicle();
        }
        return player.getVehicle();
    }
}
