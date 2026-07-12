package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.definition.RIAutomobileDefinition;
import com.sshakusora.riautomobility.definition.RIAutomobileRegistry;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import io.github.foundationgames.automobility.automobile.WheelBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VehicleEditorDraft {
    public static final String PREVIEW_NAMESPACE = "riautomobility_preview";

    public Target target = Target.FRAME;
    public ModelFormat modelFormat = ModelFormat.BBMODEL;
    public String namespace = "customvehicles";
    public String componentPath = "new_vehicle";
    public String displayName = "New Vehicle";
    public boolean overwrite;
    public boolean showInCreativeTab = true;

    public Path modelFile;
    public Path textureFile;
    public Path animationFile;
    public String modelError = "";
    public boolean previewReady;
    public final String previewKey = UUID.randomUUID().toString().replace("-", "");

    public AutomobileFrame selectedFrame;
    public AutomobileWheel selectedWheel;
    public AutomobileEngine selectedEngine;

    public float weight = 0.5F;
    public float lengthPx = 28.0F;
    public float seatHeight = 3.0F;
    public float enginePosBack = 18.0F;
    public float enginePosUp = 2.0F;
    public float rearAttachmentPos = 23.0F;
    public float frontAttachmentPos = 22.0F;
    public float widthBlocks = 1.5F;
    public float heightBlocks = 1.0F;
    public boolean hideEngine;
    public final List<WheelPoint> wheelPoints = new ArrayList<>();
    public final List<Vec3> seats = new ArrayList<>();
    public final List<Vec3> cameraPositions = new ArrayList<>();
    public final List<HitboxPoint> hitboxes = new ArrayList<>();

    public float wheelSize = 0.6F;
    public float wheelGrip = 0.5F;
    public float wheelRadius = 3.0F;
    public float wheelWidth = 3.0F;
    public float rotationY;

    public VehicleEditorDraft(AutomobileFrame frame, AutomobileWheel wheel, AutomobileEngine engine) {
        this.selectedEngine = engine;
        loadFrame(frame);
        loadWheel(wheel);
    }

    public void loadFrame(AutomobileFrame frame) {
        this.selectedFrame = frame;
        this.weight = frame.weight();
        this.lengthPx = frame.model().lengthPx();
        this.seatHeight = frame.model().seatHeight();
        this.enginePosBack = frame.model().enginePosBack();
        this.enginePosUp = frame.model().enginePosUp();
        this.rearAttachmentPos = frame.model().rearAttachmentPos();
        this.frontAttachmentPos = frame.model().frontAttachmentPos();
        this.wheelPoints.clear();
        for (WheelBase.WheelPos point : frame.model().wheelBase().wheels) {
            this.wheelPoints.add(WheelPoint.from(point));
        }

        RIAutomobileDefinition definition = RIAutomobileRegistry.get(frame);
        this.widthBlocks = definition.dimensions().width;
        this.heightBlocks = definition.dimensions().height;
        this.hideEngine = definition.hideEngine();
        this.seats.clear();
        definition.seats().forEach(seat -> this.seats.add(seat.pos()));
        if (this.seats.isEmpty()) {
            this.seats.add(Vec3.ZERO);
        }
        this.cameraPositions.clear();
        this.cameraPositions.addAll(definition.cameraPositions());
        if (this.cameraPositions.isEmpty()) {
            this.cameraPositions.add(Vec3.ZERO);
        }
        this.hitboxes.clear();
        definition.hitboxes().forEach(hitbox -> this.hitboxes.add(new HitboxPoint(
                hitbox.origin(), hitbox.width(), hitbox.height(), hitbox.hasContainer())));
        if (this.hitboxes.isEmpty()) {
            this.hitboxes.add(new HitboxPoint(Vec3.ZERO, this.widthBlocks, this.heightBlocks, false));
        }
    }

    public void loadWheel(AutomobileWheel wheel) {
        this.selectedWheel = wheel;
        this.wheelSize = wheel.size();
        this.wheelGrip = wheel.grip();
        this.wheelRadius = wheel.model().radius();
        this.wheelWidth = wheel.model().width();
    }

    public ResourceLocation componentId() {
        ResourceLocation id = ResourceLocation.tryBuild(this.namespace, this.componentPath);
        return id == null ? new ResourceLocation("customvehicles", "invalid") : id;
    }

    public String validationError() {
        if (!this.namespace.matches("[a-z0-9_.-]+")) {
            return "Namespace must use lowercase resource-location characters";
        }
        if (!this.componentPath.matches("[a-z0-9/._-]+") || this.componentPath.startsWith("/") || this.componentPath.contains("..")) {
            return "Component path is invalid";
        }
        if (this.displayName.isBlank() || this.displayName.length() > 80) {
            return "Display name must contain 1-80 characters";
        }
        if (this.modelFile == null) {
            return "Choose a model file";
        }
        if (this.modelFormat != ModelFormat.BBMODEL && this.textureFile == null) {
            return "This model format requires a PNG texture";
        }
        if (this.target == Target.FRAME && this.wheelPoints.isEmpty()) {
            return "A frame requires at least one wheel position";
        }
        return this.modelError;
    }

    public FrameSpec.ModelSpec modelSpec(boolean preview) {
        ResourceLocation id = preview
                ? new ResourceLocation(PREVIEW_NAMESPACE, this.previewKey)
                : componentId();
        String kind = this.target.path;
        ResourceLocation modelId = new ResourceLocation(id.getNamespace(), "riautomobility/" + kind + "/" + id.getPath());
        ResourceLocation texture = new ResourceLocation(id.getNamespace(), "textures/entity/automobile/" + kind + "/" + id.getPath() + ".png");
        return switch (this.modelFormat) {
            case BBMODEL -> new FrameSpec.ModelSpec(
                    "bbmodel", texture, modelId, null, "entity_cutout", this.rotationY,
                    null, null,
                    new ResourceLocation(id.getNamespace(), "models/entity/automobile/" + kind + "/" + id.getPath() + ".bbmodel"),
                    Map.of(), ""
            );
            case GECKOLIB -> new FrameSpec.ModelSpec(
                    "geckolib", texture, modelId, null, "entity_cutout", this.rotationY,
                    new ResourceLocation(id.getNamespace(), "geo/" + kind + "/" + id.getPath() + ".geo.json"),
                    new ResourceLocation(id.getNamespace(), "animations/" + kind + "/" + id.getPath() + ".animation.json"),
                    null, Map.of(), ""
            );
            case JSONEM -> new FrameSpec.ModelSpec(
                    "jsonem", texture, modelId,
                    new ResourceLocation(id.getNamespace(), "automobile/" + kind + "/" + id.getPath()),
                    "entity_cutout", this.rotationY, null, null, null, Map.of(), ""
            );
        };
    }

    public AutomobileFrame previewFrame() {
        AutomobileFrame.FrameModel base = this.selectedFrame.model();
        FrameSpec.ModelSpec model = this.target == Target.FRAME && this.previewReady ? modelSpec(true) : null;
        return new AutomobileFrame(
                new ResourceLocation(PREVIEW_NAMESPACE, "frame/" + this.previewKey),
                this.weight,
                new AutomobileFrame.FrameModel(
                        model == null ? base.texture() : model.texture(),
                        model == null ? base.modelId() : model.modelId(),
                        wheelBase(), this.lengthPx, this.seatHeight, this.enginePosBack, this.enginePosUp,
                        this.rearAttachmentPos, this.frontAttachmentPos
                )
        );
    }

    public AutomobileWheel previewWheel() {
        AutomobileWheel.WheelModel base = this.selectedWheel.model();
        FrameSpec.ModelSpec model = this.target == Target.WHEEL && this.previewReady ? modelSpec(true) : null;
        return new AutomobileWheel(
                new ResourceLocation(PREVIEW_NAMESPACE, "wheel/" + this.previewKey),
                this.wheelSize,
                this.wheelGrip,
                new AutomobileWheel.WheelModel(
                        this.wheelRadius,
                        this.wheelWidth,
                        model == null ? base.texture() : model.texture(),
                        model == null ? base.modelId() : model.modelId()
                )
        );
    }

    public FrameSpec frameSpec(boolean preview) {
        ResourceLocation id = preview ? new ResourceLocation(PREVIEW_NAMESPACE, this.previewKey) : componentId();
        return new FrameSpec(
                id, this.weight, modelSpec(preview),
                new FrameSpec.WheelBaseSpec(null, null, this.wheelPoints.stream().map(WheelPoint::toSpec).toList()),
                this.lengthPx, this.seatHeight, this.enginePosBack, this.enginePosUp, this.hideEngine,
                this.rearAttachmentPos, this.frontAttachmentPos, this.widthBlocks, this.heightBlocks,
                List.copyOf(this.seats), List.copyOf(this.cameraPositions),
                this.hitboxes.stream().map(HitboxPoint::toSpec).toList(),
                true, true, List.of(), List.of(), List.of(), List.of(), this.showInCreativeTab
        );
    }

    public WheelSpec wheelSpec(boolean preview) {
        ResourceLocation id = preview ? new ResourceLocation(PREVIEW_NAMESPACE, this.previewKey) : componentId();
        return new WheelSpec(id, this.wheelSize, this.wheelGrip, this.wheelRadius, this.wheelWidth,
                modelSpec(preview), this.showInCreativeTab);
    }

    public WheelBase wheelBase() {
        return new WheelBase(this.wheelPoints.stream().map(WheelPoint::toWheelPos).toArray(WheelBase.WheelPos[]::new));
    }

    public String packName() {
        return (this.namespace + "-" + this.componentPath.replace('/', '-') + "-" + this.target.path)
                .replaceAll("[^a-z0-9_.-]", "-");
    }

    public enum Target {
        FRAME("frame"), WHEEL("wheel");

        public final String path;

        Target(String path) {
            this.path = path;
        }
    }

    public enum ModelFormat {
        BBMODEL("BBModel"), GECKOLIB("GeckoLib"), JSONEM("JsonEM");

        public final String label;

        ModelFormat(String label) {
            this.label = label;
        }
    }

    public record WheelPoint(float forward, float right, float scale, float yaw, String end, String side) {
        static WheelPoint from(WheelBase.WheelPos point) {
            return new WheelPoint(point.forward(), point.right(), point.scale(), point.yaw(),
                    point.end().name().toLowerCase(), point.side().name().toLowerCase());
        }

        WheelBase.WheelPos toWheelPos() {
            return toSpec().toWheelPos();
        }

        FrameSpec.WheelPosSpec toSpec() {
            return new FrameSpec.WheelPosSpec(this.forward, this.right, this.scale, this.yaw, this.end, this.side);
        }
    }

    public record HitboxPoint(Vec3 origin, float width, float height, boolean hasContainer) {
        FrameSpec.HitboxSpec toSpec() {
            return new FrameSpec.HitboxSpec(this.origin, this.width, this.height, this.hasContainer);
        }
    }
}
