package com.sshakusora.riautomobility.model.bbmodel;

import com.google.gson.JsonElement;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public final class BbModelData {
    private BbModelData() {}

    public record Document(
            String formatVersion,
            String modelFormat,
            int textureWidth,
            int textureHeight,
            List<Texture> textures,
            List<Node> roots,
            List<Animation> animations
    ) {}

    public record Texture(
            int index,
            String uuid,
            String id,
            String name,
            String relativePath,
            String path,
            String source,
            String renderMode,
            boolean useAsDefault,
            int uvWidth,
            int uvHeight
    ) {}

    public sealed interface Node permits GroupNode, ElementNode {
        String uuid();
        String name();
        Vector3f origin();
        Vector3f rotation();
        Vector3f scale();
        boolean visible();
    }

    public record GroupNode(
            String uuid,
            String name,
            Vector3f origin,
            Vector3f rotation,
            Vector3f scale,
            boolean visible,
            List<Node> children
    ) implements Node {}

    public record ElementNode(
            String uuid,
            String name,
            Vector3f origin,
            Vector3f rotation,
            Vector3f scale,
            boolean visible,
            Geometry geometry
    ) implements Node {}

    public sealed interface Geometry permits Cube, Mesh, TextureMesh, EmptyGeometry {}

    public record Cube(Vector3f from, Vector3f to, float inflate, boolean mirrorUv, Map<String, CubeFace> faces) implements Geometry {}

    public record CubeFace(float[] uv, int rotation, TextureReference texture, boolean enabled) {}

    public record Mesh(Map<String, Vector3f> vertices, List<MeshFace> faces) implements Geometry {}

    public record MeshFace(List<String> vertices, Map<String, Vector2f> uv, TextureReference texture) {}

    public record TextureMesh(String textureName, Vector3f localPivot) implements Geometry {}

    public record EmptyGeometry(String type, JsonElement source) implements Geometry {}

    public record TextureReference(Integer index, String key, boolean disabled) {
        public static TextureReference none() {
            return new TextureReference(null, null, false);
        }

        public static TextureReference disabledReference() {
            return new TextureReference(null, null, true);
        }
    }

    public record Animation(String uuid, String name, float length, String loop, Map<String, Animator> animators) {}

    public record Animator(String uuid, String name, String type, List<Keyframe> keyframes) {}

    public record Keyframe(
            String channel,
            float time,
            String interpolation,
            List<DataPoint> dataPoints,
            float[] bezierLeftTime,
            float[] bezierLeftValue,
            float[] bezierRightTime,
            float[] bezierRightValue
    ) {}

    public record DataPoint(JsonElement x, JsonElement y, JsonElement z) {}
}
