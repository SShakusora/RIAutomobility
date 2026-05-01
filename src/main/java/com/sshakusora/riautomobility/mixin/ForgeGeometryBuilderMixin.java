package com.sshakusora.riautomobility.mixin;

import io.github.foundationgames.automobility.block.model.GeometryBuilder;
import io.github.foundationgames.automobility.forge.block.render.ForgeGeometryBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ForgeGeometryBuilder.class)
public class ForgeGeometryBuilderMixin {
    @Inject(method = "vertex(FFFLnet/minecraft/core/Direction;FFFLnet/minecraft/client/renderer/texture/TextureAtlasSprite;FFI)Lio/github/foundationgames/automobility/block/model/GeometryBuilder;", at = @At("HEAD"), cancellable = true, remap = false)
    private void riautomobility$vertexNullCheck(float x, float y, float z, Direction face, float nx, float ny, float nz, TextureAtlasSprite sprite, float u, float v, int color, CallbackInfoReturnable<GeometryBuilder> cir) {
        if (sprite == null) {
            cir.setReturnValue((GeometryBuilder) this);
        }
    }
}
