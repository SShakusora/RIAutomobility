package com.sshakusora.riautomobility.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sshakusora.riautomobility.entity.SeatEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class DriverSeatEntityRenderer extends EntityRenderer<SeatEntity> {
    public DriverSeatEntityRenderer (EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SeatEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {}

    @Override
    public ResourceLocation getTextureLocation(SeatEntity enitity) {
        return null;
    }
}
