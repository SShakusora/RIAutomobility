package com.sshakusora.riautomobility.editor.client;

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

final class PreviewAutomobile implements RenderableAutomobile {
    private static final long NANOS_PER_TICK = 50_000_000L;
    private final VehicleEditorDraft draft;
    private final long animationStartNanos = System.nanoTime();
    private boolean engineRunning;
    private boolean attachmentGuidesVisible;

    PreviewAutomobile(VehicleEditorDraft draft) {
        this.draft = draft;
    }

    void toggleEngine() {
        this.engineRunning = !this.engineRunning;
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
    @Override public float getWheelAngle(float tickDelta) { return this.engineRunning ? ((getTime() + tickDelta) * 12.0F) : 0; }
    @Override public float getSteering(float tickDelta) { return 0; }
    @Override public float getSuspensionBounce(float tickDelta) { return 0; }
    @Override public boolean engineRunning() { return this.engineRunning; }
    @Override public int getBoostTimer() { return 0; }
    @Override public int getTurboCharge() { return 0; }
    @Override public long getTime() { return (System.nanoTime() - this.animationStartNanos) / NANOS_PER_TICK; }
    @Override public boolean automobileOnGround() { return true; }
    @Override public boolean debris() { return false; }
    @Override public Vector3f debrisColor() { return new Vector3f(1, 1, 1); }
}
