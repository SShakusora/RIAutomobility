package com.sshakusora.riautomobility.model.gecko;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

import java.util.function.Consumer;

public class GeckoFrameModel<T extends GeoAnimatable> extends Model {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final GeoModel<T> geoModel;
    private final GeoRenderer<T> geoRenderer;
    private final T animatable;
    private final Model fallbackModel;
    private final ResourceLocation missingComponentId;
    private final Consumer<ResourceLocation> missingComponentCallback;
    private boolean broken;
    private boolean loggedBroken;

    public GeckoFrameModel(GeoModel<T> geoModel, GeoRenderer<T> geoRenderer, T animatable) {
        this(geoModel, geoRenderer, animatable, null, null, null);
    }

    public GeckoFrameModel(GeoModel<T> geoModel, GeoRenderer<T> geoRenderer, T animatable, Model fallbackModel) {
        this(geoModel, geoRenderer, animatable, fallbackModel, null, null);
    }

    public GeckoFrameModel(GeoModel<T> geoModel, GeoRenderer<T> geoRenderer, T animatable, Model fallbackModel, ResourceLocation missingComponentId, Consumer<ResourceLocation> missingComponentCallback) {
        super(RenderType::entityCutoutNoCull);
        this.geoModel = geoModel;
        this.geoRenderer = geoRenderer;
        this.animatable = animatable;
        this.fallbackModel = fallbackModel;
        this.missingComponentId = missingComponentId;
        this.missingComponentCallback = missingComponentCallback;
    }

//    @Override
//    public RenderType renderType(ResourceLocation texture) {
//        return geoRenderer.getRenderType(animatable, texture, null, 0);
//    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (this.broken) {
            renderFallback(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }

        poseStack.pushPose();
        poseStack.scale(-1, -1, 1);
        MultiBufferSource.BufferSource fakeBufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        try {
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
        } catch (RuntimeException exception) {
            this.broken = true;
            if (this.missingComponentId != null && this.missingComponentCallback != null) {
                this.missingComponentCallback.accept(this.missingComponentId);
            }
            if (!this.loggedBroken) {
                this.loggedBroken = true;
                LOGGER.error("Failed to render GeckoLib automobile model {}; falling back to placeholder", geoModel.getClass().getName(), exception);
            }
            renderFallback(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }

        fakeBufferSource.endBatch();
        poseStack.popPose();
    }

    private void renderFallback(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (this.fallbackModel == null) {
            return;
        }
        this.fallbackModel.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
