package com.sshakusora.riautomobility.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sshakusora.riautomobility.entity.DriverSeatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class DriverSeatEntityRenderer extends EntityRenderer<DriverSeatEntity> {
    public DriverSeatEntityRenderer (EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DriverSeatEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
//        Level level = entity.level();
//        BlockState state = Blocks.GLASS.defaultBlockState();
//        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
//                state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY
//        );
    }

    @Override
    public ResourceLocation getTextureLocation(DriverSeatEntity enitity) {
        return null;
    }
}
