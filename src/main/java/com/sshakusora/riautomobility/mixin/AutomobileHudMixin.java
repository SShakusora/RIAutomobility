package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import io.github.foundationgames.automobility.screen.AutomobileHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AutomobileHud.class)
public class AutomobileHudMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lio/github/foundationgames/automobility/screen/AutomobileHud;renderControlHints(Lnet/minecraft/client/gui/GuiGraphics;F)V"), cancellable = true, remap = false)
    private static void disablePassengerControlHintsHud(GuiGraphics graphics, Player player, AutomobileEntity auto, float tickDelta, CallbackInfo ci) {
        if (!RIAutomobileFrame.isRIAutomobileFrame(auto.getFrame())) return;
        if (player == auto.getFirstPassenger()) return;
        ci.cancel();
    }
}
