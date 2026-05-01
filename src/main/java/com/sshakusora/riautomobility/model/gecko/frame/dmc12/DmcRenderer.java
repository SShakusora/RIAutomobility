package com.sshakusora.riautomobility.model.gecko.frame.dmc12;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

public class DmcRenderer implements GeoRenderer<DmcAnimatable> {
    private final GeoModel<DmcAnimatable> model;
    private final DmcAnimatable animatable;

    public DmcRenderer(GeoModel<DmcAnimatable> model, DmcAnimatable animatable) {
        this.model = model;
        this.animatable = animatable;
    }

    @Override
    public GeoModel<DmcAnimatable> getGeoModel() {
        return model;
    }

    @Override
    public DmcAnimatable getAnimatable() {
        return animatable;
    }

    @Override
    public List<GeoRenderLayer<DmcAnimatable>> getRenderLayers() {
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
    public void updateAnimatedTextureFrame(DmcAnimatable animatable) {
    }
}
