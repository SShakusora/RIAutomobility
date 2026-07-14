package com.sshakusora.riautomobility.model.bbmodel;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BbModelBounds {
    private static final float FRAME_ITEM_GUI_SCALE = 0.44F;
    private static final float FRAME_ITEM_RENDER_DIVISOR = 0.77F;
    private static final Quaternionf FRAME_ITEM_GUI_ROTATION = new Quaternionf().rotationXYZ(
            (float)Math.toRadians(30.0F), (float)Math.toRadians(-45.0F), 0.0F);

    private BbModelBounds() {}

    public static float maxDimensionPx(BbModelData.Document document) {
        return sizePx(document).maxDimensionPx();
    }

    public static Size sizePx(BbModelData.Document document) {
        return measure(document).size();
    }

    public static Measurement measure(BbModelData.Document document) {
        Bounds bounds = new Bounds();
        Matrix4f identity = new Matrix4f();
        for (BbModelData.Node root : document.roots()) {
            includeNode(root, new Vector3f(), identity, document, bounds);
        }
        if (bounds.empty()) {
            throw new BbModelFormatException("BBModel has no visible geometry to determine its bounds");
        }
        Size size = new Size(
                bounds.max.x - bounds.min.x,
                bounds.max.y - bounds.min.y,
                bounds.max.z - bounds.min.z);
        float projectedSpanPx = Math.max(
                bounds.frameItemMax.x - bounds.frameItemMin.x,
                bounds.frameItemMax.y - bounds.frameItemMin.y);
        return new Measurement(size, projectedSpanPx,
                automaticFrameItemLengthPx(projectedSpanPx));
    }

    static float automaticFrameItemLengthPx(float projectedSpanPx) {
        return projectedSpanPx * FRAME_ITEM_GUI_SCALE / FRAME_ITEM_RENDER_DIVISOR;
    }

    private static void includeNode(BbModelData.Node node, Vector3f parentOrigin, Matrix4f parentTransform,
                                    BbModelData.Document document, Bounds bounds) {
        if (!node.visible()) return;

        Vector3f translation = BbCoordinateSystem.position(new Vector3f(node.origin()).sub(parentOrigin));
        Vector3f rotation = BbCoordinateSystem.rotation(node.rotation());
        Matrix4f transform = new Matrix4f(parentTransform)
                .translate(translation)
                .rotate(new Quaternionf().rotationZYX(
                        (float)Math.toRadians(rotation.z),
                        (float)Math.toRadians(rotation.y),
                        (float)Math.toRadians(rotation.x)
                ));
        boolean textureMesh = node instanceof BbModelData.ElementNode element
                && element.geometry() instanceof BbModelData.TextureMesh;
        if (!textureMesh) transform.scale(node.scale());

        if (node instanceof BbModelData.GroupNode group) {
            for (BbModelData.Node child : group.children()) {
                includeNode(child, group.origin(), transform, document, bounds);
            }
        } else if (node instanceof BbModelData.ElementNode element) {
            includeGeometry(element, transform, document, bounds);
        }
    }

    private static void includeGeometry(BbModelData.ElementNode element, Matrix4f transform,
                                        BbModelData.Document document, Bounds bounds) {
        if (element.geometry() instanceof BbModelData.Cube cube) {
            Vector3f from = new Vector3f(cube.from()).sub(element.origin())
                    .sub(cube.inflate(), cube.inflate(), cube.inflate());
            Vector3f to = new Vector3f(cube.to()).sub(element.origin())
                    .add(cube.inflate(), cube.inflate(), cube.inflate());
            for (var entry : cube.faces().entrySet()) {
                if (!entry.getValue().enabled()) continue;
                switch (entry.getKey()) {
                    case "north" -> includeFace(transform, bounds,
                            point(to.x, to.y, from.z), point(to.x, from.y, from.z),
                            point(from.x, from.y, from.z), point(from.x, to.y, from.z));
                    case "south" -> includeFace(transform, bounds,
                            point(from.x, to.y, to.z), point(from.x, from.y, to.z),
                            point(to.x, from.y, to.z), point(to.x, to.y, to.z));
                    case "east" -> includeFace(transform, bounds,
                            point(to.x, to.y, to.z), point(to.x, from.y, to.z),
                            point(to.x, from.y, from.z), point(to.x, to.y, from.z));
                    case "west" -> includeFace(transform, bounds,
                            point(from.x, to.y, from.z), point(from.x, from.y, from.z),
                            point(from.x, from.y, to.z), point(from.x, to.y, to.z));
                    case "up" -> includeFace(transform, bounds,
                            point(from.x, to.y, from.z), point(from.x, to.y, to.z),
                            point(to.x, to.y, to.z), point(to.x, to.y, from.z));
                    case "down" -> includeFace(transform, bounds,
                            point(from.x, from.y, to.z), point(from.x, from.y, from.z),
                            point(to.x, from.y, from.z), point(to.x, from.y, to.z));
                    default -> { }
                }
            }
        } else if (element.geometry() instanceof BbModelData.Mesh mesh) {
            for (BbModelData.MeshFace face : mesh.faces()) {
                if (face.vertices().size() < 3 || face.texture().disabled()) continue;
                for (String vertexId : face.vertices()) {
                    Vector3f vertex = mesh.vertices().get(vertexId);
                    if (vertex == null) {
                        throw new BbModelFormatException("Mesh face references missing vertex " + vertexId);
                    }
                    include(vertex, transform, bounds);
                }
            }
        } else if (element.geometry() instanceof BbModelData.TextureMesh mesh) {
            BbModelData.Texture texture = findTexture(document, mesh.textureName());
            float width = texture == null ? document.textureWidth() : texture.uvWidth();
            float height = texture == null ? document.textureHeight() : texture.uvHeight();
            Vector3f from = textureMeshPoint(element, mesh, -width, -1.0F, 0.0F);
            Vector3f to = textureMeshPoint(element, mesh, 0.0F, 0.0F, height);
            includeBox(from, to, transform, bounds);
        }
    }

    private static void includeBox(Vector3f from, Vector3f to, Matrix4f transform, Bounds bounds) {
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    include(new Vector3f(
                            x == 0 ? from.x : to.x,
                            y == 0 ? from.y : to.y,
                            z == 0 ? from.z : to.z
                    ), transform, bounds);
                }
            }
        }
    }

    private static void includeFace(Matrix4f transform, Bounds bounds, Vector3f... points) {
        for (Vector3f point : points) include(point, transform, bounds);
    }

    private static Vector3f point(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    private static void include(Vector3f blockbenchPoint, Matrix4f transform, Bounds bounds) {
        Vector3f point = BbCoordinateSystem.position(new Vector3f(blockbenchPoint));
        transform.transformPosition(point);
        bounds.include(point);
    }

    private static Vector3f textureMeshPoint(BbModelData.ElementNode element,
                                              BbModelData.TextureMesh mesh,
                                              float x, float y, float z) {
        return new Vector3f(
                x * element.scale().x + mesh.localPivot().x,
                y * element.scale().y + mesh.localPivot().y,
                z * element.scale().z + mesh.localPivot().z
        );
    }

    private static BbModelData.Texture findTexture(BbModelData.Document document, String key) {
        if (key == null || key.isBlank()) {
            return document.textures().stream().filter(BbModelData.Texture::useAsDefault).findFirst()
                    .orElse(document.textures().isEmpty() ? null : document.textures().get(0));
        }
        return document.textures().stream()
                .filter(texture -> key.equals(texture.uuid())
                        || key.equals(texture.id())
                        || key.equals(texture.name()))
                .findFirst()
                .orElse(null);
    }

    private static final class Bounds {
        private final Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
        private final Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
        private final Vector3f frameItemMin = new Vector3f(Float.POSITIVE_INFINITY);
        private final Vector3f frameItemMax = new Vector3f(Float.NEGATIVE_INFINITY);

        void include(Vector3f point) {
            if (!Float.isFinite(point.x) || !Float.isFinite(point.y) || !Float.isFinite(point.z)) {
                throw new BbModelFormatException("BBModel contains non-finite geometry coordinates");
            }
            min.min(point);
            max.max(point);

            // Match Automobility's component renderer Y/Z flip followed by the
            // automobile_frame.json GUI rotation. Translation does not affect span.
            Vector3f frameItemPoint = new Vector3f(point.x, -point.y, -point.z)
                    .rotate(FRAME_ITEM_GUI_ROTATION);
            frameItemMin.min(frameItemPoint);
            frameItemMax.max(frameItemPoint);
        }

        boolean empty() {
            return !Float.isFinite(min.x);
        }
    }

    public record Size(float widthPx, float heightPx, float depthPx) {
        public float maxDimensionPx() {
            return Math.max(widthPx, Math.max(heightPx, depthPx));
        }
    }

    public record Measurement(Size size, float frameItemProjectedSpanPx, float frameItemLengthPx) {}
}
