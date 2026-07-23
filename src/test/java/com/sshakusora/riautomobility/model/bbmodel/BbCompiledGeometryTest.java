package com.sshakusora.riautomobility.model.bbmodel;

import com.google.gson.JsonParser;
import com.sshakusora.riautomobility.content.FrameSpec;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BbCompiledGeometryTest {
    @Test
    void compilesCubeFacesIntoReusableQuads() {
        Map<String, BbModelData.CubeFace> faces = new LinkedHashMap<>();
        for (String direction : List.of("north", "south", "east", "west", "up", "down")) {
            faces.put(direction, new BbModelData.CubeFace(
                    new float[]{0, 0, 16, 16}, 0, BbModelData.TextureReference.none(), true));
        }
        BbModelData.ElementNode cube = new BbModelData.ElementNode(
                "cube", "cube", new Vector3f(), new Vector3f(), new Vector3f(1, 1, 1), true,
                new BbModelData.Cube(new Vector3f(), new Vector3f(16, 16, 16), 0, false, faces));
        BbModelData.Document document = new BbModelData.Document(
                "4.10", "free", 16, 16, List.of(), List.of(cube), List.of());
        ResourceLocation modelResource = new ResourceLocation("test", "models/test.bbmodel");
        ResourceLocation texture = new ResourceLocation("test", "textures/test.png");
        FrameSpec.ModelSpec spec = new FrameSpec.ModelSpec(
                "bbmodel", texture, new ResourceLocation("test", "test_model"),
                "entity_cutout", 0, modelResource, Map.of(), "");

        Map<BbModelData.ElementNode, List<BbCompiledGeometry.Quad>> compiled =
                BbCompiledGeometry.compile(modelResource, spec, document);

        assertEquals(6, compiled.get(cube).size());
        for (BbCompiledGeometry.Quad quad : compiled.get(cube)) {
            assertSame(texture, quad.texture().location());
            assertEquals(4, quad.vertices().length);
            assertEquals(4, quad.uvs().length);
            assertTrue(Float.isFinite(quad.normal().x));
            assertEquals(1.0F, quad.normal().length(), 0.0001F);
            assertEquals(3, quad.detailLevel());
        }
    }

    @Test
    void flattensStaticHierarchyIntoOneMaterialBatch() {
        BbModelData.ElementNode cube = cube("cube", new Vector3f(16, 0, 0));
        BbModelData.GroupNode group = new BbModelData.GroupNode(
                "group", "group", new Vector3f(16, 0, 0), new Vector3f(0, 90, 0),
                new Vector3f(1, 1, 1), true, List.of(cube));
        BbModelData.Document document = new BbModelData.Document(
                "4.10", "free", 16, 16, List.of(), List.of(group), List.of());
        FrameSpec.ModelSpec spec = spec();
        Map<BbModelData.ElementNode, List<BbCompiledGeometry.Quad>> geometry =
                BbCompiledGeometry.compile(spec.bbModel(), spec, document);

        BbCompiledGeometry.StaticGeometry flattened =
                BbCompiledGeometry.compileStatic(spec, document, geometry);

        assertEquals(2, flattened.nodeCount());
        assertEquals(6, flattened.inputQuadCount());
        assertEquals(6, flattened.outputQuadCount());
        assertEquals(1, flattened.batches().size());
        BbCompiledGeometry.Batch batch = flattened.batches().get(0);
        assertEquals(6, batch.quadCount());
        assertEquals(6, batch.quadCount(3));
        assertEquals(6 * BbCompiledGeometry.PACKED_QUAD_STRIDE, batch.data().length);
        for (float value : batch.data()) assertTrue(Float.isFinite(value));
    }

    @Test
    void removesExactDuplicateStaticQuads() {
        BbModelData.ElementNode first = cube("first", new Vector3f());
        BbModelData.ElementNode second = cube("second", new Vector3f());
        BbModelData.Document document = new BbModelData.Document(
                "4.10", "free", 16, 16, List.of(), List.of(first, second), List.of());
        FrameSpec.ModelSpec spec = spec();
        Map<BbModelData.ElementNode, List<BbCompiledGeometry.Quad>> geometry =
                BbCompiledGeometry.compile(spec.bbModel(), spec, document);

        BbCompiledGeometry.StaticGeometry flattened =
                BbCompiledGeometry.compileStatic(spec, document, geometry);

        assertEquals(12, flattened.inputQuadCount());
        assertEquals(6, flattened.outputQuadCount());
        assertFalse(flattened.batches().isEmpty());
    }

    @Test
    void compilesCoarsestRealGeometryForShadows() {
        BbModelData.ElementNode first = cube("first", new Vector3f());
        BbModelData.ElementNode second = cube("second", new Vector3f(32, 16, -16));
        BbModelData.Document document = new BbModelData.Document(
                "4.10", "free", 16, 16, List.of(), List.of(first, second), List.of());
        FrameSpec.ModelSpec spec = spec();
        Map<BbModelData.ElementNode, List<BbCompiledGeometry.Quad>> geometry =
                BbCompiledGeometry.compile(spec.bbModel(), spec, document);
        BbCompiledGeometry.StaticGeometry flattened =
                BbCompiledGeometry.compileStatic(spec, document, geometry);
        BbCompiledGeometry.StaticGeometry shadow = BbCompiledGeometry.compileShadowGeometry(flattened);

        int expectedQuads = flattened.batches().stream().mapToInt(batch -> batch.quadCount(3)).sum();
        assertEquals(expectedQuads, shadow.inputQuadCount());
        assertEquals(expectedQuads, shadow.outputQuadCount());
        assertEquals(flattened.batches().size(), shadow.batches().size());
        assertArrayEquals(flattened.batches().get(0).data(), shadow.batches().get(0).data());
    }

    @Test
    void compilesWorkspaceF1AsSingleStaticMaterialBatch() throws Exception {
        Path modelFile = Path.of("run/F1-rebuild.bbmodel");
        assumeTrue(Files.isRegularFile(modelFile));
        BbModelData.Document document = BbModelParser.parse(
                JsonParser.parseString(Files.readString(modelFile)).getAsJsonObject());
        FrameSpec.ModelSpec spec = spec();

        Map<BbModelData.ElementNode, List<BbCompiledGeometry.Quad>> geometry =
                BbCompiledGeometry.compile(spec.bbModel(), spec, document);
        BbCompiledGeometry.StaticGeometry flattened =
                BbCompiledGeometry.compileStatic(spec, document, geometry);

        assertTrue(document.animations().isEmpty());
        assertEquals(339, flattened.nodeCount());
        assertEquals(2004, flattened.inputQuadCount());
        assertEquals(1999, flattened.outputQuadCount());
        assertEquals(1, flattened.batches().size());
        BbCompiledGeometry.Batch batch = flattened.batches().get(0);
        assertEquals(1999, batch.quadCount(0));
        assertTrue(batch.quadCount(0) >= batch.quadCount(1));
        assertTrue(batch.quadCount(1) >= batch.quadCount(2));
        assertTrue(batch.quadCount(2) >= batch.quadCount(3));
        assertTrue(batch.quadCount(3) > 0);
        assertEquals(batch.quadCount(3),
                BbCompiledGeometry.compileShadowGeometry(flattened).outputQuadCount());
    }

    private static BbModelData.ElementNode cube(String uuid, Vector3f origin) {
        Map<String, BbModelData.CubeFace> faces = new LinkedHashMap<>();
        for (String direction : List.of("north", "south", "east", "west", "up", "down")) {
            faces.put(direction, new BbModelData.CubeFace(
                    new float[]{0, 0, 16, 16}, 0, BbModelData.TextureReference.none(), true));
        }
        return new BbModelData.ElementNode(
                uuid, uuid, new Vector3f(origin), new Vector3f(), new Vector3f(1, 1, 1), true,
                new BbModelData.Cube(new Vector3f(origin), new Vector3f(origin).add(16, 16, 16),
                        0, false, faces));
    }

    private static FrameSpec.ModelSpec spec() {
        ResourceLocation model = new ResourceLocation("test", "models/test.bbmodel");
        ResourceLocation texture = new ResourceLocation("test", "textures/test.png");
        return new FrameSpec.ModelSpec(
                "bbmodel", texture, new ResourceLocation("test", "test_model"),
                "entity_cutout", 0, model, Map.of(), "");
    }
}
