package com.sshakusora.riautomobility.model.bbmodel;

import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OculusEntityVertexWriterTest {
    @Test
    void writesCompleteOculusEntityVertices() {
        float[] quad = {
                0, 0, 1,
                0, 0, 0, 0, 0,
                1, 0, 0, 1, 0,
                1, 1, 0, 1, 1,
                0, 1, 0, 0, 1
        };
        int overlay = 0x12345678;
        int light = 0x23456789;
        OculusEntityVertexWriter.EntityIds ids = new OculusEntityVertexWriter.EntityIds(
                (short) 12, (short) 34, (short) 56);

        ByteBuffer vertices = OculusEntityVertexWriter.buildVertexData(
                new Matrix4f(), quad, new byte[]{0}, 0,
                light, overlay, 1.0F, 0.5F, 0.0F, 0.25F, ids);

        assertEquals(4 * OculusEntityVertexWriter.STRIDE, vertices.remaining());
        for (int vertex = 0; vertex < 4; vertex++) {
            int start = vertex * OculusEntityVertexWriter.STRIDE;
            assertEquals(255, Byte.toUnsignedInt(vertices.get(start + 12)));
            assertEquals(127, Byte.toUnsignedInt(vertices.get(start + 13)));
            assertEquals(0, Byte.toUnsignedInt(vertices.get(start + 14)));
            assertEquals(63, Byte.toUnsignedInt(vertices.get(start + 15)));
            assertEquals((short) 0x5678, vertices.getShort(start + 24));
            assertEquals((short) 0x1234, vertices.getShort(start + 26));
            assertEquals((short) 0x6789, vertices.getShort(start + 28));
            assertEquals((short) 0x2345, vertices.getShort(start + 30));
            assertEquals(0x007f0000, vertices.getInt(start + 32));
            assertEquals((short) 12, vertices.getShort(start + 36));
            assertEquals((short) 34, vertices.getShort(start + 38));
            assertEquals((short) 56, vertices.getShort(start + 40));
            assertEquals(0.5F, vertices.getFloat(start + 42));
            assertEquals(0.5F, vertices.getFloat(start + 46));
            assertEquals(0x8100007f, vertices.getInt(start + 50));
            assertEquals((short) 0, vertices.getShort(start + 54));
        }
    }

    @Test
    void skipsQuadsBelowTheRequestedLod() {
        float[] quads = new float[BbCompiledGeometry.PACKED_QUAD_STRIDE * 2];
        ByteBuffer vertices = OculusEntityVertexWriter.buildVertexData(
                new Matrix4f(), quads, new byte[]{0, 3}, 2,
                0, 0, 1, 1, 1, 1,
                new OculusEntityVertexWriter.EntityIds((short) 0, (short) 0, (short) 0));

        assertEquals(4 * OculusEntityVertexWriter.STRIDE, vertices.remaining());
    }

    @Test
    void transformsCopiedTemplateWithoutWritingPastItsBounds() {
        float[] quad = {
                0, 0, 1,
                0, 0, 0, 0, 0,
                1, 0, 0, 1, 0,
                1, 1, 0, 1, 1,
                0, 1, 0, 0, 1
        };
        byte[] detailLevels = {0};
        OculusEntityVertexWriter.EntityIds ids = new OculusEntityVertexWriter.EntityIds(
                (short) 12, (short) 34, (short) 56);
        ByteBuffer local = OculusEntityVertexWriter.buildVertexData(
                new Matrix4f(), quad, detailLevels, 0,
                0x23456789, 0x12345678, 1, 1, 1, 1, ids);
        byte[] template = new byte[local.remaining()];
        local.get(template);

        ByteBuffer transformed = OculusEntityVertexWriter.transformTemplate(
                new Matrix4f().translation(4, 5, 6), new Matrix3f(),
                quad, detailLevels, BbCompiledGeometry.compileTangents(quad, 1), 0, template);

        assertEquals(template.length, transformed.remaining());
        assertEquals(4.0F, transformed.getFloat(0));
        assertEquals(5.0F, transformed.getFloat(4));
        assertEquals(6.0F, transformed.getFloat(8));
        assertEquals(5.0F, transformed.getFloat(OculusEntityVertexWriter.STRIDE));
        assertEquals(12, transformed.getShort(36));
        assertEquals(0.5F, transformed.getFloat(42));
    }
}
