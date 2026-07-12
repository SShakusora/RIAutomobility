package com.sshakusora.riautomobility.model.bbmodel;

import org.joml.Vector2f;
import org.joml.Vector3f;

final class BbCoordinateSystem {
    private BbCoordinateSystem() {}

    static Vector3f position(Vector3f value) {
        return new Vector3f(value.x, -value.y, value.z);
    }

    static Vector3f rotation(Vector3f value) {
        return new Vector3f(-value.x, value.y, -value.z);
    }

    static ConvertedQuad quad(Vector3f[] vertices, Vector2f[] uvs) {
        if (vertices.length != 4 || uvs.length != 4) {
            throw new IllegalArgumentException("A BBModel render quad must contain four vertices and UV coordinates");
        }
        Vector3f[] convertedVertices = new Vector3f[4];
        Vector2f[] convertedUvs = new Vector2f[4];
        int[] reflectedWinding = {2, 1, 0, 3};
        for (int index = 0; index < 4; index++) {
            int source = reflectedWinding[index];
            convertedVertices[index] = position(vertices[source]);
            convertedUvs[index] = new Vector2f(uvs[source]);
        }
        return new ConvertedQuad(convertedVertices, convertedUvs);
    }

    record ConvertedQuad(Vector3f[] vertices, Vector2f[] uvs) {}
}
