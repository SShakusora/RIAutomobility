package com.sshakusora.riautomobility.model.gecko.frame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

public class TestGeckoFrameRenderer implements GeoRenderer<TestGeckoFrameAnimatable> {
    private final GeoModel<TestGeckoFrameAnimatable> model;
    private final TestGeckoFrameAnimatable animatable;

    public TestGeckoFrameRenderer(GeoModel<TestGeckoFrameAnimatable> model, TestGeckoFrameAnimatable animatable) {
        this.model = model;
        this.animatable = animatable;
    }

    @Override
    public GeoModel<TestGeckoFrameAnimatable> getGeoModel() {
        return model;
    }

    @Override
    public TestGeckoFrameAnimatable getAnimatable() {
        return animatable;
    }

    @Override
    public List<GeoRenderLayer<TestGeckoFrameAnimatable>> getRenderLayers() {
        return List.of();
    }

    @Override
    public void fireCompileRenderLayersEvent() {
    }

    @Override
    public boolean firePreRenderEvent(PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
        return true;
    }

    @Override
    public void firePostRenderEvent(PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
    }

    @Override
    public void updateAnimatedTextureFrame(TestGeckoFrameAnimatable animatable) {
    }
}
