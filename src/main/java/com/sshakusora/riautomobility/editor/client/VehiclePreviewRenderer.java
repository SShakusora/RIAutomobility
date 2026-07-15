package com.sshakusora.riautomobility.editor.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import com.sshakusora.riautomobility.entity.HitboxEntity;
import com.sshakusora.riautomobility.model.bbmodel.BbRenderContext;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import io.github.foundationgames.automobility.automobile.WheelBase;
import io.github.foundationgames.automobility.automobile.render.AutomobileModels;
import io.github.foundationgames.automobility.automobile.render.AutomobileRenderer;
import io.github.foundationgames.automobility.automobile.render.BaseModel;
import io.github.foundationgames.automobility.automobile.render.ExhaustFumesModel;
import io.github.foundationgames.automobility.automobile.render.wheel.WheelContextReceiver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

final class VehiclePreviewRenderer {
    enum View {
        FRAME,
        FRAME_WHEELS,
        FRAME_SEATS,
        FRAME_HITBOXES,
        FRAME_ATTACHMENTS,
        WHEEL,
        ENGINE,
        SEAT_FIRST_PERSON
    }

    private static final float COMPONENT_PREVIEW_SCALE = 2.5F;
    private static final WheelBase.WheelPos SINGLE_WHEEL_PREVIEW_POSITION = new WheelBase.WheelPos(
            0.0F, 0.0F, 1.0F, 0.0F, WheelBase.WheelEnd.FRONT, WheelBase.WheelSide.LEFT);

    private final VehicleEditorDraft draft;
    private final PreviewAutomobile preview;
    private PlayerModel<AbstractClientPlayer> seatPlayerModel;
    private View view = View.FRAME;
    private int wheelPointIndex;
    private int seatIndex;
    private int hitboxIndex = -1;
    private float firstPersonYaw;
    private float firstPersonPitch;
    private float firstPersonFov = 70.0F;
    private float rotationX = 18.0F;
    private float rotationY = 35.0F;
    private float zoom = 38.0F;
    private float panX;
    private float panY;
    private int dragButton = -1;
    private double lastMouseX;
    private double lastMouseY;

    VehiclePreviewRenderer(VehicleEditorDraft draft, PreviewAutomobile preview) {
        this.draft = draft;
        this.preview = preview;
    }

    void render(GuiGraphics graphics, float partialTick, int x0, int x1, int y0, int y1,
                View view, int wheelPointIndex, int seatIndex, int hitboxIndex) {
        this.view = view;
        this.wheelPointIndex = wheelPointIndex;
        this.seatIndex = seatIndex;
        this.hitboxIndex = hitboxIndex;
        if (view == View.SEAT_FIRST_PERSON) {
            renderFirstPersonVehicle(graphics, partialTick, x0, x1, y0, y1);
        } else {
            renderOrbitVehicle(graphics, partialTick, x0, x1, y0, y1);
        }
    }

    void resetView(boolean firstPerson) {
        if (firstPerson) {
            firstPersonYaw = 0.0F;
            firstPersonPitch = 0.0F;
            firstPersonFov = 70.0F;
        } else {
            rotationX = 18.0F;
            rotationY = 35.0F;
            zoom = 38.0F;
            panX = 0.0F;
            panY = 0.0F;
        }
    }

    boolean beginDrag(double mouseX, double mouseY, int button, boolean perspective) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT
                && (perspective || button != GLFW.GLFW_MOUSE_BUTTON_RIGHT)) return false;
        dragButton = button;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return true;
    }

    boolean mouseDragged(double mouseX, double mouseY, int button, boolean firstPerson) {
        if (dragButton != button) return false;
        if (firstPerson) {
            firstPersonYaw += (float) (mouseX - lastMouseX) * 0.35F;
            firstPersonPitch = Math.max(-80.0F, Math.min(80.0F,
                    firstPersonPitch + (float) (mouseY - lastMouseY) * 0.35F));
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            rotationY += (float) (mouseX - lastMouseX);
            rotationX = Math.max(-80.0F, Math.min(80.0F,
                    rotationX + (float) (mouseY - lastMouseY)));
        } else {
            panX += (float) (mouseX - lastMouseX);
            panY += (float) (mouseY - lastMouseY);
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return true;
    }

    void mouseReleased(int button) {
        if (dragButton == button) dragButton = -1;
    }

    void mouseScrolled(double amount, boolean firstPerson) {
        if (firstPerson) {
            firstPersonFov = Math.max(30.0F, Math.min(100.0F, firstPersonFov - (float) amount * 4.0F));
        } else {
            zoom = Math.max(12.0F, Math.min(80.0F, zoom + (float) amount * 3.0F));
        }
    }

    private void renderOrbitVehicle(GuiGraphics graphics, float partialTick,
                                    int x0, int x1, int y0, int y1) {
        OutlineBufferSource outlineBuffers = preparePreviewOutline();
        graphics.enableScissor(x0, y0, x1, y1);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate((x0 + x1) / 2.0F + panX, (y0 + y1) / 2.0F + 24 + panY, 160);
        pose.mulPoseMatrix(new Matrix4f().scaling(zoom, zoom, -zoom));
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));
        pose.mulPose(new Quaternionf().rotationX((float) Math.toRadians(rotationX)));
        pose.mulPose(new Quaternionf().rotationY((float) Math.toRadians(rotationY)));
        RenderSystem.enableDepthTest();
        Lighting.setupForEntityInInventory();
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        switch (view) {
            case FRAME, FRAME_WHEELS, FRAME_SEATS, FRAME_HITBOXES, FRAME_ATTACHMENTS -> {
                if (!draft.isPartVisible(VehicleEditorDraft.Target.FRAME)) break;
                AutomobileRenderer.render(pose, buffers, LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, partialTick, preview);
                if (view == View.FRAME_WHEELS) renderSelectedWheelOutline(pose, outlineBuffers, partialTick);
                if (view == View.FRAME_HITBOXES) renderHitboxOutlines(pose, buffers);
                if (view == View.FRAME_SEATS) renderSeatPlayers(pose, buffers, outlineBuffers);
                if (view == View.FRAME_ATTACHMENTS) renderAttachmentOutlines(pose, outlineBuffers, partialTick);
            }
            case WHEEL -> renderSingleWheel(pose, buffers, partialTick);
            case ENGINE -> renderSingleEngine(pose, buffers, partialTick);
            case SEAT_FIRST_PERSON -> {
            }
        }
        buffers.endBatch();
        finishPreviewOutline(outlineBuffers, partialTick);
        pose.popPose();
        Lighting.setupFor3DItems();
        RenderSystem.disableDepthTest();
        graphics.disableScissor();
    }

    private void renderFirstPersonVehicle(GuiGraphics graphics, float partialTick,
                                          int x0, int x1, int y0, int y1) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer player = minecraft.player;
        if (player == null || seatIndex < 0 || seatIndex >= draft.seats.size()) {
            View requestedView = view;
            try {
                view = View.FRAME_SEATS;
                renderOrbitVehicle(graphics, partialTick, x0, x1, y0, y1);
            } finally {
                view = requestedView;
            }
            return;
        }
        Vec3 eye = VehicleEditorDraft.firstPersonEyePosition(draft.seats.get(seatIndex), draft.wheelRadius,
                player.getMyRidingOffset(), player.getEyeHeight(Pose.STANDING));
        renderPerspectiveVehicle(graphics, partialTick, x0, x1, y0, y1,
                eye, firstPersonYaw, firstPersonPitch, firstPersonFov, seatIndex);
    }

    private void renderPerspectiveVehicle(GuiGraphics graphics, float partialTick,
                                          int x0, int x1, int y0, int y1,
                                          Vec3 cameraPosition, float yaw, float pitch, float fov,
                                          int hiddenSeatIndex) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.flush();
        graphics.enableScissor(x0, y0, x1, y1);
        Window window = minecraft.getWindow();
        double guiScale = window.getGuiScale();
        int viewportX = (int) Math.round(x0 * guiScale);
        int viewportY = window.getHeight() - (int) Math.round(y1 * guiScale);
        int viewportWidth = Math.max(1, (int) Math.round((x1 - x0) * guiScale));
        int viewportHeight = Math.max(1, (int) Math.round((y1 - y0) * guiScale));
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        try {
            modelView.setIdentity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.viewport(viewportX, viewportY, viewportWidth, viewportHeight);
            Matrix4f perspective = new Matrix4f().setPerspective(
                    (float) Math.toRadians(fov), (float) viewportWidth / viewportHeight, 0.05F, 100.0F);
            RenderSystem.setProjectionMatrix(perspective, VertexSorting.DISTANCE_TO_ORIGIN);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.clear(256, Minecraft.ON_OSX);
            Lighting.setupForEntityInInventory();
            PoseStack pose = new PoseStack();
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.mulPose(Axis.YP.rotationDegrees(yaw + 180.0F));
            pose.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
            MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
            AutomobileRenderer.render(pose, buffers, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, partialTick, preview);
            if (hiddenSeatIndex >= 0) renderSeatPlayers(pose, buffers, hiddenSeatIndex);
            buffers.endBatch();
        } finally {
            RenderSystem.setProjectionMatrix(previousProjection, VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.viewport(0, 0, window.getWidth(), window.getHeight());
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();
            RenderSystem.disableDepthTest();
            graphics.disableScissor();
        }
        int centerX = (x0 + x1) / 2;
        int centerY = (y0 + y1) / 2;
        graphics.fill(centerX - 4, centerY, centerX + 5, centerY + 1, 0xCCFFFFFF);
        graphics.fill(centerX, centerY - 4, centerX + 1, centerY + 5, 0xCCFFFFFF);
    }

    private OutlineBufferSource preparePreviewOutline() {
        if (view != View.FRAME_WHEELS && view != View.FRAME_SEATS
                && view != View.FRAME_ATTACHMENTS) return null;
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget entityTarget = minecraft.levelRenderer.entityTarget();
        PostChain entityEffect = minecraft.levelRenderer.entityEffect;
        if (entityTarget == null || entityEffect == null || minecraft.player == null) return null;
        entityTarget.clear(Minecraft.ON_OSX);
        minecraft.getMainRenderTarget().bindWrite(false);
        OutlineBufferSource outlineBuffers = minecraft.renderBuffers().outlineBufferSource();
        outlineBuffers.setColor(255, 184, 31, 255);
        return outlineBuffers;
    }

    private void finishPreviewOutline(OutlineBufferSource outlineBuffers, float partialTick) {
        if (outlineBuffers == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        PostChain entityEffect = minecraft.levelRenderer.entityEffect;
        if (entityEffect == null) return;
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        try {
            outlineBuffers.endOutlineBatch();
            entityEffect.process(partialTick);
            minecraft.getMainRenderTarget().bindWrite(false);
            minecraft.levelRenderer.doEntityOutline();
        } finally {
            RenderSystem.setProjectionMatrix(previousProjection, VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            minecraft.getMainRenderTarget().bindWrite(false);
        }
    }

    private void renderSelectedWheelOutline(PoseStack pose, OutlineBufferSource outlineBuffers,
                                            float partialTick) {
        if (outlineBuffers == null) return;
        AutomobileFrame frame = preview.getFrame();
        AutomobileWheel wheels = preview.getWheels();
        WheelBase.WheelPos[] wheelPositions = frame.model().wheelBase().wheels;
        if (wheels.isEmpty() || wheelPointIndex < 0 || wheelPointIndex >= wheelPositions.length) return;
        WheelBase.WheelPos wheelPosition = wheelPositions[wheelPointIndex];
        Model wheelModel = AutomobileModels.getModel(wheels.model().modelId());
        if (wheelModel == null) return;
        if (wheelModel instanceof WheelContextReceiver receiver) receiver.provideContext(wheelPosition);
        float chassisRaise = wheels.model().radius() / 16.0F;
        float scale = wheelPosition.scale();
        float wheelRadius = wheels.model().radius() - wheels.model().radius() * (scale - 1.0F);
        VertexConsumer outlineBuffer = outlineBuffers.getBuffer(wheelModel.renderType(wheels.model().texture()));
        pose.pushPose();
        applyWheelTransform(pose, wheelPosition, chassisRaise, wheelRadius, scale, partialTick);
        BbRenderContext.begin(outlineBuffers, preview, partialTick);
        try {
            wheelModel.renderToBuffer(pose, outlineBuffer, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } finally {
            BbRenderContext.end();
            pose.popPose();
        }
    }

    private void renderAttachmentOutlines(PoseStack pose, OutlineBufferSource outlineBuffers,
                                          float partialTick) {
        if (outlineBuffers == null) return;
        AutomobileFrame frame = preview.getFrame();
        float chassisRaise = preview.getWheels().model().radius() / 16.0F;
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        pose.mulPose(Axis.YP.rotationDegrees(preview.getAutomobileYaw(partialTick) + 180.0F));
        pose.translate(0.0F, -chassisRaise, 0.0F);
        renderRearAttachmentOutline(pose, outlineBuffers, frame, chassisRaise, partialTick);
        renderFrontAttachmentOutline(pose, outlineBuffers, frame);
        pose.popPose();
    }

    private void renderRearAttachmentOutline(PoseStack pose, OutlineBufferSource outlineBuffers,
                                             AutomobileFrame frame, float chassisRaise, float partialTick) {
        var type = preview.getRearAttachmentType();
        if (type.isEmpty()) return;
        Model model = AutomobileModels.getModel(type.model().modelId());
        if (model == null) return;
        outlineBuffers.setColor(255, 72, 184, 255);
        VertexConsumer buffer = outlineBuffers.getBuffer(model.renderType(type.model().texture()));
        pose.pushPose();
        pose.translate(0.0F, chassisRaise, frame.model().rearAttachmentPos() / 16.0F);
        pose.mulPose(Axis.YN.rotationDegrees(preview.getAutomobileYaw(partialTick)
                - preview.getRearAttachmentYaw(partialTick)));
        pose.translate(0.0F, 0.0F, type.model().pivotDistPx() / 16.0F);
        model.renderToBuffer(pose, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }

    private void renderFrontAttachmentOutline(PoseStack pose, OutlineBufferSource outlineBuffers,
                                              AutomobileFrame frame) {
        var type = preview.getFrontAttachmentType();
        if (type.isEmpty()) return;
        Model model = AutomobileModels.getModel(type.model().modelId());
        if (model == null) return;
        outlineBuffers.setColor(48, 210, 255, 255);
        VertexConsumer buffer = outlineBuffers.getBuffer(model.renderType(type.model().texture()));
        pose.pushPose();
        pose.translate(0.0F, 0.0F, frame.model().frontAttachmentPos() / -16.0F);
        model.renderToBuffer(pose, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }

    private void renderSingleWheel(PoseStack pose, MultiBufferSource.BufferSource buffers, float partialTick) {
        if (!draft.isPartVisible(VehicleEditorDraft.Target.WHEEL)) return;
        AutomobileWheel wheel = draft.previewWheel();
        Model model = AutomobileModels.getModel(wheel.model().modelId());
        if (model == null) return;
        if (model instanceof WheelContextReceiver receiver) receiver.provideContext(SINGLE_WHEEL_PREVIEW_POSITION);
        pose.pushPose();
        applySingleComponentTransform(pose);
        renderSingleModel(pose, buffers, model, wheel.model().texture(), partialTick);
        pose.popPose();
    }

    private void renderSingleEngine(PoseStack pose, MultiBufferSource.BufferSource buffers, float partialTick) {
        if (!draft.isPartVisible(VehicleEditorDraft.Target.ENGINE)) return;
        AutomobileEngine engine = draft.previewEngine();
        Model model = AutomobileModels.getModel(engine.model().modelId());
        if (model == null) return;
        pose.pushPose();
        applySingleComponentTransform(pose);
        applyEngineRunningAnimation(pose, partialTick);
        renderSingleModel(pose, buffers, model, engine.model().texture(), partialTick);
        renderEngineExhaust(pose, buffers, engine, partialTick);
        pose.popPose();
    }

    private void renderEngineExhaust(PoseStack pose, MultiBufferSource.BufferSource buffers,
                                     AutomobileEngine engine, float partialTick) {
        if (!preview.engineRunning()) return;
        Model fumes = AutomobileModels.getModel(AutomobileModels.EXHAUST_FUMES);
        if (fumes == null) return;
        ResourceLocation[] textures;
        RenderType renderType;
        if (preview.getBoostTimer() > 0) {
            textures = ExhaustFumesModel.FLAME_TEXTURES;
            int frame = (int) (preview.getTime() % textures.length);
            renderType = RenderType.eyes(textures[frame]);
        } else {
            textures = ExhaustFumesModel.SMOKE_TEXTURES;
            int frame = (int) Math.floor(((preview.getTime() + partialTick) / 1.5F) % textures.length);
            renderType = RenderType.entityTranslucent(textures[frame]);
        }
        VertexConsumer fumesBuffer = buffers.getBuffer(renderType);
        for (AutomobileEngine.ExhaustPos exhaust : engine.model().exhausts()) {
            pose.pushPose();
            pose.translate(exhaust.x() / 16.0F, -exhaust.y() / 16.0F, exhaust.z() / 16.0F);
            pose.mulPose(Axis.YP.rotationDegrees(exhaust.yaw()));
            pose.mulPose(Axis.XP.rotationDegrees(exhaust.pitch()));
            fumes.renderToBuffer(pose, fumesBuffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F);
            pose.popPose();
        }
    }

    private void applyEngineRunningAnimation(PoseStack pose, float partialTick) {
        if (!preview.engineRunning()) return;
        double animationTime = preview.getTime() + partialTick;
        pose.translate(0.0D, Math.cos(animationTime * 2.7D) / 156.0D, 0.0D);
    }

    private void renderSingleModel(PoseStack pose, MultiBufferSource.BufferSource buffers, Model model,
                                   ResourceLocation texture, float partialTick) {
        BbRenderContext.begin(buffers, preview, partialTick);
        try {
            model.renderToBuffer(pose, buffers.getBuffer(model.renderType(texture)),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F);
            if (model instanceof BaseModel base) {
                base.doOtherLayerRender(pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
        } finally {
            BbRenderContext.end();
        }
    }

    private static void applySingleComponentTransform(PoseStack pose) {
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        pose.scale(COMPONENT_PREVIEW_SCALE, COMPONENT_PREVIEW_SCALE, COMPONENT_PREVIEW_SCALE);
    }

    private void applyWheelTransform(PoseStack pose, WheelBase.WheelPos wheelPosition,
                                     float chassisRaise, float wheelRadius, float scale, float partialTick) {
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        pose.mulPose(Axis.YP.rotationDegrees(preview.getAutomobileYaw(partialTick) + 180.0F));
        pose.translate(0.0F, -chassisRaise, 0.0F);
        pose.translate(wheelPosition.right() / 16.0F, wheelRadius / 16.0F,
                -wheelPosition.forward() / 16.0F);
        if (wheelPosition.end() == WheelBase.WheelEnd.FRONT) {
            pose.mulPose(Axis.YP.rotationDegrees(preview.getSteering(partialTick) * 27.0F));
        }
        pose.translate(0.0F, -chassisRaise, 0.0F);
        pose.mulPose(Axis.XP.rotationDegrees(preview.getWheelAngle(partialTick)));
        pose.scale(scale, scale, scale);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F + wheelPosition.yaw()));
    }

    private void renderHitboxOutlines(PoseStack pose, MultiBufferSource.BufferSource buffers) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        double entityHalfWidth = draft.widthBlocks * 0.5D;
        AABB entityBox = previewHitbox(Vec3.ZERO, entityHalfWidth, draft.heightBlocks);
        boolean entitySelected = hitboxIndex < 0;
        LevelRenderer.renderLineBox(pose, lines, entityBox,
                entitySelected ? 1.0F : 0.15F, entitySelected ? 0.72F : 0.85F,
                entitySelected ? 0.12F : 0.85F, 1.0F);
        for (int index = 0; index < draft.hitboxes.size(); index++) {
            VehicleEditorDraft.HitboxPoint hitbox = draft.hitboxes.get(index);
            double inset = HitboxEntity.horizontalCollisionInset(hitbox.width());
            double halfWidth = Math.max(0.0D, hitbox.width() - inset * 2.0D) * 0.5D;
            AABB box = previewHitbox(hitbox.origin(), halfWidth, hitbox.height());
            boolean selected = index == hitboxIndex;
            LevelRenderer.renderLineBox(pose, lines, box,
                    selected ? 1.0F : 0.15F, selected ? 0.72F : 0.8F,
                    selected ? 0.12F : 1.0F, 1.0F);
        }
        pose.popPose();
    }

    private static AABB previewHitbox(Vec3 origin, double halfWidth, double height) {
        return new AABB(origin.x - halfWidth, -origin.y - height, -origin.z - halfWidth,
                origin.x + halfWidth, -origin.y, -origin.z + halfWidth);
    }

    private void renderSeatPlayers(PoseStack pose, MultiBufferSource.BufferSource buffers,
                                   OutlineBufferSource outlineBuffers) {
        renderSeatPlayers(pose, buffers, -1, outlineBuffers);
    }

    private void renderSeatPlayers(PoseStack pose, MultiBufferSource.BufferSource buffers,
                                   int hiddenSeatIndex) {
        renderSeatPlayers(pose, buffers, hiddenSeatIndex, null);
    }

    private void renderSeatPlayers(PoseStack pose, MultiBufferSource.BufferSource buffers,
                                   int hiddenSeatIndex, OutlineBufferSource outlineBuffers) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer player = minecraft.player;
        if (player == null) return;
        if (seatPlayerModel == null) {
            seatPlayerModel = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        }
        seatPlayerModel.riding = true;
        seatPlayerModel.young = false;
        seatPlayerModel.attackTime = 0.0F;
        seatPlayerModel.prepareMobModel(player, 0.0F, 0.0F, 0.0F);
        seatPlayerModel.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        for (int index = 0; index < draft.seats.size(); index++) {
            if (index == hiddenSeatIndex) continue;
            Vec3 seat = draft.seats.get(index);
            pose.pushPose();
            Vec3 passengerPosition = VehicleEditorDraft.passengerPosition(
                    seat, draft.wheelRadius, player.getMyRidingOffset());
            pose.translate(passengerPosition.x, passengerPosition.y, passengerPosition.z);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.scale(-1.0F, -1.0F, 1.0F);
            pose.scale(0.9375F, 0.9375F, 0.9375F);
            pose.translate(0.0F, -1.501F, 0.0F);
            VertexConsumer playerBuffer = index == seatIndex && outlineBuffers != null
                    ? outlineBuffers.getBuffer(seatPlayerModel.renderType(player.getSkinTextureLocation()))
                    : buffers.getBuffer(seatPlayerModel.renderType(player.getSkinTextureLocation()));
            seatPlayerModel.renderToBuffer(pose, playerBuffer, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            pose.popPose();
        }
    }
}
