package com.sshakusora.riautomobility.model.gecko;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

public class DynamicGeckoRenderer implements GeoRenderer<DynamicGeckoAnimatable> {
    private final GeoModel<DynamicGeckoAnimatable> model;
    private final DynamicGeckoAnimatable animatable;

    public DynamicGeckoRenderer(GeoModel<DynamicGeckoAnimatable> model, DynamicGeckoAnimatable animatable) {
        this.model = model;
        this.animatable = animatable;
    }

    @Override
    public GeoModel<DynamicGeckoAnimatable> getGeoModel() {
        return this.model;
    }

    @Override
    public DynamicGeckoAnimatable getAnimatable() {
        return this.animatable;
    }

    @Override
    public List<GeoRenderLayer<DynamicGeckoAnimatable>> getRenderLayers() {
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
    public void updateAnimatedTextureFrame(DynamicGeckoAnimatable animatable) {
    }
}
