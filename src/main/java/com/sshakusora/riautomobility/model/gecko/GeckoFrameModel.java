package com.sshakusora.riautomobility.model.gecko;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class GeckoFrameModel<T extends GeoAnimatable> extends Model {

    private final GeoModel<T> geoModel;
    private final GeoRenderer<T> geoRenderer;
    private final T animatable;

    public GeckoFrameModel(GeoModel<T> geoModel, GeoRenderer<T> geoRenderer, T animatable) {
        super(RenderType::entityCutoutNoCull);
        this.geoModel = geoModel;
        this.geoRenderer = geoRenderer;
        this.animatable = animatable;
    }

//    @Override
//    public RenderType renderType(ResourceLocation texture) {
//        return geoRenderer.getRenderType(animatable, texture, null, 0);
//    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.scale(-1, -1, 1);
        MultiBufferSource.BufferSource fakeBufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        geoRenderer.defaultRender(
                poseStack,
                animatable,
                fakeBufferSource,
                geoRenderer.getRenderType(animatable, geoRenderer.getTextureLocation(animatable), fakeBufferSource, 0),
                buffer,
                0,
                0,
                packedLight
        );

        fakeBufferSource.endBatch();
        poseStack.popPose();
    }
}
