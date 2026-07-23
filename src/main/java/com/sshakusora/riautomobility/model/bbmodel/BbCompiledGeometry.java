package com.sshakusora.riautomobility.model.bbmodel;

import com.sshakusora.riautomobility.content.FrameSpec;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.*;

final class BbCompiledGeometry {
    private static final float PIXEL = 1.0F / 16.0F;
    static final int PACKED_QUAD_STRIDE = 23;

    private BbCompiledGeometry() {
    }

    static Map<BbModelData.ElementNode, List<Quad>> compile(
            ResourceLocation modelResource,
            FrameSpec.ModelSpec spec,
            BbModelData.Document document) {
        Map<BbModelData.ElementNode, List<Quad>> geometry = new IdentityHashMap<>();
        for (BbModelData.Node root : document.roots()) {
            compileNode(root, modelResource, spec, document, geometry);
        }
        return geometry;
    }

    static Map<BbModelData.Node, NodeTransform> compileTransforms(BbModelData.Document document) {
        Map<BbModelData.Node, NodeTransform> transforms = new IdentityHashMap<>();
        Vector3f rootOrigin = new Vector3f();
        for (BbModelData.Node root : document.roots()) {
            compileTransform(root, rootOrigin, transforms);
        }
        return transforms;
    }

    static StaticGeometry compileStatic(FrameSpec.ModelSpec spec,
                                         BbModelData.Document document,
                                         Map<BbModelData.ElementNode, List<Quad>> geometry) {
        boolean reorderable = !spec.renderType().toLowerCase(Locale.ROOT).contains("translucent");
        StaticBatchCollector collector = new StaticBatchCollector(reorderable);
        Matrix4f rootPose = new Matrix4f();
        if (spec.rotationY() != 0.0F) {
            rootPose.rotateY((float) Math.toRadians(spec.rotationY()));
        }
        Vector3f rootOrigin = new Vector3f();
        int[] nodeCount = new int[1];
        for (BbModelData.Node root : document.roots()) {
            flattenNode(root, rootOrigin, rootPose, geometry, collector, nodeCount);
        }
        return new StaticGeometry(collector.build(), nodeCount[0], collector.inputQuadCount(),
                collector.outputQuadCount());
    }

    private static void flattenNode(BbModelData.Node node, Vector3f parentOrigin, Matrix4f parentPose,
                                    Map<BbModelData.ElementNode, List<Quad>> geometry,
                                    StaticBatchCollector collector, int[] nodeCount) {
        if (!node.visible()) return;
        nodeCount[0]++;

        NodeTransform transform = createTransform(node, parentOrigin);
        Matrix4f pose = new Matrix4f(parentPose)
                .translate(transform.translation())
                .rotate(transform.quaternion())
                .scale(transform.scale());
        if (node instanceof BbModelData.GroupNode group) {
            for (BbModelData.Node child : group.children()) {
                flattenNode(child, group.origin(), pose, geometry, collector, nodeCount);
            }
        } else if (node instanceof BbModelData.ElementNode element) {
            for (Quad quad : geometry.getOrDefault(element, List.of())) {
                collector.add(quad.texture(), transformQuad(quad, pose), quad.detailLevel());
            }
        }
    }

    private static NodeTransform createTransform(BbModelData.Node node, Vector3f parentOrigin) {
        Vector3f translation = BbCoordinateSystem.position(
                new Vector3f(node.origin()).sub(parentOrigin)).mul(PIXEL);
        Vector3f rotation = BbCoordinateSystem.rotation(new Vector3f(node.rotation()));
        Quaternionf quaternion = rotationQuaternion(rotation);
        boolean textureMesh = node instanceof BbModelData.ElementNode element
                && element.geometry() instanceof BbModelData.TextureMesh;
        Vector3f scale = textureMesh ? new Vector3f(1, 1, 1) : new Vector3f(node.scale());
        return new NodeTransform(translation, rotation, quaternion, scale);
    }

    private static PackedQuad transformQuad(Quad quad, Matrix4f pose) {
        Vector3f[] vertices = new Vector3f[4];
        for (int index = 0; index < 4; index++) {
            vertices[index] = pose.transformPosition(new Vector3f(quad.vertices()[index]));
        }
        Vector3f normal = new Vector3f(vertices[1]).sub(vertices[0])
                .cross(new Vector3f(vertices[2]).sub(vertices[0]));
        if (normal.lengthSquared() > 1.0E-12F) normal.normalize();
        else normal.set(quad.normal());

        float[] data = new float[PACKED_QUAD_STRIDE];
        data[0] = normal.x;
        data[1] = normal.y;
        data[2] = normal.z;
        int cursor = 3;
        for (int index = 0; index < 4; index++) {
            Vector3f vertex = vertices[index];
            Vector2f uv = quad.uvs()[index];
            data[cursor++] = vertex.x;
            data[cursor++] = vertex.y;
            data[cursor++] = vertex.z;
            data[cursor++] = uv.x;
            data[cursor++] = uv.y;
        }
        return new PackedQuad(data);
    }

    private static int detailLevel(BbModelData.Geometry geometry) {
        float maximum = Float.POSITIVE_INFINITY;
        if (geometry instanceof BbModelData.Cube cube) {
            maximum = Math.max(Math.abs(cube.to().x - cube.from().x),
                    Math.max(Math.abs(cube.to().y - cube.from().y), Math.abs(cube.to().z - cube.from().z)));
        } else if (geometry instanceof BbModelData.Mesh mesh && !mesh.vertices().isEmpty()) {
            float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
            for (Vector3f vertex : mesh.vertices().values()) {
                minX = Math.min(minX, vertex.x); minY = Math.min(minY, vertex.y); minZ = Math.min(minZ, vertex.z);
                maxX = Math.max(maxX, vertex.x); maxY = Math.max(maxY, vertex.y); maxZ = Math.max(maxZ, vertex.z);
            }
            maximum = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        }
        if (maximum <= 4.0F) return 0;
        if (maximum <= 8.0F) return 1;
        if (maximum <= 12.0F) return 2;
        return 3;
    }

    private static void compileTransform(BbModelData.Node node, Vector3f parentOrigin,
                                         Map<BbModelData.Node, NodeTransform> transforms) {
        transforms.put(node, createTransform(node, parentOrigin));
        if (node instanceof BbModelData.GroupNode group) {
            for (BbModelData.Node child : group.children()) {
                compileTransform(child, group.origin(), transforms);
            }
        }
    }

    static Quaternionf rotationQuaternion(Vector3f rotation) {
        return new Quaternionf().rotationZYX(
                (float) Math.toRadians(rotation.z),
                (float) Math.toRadians(rotation.y),
                (float) Math.toRadians(rotation.x));
    }

    private static void compileNode(BbModelData.Node node,
                                    ResourceLocation modelResource,
                                    FrameSpec.ModelSpec spec,
                                    BbModelData.Document document,
                                    Map<BbModelData.ElementNode, List<Quad>> geometry) {
        if (node instanceof BbModelData.GroupNode group) {
            for (BbModelData.Node child : group.children()) {
                compileNode(child, modelResource, spec, document, geometry);
            }
        } else if (node instanceof BbModelData.ElementNode element) {
            geometry.put(element, compileElement(element, modelResource, spec, document));
        }
    }

    private static List<Quad> compileElement(BbModelData.ElementNode element,
                                             ResourceLocation modelResource,
                                             FrameSpec.ModelSpec spec,
                                             BbModelData.Document document) {
        List<Quad> quads = new ArrayList<>();
        if (element.geometry() instanceof BbModelData.Cube cube) {
            compileCube(element, cube, modelResource, spec, document, quads);
        } else if (element.geometry() instanceof BbModelData.Mesh mesh) {
            compileMesh(mesh, modelResource, spec, document, quads);
        } else if (element.geometry() instanceof BbModelData.TextureMesh mesh) {
            compileTextureMesh(element, mesh, modelResource, spec, document, quads);
        }
        int detailLevel = detailLevel(element.geometry());
        quads.replaceAll(quad -> new Quad(
                quad.texture(), quad.vertices(), quad.uvs(), quad.normal(), detailLevel));
        return List.copyOf(quads);
    }

    private static void compileCube(BbModelData.ElementNode element,
                                    BbModelData.Cube cube,
                                    ResourceLocation modelResource,
                                    FrameSpec.ModelSpec spec,
                                    BbModelData.Document document,
                                    List<Quad> quads) {
        Vector3f from = new Vector3f(cube.from()).sub(element.origin())
                .sub(cube.inflate(), cube.inflate(), cube.inflate()).mul(PIXEL);
        Vector3f to = new Vector3f(cube.to()).sub(element.origin())
                .add(cube.inflate(), cube.inflate(), cube.inflate()).mul(PIXEL);
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
            Vector3f[] faceVertices = vertices.get(entry.getKey());
            if (!face.enabled() || faceVertices == null) continue;
            BbModelRepository.ResolvedTexture texture = BbModelRepository.resolveTexture(
                    modelResource, spec, document, face.texture());
            addQuad(quads, texture, faceVertices,
                    cubeUv(face, cube.mirrorUv(), texture.uvWidth(), texture.uvHeight()));
        }
    }

    private static void compileMesh(BbModelData.Mesh mesh,
                                    ResourceLocation modelResource,
                                    FrameSpec.ModelSpec spec,
                                    BbModelData.Document document,
                                    List<Quad> quads) {
        for (BbModelData.MeshFace face : mesh.faces()) {
            if (face.vertices().size() < 3 || face.texture().disabled()) continue;
            BbModelRepository.ResolvedTexture texture = BbModelRepository.resolveTexture(
                    modelResource, spec, document, face.texture());
            List<Vector3f> points = new ArrayList<>(face.vertices().size());
            List<Vector2f> uvs = new ArrayList<>(face.vertices().size());
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
                addQuad(quads, texture, points.toArray(Vector3f[]::new), uvs.toArray(Vector2f[]::new));
            } else {
                for (int index = 1; index < points.size() - 1; index++) {
                    addQuad(quads, texture,
                            new Vector3f[]{points.get(0), points.get(index), points.get(index + 1), points.get(index + 1)},
                            new Vector2f[]{uvs.get(0), uvs.get(index), uvs.get(index + 1), uvs.get(index + 1)});
                }
            }
        }
    }

    private static void compileTextureMesh(BbModelData.ElementNode element,
                                           BbModelData.TextureMesh mesh,
                                           ResourceLocation modelResource,
                                           FrameSpec.ModelSpec spec,
                                           BbModelData.Document document,
                                           List<Quad> quads) {
        BbModelData.TextureReference reference = mesh.textureName().isBlank()
                ? BbModelData.TextureReference.none()
                : new BbModelData.TextureReference(null, mesh.textureName(), false);
        BbModelRepository.ResolvedTexture texture = BbModelRepository.resolveTexture(
                modelResource, spec, document, reference);
        BbModelRepository.PixelShape shape = BbModelRepository.getTextureShape(texture.location());
        if (shape == null) shape = new BbModelRepository.PixelShape(1, 1, new boolean[]{true});

        Vector2f[] fullUv = {new Vector2f(1, 1), new Vector2f(1, 0), new Vector2f(0, 0), new Vector2f(0, 1)};
        addQuad(quads, texture, quad(
                textureMeshPoint(element, mesh, -texture.uvWidth(), 0, 0),
                textureMeshPoint(element, mesh, -texture.uvWidth(), 0, texture.uvHeight()),
                textureMeshPoint(element, mesh, 0, 0, texture.uvHeight()),
                textureMeshPoint(element, mesh, 0, 0, 0)), fullUv);
        addQuad(quads, texture, quad(
                textureMeshPoint(element, mesh, 0, -1, 0),
                textureMeshPoint(element, mesh, 0, -1, texture.uvHeight()),
                textureMeshPoint(element, mesh, -texture.uvWidth(), -1, texture.uvHeight()),
                textureMeshPoint(element, mesh, -texture.uvWidth(), -1, 0)), fullUv);

        float pixelWidth = texture.uvWidth() / (float) shape.width();
        float pixelHeight = texture.uvHeight() / (float) shape.height();
        compileVerticalTextureMeshEdges(element, mesh, texture, shape, pixelWidth, pixelHeight, false, quads);
        compileVerticalTextureMeshEdges(element, mesh, texture, shape, pixelWidth, pixelHeight, true, quads);
        compileHorizontalTextureMeshEdges(element, mesh, texture, shape, pixelWidth, pixelHeight, false, quads);
        compileHorizontalTextureMeshEdges(element, mesh, texture, shape, pixelWidth, pixelHeight, true, quads);
    }

    private static void compileVerticalTextureMeshEdges(
            BbModelData.ElementNode element, BbModelData.TextureMesh mesh,
            BbModelRepository.ResolvedTexture texture, BbModelRepository.PixelShape shape,
            float pixelWidth, float pixelHeight, boolean right, List<Quad> quads) {
        for (int x = 0; x < shape.width(); x++) {
            int y = 0;
            while (y < shape.height()) {
                while (y < shape.height() && (!shape.opaque(x, y)
                        || shape.opaque(right ? x + 1 : x - 1, y))) y++;
                if (y >= shape.height()) break;
                int start = y++;
                while (y < shape.height() && shape.opaque(x, y)
                        && !shape.opaque(right ? x + 1 : x - 1, y)) y++;

                float edgeX = -texture.uvWidth() + (x + (right ? 1 : 0)) * pixelWidth;
                float z0 = start * pixelHeight;
                float z1 = y * pixelHeight;
                Vector2f[] uv = edgeUv(x, start, x + 1, y, shape);
                Vector3f[] vertices = right
                        ? quad(textureMeshPoint(element, mesh, edgeX, 0, z0),
                        textureMeshPoint(element, mesh, edgeX, 0, z1),
                        textureMeshPoint(element, mesh, edgeX, -1, z1),
                        textureMeshPoint(element, mesh, edgeX, -1, z0))
                        : quad(textureMeshPoint(element, mesh, edgeX, -1, z0),
                        textureMeshPoint(element, mesh, edgeX, -1, z1),
                        textureMeshPoint(element, mesh, edgeX, 0, z1),
                        textureMeshPoint(element, mesh, edgeX, 0, z0));
                addQuad(quads, texture, vertices, uv);
            }
        }
    }

    private static void compileHorizontalTextureMeshEdges(
            BbModelData.ElementNode element, BbModelData.TextureMesh mesh,
            BbModelRepository.ResolvedTexture texture, BbModelRepository.PixelShape shape,
            float pixelWidth, float pixelHeight, boolean bottom, List<Quad> quads) {
        for (int y = 0; y < shape.height(); y++) {
            int x = 0;
            while (x < shape.width()) {
                while (x < shape.width() && (!shape.opaque(x, y)
                        || shape.opaque(x, bottom ? y + 1 : y - 1))) x++;
                if (x >= shape.width()) break;
                int start = x++;
                while (x < shape.width() && shape.opaque(x, y)
                        && !shape.opaque(x, bottom ? y + 1 : y - 1)) x++;

                float x0 = -texture.uvWidth() + start * pixelWidth;
                float x1 = -texture.uvWidth() + x * pixelWidth;
                float edgeZ = (y + (bottom ? 1 : 0)) * pixelHeight;
                Vector2f[] uv = edgeUv(start, y, x, y + 1, shape);
                Vector3f[] vertices = bottom
                        ? quad(textureMeshPoint(element, mesh, x0, -1, edgeZ),
                        textureMeshPoint(element, mesh, x1, -1, edgeZ),
                        textureMeshPoint(element, mesh, x1, 0, edgeZ),
                        textureMeshPoint(element, mesh, x0, 0, edgeZ))
                        : quad(textureMeshPoint(element, mesh, x0, 0, edgeZ),
                        textureMeshPoint(element, mesh, x1, 0, edgeZ),
                        textureMeshPoint(element, mesh, x1, -1, edgeZ),
                        textureMeshPoint(element, mesh, x0, -1, edgeZ));
                addQuad(quads, texture, vertices, uv);
            }
        }
    }

    private static Vector2f[] edgeUv(int x0, int y0, int x1, int y1,
                                    BbModelRepository.PixelShape shape) {
        return new Vector2f[]{
                new Vector2f(x0 / (float) shape.width(), y0 / (float) shape.height()),
                new Vector2f(x0 / (float) shape.width(), y1 / (float) shape.height()),
                new Vector2f(x1 / (float) shape.width(), y1 / (float) shape.height()),
                new Vector2f(x1 / (float) shape.width(), y0 / (float) shape.height())
        };
    }

    private static void addQuad(List<Quad> quads, BbModelRepository.ResolvedTexture texture,
                                Vector3f[] vertices, Vector2f[] uvs) {
        BbCoordinateSystem.ConvertedQuad converted = BbCoordinateSystem.quad(vertices, uvs);
        Vector3f[] convertedVertices = converted.vertices();
        Vector3f normal = new Vector3f(convertedVertices[1]).sub(convertedVertices[0])
                .cross(new Vector3f(convertedVertices[2]).sub(convertedVertices[0])).normalize();
        quads.add(new Quad(texture, convertedVertices, converted.uvs(), normal, 0));
    }

    private static Vector3f textureMeshPoint(BbModelData.ElementNode element,
                                             BbModelData.TextureMesh mesh,
                                             float x, float y, float z) {
        return new Vector3f(
                x * element.scale().x + mesh.localPivot().x,
                y * element.scale().y + mesh.localPivot().y,
                z * element.scale().z + mesh.localPivot().z).mul(PIXEL);
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
        Vector2f[] values = {new Vector2f(u1, v1), new Vector2f(u1, v2), new Vector2f(u2, v2), new Vector2f(u2, v1)};
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

    private static Vector3f v(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    private static Vector3f[] quad(Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
        return new Vector3f[]{a, b, c, d};
    }

    record Quad(BbModelRepository.ResolvedTexture texture, Vector3f[] vertices,
                Vector2f[] uvs, Vector3f normal, int detailLevel) {
    }

    record StaticGeometry(List<Batch> batches, int nodeCount, int inputQuadCount, int outputQuadCount) {
        static final StaticGeometry EMPTY = new StaticGeometry(List.of(), 0, 0, 0);
    }

    record Batch(BbModelRepository.ResolvedTexture texture, float[] data, byte[] detailLevels) {
        int quadCount() {
            return detailLevels.length;
        }

        int quadCount(int lod) {
            int count = 0;
            for (byte detailLevel : detailLevels) {
                if (detailLevel >= lod) count++;
            }
            return count;
        }
    }

    private record PackedQuad(float[] data) {
    }

    private static final class StaticBatchCollector {
        private final boolean reorderable;
        private final Map<BbModelRepository.ResolvedTexture, BatchBuilder> grouped = new LinkedHashMap<>();
        private final List<BatchBuilder> ordered = new ArrayList<>();
        private int inputQuadCount;

        private StaticBatchCollector(boolean reorderable) {
            this.reorderable = reorderable;
        }

        void add(BbModelRepository.ResolvedTexture texture, PackedQuad quad, int detailLevel) {
            inputQuadCount++;
            BatchBuilder builder;
            if (reorderable) {
                builder = grouped.computeIfAbsent(texture, BatchBuilder::new);
            } else if (!ordered.isEmpty() && ordered.get(ordered.size() - 1).texture.equals(texture)) {
                builder = ordered.get(ordered.size() - 1);
            } else {
                builder = new BatchBuilder(texture);
                ordered.add(builder);
            }
            builder.add(quad.data(), detailLevel);
        }

        List<Batch> build() {
            List<BatchBuilder> builders = reorderable ? new ArrayList<>(grouped.values()) : ordered;
            return builders.stream().map(BatchBuilder::build).filter(batch -> batch.quadCount() > 0).toList();
        }

        int inputQuadCount() {
            return inputQuadCount;
        }

        int outputQuadCount() {
            return (reorderable ? grouped.values() : ordered).stream().mapToInt(BatchBuilder::size).sum();
        }
    }

    private static final class BatchBuilder {
        private final BbModelRepository.ResolvedTexture texture;
        private final Map<QuadKey, Integer> quads = new HashMap<>();
        private float[] data = new float[PACKED_QUAD_STRIDE * 64];
        private byte[] detailLevels = new byte[64];
        private int size;

        private BatchBuilder(BbModelRepository.ResolvedTexture texture) {
            this.texture = texture;
        }

        void add(float[] quad, int detailLevel) {
            QuadKey key = new QuadKey(quad);
            Integer existing = quads.get(key);
            if (existing != null) {
                detailLevels[existing] = (byte) Math.max(detailLevels[existing], detailLevel);
                return;
            }
            ensureCapacity(size + 1);
            System.arraycopy(quad, 0, data, size * PACKED_QUAD_STRIDE, PACKED_QUAD_STRIDE);
            detailLevels[size] = (byte) detailLevel;
            quads.put(key, size);
            size++;
        }

        int size() {
            return size;
        }

        Batch build() {
            return new Batch(texture, Arrays.copyOf(data, size * PACKED_QUAD_STRIDE),
                    Arrays.copyOf(detailLevels, size));
        }

        private void ensureCapacity(int required) {
            if (required <= detailLevels.length) return;
            int capacity = Math.max(required, detailLevels.length * 2);
            data = Arrays.copyOf(data, capacity * PACKED_QUAD_STRIDE);
            detailLevels = Arrays.copyOf(detailLevels, capacity);
        }
    }

    private static final class QuadKey {
        private final int[] bits;
        private final int hash;

        private QuadKey(float[] data) {
            bits = new int[data.length];
            for (int index = 0; index < data.length; index++) bits[index] = Float.floatToIntBits(data[index]);
            hash = Arrays.hashCode(bits);
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof QuadKey key && Arrays.equals(bits, key.bits);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    record NodeTransform(Vector3f translation, Vector3f rotation,
                         Quaternionf quaternion, Vector3f scale) {
    }
}
