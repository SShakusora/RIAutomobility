package com.sshakusora.riautomobility.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import io.github.foundationgames.automobility.automobile.render.AutomobileRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

public class RIAutomobileEntityRenderer extends EntityRenderer<RIAutomobileEntity> {
    public RIAutomobileEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(RIAutomobileEntity entity) {
        return null;
    }

    @Override
    public void render(RIAutomobileEntity entity, float yaw, float tickDelta, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float angX = entity.getDisplacement().getAngularX(tickDelta);
        float angZ = entity.getDisplacement().getAngularZ(tickDelta);
        float offsetY = entity.getDisplacement().getVertical(tickDelta);

        pose.translate(0, offsetY, 0);
        pose.mulPose(new Quaternionf().rotationXYZ((float) Math.toRadians(angX), 0, (float) Math.toRadians(angZ)));

        AutomobileRenderer.render(pose, buffers, light, OverlayTexture.NO_OVERLAY, tickDelta, entity);
        pose.popPose();
    }
}
