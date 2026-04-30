package com.sshakusora.riautomobility.model.gecko.wheel.standard_formula;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

public class StandardFormulaWheelRenderer implements GeoRenderer<StandardFormulaWheelAnimatable> {
    private final GeoModel<StandardFormulaWheelAnimatable> model;
    private final StandardFormulaWheelAnimatable animatable;

    public StandardFormulaWheelRenderer(GeoModel<StandardFormulaWheelAnimatable> model, StandardFormulaWheelAnimatable animatable) {
        this.model = model;
        this.animatable = animatable;
    }

    @Override
    public GeoModel<StandardFormulaWheelAnimatable> getGeoModel() {
        return model;
    }

    @Override
    public StandardFormulaWheelAnimatable getAnimatable() {
        return animatable;
    }

    @Override
    public List<GeoRenderLayer<StandardFormulaWheelAnimatable>> getRenderLayers() {
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
    public void updateAnimatedTextureFrame(StandardFormulaWheelAnimatable animatable) {
    }
}
