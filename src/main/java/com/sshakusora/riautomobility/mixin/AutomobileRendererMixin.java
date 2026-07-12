package com.sshakusora.riautomobility.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sshakusora.riautomobility.definition.RIAutomobileRegistry;
import com.sshakusora.riautomobility.model.bbmodel.BbRenderContext;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.render.AutomobileRenderer;
import io.github.foundationgames.automobility.automobile.render.RenderableAutomobile;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AutomobileRenderer.class)
public class AutomobileRendererMixin {
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIFLio/github/foundationgames/automobility/automobile/render/RenderableAutomobile;)V",
            at = @At("HEAD"),
            remap = false
    )
    private static void riautomobility$beginBbModelRender(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float tickDelta, RenderableAutomobile automobile, CallbackInfo ci) {
        BbRenderContext.begin(buffers, automobile, tickDelta);
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIFLio/github/foundationgames/automobility/automobile/render/RenderableAutomobile;)V",
            at = @At("RETURN"),
            remap = false
    )
    private static void riautomobility$endBbModelRender(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float tickDelta, RenderableAutomobile automobile, CallbackInfo ci) {
        BbRenderContext.end();
    }

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
