package com.sshakusora.riautomobility.model.gecko;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.List;

public class AllocationFreeGeoRenderer<T extends GeoAnimatable> implements GeoRenderer<T> {
    private final GeoModel<T> model;
    private final T animatable;
    private final Vector3f normalScratch = new Vector3f();
    private final Vector4f vertexScratch = new Vector4f();

    public AllocationFreeGeoRenderer(GeoModel<T> model, T animatable) {
        this.model = model;
        this.animatable = animatable;
    }

    @Override
    public GeoModel<T> getGeoModel() {
        return model;
    }

    @Override
    public T getAnimatable() {
        return animatable;
    }

    @Override
    public List<GeoRenderLayer<T>> getRenderLayers() {
        return List.of();
    }

    @Override
    public void renderCube(PoseStack poseStack, GeoCube cube, VertexConsumer buffer, int packedLight,
                           int packedOverlay, float red, float green, float blue, float alpha) {
        RenderUtils.translateToPivotPoint(poseStack, cube);
        RenderUtils.rotateMatrixAroundCube(poseStack, cube);
        RenderUtils.translateAwayFromPivotPoint(poseStack, cube);
        Matrix3f normalMatrix = poseStack.last().normal();
        Matrix4f pose = poseStack.last().pose();
        for (GeoQuad quad : cube.quads()) {
            if (quad == null) continue;
            normalScratch.set(quad.normal());
            normalMatrix.transform(normalScratch);
            RenderUtils.fixInvertedFlatCube(cube, normalScratch);
            createVerticesOfQuad(quad, pose, normalScratch, buffer, packedLight, packedOverlay,
                    red, green, blue, alpha);
        }
    }

    @Override
    public void createVerticesOfQuad(GeoQuad quad, Matrix4f pose, Vector3f normal, VertexConsumer buffer,
                                     int packedLight, int packedOverlay,
                                     float red, float green, float blue, float alpha) {
        for (GeoVertex vertex : quad.vertices()) {
            Vector3f position = vertex.position();
            vertexScratch.set(position.x(), position.y(), position.z(), 1.0F);
            pose.transform(vertexScratch);
            buffer.vertex(vertexScratch.x(), vertexScratch.y(), vertexScratch.z(), red, green, blue, alpha,
                    vertex.texU(), vertex.texV(), packedOverlay, packedLight,
                    normal.x(), normal.y(), normal.z());
        }
    }

    @Override
    public void fireCompileRenderLayersEvent() {
    }

    @Override
    public boolean firePreRenderEvent(PoseStack poseStack, BakedGeoModel model,
                                      MultiBufferSource bufferSource, float partialTick, int packedLight) {
        return true;
    }

    @Override
    public void firePostRenderEvent(PoseStack poseStack, BakedGeoModel model,
                                    MultiBufferSource bufferSource, float partialTick, int packedLight) {
    }

    @Override
    public void updateAnimatedTextureFrame(T animatable) {
    }
}
