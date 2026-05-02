package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.definition.RIAutomobileRegistry;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.render.AutomobileRenderer;
import io.github.foundationgames.automobility.automobile.render.RenderableAutomobile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AutomobileRenderer.class)
public class AutomobileRendererMixin {
    @Redirect(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIFLio/github/foundationgames/automobility/automobile/render/RenderableAutomobile;)V",
            at = @At(value = "INVOKE", target = "Lio/github/foundationgames/automobility/automobile/render/RenderableAutomobile;getEngine()Lio/github/foundationgames/automobility/automobile/AutomobileEngine;"),
            remap = false
    )
    private static AutomobileEngine hideFrameEngine(RenderableAutomobile automobile) {
        if (RIAutomobileRegistry.hidesEngine(automobile.getFrame())) {
            return AutomobileEngine.EMPTY;
        }

        return automobile.getEngine();
    }
}
