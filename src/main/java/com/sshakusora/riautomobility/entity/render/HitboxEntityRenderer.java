package com.sshakusora.riautomobility.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sshakusora.riautomobility.entity.HitboxEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class HitboxEntityRenderer extends EntityRenderer<HitboxEntity> {
    public HitboxEntityRenderer (EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(HitboxEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {}

    @Override
    public ResourceLocation getTextureLocation(HitboxEntity enitity) {
        return null;
    }
}
