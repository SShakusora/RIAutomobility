package com.sshakusora.riautomobility.model.gecko.frame.lorry;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

public class LorryRenderer implements GeoRenderer<LorryAnimatable> {
    private final GeoModel<LorryAnimatable> model;
    private final LorryAnimatable animatable;

    public LorryRenderer(GeoModel<LorryAnimatable> model, LorryAnimatable animatable) {
        this.model = model;
        this.animatable = animatable;
    }

    @Override
    public GeoModel<LorryAnimatable> getGeoModel() {
        return model;
    }

    @Override
    public LorryAnimatable getAnimatable() {
        return animatable;
    }

    @Override
    public List<GeoRenderLayer<LorryAnimatable>> getRenderLayers() {
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
    public void updateAnimatedTextureFrame(LorryAnimatable animatable) {
    }
}
