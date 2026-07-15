package com.sshakusora.riautomobility.model.bbmodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.model.PlaceholderAutomobileModel;
import com.sshakusora.riautomobility.model.RIAutomobileModels;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.*;
import org.slf4j.Logger;

import java.lang.Math;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public final class DynamicBbModel extends Model {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float PIXEL = 1.0F / 16.0F;

    private final ResourceLocation modelResource;
    private final FrameSpec.ModelSpec spec;
    private final Model fallbackModel;
    private final ResourceLocation componentId;
    private final Consumer<ResourceLocation> missingCallback;
    private final Quaternionf modelRotation;
    private final RenderScratch renderScratch = new RenderScratch();
    private BbModelData.Document compiledDocument;
    private Map<BbModelData.ElementNode, List<BbCompiledGeometry.Quad>> compiledGeometry = Map.of();
    private Map<BbModelData.Node, BbCompiledGeometry.NodeTransform> compiledTransforms = Map.of();
    private BbCompiledGeometry.StaticGeometry staticGeometry = BbCompiledGeometry.StaticGeometry.EMPTY;
    private boolean staticModel;
    private boolean loggedBroken;

    public DynamicBbModel(ResourceLocation componentId, FrameSpec.ModelSpec spec, Model fallbackModel, Consumer<ResourceLocation> missingCallback) {
        super(renderTypeFactory(spec.renderType()));
        this.modelResource = spec.bbModel();
        this.spec = spec;
        this.fallbackModel = fallbackModel;
        this.componentId = componentId;
        this.missingCallback = missingCallback;
        this.modelRotation = new Quaternionf().rotationY((float) Math.toRadians(spec.rotationY()));
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer defaultConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        BbModelData.Document document = BbModelRepository.get(this.modelResource);
        if (document == null) {
            fail(new BbModelFormatException("BBModel resource is not loaded: " + this.modelResource));
            renderFallback(poseStack, defaultConsumer, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }

        try {
            prepareGeometry(document);
            BbRenderContext context = BbRenderContext.current();
            if (this.staticModel) {
                renderStatic(poseStack.last(), context, defaultConsumer, packedLight, packedOverlay,
                        red, green, blue, alpha);
                RIAutomobileModels.clearMissingComponent(this.componentId);
                return;
            }
            Map<String, BbAnimationPlayer.Transform> animations = BbAnimationPlayer.sample(document, this.spec.bbAnimation(), context);
            Matrix4f rootPose = this.renderScratch.pose(0).set(poseStack.last().pose());
            Matrix3f rootNormal = this.renderScratch.normal(0).set(poseStack.last().normal());
            if (this.spec.rotationY() != 0.0F) {
                rootPose.rotate(this.modelRotation);
                rootNormal.rotate(this.modelRotation);
            }
            for (BbModelData.Node root : document.roots()) {
                renderNode(root, animations, context, rootPose, rootNormal, 1, defaultConsumer,
                        packedLight, packedOverlay, red, green, blue, alpha);
            }
            RIAutomobileModels.clearMissingComponent(this.componentId);
        } catch (RuntimeException exception) {
            fail(exception);
            renderFallback(poseStack, defaultConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    private void renderNode(BbModelData.Node node, Map<String, BbAnimationPlayer.Transform> animations,
                            BbRenderContext context, Matrix4f parentPose, Matrix3f parentNormal, int depth,
                            VertexConsumer defaultConsumer,
                            int light, int overlay, float red, float green, float blue, float alpha) {
        if (!node.visible()) return;
        BbAnimationPlayer.Transform animation = animations.getOrDefault(node.uuid(), BbAnimationPlayer.Transform.IDENTITY);
        BbCompiledGeometry.NodeTransform transform = this.compiledTransforms.get(node);
        Matrix4f pose = this.renderScratch.pose(depth).set(parentPose);
        Matrix3f normal = this.renderScratch.normal(depth).set(parentNormal);
        float scaleX;
        float scaleY;
        float scaleZ;
        if (animation == BbAnimationPlayer.Transform.IDENTITY) {
            pose.translate(transform.translation()).rotate(transform.quaternion());
            normal.rotate(transform.quaternion());
            scaleX = transform.scale().x;
            scaleY = transform.scale().y;
            scaleZ = transform.scale().z;
        } else {
            Vector3f animatedPosition = animation.position();
            Vector3f translation = this.renderScratch.translation
                    .set(animatedPosition.x, -animatedPosition.y, animatedPosition.z)
                    .mul(PIXEL).add(transform.translation());
            Vector3f animatedRotation = animation.rotation();
            Vector3f rotation = this.renderScratch.rotation
                    .set(-animatedRotation.x, animatedRotation.y, -animatedRotation.z)
                    .add(transform.rotation());
            Quaternionf quaternion = this.renderScratch.quaternion.rotationZYX(
                    (float) Math.toRadians(rotation.z),
                    (float) Math.toRadians(rotation.y),
                    (float) Math.toRadians(rotation.x));
            pose.translate(translation).rotate(quaternion);
            normal.rotate(quaternion);
            scaleX = transform.scale().x * animation.scale().x;
            scaleY = transform.scale().y * animation.scale().y;
            scaleZ = transform.scale().z * animation.scale().z;
        }
        pose.scale(scaleX, scaleY, scaleZ);
        scaleNormal(normal, scaleX, scaleY, scaleZ);

        if (node instanceof BbModelData.GroupNode group) {
            for (BbModelData.Node child : group.children()) {
                renderNode(child, animations, context, pose, normal, depth + 1, defaultConsumer,
                        light, overlay, red, green, blue, alpha);
            }
        } else if (node instanceof BbModelData.ElementNode element) {
            renderGeometry(element, context, pose, normal, defaultConsumer,
                    light, overlay, red, green, blue, alpha);
        }
    }

    private static void scaleNormal(Matrix3f normal, float scaleX, float scaleY, float scaleZ) {
        if (scaleX == scaleY && scaleY == scaleZ) {
            if (scaleX < 0.0F) normal.scale(-1.0F);
            return;
        }
        float inverseX = 1.0F / scaleX;
        float inverseY = 1.0F / scaleY;
        float inverseZ = 1.0F / scaleZ;
        float normalization = Mth.fastInvCubeRoot(inverseX * inverseY * inverseZ);
        normal.scale(normalization * inverseX, normalization * inverseY, normalization * inverseZ);
    }

    private void renderGeometry(BbModelData.ElementNode element, BbRenderContext context,
                                Matrix4f pose, Matrix3f normal, VertexConsumer defaultConsumer,
                                int light, int overlay, float red, float green, float blue, float alpha) {
        BbModelRepository.ResolvedTexture lastTexture = null;
        VertexConsumer consumer = defaultConsumer;
        for (BbCompiledGeometry.Quad quad : this.compiledGeometry.getOrDefault(element, List.of())) {
            if (!quad.texture().equals(lastTexture)) {
                lastTexture = quad.texture();
                consumer = consumer(context, defaultConsumer, lastTexture);
            }
            emitCompiledQuad(pose, normal, consumer, quad, light, overlay, red, green, blue, alpha);
        }
    }

    private void renderStatic(PoseStack.Pose pose, BbRenderContext context, VertexConsumer fallback,
                              int light, int overlay, float red, float green, float blue, float alpha) {
        int lod = lodLevel(pose.pose(), context);
        if (BbInstancedRenderer.tryEnqueue(this, this.staticGeometry, pose, context, lod,
                light, overlay, red, green, blue, alpha)) {
            return;
        }
        for (BbCompiledGeometry.Batch batch : this.staticGeometry.batches()) {
            VertexConsumer consumer = consumer(context, fallback, batch.texture());
            emitPackedBatch(pose, consumer, batch, lod, light, overlay, red, green, blue, alpha);
        }
    }

    private static int lodLevel(Matrix4f pose, BbRenderContext context) {
        if (context == null || !(context.automobile() instanceof Entity)) return 0;
        float distanceSquared = pose.m30() * pose.m30() + pose.m31() * pose.m31() + pose.m32() * pose.m32();
        if (distanceSquared > 128.0F * 128.0F) return 3;
        if (distanceSquared > 80.0F * 80.0F) return 2;
        if (distanceSquared > 48.0F * 48.0F) return 1;
        return 0;
    }

    private void prepareGeometry(BbModelData.Document document) {
        if (this.compiledDocument == document) return;
        Map<BbModelData.ElementNode, List<BbCompiledGeometry.Quad>> geometry =
                BbCompiledGeometry.compile(this.modelResource, this.spec, document);
        this.staticModel = document.animations().isEmpty();
        if (this.staticModel) {
            this.staticGeometry = BbCompiledGeometry.compileStatic(this.spec, document, geometry);
            this.compiledGeometry = Map.of();
            this.compiledTransforms = Map.of();
            LOGGER.debug("Compiled static BBModel {}: {} nodes, {} quads, {} batches ({} duplicates removed)",
                    this.modelResource, this.staticGeometry.nodeCount(), this.staticGeometry.outputQuadCount(),
                    this.staticGeometry.batches().size(),
                    this.staticGeometry.inputQuadCount() - this.staticGeometry.outputQuadCount());
        } else {
            this.staticGeometry = BbCompiledGeometry.StaticGeometry.EMPTY;
            this.compiledGeometry = geometry;
            this.compiledTransforms = BbCompiledGeometry.compileTransforms(document);
        }
        this.compiledDocument = document;
    }

    private static void emitPackedBatch(PoseStack.Pose pose, VertexConsumer consumer,
                                        BbCompiledGeometry.Batch batch, int lod,
                                        int light, int overlay,
                                        float red, float green, float blue, float alpha) {
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        float[] data = batch.data();
        byte[] detailLevels = batch.detailLevels();
        for (int quadIndex = 0; quadIndex < detailLevels.length; quadIndex++) {
            if (detailLevels[quadIndex] < lod) continue;
            int cursor = quadIndex * BbCompiledGeometry.PACKED_QUAD_STRIDE;
            float normalX = data[cursor++];
            float normalY = data[cursor++];
            float normalZ = data[cursor++];
            float transformedNormalX = normalMatrix.m00() * normalX + normalMatrix.m10() * normalY + normalMatrix.m20() * normalZ;
            float transformedNormalY = normalMatrix.m01() * normalX + normalMatrix.m11() * normalY + normalMatrix.m21() * normalZ;
            float transformedNormalZ = normalMatrix.m02() * normalX + normalMatrix.m12() * normalY + normalMatrix.m22() * normalZ;
            for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                float x = data[cursor++];
                float y = data[cursor++];
                float z = data[cursor++];
                float u = data[cursor++];
                float v = data[cursor++];
                emitVertex(consumer, matrix, x, y, z, u, v, transformedNormalX, transformedNormalY,
                        transformedNormalZ, light, overlay, red, green, blue, alpha);
            }
        }
    }

    private static void emitCompiledQuad(Matrix4f matrix, Matrix3f normalMatrix, VertexConsumer consumer,
                                         BbCompiledGeometry.Quad quad,
                                         int light, int overlay,
                                         float red, float green, float blue, float alpha) {
        float normalX = quad.normal().x;
        float normalY = quad.normal().y;
        float normalZ = quad.normal().z;
        float transformedNormalX = normalMatrix.m00() * normalX + normalMatrix.m10() * normalY + normalMatrix.m20() * normalZ;
        float transformedNormalY = normalMatrix.m01() * normalX + normalMatrix.m11() * normalY + normalMatrix.m21() * normalZ;
        float transformedNormalZ = normalMatrix.m02() * normalX + normalMatrix.m12() * normalY + normalMatrix.m22() * normalZ;
        for (int index = 0; index < 4; index++) {
            Vector3f vertex = quad.vertices()[index];
            Vector2f tex = quad.uvs()[index];
            emitVertex(consumer, matrix, vertex.x, vertex.y, vertex.z, tex.x, tex.y,
                    transformedNormalX, transformedNormalY, transformedNormalZ,
                    light, overlay, red, green, blue, alpha);
        }
    }

    private static void emitVertex(VertexConsumer consumer, Matrix4f matrix,
                                   float x, float y, float z, float u, float v,
                                   float normalX, float normalY, float normalZ,
                                   int light, int overlay,
                                   float red, float green, float blue, float alpha) {
        float transformedX = matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30();
        float transformedY = matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31();
        float transformedZ = matrix.m02() * x + matrix.m12() * y + matrix.m22() * z + matrix.m32();
        consumer.vertex(transformedX, transformedY, transformedZ, red, green, blue, alpha,
                u, v, overlay, light, normalX, normalY, normalZ);
    }

    private static final class RenderScratch {
        private Matrix4f[] poses = createPoses(16);
        private Matrix3f[] normals = createNormals(16);
        private final Vector3f translation = new Vector3f();
        private final Vector3f rotation = new Vector3f();
        private final Quaternionf quaternion = new Quaternionf();

        Matrix4f pose(int depth) {
            ensureCapacity(depth);
            return poses[depth];
        }

        Matrix3f normal(int depth) {
            ensureCapacity(depth);
            return normals[depth];
        }

        private void ensureCapacity(int depth) {
            if (depth < poses.length) return;
            int previous = poses.length;
            int capacity = Math.max(depth + 1, previous * 2);
            poses = Arrays.copyOf(poses, capacity);
            normals = Arrays.copyOf(normals, capacity);
            for (int index = previous; index < capacity; index++) {
                poses[index] = new Matrix4f();
                normals[index] = new Matrix3f();
            }
        }

        private static Matrix4f[] createPoses(int size) {
            Matrix4f[] values = new Matrix4f[size];
            for (int index = 0; index < size; index++) values[index] = new Matrix4f();
            return values;
        }

        private static Matrix3f[] createNormals(int size) {
            Matrix3f[] values = new Matrix3f[size];
            for (int index = 0; index < size; index++) values[index] = new Matrix3f();
            return values;
        }
    }

    private VertexConsumer consumer(BbRenderContext context, VertexConsumer fallback, BbModelRepository.ResolvedTexture texture) {
        if (context == null) {
            return fallback;
        }
        return context.buffers().getBuffer(renderType(texture.location(), texture.renderMode(), this.spec.renderType()));
    }

    boolean supportsInstancedRendering() {
        String configuredType = this.spec.renderType().toLowerCase(Locale.ROOT);
        if (configuredType.contains("translucent")) return false;
        for (BbCompiledGeometry.Batch batch : this.staticGeometry.batches()) {
            if (!"default".equalsIgnoreCase(batch.texture().renderMode())) return false;
        }
        return true;
    }

    RenderType instancedRenderType(BbModelRepository.ResolvedTexture texture) {
        return renderType(texture.location(), texture.renderMode(), this.spec.renderType());
    }

    ResourceLocation modelResource() {
        return this.modelResource;
    }

    private void renderFallback(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
        if (this.fallbackModel == null) {
            return;
        }
        BbRenderContext context = BbRenderContext.current();
        VertexConsumer fallbackConsumer = context == null
                ? consumer
                : context.buffers().getBuffer(this.fallbackModel.renderType(PlaceholderAutomobileModel.TEXTURE));
        this.fallbackModel.renderToBuffer(poseStack, fallbackConsumer, light, overlay, red, green, blue, alpha);
    }

    private void fail(RuntimeException exception) {
        this.missingCallback.accept(this.componentId);
        if (!this.loggedBroken) {
            this.loggedBroken = true;
            LOGGER.error("Failed to render Blockbench automobile model {}", this.modelResource, exception);
        }
    }

    public static Function<ResourceLocation, RenderType> renderTypeFactory(String name) {
        return texture -> renderType(texture, "default", name);
    }

    private static RenderType renderType(ResourceLocation texture, String textureMode, String configuredType) {
        if ("emissive".equalsIgnoreCase(textureMode)) {
            return RenderType.eyes(texture);
        }
        return switch (configuredType) {
            case "entity_cutout_no_cull" -> RenderType.entityCutoutNoCull(texture);
            case "entity_translucent" -> RenderType.entityTranslucent(texture);
            case "entity_translucent_cull" -> RenderType.entityTranslucentCull(texture);
            case "entity_solid" -> RenderType.entitySolid(texture);
            default -> RenderType.entityCutout(texture);
        };
    }

}
