package com.sshakusora.riautomobility.model.bbmodel;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class BbInstancedRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int STATIC_VERTEX_STRIDE = 8 * Float.BYTES;
    private static final int INSTANCE_STRIDE = 36 * Float.BYTES;
    private static final int MIN_RETAINED_INSTANCES = 64;
    private static final int CACHE_SHRINK_DELAY_FRAMES = 120;
    private static final int[] TRIANGLE_VERTICES = {0, 1, 2, 2, 3, 0};

    private static final VertexFormatElement FLOAT_NORMAL = new VertexFormatElement(
            0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.NORMAL, 3);
    private static final VertexFormatElement FLOAT4 = new VertexFormatElement(
            0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4);
    private static final VertexFormat SHADER_FORMAT = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                    .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                    .put("UV0", DefaultVertexFormat.ELEMENT_UV0)
                    .put("Normal", FLOAT_NORMAL)
                    .put("InstanceModel0", FLOAT4)
                    .put("InstanceModel1", FLOAT4)
                    .put("InstanceModel2", FLOAT4)
                    .put("InstanceModel3", FLOAT4)
                    .put("InstanceNormal0", FLOAT4)
                    .put("InstanceNormal1", FLOAT4)
                    .put("InstanceNormal2", FLOAT4)
                    .put("InstanceColor", FLOAT4)
                    .put("InstanceOverlayLight", FLOAT4)
                    .build());

    private static final Map<DynamicBbModel, PendingModel> PENDING = new IdentityHashMap<>();
    private static final Map<DynamicBbModel, GpuModel> GPU_MODELS = new IdentityHashMap<>();
    private static ShaderInstance shader;
    private static ByteBuffer instanceData;
    private static int instanceBufferId;
    private static boolean disabled;
    private static boolean warnedUnsupported;

    private BbInstancedRenderer() {
    }

    public static void registerShader(RegisterShadersEvent event) throws IOException {
        disposeGpuResources();
        shader = null;
        disabled = false;
        warnedUnsupported = false;
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(), RIAutomobility.rl("bbmodel_instanced"), SHADER_FORMAT),
                loaded -> shader = loaded);
    }

    static boolean tryEnqueue(DynamicBbModel model, BbCompiledGeometry.StaticGeometry geometry,
                              PoseStack.Pose pose, BbRenderContext context, int lod,
                              int light, int overlay,
                              float red, float green, float blue, float alpha) {
        if (shader == null || disabled || alpha < 0.999F || geometry.batches().isEmpty()
                || context == null || !(context.automobile() instanceof Entity)
                || !(context.buffers() instanceof MultiBufferSource.BufferSource)
                || !model.supportsInstancedRendering() || !RenderSystem.isOnRenderThread()) {
            return false;
        }
        if (!supportsInstancing()) {
            disabled = true;
            if (!warnedUnsupported) {
                warnedUnsupported = true;
                LOGGER.warn("GPU instancing is unavailable; static BBModels will use streamed rendering");
            }
            return false;
        }

        try {
            GpuModel gpu = GPU_MODELS.get(model);
            if (gpu == null || gpu.geometry != geometry) {
                if (gpu != null) gpu.close();
                gpu = new GpuModel(geometry);
                GPU_MODELS.put(model, gpu);
            }
            ensureInstanceBuffer();
            gpu.ensureLod(model, lod);

            PendingModel pending = PENDING.computeIfAbsent(model, ignored -> new PendingModel());
            if (pending.geometry != geometry) {
                pending.reset();
                pending.geometry = geometry;
            }
            pending.bucket(lod).add(pose.pose(), pose.normal(), light, overlay, red, green, blue, alpha);
            return true;
        } catch (RuntimeException exception) {
            disabled = true;
            LOGGER.error("Failed to prepare instanced BBModel rendering; falling back to streamed rendering", exception);
            return false;
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        flush(event.getProjectionMatrix());
    }

    public static void clearGpuResources() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(BbInstancedRenderer::clearGpuResources);
            return;
        }
        disposeGpuResources();
    }

    public static void clearModel(DynamicBbModel model) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> clearModel(model));
            return;
        }
        PENDING.remove(model);
        GpuModel gpu = GPU_MODELS.remove(model);
        if (gpu != null) gpu.close();
    }

    private static void flush(Matrix4f projectionMatrix) {
        if (shader == null || disabled) {
            resetPending();
            return;
        }
        try {
            for (Map.Entry<DynamicBbModel, PendingModel> entry : PENDING.entrySet()) {
                DynamicBbModel model = entry.getKey();
                PendingModel pending = entry.getValue();
                GpuModel gpu = GPU_MODELS.get(model);
                if (gpu == null || gpu.geometry != pending.geometry) continue;
                for (int lod = 0; lod < pending.buckets.length; lod++) {
                    InstanceBucket instances = pending.buckets[lod];
                    if (instances.size == 0) continue;
                    uploadInstances(instances);
                    draw(model, gpu.ensureLod(model, lod), instances.size, projectionMatrix);
                }
            }
        } catch (RuntimeException exception) {
            disabled = true;
            LOGGER.error("Failed to draw instanced BBModels; disabling instancing for this session", exception);
        } finally {
            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            BufferUploader.invalidate();
            resetPending();
        }
    }

    private static void draw(DynamicBbModel model, GpuLod gpu, int instanceCount,
                             Matrix4f projectionMatrix) {
        for (GpuBatch batch : gpu.batches) {
            RenderType renderType = model.instancedRenderType(batch.texture);
            renderType.setupRenderState();
            try {
                RenderSystem.setShader(() -> shader);
                applyShaderUniforms(projectionMatrix);
                GL30.glBindVertexArray(batch.vertexArrayId);
                shader.apply();
                GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, 0, batch.vertexCount, instanceCount);
                shader.clear();
            } finally {
                GL30.glBindVertexArray(0);
                renderType.clearRenderState();
            }
        }
    }

    private static void applyShaderUniforms(Matrix4f projectionMatrix) {
        for (int index = 0; index < 12; index++) {
            shader.setSampler("Sampler" + index, RenderSystem.getShaderTexture(index));
        }
        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
        }
        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(projectionMatrix);
        }
        if (shader.INVERSE_VIEW_ROTATION_MATRIX != null) {
            shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
        }
        if (shader.COLOR_MODULATOR != null) {
            shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }
        if (shader.GLINT_ALPHA != null) {
            shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        }
        if (shader.FOG_START != null) {
            shader.FOG_START.set(RenderSystem.getShaderFogStart());
        }
        if (shader.FOG_END != null) {
            shader.FOG_END.set(RenderSystem.getShaderFogEnd());
        }
        if (shader.FOG_COLOR != null) {
            shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        }
        if (shader.FOG_SHAPE != null) {
            shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        }
        if (shader.TEXTURE_MATRIX != null) {
            shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }
        if (shader.GAME_TIME != null) {
            shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
        }
        if (shader.SCREEN_SIZE != null) {
            Window window = Minecraft.getInstance().getWindow();
            shader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
        }
        RenderSystem.setupShaderLights(shader);
    }

    private static void uploadInstances(InstanceBucket bucket) {
        int bytes = bucket.size * INSTANCE_STRIDE;
        ensureInstanceCapacity(bytes);
        instanceData.clear();
        for (int index = 0; index < bucket.size; index++) {
            writeInstance(instanceData, bucket.values.get(index));
        }
        instanceData.flip();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceBufferId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bytes, GL15.GL_STREAM_DRAW);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instanceData);
    }

    private static void writeInstance(ByteBuffer target, Instance instance) {
        Matrix4f model = instance.model;
        put4(target, model.m00(), model.m01(), model.m02(), model.m03());
        put4(target, model.m10(), model.m11(), model.m12(), model.m13());
        put4(target, model.m20(), model.m21(), model.m22(), model.m23());
        put4(target, model.m30(), model.m31(), model.m32(), model.m33());

        Matrix3f normal = instance.normal;
        put4(target, normal.m00(), normal.m01(), normal.m02(), 0.0F);
        put4(target, normal.m10(), normal.m11(), normal.m12(), 0.0F);
        put4(target, normal.m20(), normal.m21(), normal.m22(), 0.0F);
        put4(target, instance.red, instance.green, instance.blue, instance.alpha);
        put4(target,
                instance.overlay & 0xFFFF, (instance.overlay >>> 16) & 0xFFFF,
                instance.light & 0xFFFF, (instance.light >>> 16) & 0xFFFF);
    }

    private static GpuBatch buildBatch(BbCompiledGeometry.Batch batch, int lod) {
        int vertexCount = batch.quadCount(lod) * TRIANGLE_VERTICES.length;
        ByteBuffer vertices = MemoryUtil.memAlloc(vertexCount * STATIC_VERTEX_STRIDE);
        try {
            float[] data = batch.data();
            byte[] detailLevels = batch.detailLevels();
            for (int quad = 0; quad < detailLevels.length; quad++) {
                if (detailLevels[quad] < lod) continue;
                int base = quad * BbCompiledGeometry.PACKED_QUAD_STRIDE;
                float normalX = data[base];
                float normalY = data[base + 1];
                float normalZ = data[base + 2];
                for (int vertex : TRIANGLE_VERTICES) {
                    int cursor = base + 3 + vertex * 5;
                    vertices.putFloat(data[cursor]);
                    vertices.putFloat(data[cursor + 1]);
                    vertices.putFloat(data[cursor + 2]);
                    vertices.putFloat(data[cursor + 3]);
                    vertices.putFloat(data[cursor + 4]);
                    vertices.putFloat(normalX);
                    vertices.putFloat(normalY);
                    vertices.putFloat(normalZ);
                }
            }
            vertices.flip();

            int vertexArray = GL30.glGenVertexArrays();
            int vertexBuffer = GL15.glGenBuffers();
            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
            vertexAttribute(0, 3, STATIC_VERTEX_STRIDE, 0, 0);
            vertexAttribute(1, 2, STATIC_VERTEX_STRIDE, 3L * Float.BYTES, 0);
            vertexAttribute(2, 3, STATIC_VERTEX_STRIDE, 5L * Float.BYTES, 0);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceBufferId);
            for (int column = 0; column < 4; column++) {
                vertexAttribute(3 + column, 4, INSTANCE_STRIDE,
                        (long) column * 4 * Float.BYTES, 1);
            }
            for (int column = 0; column < 3; column++) {
                vertexAttribute(7 + column, 4, INSTANCE_STRIDE,
                        (long) (16 + column * 4) * Float.BYTES, 1);
            }
            vertexAttribute(10, 4, INSTANCE_STRIDE, 28L * Float.BYTES, 1);
            vertexAttribute(11, 4, INSTANCE_STRIDE, 32L * Float.BYTES, 1);
            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            return new GpuBatch(batch.texture(), vertexArray, vertexBuffer, vertexCount);
        } finally {
            MemoryUtil.memFree(vertices);
        }
    }

    private static void vertexAttribute(int location, int count, int stride, long offset, int divisor) {
        GL20.glEnableVertexAttribArray(location);
        GL20.glVertexAttribPointer(location, count, GL11.GL_FLOAT, false, stride, offset);
        if (GL.getCapabilities().OpenGL33) {
            GL33.glVertexAttribDivisor(location, divisor);
        } else {
            ARBInstancedArrays.glVertexAttribDivisorARB(location, divisor);
        }
    }

    private static boolean supportsInstancing() {
        return GL.getCapabilities().OpenGL33 || GL.getCapabilities().GL_ARB_instanced_arrays;
    }

    private static void ensureInstanceBuffer() {
        if (instanceBufferId == 0) instanceBufferId = GL15.glGenBuffers();
    }

    private static void ensureInstanceCapacity(int required) {
        if (instanceData != null && instanceData.capacity() >= required) return;
        int capacity = 4096;
        while (capacity < required) capacity *= 2;
        if (instanceData != null) MemoryUtil.memFree(instanceData);
        instanceData = MemoryUtil.memAlloc(capacity);
    }

    private static void resetPending() {
        for (PendingModel pending : PENDING.values()) pending.resetCounts();
    }

    private static void disposeGpuResources() {
        PENDING.clear();
        for (GpuModel model : GPU_MODELS.values()) model.close();
        GPU_MODELS.clear();
        if (instanceBufferId != 0) {
            GL15.glDeleteBuffers(instanceBufferId);
            instanceBufferId = 0;
        }
        if (instanceData != null) {
            MemoryUtil.memFree(instanceData);
            instanceData = null;
        }
    }

    private static void put4(ByteBuffer target, float x, float y, float z, float w) {
        target.putFloat(x).putFloat(y).putFloat(z).putFloat(w);
    }

    private static final class PendingModel {
        private BbCompiledGeometry.StaticGeometry geometry;
        private final InstanceBucket[] buckets = {
                new InstanceBucket(), new InstanceBucket(), new InstanceBucket(), new InstanceBucket()
        };

        InstanceBucket bucket(int lod) {
            return buckets[Math.max(0, Math.min(buckets.length - 1, lod))];
        }

        void resetCounts() {
            for (InstanceBucket bucket : buckets) bucket.finishFrame();
        }

        void reset() {
            resetCounts();
            geometry = null;
        }
    }

    private static final class InstanceBucket {
        private final List<Instance> values = new ArrayList<>();
        private int size;
        private int underusedFrames;

        void add(Matrix4f model, Matrix3f normal, int light, int overlay,
                 float red, float green, float blue, float alpha) {
            if (size == values.size()) values.add(new Instance());
            values.get(size++).set(model, normal, light, overlay, red, green, blue, alpha);
        }

        void finishFrame() {
            int used = size;
            size = 0;
            if (values.size() <= MIN_RETAINED_INSTANCES || used * 4 >= values.size()) {
                underusedFrames = 0;
                return;
            }
            if (++underusedFrames < CACHE_SHRINK_DELAY_FRAMES) return;
            int retained = Math.max(MIN_RETAINED_INSTANCES, used * 2);
            values.subList(retained, values.size()).clear();
            underusedFrames = 0;
        }
    }

    private static final class Instance {
        private final Matrix4f model = new Matrix4f();
        private final Matrix3f normal = new Matrix3f();
        private int light;
        private int overlay;
        private float red;
        private float green;
        private float blue;
        private float alpha;

        void set(Matrix4f model, Matrix3f normal, int light, int overlay,
                 float red, float green, float blue, float alpha) {
            this.model.set(model);
            this.normal.set(normal);
            this.light = light;
            this.overlay = overlay;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }
    }

    private static final class GpuModel {
        private final BbCompiledGeometry.StaticGeometry geometry;
        private final GpuLod[] lods = new GpuLod[4];
        private boolean announced;

        private GpuModel(BbCompiledGeometry.StaticGeometry geometry) {
            this.geometry = geometry;
        }

        GpuLod ensureLod(DynamicBbModel model, int requestedLod) {
            int lod = Math.max(0, Math.min(lods.length - 1, requestedLod));
            GpuLod existing = lods[lod];
            if (existing != null) return existing;
            List<GpuBatch> batches = new ArrayList<>();
            try {
                for (BbCompiledGeometry.Batch batch : geometry.batches()) {
                    if (batch.quadCount(lod) > 0) batches.add(buildBatch(batch, lod));
                }
                existing = new GpuLod(List.copyOf(batches));
                lods[lod] = existing;
                if (!announced) {
                    announced = true;
                    LOGGER.info("Enabled GPU instancing for static BBModel {}: {} quads, {} material batches",
                            model.modelResource(), geometry.outputQuadCount(), geometry.batches().size());
                }
                return existing;
            } catch (RuntimeException exception) {
                batches.forEach(GpuBatch::close);
                throw exception;
            }
        }

        void close() {
            for (GpuLod lod : lods) if (lod != null) lod.close();
        }
    }

    private record GpuLod(List<GpuBatch> batches) {
        void close() {
            batches.forEach(GpuBatch::close);
        }
    }

    private record GpuBatch(BbModelRepository.ResolvedTexture texture, int vertexArrayId,
                            int vertexBufferId, int vertexCount) {
        void close() {
            GL30.glDeleteVertexArrays(vertexArrayId);
            GL15.glDeleteBuffers(vertexBufferId);
        }
    }
}
