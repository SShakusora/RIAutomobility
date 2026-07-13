package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.model.bbmodel.BbRenderContext;
import io.github.foundationgames.automobility.forge.client.BEWLRs;
import io.github.foundationgames.automobility.util.HexCons;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BEWLRs.class)
public abstract class BEWLRsMixin {
    @Redirect(
            method = "tryRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/github/foundationgames/automobility/util/HexCons;accept(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V"
            ),
            remap = false
    )
    private static void riautomobility$provideBbItemTextureBuffers(
            HexCons<Object, Object, Object, Object, Object, Object> renderer,
            Object stack, Object displayContext, Object pose, Object bufferSource,
            Object light, Object overlay
    ) {
        MultiBufferSource buffers = (MultiBufferSource) bufferSource;
        BbRenderContext.begin(buffers, null, 0.0F);
        try {
            renderer.accept(stack, displayContext, pose, bufferSource, light, overlay);
        } finally {
            BbRenderContext.end();
        }
    }
}
