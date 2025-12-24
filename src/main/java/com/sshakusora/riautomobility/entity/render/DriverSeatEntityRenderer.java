package com.sshakusora.riautomobility.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sshakusora.riautomobility.entity.DriverSeatEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class DriverSeatEntityRenderer extends EntityRenderer<DriverSeatEntity> {
    public DriverSeatEntityRenderer (EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DriverSeatEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {}

    @Override
    public ResourceLocation getTextureLocation(DriverSeatEntity enitity) {
        return null;
    }
}
