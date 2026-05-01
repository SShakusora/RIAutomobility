package com.sshakusora.riautomobility.model.gecko.wheel.dmc12;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

public class DmcWheelRenderer implements GeoRenderer<DmcWheelAnimatable> {
    private final GeoModel<DmcWheelAnimatable> model;
    private final DmcWheelAnimatable animatable;

    public DmcWheelRenderer(GeoModel<DmcWheelAnimatable> model, DmcWheelAnimatable animatable) {
        this.model = model;
        this.animatable = animatable;
    }

    @Override
    public GeoModel<DmcWheelAnimatable> getGeoModel() {
        return model;
    }

    @Override
    public DmcWheelAnimatable getAnimatable() {
        return animatable;
    }

    @Override
    public List<GeoRenderLayer<DmcWheelAnimatable>> getRenderLayers() {
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
    public void updateAnimatedTextureFrame(DmcWheelAnimatable animatable) {
    }
}
