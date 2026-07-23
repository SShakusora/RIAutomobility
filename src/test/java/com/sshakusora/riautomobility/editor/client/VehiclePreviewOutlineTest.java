package com.sshakusora.riautomobility.editor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehiclePreviewOutlineTest {
    @Test
    void physicalHitboxUsesVehicleLocalCoordinatesDirectly() {
        AABB box = VehiclePreviewRenderer.vehicleHitbox(
                new Vec3(1.5D, 0.25D, -2.0D), 0.75D, 1.25D);

        assertEquals(0.75D, box.minX);
        assertEquals(2.25D, box.maxX);
        assertEquals(0.25D, box.minY);
        assertEquals(1.5D, box.maxY);
        assertEquals(-2.75D, box.minZ);
        assertEquals(-1.25D, box.maxZ);
    }

    @Test
    void localLeftOffsetMovesToScreenRightAfterHalfTurn() {
        float initialScreenX = transformedScreenX(35.0F);
        float halfTurnScreenX = transformedScreenX(215.0F);

        assertTrue(initialScreenX < 0.0F);
        assertTrue(halfTurnScreenX > 0.0F);
        assertEquals(-initialScreenX, halfTurnScreenX, 0.0001F);
    }

    @Test
    void previewLineQuadExpandsSymmetricallyInScreenSpace() {
        Vector3f start = new Vector3f(10.0F, 20.0F, 3.0F);
        Vector3f end = new Vector3f(10.0F, 40.0F, 5.0F);

        Vector3f[] quad = VehiclePreviewRenderer.previewLineQuad(start, end);

        assertEquals(4, quad.length);
        assertEquals(start.x, (quad[0].x + quad[1].x) * 0.5F, 0.0001F);
        assertEquals(start.y, (quad[0].y + quad[1].y) * 0.5F, 0.0001F);
        assertEquals(end.x, (quad[2].x + quad[3].x) * 0.5F, 0.0001F);
        assertEquals(end.y, (quad[2].y + quad[3].y) * 0.5F, 0.0001F);
        assertEquals(start.z, quad[0].z, 0.0001F);
        assertEquals(end.z, quad[2].z, 0.0001F);
    }

    private static float transformedScreenX(float yawDegrees) {
        PoseStack pose = new PoseStack();
        VehiclePreviewRenderer.applyOrbitRotation(
                pose, new Quaternionf(), new Quaternionf(), 18.0F, yawDegrees);
        Vector4f position = new Vector4f(-1.0F, 0.0F, 0.0F, 1.0F);
        pose.last().pose().transform(position);
        return position.x;
    }
}
