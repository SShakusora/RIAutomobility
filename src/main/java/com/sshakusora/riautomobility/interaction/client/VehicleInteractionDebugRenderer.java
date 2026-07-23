package com.sshakusora.riautomobility.interaction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import com.sshakusora.riautomobility.interaction.VehicleInteractionBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class VehicleInteractionDebugRenderer {
    private static final int[][] EDGES = {
            {0, 1}, {2, 3}, {4, 5}, {6, 7},
            {0, 2}, {1, 3}, {4, 6}, {5, 7},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
    private static final float RED = 0.25F;
    private static final float GREEN = 1.0F;
    private static final float BLUE = 0.55F;

    private VehicleInteractionDebugRenderer() {
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || !minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes()
                || minecraft.showOnlyReducedInfo()) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        PoseStack.Pose pose = event.getPoseStack().last();
        boolean rendered = false;

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof RIAutomobileEntity automobile)
                    || automobile.isInvisible()
                    || automobile.getInteractionBoxes().isEmpty()
                    || !minecraft.getEntityRenderDispatcher().shouldRender(
                    automobile, event.getFrustum(), camera.x, camera.y, camera.z)) {
                continue;
            }

            for (VehicleInteractionBox box : automobile.getInteractionBoxes()) {
                renderBox(lines, pose, box.worldCorners(automobile, event.getPartialTick()), camera);
                rendered = true;
            }
        }

        if (rendered) {
            buffers.endBatch(RenderType.lines());
        }
    }

    private static void renderBox(VertexConsumer lines, PoseStack.Pose pose,
                                  Vec3[] corners, Vec3 camera) {
        for (int[] edge : EDGES) {
            Vec3 start = corners[edge[0]].subtract(camera);
            Vec3 end = corners[edge[1]].subtract(camera);
            Vec3 direction = end.subtract(start).normalize();
            vertex(lines, pose, start, direction);
            vertex(lines, pose, end, direction);
        }
    }

    private static void vertex(VertexConsumer lines, PoseStack.Pose pose,
                               Vec3 position, Vec3 direction) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        lines.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(RED, GREEN, BLUE, 1.0F)
                .normal(normal, (float) direction.x, (float) direction.y, (float) direction.z)
                .endVertex();
    }
}
