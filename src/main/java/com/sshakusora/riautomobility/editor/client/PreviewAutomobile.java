package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.model.bbmodel.BbInstancedRenderer;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import io.github.foundationgames.automobility.automobile.attachment.FrontAttachmentType;
import io.github.foundationgames.automobility.automobile.attachment.RearAttachmentType;
import io.github.foundationgames.automobility.automobile.attachment.front.FrontAttachment;
import io.github.foundationgames.automobility.automobile.attachment.rear.RearAttachment;
import io.github.foundationgames.automobility.automobile.render.RenderableAutomobile;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

final class PreviewAutomobile implements RenderableAutomobile, BbInstancedRenderer.ImmediateTarget {
    private static final long NANOS_PER_TICK = 50_000_000L;
    private static final int ENGINE_PREVIEW_CYCLE_TICKS = 160;
    private static final int ENGINE_ACCELERATION_START = 40;
    private static final int ENGINE_BOOST_START = 90;
    private static final int ENGINE_DECELERATION_START = 110;
    private final VehicleEditorDraft draft;
    private final long animationStartNanos = System.nanoTime();
    private long engineAnimationStartNanos = animationStartNanos;
    private boolean engineRunning;
    private boolean attachmentGuidesVisible;

    PreviewAutomobile(VehicleEditorDraft draft) {
        this.draft = draft;
    }

    void toggleEngine() {
        this.engineRunning = !this.engineRunning;
        if (this.engineRunning) this.engineAnimationStartNanos = System.nanoTime();
    }

    void setAttachmentGuidesVisible(boolean visible) {
        this.attachmentGuidesVisible = visible;
    }

    @Override public AutomobileFrame getFrame() {
        return this.draft.isPartVisible(VehicleEditorDraft.Target.FRAME) ? this.draft.previewFrame() : this.draft.previewSupportFrame();
    }
    @Override public AutomobileEngine getEngine() {
        return !this.draft.hideEngine && this.draft.isPartVisible(VehicleEditorDraft.Target.ENGINE)
                ? this.draft.previewEngine() : AutomobileEngine.EMPTY;
    }
    @Override public AutomobileWheel getWheels() {
        return this.draft.isPartVisible(VehicleEditorDraft.Target.WHEEL) ? this.draft.previewWheel() : AutomobileWheel.EMPTY;
    }
    @Override public @Nullable RearAttachment getRearAttachment() { return null; }
    @Override public @Nullable FrontAttachment getFrontAttachment() { return null; }
    @Override public RearAttachmentType<?> getRearAttachmentType() {
        return this.attachmentGuidesVisible && this.draft.rearAttachmentEnabled
                ? RearAttachmentType.PASSENGER_SEAT : RearAttachmentType.EMPTY;
    }
    @Override public FrontAttachmentType<?> getFrontAttachmentType() {
        return this.attachmentGuidesVisible && this.draft.frontAttachmentEnabled
                ? FrontAttachmentType.CROP_HARVESTER : FrontAttachmentType.EMPTY;
    }
    @Override public float getAutomobileYaw(float tickDelta) { return 0; }
    @Override public float getRearAttachmentYaw(float tickDelta) { return 0; }
    @Override public float getWheelAngle(float tickDelta) {
        if (!this.engineRunning) return 0.0F;
        double elapsed = enginePreviewTicks(tickDelta);
        int cycles = (int) (elapsed / ENGINE_PREVIEW_CYCLE_TICKS);
        float phase = (float) (elapsed % ENGINE_PREVIEW_CYCLE_TICKS);
        float angle = cycles * 1740.0F;
        if (phase <= ENGINE_ACCELERATION_START) return angle + phase * 4.0F;
        angle += ENGINE_ACCELERATION_START * 4.0F;
        if (phase <= ENGINE_BOOST_START) {
            float accelerating = phase - ENGINE_ACCELERATION_START;
            return angle + accelerating * 4.0F + accelerating * accelerating * 0.14F;
        }
        angle += 550.0F;
        if (phase <= ENGINE_DECELERATION_START) return angle + (phase - ENGINE_BOOST_START) * 24.0F;
        angle += (ENGINE_DECELERATION_START - ENGINE_BOOST_START) * 24.0F;
        float decelerating = phase - ENGINE_DECELERATION_START;
        return angle + decelerating * 18.0F - decelerating * decelerating * 0.14F;
    }
    @Override public float getSteering(float tickDelta) { return 0; }
    @Override public float getSuspensionBounce(float tickDelta) { return 0; }
    @Override public boolean engineRunning() { return this.engineRunning; }
    @Override public int getBoostTimer() {
        if (!this.engineRunning) return 0;
        int phase = (int) enginePreviewPhase(0.0F);
        return phase >= ENGINE_BOOST_START && phase < ENGINE_DECELERATION_START
                ? ENGINE_DECELERATION_START - phase : 0;
    }
    @Override public int getTurboCharge() {
        if (!this.engineRunning) return 0;
        int phase = (int) enginePreviewPhase(0.0F);
        if (phase < ENGINE_ACCELERATION_START) return 0;
        if (phase < ENGINE_BOOST_START) return phase - ENGINE_ACCELERATION_START;
        if (phase < ENGINE_DECELERATION_START) return ENGINE_BOOST_START - ENGINE_ACCELERATION_START;
        return Math.max(0, ENGINE_PREVIEW_CYCLE_TICKS - phase);
    }
    @Override public long getTime() { return (System.nanoTime() - this.animationStartNanos) / NANOS_PER_TICK; }
    @Override public boolean automobileOnGround() { return true; }
    @Override public boolean debris() { return false; }
    @Override public Vector3f debrisColor() { return new Vector3f(1, 1, 1); }

    private float enginePreviewPhase(float tickDelta) {
        return (float) (enginePreviewTicks(tickDelta) % ENGINE_PREVIEW_CYCLE_TICKS);
    }

    private double enginePreviewTicks(float tickDelta) {
        return (System.nanoTime() - this.engineAnimationStartNanos) / (double) NANOS_PER_TICK + tickDelta;
    }
}
