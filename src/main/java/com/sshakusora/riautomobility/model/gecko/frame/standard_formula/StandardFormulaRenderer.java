package com.sshakusora.riautomobility.model.gecko.frame.standard_formula;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

public class StandardFormulaRenderer implements GeoRenderer<StandardFormulaAnimatable> {
    private final GeoModel<StandardFormulaAnimatable> model;
    private final StandardFormulaAnimatable animatable;

    public StandardFormulaRenderer(GeoModel<StandardFormulaAnimatable> model, StandardFormulaAnimatable animatable) {
        this.model = model;
        this.animatable = animatable;
    }

    @Override
    public GeoModel<StandardFormulaAnimatable> getGeoModel() {
        return model;
    }

    @Override
    public StandardFormulaAnimatable getAnimatable() {
        return animatable;
    }

    @Override
    public List<GeoRenderLayer<StandardFormulaAnimatable>> getRenderLayers() {
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
    public void updateAnimatedTextureFrame(StandardFormulaAnimatable animatable) {
    }
}
