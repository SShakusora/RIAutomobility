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
import org.joml.*;
import org.slf4j.Logger;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
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
    private boolean loggedBroken;

    public DynamicBbModel(ResourceLocation componentId, FrameSpec.ModelSpec spec, Model fallbackModel, Consumer<ResourceLocation> missingCallback) {
        super(renderTypeFactory(spec.renderType()));
        this.modelResource = spec.bbModel();
        this.spec = spec;
        this.fallbackModel = fallbackModel;
        this.componentId = componentId;
        this.missingCallback = missingCallback;
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
            BbRenderContext context = BbRenderContext.current();
            Map<String, BbAnimationPlayer.Transform> animations = BbAnimationPlayer.sample(document, this.spec.bbAnimation(), context);
            poseStack.pushPose();
            try {
                if (this.spec.rotationY() != 0.0F) {
                    poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(this.spec.rotationY())));
                }
                for (BbModelData.Node root : document.roots()) {
                    renderNode(root, new Vector3f(), document, animations, context, poseStack, defaultConsumer, packedLight, packedOverlay, red, green, blue, alpha);
                }
            } finally {
                poseStack.popPose();
            }
            RIAutomobileModels.clearMissingComponent(this.componentId);
        } catch (RuntimeException exception) {
            fail(exception);
            renderFallback(poseStack, defaultConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    private void renderNode(BbModelData.Node node, Vector3f parentOrigin, BbModelData.Document document, Map<String, BbAnimationPlayer.Transform> animations, BbRenderContext context, PoseStack poseStack, VertexConsumer defaultConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        if (!node.visible()) {
            return;
        }
        BbAnimationPlayer.Transform animation = animations.getOrDefault(node.uuid(), BbAnimationPlayer.Transform.IDENTITY);
        poseStack.pushPose();
        try {
            Vector3f translation = BbCoordinateSystem.position(
                    new Vector3f(node.origin()).sub(parentOrigin).add(animation.position())
            ).mul(PIXEL);
            poseStack.translate(translation.x, translation.y, translation.z);
            Vector3f rotation = BbCoordinateSystem.rotation(
                    new Vector3f(node.rotation()).add(animation.rotation())
            );
            poseStack.mulPose(new Quaternionf().rotationZYX(
                    (float) Math.toRadians(rotation.z),
                    (float) Math.toRadians(rotation.y),
                    (float) Math.toRadians(rotation.x)
            ));
            boolean textureMesh = node instanceof BbModelData.ElementNode element && element.geometry() instanceof BbModelData.TextureMesh;
            poseStack.scale(
                    (textureMesh ? 1.0F : node.scale().x) * animation.scale().x,
                    (textureMesh ? 1.0F : node.scale().y) * animation.scale().y,
                    (textureMesh ? 1.0F : node.scale().z) * animation.scale().z
            );

            if (node instanceof BbModelData.GroupNode group) {
                for (BbModelData.Node child : group.children()) {
                    renderNode(child, group.origin(), document, animations, context, poseStack, defaultConsumer, light, overlay, red, green, blue, alpha);
                }
            } else if (node instanceof BbModelData.ElementNode element) {
                renderGeometry(element, document, context, poseStack, defaultConsumer, light, overlay, red, green, blue, alpha);
            }
        } finally {
            poseStack.popPose();
        }
    }

    private void renderGeometry(BbModelData.ElementNode element, BbModelData.Document document, BbRenderContext context, PoseStack poseStack, VertexConsumer defaultConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        if (element.geometry() instanceof BbModelData.Cube cube) {
            renderCube(element, cube, document, context, poseStack, defaultConsumer, light, overlay, red, green, blue, alpha);
        } else if (element.geometry() instanceof BbModelData.Mesh mesh) {
            renderMesh(mesh, document, context, poseStack, defaultConsumer, light, overlay, red, green, blue, alpha);
        } else if (element.geometry() instanceof BbModelData.TextureMesh mesh) {
            renderTextureMesh(element, mesh, document, context, poseStack, defaultConsumer, light, overlay, red, green, blue, alpha);
        }
    }

    private void renderCube(BbModelData.ElementNode element, BbModelData.Cube cube, BbModelData.Document document, BbRenderContext context, PoseStack poseStack, VertexConsumer defaultConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        Vector3f from = new Vector3f(cube.from()).sub(element.origin()).sub(cube.inflate(), cube.inflate(), cube.inflate()).mul(PIXEL);
        Vector3f to = new Vector3f(cube.to()).sub(element.origin()).add(cube.inflate(), cube.inflate(), cube.inflate()).mul(PIXEL);
        Map<String, Vector3f[]> vertices = Map.of(
                "north", quad(v(to.x, to.y, from.z), v(to.x, from.y, from.z), v(from.x, from.y, from.z), v(from.x, to.y, from.z)),
                "south", quad(v(from.x, to.y, to.z), v(from.x, from.y, to.z), v(to.x, from.y, to.z), v(to.x, to.y, to.z)),
                "east", quad(v(to.x, to.y, to.z), v(to.x, from.y, to.z), v(to.x, from.y, from.z), v(to.x, to.y, from.z)),
                "west", quad(v(from.x, to.y, from.z), v(from.x, from.y, from.z), v(from.x, from.y, to.z), v(from.x, to.y, to.z)),
                "up", quad(v(from.x, to.y, from.z), v(from.x, to.y, to.z), v(to.x, to.y, to.z), v(to.x, to.y, from.z)),
                "down", quad(v(from.x, from.y, to.z), v(from.x, from.y, from.z), v(to.x, from.y, from.z), v(to.x, from.y, to.z))
        );
        for (Map.Entry<String, BbModelData.CubeFace> entry : cube.faces().entrySet()) {
            BbModelData.CubeFace face = entry.getValue();
            if (!face.enabled() || !vertices.containsKey(entry.getKey())) {
                continue;
            }
            BbModelRepository.ResolvedTexture texture = BbModelRepository.resolveTexture(this.modelResource, this.spec, document, face.texture());
            VertexConsumer consumer = consumer(context, defaultConsumer, texture);
            Vector2f[] uv = cubeUv(face, cube.mirrorUv(), texture.uvWidth(), texture.uvHeight());
            emitBbQuad(poseStack.last(), consumer, vertices.get(entry.getKey()), uv, light, overlay, red, green, blue, alpha);
        }
    }

    private void renderMesh(BbModelData.Mesh mesh, BbModelData.Document document, BbRenderContext context, PoseStack poseStack, VertexConsumer defaultConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        for (BbModelData.MeshFace face : mesh.faces()) {
            if (face.vertices().size() < 3 || face.texture().disabled()) {
                continue;
            }
            BbModelRepository.ResolvedTexture texture = BbModelRepository.resolveTexture(this.modelResource, this.spec, document, face.texture());
            VertexConsumer consumer = consumer(context, defaultConsumer, texture);
            List<Vector3f> points = new ArrayList<>();
            List<Vector2f> uvs = new ArrayList<>();
            for (String vertexId : face.vertices()) {
                Vector3f point = mesh.vertices().get(vertexId);
                if (point == null) {
                    throw new BbModelFormatException("Mesh face references missing vertex " + vertexId);
                }
                points.add(new Vector3f(point).mul(PIXEL));
                Vector2f uv = face.uv().getOrDefault(vertexId, new Vector2f());
                uvs.add(new Vector2f(uv.x / texture.uvWidth(), uv.y / texture.uvHeight()));
            }
            if (points.size() == 4) {
                emitBbQuad(poseStack.last(), consumer, points.toArray(Vector3f[]::new), uvs.toArray(Vector2f[]::new), light, overlay, red, green, blue, alpha);
            } else {
                for (int index = 1; index < points.size() - 1; index++) {
                    emitTriangleAsQuad(poseStack.last(), consumer,
                            points.get(0), points.get(index), points.get(index + 1),
                            uvs.get(0), uvs.get(index), uvs.get(index + 1),
                            light, overlay, red, green, blue, alpha);
                }
            }
        }
    }

    private void renderTextureMesh(BbModelData.ElementNode element, BbModelData.TextureMesh mesh, BbModelData.Document document, BbRenderContext context, PoseStack poseStack, VertexConsumer defaultConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        BbModelData.TextureReference reference = mesh.textureName().isBlank()
                ? BbModelData.TextureReference.none()
                : new BbModelData.TextureReference(null, mesh.textureName(), false);
        BbModelRepository.ResolvedTexture texture = BbModelRepository.resolveTexture(this.modelResource, this.spec, document, reference);
        VertexConsumer consumer = consumer(context, defaultConsumer, texture);
        BbModelRepository.PixelShape shape = BbModelRepository.getTextureShape(texture.location());
        if (shape == null) {
            shape = new BbModelRepository.PixelShape(1, 1, new boolean[]{true});
        }

        float pixelWidth = texture.uvWidth() / (float) shape.width();
        float pixelHeight = texture.uvHeight() / (float) shape.height();
        Vector2f[] fullUv = new Vector2f[]{new Vector2f(1, 1), new Vector2f(1, 0), new Vector2f(0, 0), new Vector2f(0, 1)};
        emitBbQuad(poseStack.last(), consumer, quad(
                textureMeshPoint(element, mesh, -texture.uvWidth(), 0, 0),
                textureMeshPoint(element, mesh, -texture.uvWidth(), 0, texture.uvHeight()),
                textureMeshPoint(element, mesh, 0, 0, texture.uvHeight()),
                textureMeshPoint(element, mesh, 0, 0, 0)
        ), fullUv, light, overlay, red, green, blue, alpha);
        emitBbQuad(poseStack.last(), consumer, quad(
                textureMeshPoint(element, mesh, 0, -1, 0),
                textureMeshPoint(element, mesh, 0, -1, texture.uvHeight()),
                textureMeshPoint(element, mesh, -texture.uvWidth(), -1, texture.uvHeight()),
                textureMeshPoint(element, mesh, -texture.uvWidth(), -1, 0)
        ), fullUv, light, overlay, red, green, blue, alpha);

        for (int y = 0; y < shape.height(); y++) {
            for (int x = 0; x < shape.width(); x++) {
                if (!shape.opaque(x, y)) {
                    continue;
                }
                float x0 = -texture.uvWidth() + x * pixelWidth;
                float x1 = x0 + pixelWidth;
                float y0 = 0;
                float y1 = -1;
                float z0 = y * pixelHeight;
                float z1 = z0 + pixelHeight;
                float u0 = x / (float) shape.width();
                float u1 = (x + 1) / (float) shape.width();
                float v0 = y / (float) shape.height();
                float v1 = (y + 1) / (float) shape.height();
                Vector2f[] uv = new Vector2f[]{new Vector2f(u0, v0), new Vector2f(u0, v1), new Vector2f(u1, v1), new Vector2f(u1, v0)};

                if (!shape.opaque(x - 1, y)) {
                    emitBbQuad(poseStack.last(), consumer, quad(textureMeshPoint(element, mesh, x0, y1, z0), textureMeshPoint(element, mesh, x0, y1, z1), textureMeshPoint(element, mesh, x0, y0, z1), textureMeshPoint(element, mesh, x0, y0, z0)), uv, light, overlay, red, green, blue, alpha);
                }
                if (!shape.opaque(x + 1, y)) {
                    emitBbQuad(poseStack.last(), consumer, quad(textureMeshPoint(element, mesh, x1, y0, z0), textureMeshPoint(element, mesh, x1, y0, z1), textureMeshPoint(element, mesh, x1, y1, z1), textureMeshPoint(element, mesh, x1, y1, z0)), uv, light, overlay, red, green, blue, alpha);
                }
                if (!shape.opaque(x, y - 1)) {
                    emitBbQuad(poseStack.last(), consumer, quad(textureMeshPoint(element, mesh, x0, y0, z0), textureMeshPoint(element, mesh, x1, y0, z0), textureMeshPoint(element, mesh, x1, y1, z0), textureMeshPoint(element, mesh, x0, y1, z0)), uv, light, overlay, red, green, blue, alpha);
                }
                if (!shape.opaque(x, y + 1)) {
                    emitBbQuad(poseStack.last(), consumer, quad(textureMeshPoint(element, mesh, x0, y1, z1), textureMeshPoint(element, mesh, x1, y1, z1), textureMeshPoint(element, mesh, x1, y0, z1), textureMeshPoint(element, mesh, x0, y0, z1)), uv, light, overlay, red, green, blue, alpha);
                }
            }
        }
    }

    private static Vector3f textureMeshPoint(BbModelData.ElementNode element, BbModelData.TextureMesh mesh, float x, float y, float z) {
        return new Vector3f(
                x * element.scale().x + mesh.localPivot().x,
                y * element.scale().y + mesh.localPivot().y,
                z * element.scale().z + mesh.localPivot().z
        ).mul(PIXEL);
    }

    private VertexConsumer consumer(BbRenderContext context, VertexConsumer fallback, BbModelRepository.ResolvedTexture texture) {
        if (context == null) {
            return fallback;
        }
        return context.buffers().getBuffer(renderType(texture.location(), texture.renderMode(), this.spec.renderType()));
    }

    private static Vector2f[] cubeUv(BbModelData.CubeFace face, boolean mirror, int width, int height) {
        float[] raw = face.uv();
        float u1 = raw[0] / width;
        float v1 = raw[1] / height;
        float u2 = raw[2] / width;
        float v2 = raw[3] / height;
        if (mirror) {
            float swap = u1;
            u1 = u2;
            u2 = swap;
        }
        Vector2f[] values = new Vector2f[]{new Vector2f(u1, v1), new Vector2f(u1, v2), new Vector2f(u2, v2), new Vector2f(u2, v1)};
        int rotations = Math.floorMod(face.rotation(), 360) / 90;
        for (int count = 0; count < rotations; count++) {
            Vector2f last = values[3];
            values[3] = values[2];
            values[2] = values[1];
            values[1] = values[0];
            values[0] = last;
        }
        return values;
    }

    private static void emitTriangleAsQuad(PoseStack.Pose pose, VertexConsumer consumer, Vector3f a, Vector3f b, Vector3f c, Vector2f ua, Vector2f ub, Vector2f uc, int light, int overlay, float red, float green, float blue, float alpha) {
        emitBbQuad(pose, consumer, new Vector3f[]{a, b, c, c}, new Vector2f[]{ua, ub, uc, uc}, light, overlay, red, green, blue, alpha);
    }

    private static void emitBbQuad(PoseStack.Pose pose, VertexConsumer consumer, Vector3f[] vertices, Vector2f[] uv, int light, int overlay, float red, float green, float blue, float alpha) {
        BbCoordinateSystem.ConvertedQuad converted = BbCoordinateSystem.quad(vertices, uv);
        emitQuad(pose, consumer, converted.vertices(), converted.uvs(), light, overlay, red, green, blue, alpha);
    }

    private static void emitQuad(PoseStack.Pose pose, VertexConsumer consumer, Vector3f[] vertices, Vector2f[] uv, int light, int overlay, float red, float green, float blue, float alpha) {
        Vector3f normal = new Vector3f(vertices[1]).sub(vertices[0]).cross(new Vector3f(vertices[2]).sub(vertices[0])).normalize();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        for (int index = 0; index < 4; index++) {
            Vector3f vertex = vertices[index];
            Vector2f tex = uv[index];
            consumer.vertex(matrix, vertex.x, vertex.y, vertex.z)
                    .color(red, green, blue, alpha)
                    .uv(tex.x, tex.y)
                    .overlayCoords(overlay)
                    .uv2(light)
                    .normal(normalMatrix, normal.x, normal.y, normal.z)
                    .endVertex();
        }
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

    private static Vector3f v(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    private static Vector3f[] quad(Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
        return new Vector3f[]{a, b, c, d};
    }
}
