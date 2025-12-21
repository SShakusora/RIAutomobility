package com.sshakusora.riautomobility.model.gecko.frame.lobby;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

public class LobbyRenderer implements GeoRenderer<LobbyAnimatable> {
    private final GeoModel<LobbyAnimatable> model;
    private final LobbyAnimatable animatable;

    public LobbyRenderer(GeoModel<LobbyAnimatable> model, LobbyAnimatable animatable) {
        this.model = model;
        this.animatable = animatable;
    }

    @Override
    public GeoModel<LobbyAnimatable> getGeoModel() {
        return model;
    }

    @Override
    public LobbyAnimatable getAnimatable() {
        return animatable;
    }

    @Override
    public List<GeoRenderLayer<LobbyAnimatable>> getRenderLayers() {
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
    public void updateAnimatedTextureFrame(LobbyAnimatable animatable) {
    }
}
