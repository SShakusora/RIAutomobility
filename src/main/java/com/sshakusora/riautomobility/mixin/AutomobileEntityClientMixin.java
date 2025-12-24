package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@OnlyIn(Dist.CLIENT)
@Mixin(AutomobileEntity.class)
public class AutomobileEntityClientMixin {
    @Inject(method = "provideClientInput", at = @At("HEAD"), cancellable = true, remap = false)
    public void disableNotDriverInput(boolean fwd, boolean back, boolean left, boolean right, boolean space, CallbackInfo ci) {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;

        List<Entity> passengers = self.getPassengers();
        if(passengers.isEmpty() || passengers.get(0).getFirstPassenger() != player) ci.cancel();
    }
}
