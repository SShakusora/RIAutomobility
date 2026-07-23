package com.sshakusora.riautomobility.model.bbmodel;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Writes Oculus' extended entity format without the per-attribute VertexConsumer dispatch. */
final class OculusEntityVertexWriter {
    static final int STRIDE = 56;
    private static final int INITIAL_CAPACITY = 64 * 1024;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Probe PROBE = discoverProbe();
    private static final ThreadLocal<ByteBuffer> SCRATCH = ThreadLocal.withInitial(
            () -> allocate(INITIAL_CAPACITY));
    private static final Matrix4f IDENTITY_POSE = new Matrix4f();
    private static final Matrix3f IDENTITY_NORMAL = new Matrix3f();
    private static final Map<BbCompiledGeometry.Batch, Template[]> TEMPLATES = new WeakHashMap<>();
    private static boolean announced;
    private static boolean directWriteDisabled;
    private static boolean directWriteAnnounced;

    private OculusEntityVertexWriter() {
    }

    static boolean tryWrite(PoseStack.Pose pose, VertexConsumer consumer,
                            BbCompiledGeometry.Batch batch, int lod,
                            int light, int overlay,
                            float red, float green, float blue, float alpha) {
        if (!(consumer instanceof BufferBuilder builder)) return false;
        EntityIds ids = PROBE.entityIds(builder);
        if (ids == null) return false;

        int quadCount = batch.quadCount(lod);
        if (quadCount == 0) return true;
        Template template = template(batch, lod, light, overlay, red, green, blue, alpha, ids);
        if (!tryWriteDirect(builder, pose.pose(), pose.normal(), batch, lod, template.vertices())) {
            ByteBuffer vertices = transformTemplate(pose.pose(), pose.normal(), batch.data(),
                    batch.detailLevels(), batch.tangents(), lod, template.vertices());
            builder.putBulkData(vertices);
        }
        if (!announced) {
            announced = true;
            LOGGER.info("Enabled Oculus extended entity bulk writing for static BBModels");
        }
        return true;
    }

    private static boolean tryWriteDirect(BufferBuilder builder, Matrix4f matrix, Matrix3f normalMatrix,
                                          BbCompiledGeometry.Batch batch, int lod, byte[] template) {
        if (directWriteDisabled) return false;
        ByteBuffer target = null;
        try {
            int start = builder.nextElementByte;
            builder.ensureCapacity(template.length + STRIDE);
            target = builder.buffer;
            target.position(start);
            target.put(template);
            target.position(0);
            transformVertices(MemoryUtil.memAddress(target) + start, matrix, normalMatrix,
                    batch.data(), batch.detailLevels(), batch.tangents(), lod);
            builder.vertices += template.length / STRIDE;
            builder.nextElementByte = start + template.length;
            if (!directWriteAnnounced) {
                directWriteAnnounced = true;
                LOGGER.info("Enabled direct Oculus entity buffer writing for static BBModels");
            }
            return true;
        } catch (RuntimeException | LinkageError exception) {
            directWriteDisabled = true;
            LOGGER.warn("Direct Oculus entity buffer writing failed; using the copied bulk writer", exception);
            return false;
        } finally {
            if (target != null) target.position(0);
        }
    }

    private static Template template(BbCompiledGeometry.Batch batch, int lod,
                                     int light, int overlay,
                                     float red, float green, float blue, float alpha,
                                     EntityIds ids) {
        int level = Math.max(0, Math.min(3, lod));
        TemplateKey key = new TemplateKey(light, overlay,
                color(red), color(green), color(blue), color(alpha), ids);
        Template[] levels = TEMPLATES.computeIfAbsent(batch, ignored -> new Template[4]);
        Template cached = levels[level];
        if (cached != null && cached.key().equals(key)) return cached;

        ByteBuffer local = buildVertexData(IDENTITY_POSE, IDENTITY_NORMAL,
                batch.data(), batch.detailLevels(), batch.tangents(), level,
                light, overlay, red, green, blue, alpha, ids);
        byte[] vertices = new byte[local.remaining()];
        local.get(vertices);
        cached = new Template(key, vertices);
        levels[level] = cached;
        return cached;
    }

    static ByteBuffer transformTemplate(Matrix4f matrix, Matrix3f normalMatrix,
                                        float[] data, byte[] detailLevels, float[] tangents,
                                        int lod, byte[] template) {
        ByteBuffer target = scratch(template.length);
        target.put(template);
        target.position(0);
        transformVertices(MemoryUtil.memAddress(target), matrix, normalMatrix,
                data, detailLevels, tangents, lod);
        target.position(0);
        target.limit(template.length);
        return target;
    }

    private static void transformVertices(long address, Matrix4f matrix, Matrix3f normalMatrix,
                                          float[] data, byte[] detailLevels, float[] tangents, int lod) {
        float matrix00 = matrix.m00(), matrix01 = matrix.m01(), matrix02 = matrix.m02();
        float matrix10 = matrix.m10(), matrix11 = matrix.m11(), matrix12 = matrix.m12();
        float matrix20 = matrix.m20(), matrix21 = matrix.m21(), matrix22 = matrix.m22();
        float matrix30 = matrix.m30(), matrix31 = matrix.m31(), matrix32 = matrix.m32();
        float normal00 = normalMatrix.m00(), normal01 = normalMatrix.m01(), normal02 = normalMatrix.m02();
        float normal10 = normalMatrix.m10(), normal11 = normalMatrix.m11(), normal12 = normalMatrix.m12();
        float normal20 = normalMatrix.m20(), normal21 = normalMatrix.m21(), normal22 = normalMatrix.m22();
        float handednessScale = normalMatrix.determinant() < 0.0F ? -1.0F : 1.0F;
        int outputQuad = 0;

        for (int quadIndex = 0; quadIndex < detailLevels.length; quadIndex++) {
            if (detailLevels[quadIndex] < lod) continue;
            int quadBase = quadIndex * BbCompiledGeometry.PACKED_QUAD_STRIDE;
            float normalX = normal00 * data[quadBase] + normal10 * data[quadBase + 1] + normal20 * data[quadBase + 2];
            float normalY = normal01 * data[quadBase] + normal11 * data[quadBase + 1] + normal21 * data[quadBase + 2];
            float normalZ = normal02 * data[quadBase] + normal12 * data[quadBase + 1] + normal22 * data[quadBase + 2];
            float normalScale = inverseSqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
            int packedNormal = packNormal(normalX * normalScale, normalY * normalScale,
                    normalZ * normalScale, 0.0F);

            int tangentBase = quadIndex * 4;
            float tangentX = normal00 * tangents[tangentBase]
                    + normal10 * tangents[tangentBase + 1] + normal20 * tangents[tangentBase + 2];
            float tangentY = normal01 * tangents[tangentBase]
                    + normal11 * tangents[tangentBase + 1] + normal21 * tangents[tangentBase + 2];
            float tangentZ = normal02 * tangents[tangentBase]
                    + normal12 * tangents[tangentBase + 1] + normal22 * tangents[tangentBase + 2];
            float tangentScale = inverseSqrt(tangentX * tangentX + tangentY * tangentY + tangentZ * tangentZ);
            int packedTangent = packNormal(tangentX * tangentScale, tangentY * tangentScale,
                    tangentZ * tangentScale, tangents[tangentBase + 3] * handednessScale);

            int source = quadBase + 3;
            int destination = outputQuad++ * 4 * STRIDE;
            for (int vertex = 0; vertex < 4; vertex++) {
                float x = data[source++];
                float y = data[source++];
                float z = data[source++];
                source += 2;
                long vertexAddress = address + destination + vertex * STRIDE;
                MemoryUtil.memPutFloat(vertexAddress, matrix00 * x + matrix10 * y + matrix20 * z + matrix30);
                MemoryUtil.memPutFloat(vertexAddress + 4, matrix01 * x + matrix11 * y + matrix21 * z + matrix31);
                MemoryUtil.memPutFloat(vertexAddress + 8, matrix02 * x + matrix12 * y + matrix22 * z + matrix32);
                MemoryUtil.memPutInt(vertexAddress + 32, packedNormal);
                MemoryUtil.memPutInt(vertexAddress + 50, packedTangent);
            }
        }
    }

    static ByteBuffer buildVertexData(Matrix4f matrix, float[] data, byte[] detailLevels, int lod,
                                      int light, int overlay,
                                      float red, float green, float blue, float alpha,
                                      EntityIds ids) {
        return buildVertexData(matrix, new Matrix3f(), data, detailLevels,
                BbCompiledGeometry.compileTangents(data, detailLevels.length), lod,
                light, overlay, red, green, blue, alpha, ids);
    }

    private static ByteBuffer buildVertexData(Matrix4f matrix, Matrix3f normalMatrix,
                                              float[] data, byte[] detailLevels, float[] tangents, int lod,
                                              int light, int overlay,
                                              float red, float green, float blue, float alpha,
                                              EntityIds ids) {
        int quadCount = 0;
        for (byte detailLevel : detailLevels) {
            if (detailLevel >= lod) quadCount++;
        }
        ByteBuffer target = scratch(quadCount * 4 * STRIDE);
        byte colorRed = color(red);
        byte colorGreen = color(green);
        byte colorBlue = color(blue);
        byte colorAlpha = color(alpha);
        float handednessScale = normalMatrix.determinant() < 0.0F ? -1.0F : 1.0F;
        float matrix00 = matrix.m00(), matrix01 = matrix.m01(), matrix02 = matrix.m02();
        float matrix10 = matrix.m10(), matrix11 = matrix.m11(), matrix12 = matrix.m12();
        float matrix20 = matrix.m20(), matrix21 = matrix.m21(), matrix22 = matrix.m22();
        float matrix30 = matrix.m30(), matrix31 = matrix.m31(), matrix32 = matrix.m32();
        float normal00 = normalMatrix.m00(), normal01 = normalMatrix.m01(), normal02 = normalMatrix.m02();
        float normal10 = normalMatrix.m10(), normal11 = normalMatrix.m11(), normal12 = normalMatrix.m12();
        float normal20 = normalMatrix.m20(), normal21 = normalMatrix.m21(), normal22 = normalMatrix.m22();
        long address = MemoryUtil.memAddress(target);
        int targetOffset = 0;

        for (int quadIndex = 0; quadIndex < detailLevels.length; quadIndex++) {
            if (detailLevels[quadIndex] < lod) continue;
            int quadBase = quadIndex * BbCompiledGeometry.PACKED_QUAD_STRIDE;
            int source = quadBase + 3;
            float normalX = normal00 * data[quadBase] + normal10 * data[quadBase + 1] + normal20 * data[quadBase + 2];
            float normalY = normal01 * data[quadBase] + normal11 * data[quadBase + 1] + normal21 * data[quadBase + 2];
            float normalZ = normal02 * data[quadBase] + normal12 * data[quadBase + 1] + normal22 * data[quadBase + 2];
            float normalScale = inverseSqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
            int packedNormal = packNormal(normalX * normalScale, normalY * normalScale,
                    normalZ * normalScale, 0.0F);

            int tangentBase = quadIndex * 4;
            float tangentX = normal00 * tangents[tangentBase]
                    + normal10 * tangents[tangentBase + 1] + normal20 * tangents[tangentBase + 2];
            float tangentY = normal01 * tangents[tangentBase]
                    + normal11 * tangents[tangentBase + 1] + normal21 * tangents[tangentBase + 2];
            float tangentZ = normal02 * tangents[tangentBase]
                    + normal12 * tangents[tangentBase + 1] + normal22 * tangents[tangentBase + 2];
            float tangentScale = inverseSqrt(tangentX * tangentX + tangentY * tangentY + tangentZ * tangentZ);
            int packedTangent = packNormal(tangentX * tangentScale, tangentY * tangentScale,
                    tangentZ * tangentScale, tangents[tangentBase + 3] * handednessScale);
            float midU = (data[quadBase + 6] + data[quadBase + 11]
                    + data[quadBase + 16] + data[quadBase + 21]) * 0.25F;
            float midV = (data[quadBase + 7] + data[quadBase + 12]
                    + data[quadBase + 17] + data[quadBase + 22]) * 0.25F;

            for (int vertex = 0; vertex < 4; vertex++) {
                float x = data[source++];
                float y = data[source++];
                float z = data[source++];
                float u = data[source++];
                float v = data[source++];
                long vertexAddress = address + targetOffset;
                MemoryUtil.memPutFloat(vertexAddress, matrix00 * x + matrix10 * y + matrix20 * z + matrix30);
                MemoryUtil.memPutFloat(vertexAddress + 4, matrix01 * x + matrix11 * y + matrix21 * z + matrix31);
                MemoryUtil.memPutFloat(vertexAddress + 8, matrix02 * x + matrix12 * y + matrix22 * z + matrix32);
                MemoryUtil.memPutByte(vertexAddress + 12, colorRed);
                MemoryUtil.memPutByte(vertexAddress + 13, colorGreen);
                MemoryUtil.memPutByte(vertexAddress + 14, colorBlue);
                MemoryUtil.memPutByte(vertexAddress + 15, colorAlpha);
                MemoryUtil.memPutFloat(vertexAddress + 16, u);
                MemoryUtil.memPutFloat(vertexAddress + 20, v);
                MemoryUtil.memPutInt(vertexAddress + 24, overlay);
                MemoryUtil.memPutInt(vertexAddress + 28, light);
                MemoryUtil.memPutInt(vertexAddress + 32, packedNormal);
                MemoryUtil.memPutShort(vertexAddress + 36, ids.entity());
                MemoryUtil.memPutShort(vertexAddress + 38, ids.blockEntity());
                MemoryUtil.memPutShort(vertexAddress + 40, ids.item());
                MemoryUtil.memPutFloat(vertexAddress + 42, midU);
                MemoryUtil.memPutFloat(vertexAddress + 46, midV);
                MemoryUtil.memPutInt(vertexAddress + 50, packedTangent);
                MemoryUtil.memPutShort(vertexAddress + 54, (short) 0);
                targetOffset += STRIDE;
            }
        }
        target.position(0);
        target.limit(targetOffset);
        return target;
    }

    private static float inverseSqrt(float value) {
        return value == 0.0F ? 1.0F : (float) (1.0 / Math.sqrt(value));
    }

    private static int packNormal(float x, float y, float z, float w) {
        return ((int) (x * 127.0F) & 0xff)
                | (((int) (y * 127.0F) & 0xff) << 8)
                | (((int) (z * 127.0F) & 0xff) << 16)
                | (((int) (w * 127.0F) & 0xff) << 24);
    }

    private static byte color(float value) {
        return (byte) ((int) (value * 255.0F));
    }

    private static ByteBuffer scratch(int required) {
        ByteBuffer buffer = SCRATCH.get();
        if (buffer.capacity() < required) {
            int capacity = buffer.capacity();
            while (capacity < required) capacity = Math.max(capacity * 2, required);
            buffer = allocate(capacity);
            SCRATCH.set(buffer);
        }
        buffer.clear();
        buffer.limit(required);
        return buffer;
    }

    private static ByteBuffer allocate(int capacity) {
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    private static Probe discoverProbe() {
        try {
            ClassLoader loader = OculusEntityVertexWriter.class.getClassLoader();
            Class<?> formatsClass = Class.forName("net.irisshaders.iris.vertices.IrisVertexFormats", false, loader);
            Field entityField = formatsClass.getField("ENTITY");
            VertexFormat entityFormat = (VertexFormat) entityField.get(null);
            if (!hasExpectedLayout(entityFormat)) {
                LOGGER.warn("Oculus ENTITY vertex layout is unsupported; using the standard BBModel writer");
                return Probe.NONE;
            }

            Method format = BufferBuilder.class.getMethod("iris$format");
            Method mode = BufferBuilder.class.getMethod("iris$mode");
            Method extending = BufferBuilder.class.getMethod("iris$extending");
            Method vertexCount = BufferBuilder.class.getMethod("iris$vertexCount");

            Class<?> stateClass = Class.forName("net.irisshaders.iris.uniforms.CapturedRenderingState", false, loader);
            Object state = stateClass.getField("INSTANCE").get(null);
            Method entity = stateClass.getMethod("getCurrentRenderedEntity");
            Method blockEntity = stateClass.getMethod("getCurrentRenderedBlockEntity");
            Method item = stateClass.getMethod("getCurrentRenderedItem");
            return new ReflectiveProbe(entityFormat, format, mode, extending, vertexCount,
                    state, entity, blockEntity, item);
        } catch (ClassNotFoundException ignored) {
            return Probe.NONE;
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Unable to initialize the Oculus BBModel bulk writer; using the standard writer", exception);
            return Probe.NONE;
        }
    }

    private static boolean hasExpectedLayout(VertexFormat format) {
        List<Integer> sizes = format.getElements().stream().map(element -> element.getByteSize()).toList();
        return format.getVertexSize() == STRIDE
                && sizes.equals(List.of(12, 4, 8, 4, 4, 3, 1, 6, 8, 4, 2));
    }

    record EntityIds(short entity, short blockEntity, short item) {
    }

    private record TemplateKey(int light, int overlay, byte red, byte green, byte blue, byte alpha,
                               EntityIds ids) {
    }

    private record Template(TemplateKey key, byte[] vertices) {
    }

    private interface Probe {
        Probe NONE = builder -> null;

        EntityIds entityIds(BufferBuilder builder);
    }

    private static final class ReflectiveProbe implements Probe {
        private final VertexFormat entityFormat;
        private final Method format;
        private final Method mode;
        private final Method extending;
        private final Method vertexCount;
        private final Object state;
        private final Method entity;
        private final Method blockEntity;
        private final Method item;
        private boolean disabled;

        private ReflectiveProbe(VertexFormat entityFormat, Method format, Method mode, Method extending,
                                Method vertexCount, Object state, Method entity, Method blockEntity, Method item) {
            this.entityFormat = entityFormat;
            this.format = format;
            this.mode = mode;
            this.extending = extending;
            this.vertexCount = vertexCount;
            this.state = state;
            this.entity = entity;
            this.blockEntity = blockEntity;
            this.item = item;
        }

        @Override
        public EntityIds entityIds(BufferBuilder builder) {
            if (this.disabled) return null;
            try {
                if (!builder.building()
                        || this.format.invoke(builder) != this.entityFormat
                        || this.mode.invoke(builder) != VertexFormat.Mode.QUADS
                        || !(boolean) this.extending.invoke(builder)
                        || (int) this.vertexCount.invoke(builder) != 0) {
                    return null;
                }
                return new EntityIds(
                        (short) (int) this.entity.invoke(this.state),
                        (short) (int) this.blockEntity.invoke(this.state),
                        (short) (int) this.item.invoke(this.state));
            } catch (ReflectiveOperationException | RuntimeException exception) {
                this.disabled = true;
                LOGGER.warn("Oculus BBModel bulk writing failed; using the standard writer", exception);
                return null;
            }
        }
    }
}
