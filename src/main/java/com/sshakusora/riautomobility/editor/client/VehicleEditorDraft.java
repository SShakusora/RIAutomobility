package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.content.EngineSpec;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.definition.RIAutomobileDefinition;
import com.sshakusora.riautomobility.definition.RIAutomobileRegistry;
import com.sshakusora.riautomobility.model.bbmodel.BbModelBounds;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import io.github.foundationgames.automobility.automobile.WheelBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.*;

public final class VehicleEditorDraft {
    public static final String PREVIEW_NAMESPACE = "riautomobility_preview";
    public static final String GENERATED_NAMESPACE = "riautomobility";
    public static final String GENERATED_COMPONENT_PREFIX = "auto_";
    public static final float NORMALIZED_SEAT_HEIGHT_PX = 4.0F;

    public Target target = Target.FRAME;
    public Target previewTarget;
    public String displayName = VehicleImportText.string("default_name");
    public boolean overwrite;
    public boolean showInCreativeTab = true;
    public String modelError = "";

    private final EnumMap<Target, Path> modelFiles = new EnumMap<>(Target.class);
    private final EnumMap<Target, Boolean> previewReady = new EnumMap<>(Target.class);
    private final EnumMap<Target, String> previewKeys = new EnumMap<>(Target.class);
    private final EnumSet<Target> visibleParts = EnumSet.noneOf(Target.class);
    private final String componentPath = generateComponentPath();
    private Vec3 automaticCameraOffset;
    private boolean automaticWheelModelSize;

    public AutomobileFrame selectedFrame;
    public AutomobileWheel selectedWheel;
    public AutomobileEngine selectedEngine;

    public float weight = 0.5F;
    public float lengthPx = 28.0F;
    public float enginePosBack = 18.0F;
    public float enginePosUp = 2.0F;
    public float rearAttachmentPos = 23.0F;
    public float frontAttachmentPos = 22.0F;
    public float widthBlocks = 1.5F;
    public float heightBlocks = 1.0F;
    public boolean hideEngine;
    public boolean frontAttachmentEnabled = true;
    public boolean rearAttachmentEnabled = true;
    public boolean frontAttachmentWhitelistMode = true;
    public boolean rearAttachmentWhitelistMode = true;
    public String frontAttachmentListText = "";
    public String rearAttachmentListText = "";
    public final List<WheelPoint> wheelPoints = new ArrayList<>();
    public final List<Vec3> seats = new ArrayList<>();
    public final List<Vec3> cameraPositions = new ArrayList<>();
    public final List<HitboxPoint> hitboxes = new ArrayList<>();

    public float wheelSize = 0.6F;
    public float wheelGrip = 0.5F;
    public float wheelRadius = 3.0F;
    public float wheelWidth = 3.0F;
    public float wheelRotationY;
    public float engineRotationY;
    public float engineTorque = 0.5F;
    public float engineSpeed = 0.75F;
    public final List<EngineSpec.ExhaustSpec> exhausts = new ArrayList<>();

    public VehicleEditorDraft(AutomobileFrame frame, AutomobileWheel wheel, AutomobileEngine engine) {
        for (Target value : Target.values()) {
            previewReady.put(value, false);
            previewKeys.put(value, UUID.randomUUID().toString().replace("-", ""));
        }
        loadFrame(frame);
        loadWheel(wheel);
        loadEngine(engine);
    }

    public Path modelFile() { return modelFiles.get(target); }
    public Path modelFile(Target part) { return modelFiles.get(part); }
    public void setModelFile(Target part, Path path) {
        modelFiles.put(part, path);
        previewReady.put(part, false);
        if (part == Target.WHEEL) automaticWheelModelSize = true;
    }
    public boolean previewReady(Target part) { return previewReady.getOrDefault(part, false); }
    public void setPreviewReady(Target part, boolean ready) { previewReady.put(part, ready); }
    public String previewKey(Target part) { return previewKeys.get(part); }
    public void showPart(Target part) { visibleParts.add(part); previewTarget = part; }
    public boolean isPartVisible(Target part) { return visibleParts.contains(part); }

    public void loadFrame(AutomobileFrame frame) {
        selectedFrame = frame;
        automaticCameraOffset = null;
        weight = frame.weight();
        lengthPx = frame.model().lengthPx();
        double legacySeatYOffset = normalizedSeatYOffset(frame.model().seatHeight());
        enginePosBack = frame.model().enginePosBack();
        enginePosUp = frame.model().enginePosUp();
        rearAttachmentPos = frame.model().rearAttachmentPos();
        frontAttachmentPos = frame.model().frontAttachmentPos();
        wheelPoints.clear();
        for (WheelBase.WheelPos point : frame.model().wheelBase().wheels) wheelPoints.add(WheelPoint.from(point));
        RIAutomobileDefinition definition = RIAutomobileRegistry.get(frame);
        widthBlocks = definition.dimensions().width;
        heightBlocks = definition.dimensions().height;
        hideEngine = definition.hideEngine();
        frontAttachmentEnabled = definition.frontAttachmentEnabled();
        rearAttachmentEnabled = definition.rearAttachmentEnabled();
        frontAttachmentWhitelistMode = !definition.frontAttachmentWhitelist().isEmpty()
                || definition.frontAttachmentBlacklist().isEmpty();
        rearAttachmentWhitelistMode = !definition.rearAttachmentWhitelist().isEmpty()
                || definition.rearAttachmentBlacklist().isEmpty();
        frontAttachmentListText = formatResourceLocations(frontAttachmentWhitelistMode
                ? definition.frontAttachmentWhitelist() : definition.frontAttachmentBlacklist());
        rearAttachmentListText = formatResourceLocations(rearAttachmentWhitelistMode
                ? definition.rearAttachmentWhitelist() : definition.rearAttachmentBlacklist());
        seats.clear();
        definition.seats().forEach(seat -> seats.add(seat.pos().add(0.0D, legacySeatYOffset, 0.0D)));
        if (seats.isEmpty()) seats.add(defaultSeatPosition());
        cameraPositions.clear();
        cameraPositions.addAll(definition.cameraPositions());
        if (cameraPositions.isEmpty()) cameraPositions.add(Vec3.ZERO);
        hitboxes.clear();
        definition.hitboxes().forEach(h -> hitboxes.add(new HitboxPoint(h.origin(), h.width(), h.height(), h.hasContainer())));
    }

    public void loadWheel(AutomobileWheel wheel) {
        selectedWheel = wheel;
        wheelSize = wheel.size(); wheelGrip = wheel.grip();
        wheelRadius = wheel.model().radius(); wheelWidth = wheel.model().width();
        wheelRotationY = 0.0F;
        automaticWheelModelSize = false;
    }

    public void loadEngine(AutomobileEngine engine) {
        selectedEngine = engine;
        engineTorque = engine.torque(); engineSpeed = engine.speed();
        engineRotationY = 0.0F;
        exhausts.clear();
        for (AutomobileEngine.ExhaustPos e : engine.model().exhausts())
            exhausts.add(new EngineSpec.ExhaustSpec(e.x(), e.y(), e.z(), e.pitch(), e.yaw()));
    }

    public ResourceLocation componentId() {
        return new ResourceLocation(GENERATED_NAMESPACE, componentPath);
    }

    public String namespace() { return GENERATED_NAMESPACE; }
    public String componentPath() { return componentPath; }
    static String generateComponentPath() {
        return GENERATED_COMPONENT_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }

    public String validationError() {
        if (displayName.isBlank() || displayName.length() > 80) return VehicleImportText.string("validation.display_name_length");
        if (target == Target.FRAME) {
            if (!Float.isFinite(weight) || weight <= 0.0F) return VehicleImportText.string("validation.frame_weight");
            if (!Float.isFinite(lengthPx) || lengthPx <= 0.0F) return VehicleImportText.string("validation.item_length");
            if (!Float.isFinite(enginePosBack) || !Float.isFinite(enginePosUp)
                    || !Float.isFinite(rearAttachmentPos) || !Float.isFinite(frontAttachmentPos)) {
                return VehicleImportText.string("validation.frame_positions");
            }
            if (!Float.isFinite(widthBlocks) || widthBlocks <= 0.0F
                    || !Float.isFinite(heightBlocks) || heightBlocks <= 0.0F) {
                return VehicleImportText.string("validation.frame_dimensions");
            }
            if (seats.stream().anyMatch(pos -> !isFinite(pos))
                    || cameraPositions.stream().anyMatch(pos -> !isFinite(pos))) {
                return VehicleImportText.string("validation.seat_camera_positions");
            }
            if (hitboxes.stream().anyMatch(hitbox -> !isFinite(hitbox.origin())
                    || !Float.isFinite(hitbox.width()) || hitbox.width() <= 0.0F
                    || !Float.isFinite(hitbox.height()) || hitbox.height() <= 0.0F)) {
                return VehicleImportText.string("validation.hitbox_dimensions");
            }
            String resourceError = resourceListValidationError("front", frontAttachmentListText);
            if (resourceError != null) return resourceError;
            resourceError = resourceListValidationError("rear", rearAttachmentListText);
            if (resourceError != null) return resourceError;
        }
        Path file = modelFile();
        if (file == null) return VehicleImportText.string("validation.choose_model", VehicleImportText.string("page." + target.path));
        if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".bbmodel")) return VehicleImportText.string("validation.bbmodel_only");
        if (target == Target.FRAME && wheelPoints.isEmpty()) return VehicleImportText.string("validation.frame_wheels");
        return modelError;
    }

    public FrameSpec.ModelSpec modelSpec(Target part, boolean preview) {
        ResourceLocation id = preview ? new ResourceLocation(PREVIEW_NAMESPACE, previewKey(part)) : componentId();
        ResourceLocation modelId = new ResourceLocation(id.getNamespace(), "riautomobility/" + part.path + "/" + id.getPath());
        ResourceLocation texture = new ResourceLocation(id.getNamespace(), "textures/entity/automobile/" + part.path + "/" + id.getPath() + ".png");
        float rotationY = switch (part) {
            case FRAME -> 0.0F;
            case WHEEL -> wheelRotationY;
            case ENGINE -> engineRotationY;
        };
        return new FrameSpec.ModelSpec("bbmodel", texture, modelId, null, "entity_cutout", rotationY,
                null, null, new ResourceLocation(id.getNamespace(), "models/entity/automobile/" + part.path + "/" + id.getPath() + ".bbmodel"), Map.of(), "");
    }

    public FrameSpec.ModelSpec modelSpec(boolean preview) { return modelSpec(target, preview); }

    public AutomobileFrame previewFrame() {
        AutomobileFrame.FrameModel base = selectedFrame.model();
        FrameSpec.ModelSpec model = previewReady(Target.FRAME) ? modelSpec(Target.FRAME, true) : null;
        return frameWithModel(model == null ? base.texture() : model.texture(), model == null ? base.modelId() : model.modelId());
    }

    public AutomobileFrame previewSupportFrame() {
        return frameWithModel(AutomobileFrame.EMPTY.model().texture(), AutomobileFrame.EMPTY.model().modelId());
    }

    private AutomobileFrame frameWithModel(ResourceLocation texture, ResourceLocation modelId) {
        return new AutomobileFrame(new ResourceLocation(PREVIEW_NAMESPACE, "frame/" + previewKey(Target.FRAME)), weight,
                new AutomobileFrame.FrameModel(texture, modelId, wheelBase(), lengthPx, NORMALIZED_SEAT_HEIGHT_PX, enginePosBack, enginePosUp,
                        rearAttachmentPos, frontAttachmentPos));
    }

    public AutomobileWheel previewWheel() {
        AutomobileWheel.WheelModel base = selectedWheel.model();
        FrameSpec.ModelSpec model = previewReady(Target.WHEEL) ? modelSpec(Target.WHEEL, true) : null;
        return new AutomobileWheel(new ResourceLocation(PREVIEW_NAMESPACE, "wheel/" + previewKey(Target.WHEEL)), wheelSize, wheelGrip,
                new AutomobileWheel.WheelModel(wheelRadius, wheelWidth, model == null ? base.texture() : model.texture(),
                        model == null ? base.modelId() : model.modelId()));
    }

    public AutomobileEngine previewEngine() {
        AutomobileEngine.EngineModel base = selectedEngine.model();
        FrameSpec.ModelSpec model = previewReady(Target.ENGINE) ? modelSpec(Target.ENGINE, true) : null;
        return new AutomobileEngine(new ResourceLocation(PREVIEW_NAMESPACE, "engine/" + previewKey(Target.ENGINE)), engineTorque, engineSpeed,
                selectedEngine.sound(), new AutomobileEngine.EngineModel(model == null ? base.texture() : model.texture(),
                model == null ? base.modelId() : model.modelId(), exhausts.stream().map(EngineSpec.ExhaustSpec::toExhaust).toArray(AutomobileEngine.ExhaustPos[]::new)));
    }

    public FrameSpec frameSpec(boolean preview) {
        syncAutomaticCameraPositions();
        ResourceLocation id = preview ? new ResourceLocation(PREVIEW_NAMESPACE, previewKey(Target.FRAME)) : componentId();
        return new FrameSpec(id, weight, modelSpec(Target.FRAME, preview),
                new FrameSpec.WheelBaseSpec(null, null, wheelPoints.stream().map(WheelPoint::toSpec).toList()),
                lengthPx, NORMALIZED_SEAT_HEIGHT_PX, enginePosBack, enginePosUp, hideEngine, rearAttachmentPos, frontAttachmentPos,
                widthBlocks, heightBlocks, List.copyOf(seats), List.copyOf(cameraPositions),
                hitboxes.stream().map(HitboxPoint::toSpec).toList(), frontAttachmentEnabled, rearAttachmentEnabled,
                frontAttachmentWhitelistMode ? parseResourceLocations(frontAttachmentListText) : List.of(),
                frontAttachmentWhitelistMode ? List.of() : parseResourceLocations(frontAttachmentListText),
                rearAttachmentWhitelistMode ? parseResourceLocations(rearAttachmentListText) : List.of(),
                rearAttachmentWhitelistMode ? List.of() : parseResourceLocations(rearAttachmentListText),
                showInCreativeTab);
    }

    public WheelSpec wheelSpec(boolean preview) {
        ResourceLocation id = preview ? new ResourceLocation(PREVIEW_NAMESPACE, previewKey(Target.WHEEL)) : componentId();
        return new WheelSpec(id, wheelSize, wheelGrip, wheelRadius, wheelWidth, modelSpec(Target.WHEEL, preview), showInCreativeTab);
    }

    public EngineSpec engineSpec(boolean preview) {
        ResourceLocation id = preview ? new ResourceLocation(PREVIEW_NAMESPACE, previewKey(Target.ENGINE)) : componentId();
        return new EngineSpec(id, engineTorque, engineSpeed, modelSpec(Target.ENGINE, preview), List.copyOf(exhausts), showInCreativeTab);
    }

    public WheelBase wheelBase() { return new WheelBase(wheelPoints.stream().map(WheelPoint::toWheelPos).toArray(WheelBase.WheelPos[]::new)); }
    static Vec3 defaultSeatPosition() { return Vec3.ZERO; }
    static Vec3 passengerPosition(Vec3 seat, float wheelRadiusPx, double ridingOffset) {
        return new Vec3(seat.x, wheelRadiusPx / 16.0D + seat.y + ridingOffset, seat.z);
    }
    static Vec3 firstPersonEyePosition(Vec3 seat, float wheelRadiusPx, double ridingOffset, float eyeHeight) {
        return passengerPosition(seat, wheelRadiusPx, ridingOffset).add(0.0D, eyeHeight, 0.0D);
    }

    static double normalizedSeatYOffset(float legacySeatHeightPx) {
        return (legacySeatHeightPx - NORMALIZED_SEAT_HEIGHT_PX) / 16.0D;
    }
    void applyAutomaticFrameModelSize(BbModelBounds.Measurement measurement) {
        BbModelBounds.Size size = measurement.size();
        lengthPx = measurement.frameItemLengthPx();
        automaticCameraOffset = automaticThirdPersonCameraOffset(
                size.widthPx(), size.heightPx(), size.depthPx());
        syncAutomaticCameraPositions();
    }

    void applyAutomaticWheelModelSize(BbModelBounds.Measurement measurement) {
        if (!automaticWheelModelSize) return;
        AutomaticWheelModelSize size = automaticWheelModelSize(measurement.size());
        wheelRadius = size.radiusPx();
        wheelWidth = size.widthPx();
        wheelRotationY = size.rotationY();
    }

    void setManualWheelRadius(float value) {
        wheelRadius = value;
        automaticWheelModelSize = false;
    }

    void setManualWheelWidth(float value) {
        wheelWidth = value;
        automaticWheelModelSize = false;
    }

    void setManualWheelRotationY(float value) {
        wheelRotationY = value;
        automaticWheelModelSize = false;
    }

    static AutomaticWheelModelSize automaticWheelModelSize(BbModelBounds.Size size) {
        if (size.depthPx() < size.widthPx()) {
            return new AutomaticWheelModelSize(
                    Math.max(size.widthPx(), size.heightPx()) * 0.5F,
                    size.depthPx(), -90.0F);
        }
        return new AutomaticWheelModelSize(
                Math.max(size.heightPx(), size.depthPx()) * 0.5F,
                size.widthPx(), 0.0F);
    }

    private void syncAutomaticCameraPositions() {
        if (automaticCameraOffset == null) return;
        cameraPositions.clear();
        for (int index = 0; index < Math.max(1, seats.size()); index++) {
            cameraPositions.add(automaticCameraOffset);
        }
    }

    static Vec3 automaticThirdPersonCameraOffset(float widthPx, float heightPx, float depthPx) {
        double width = widthPx;
        double height = heightPx;
        double depth = depthPx;
        double radiusBlocks = Math.sqrt(width * width + height * height + depth * depth) / 32.0D;
        double requiredDistance = radiusBlocks * 1.35D / Math.sin(Math.toRadians(35.0D));
        return new Vec3(Math.min(0.0D, 4.0D - requiredDistance), 0.0D, 0.0D);
    }

    static List<ResourceLocation> parseResourceLocations(String text) {
        if (text == null || text.isBlank()) return List.of();
        LinkedHashSet<ResourceLocation> values = new LinkedHashSet<>();
        for (String token : text.trim().split("[\\s,;]+")) {
            if (token.isEmpty()) continue;
            ResourceLocation id = ResourceLocation.tryParse(token);
            if (id == null) throw new IllegalArgumentException("Invalid resource location: " + token);
            values.add(id);
        }
        return List.copyOf(values);
    }

    private static String formatResourceLocations(List<ResourceLocation> ids) {
        return String.join(", ", ids.stream().map(ResourceLocation::toString).toList());
    }

    private static String resourceListValidationError(String label, String text) {
        try {
            parseResourceLocations(text);
            return null;
        } catch (IllegalArgumentException exception) {
            return VehicleImportText.string("validation.invalid_resource_id", VehicleImportText.string("label." + label + "_list"), exception.getMessage());
        }
    }

    private static boolean isFinite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }
    public String packName() { return GENERATED_NAMESPACE + "-" + componentPath + "-" + target.path; }

    public enum Target {
        FRAME("frame"), WHEEL("wheel"), ENGINE("engine");
        public final String path;
        Target(String path) { this.path = path; }
    }

    public record WheelPoint(float forward, float right, float scale, float yaw, String end, String side) {
        static WheelPoint from(WheelBase.WheelPos p) { return new WheelPoint(p.forward(), p.right(), p.scale(), p.yaw(), p.end().name().toLowerCase(), p.side().name().toLowerCase()); }
        WheelPoint mirrored() {
            float mirroredYaw = (yaw + 180.0F) % 360.0F;
            if (mirroredYaw < 0.0F) mirroredYaw += 360.0F;
            return new WheelPoint(forward, -right, scale, mirroredYaw, end, side.equals("left") ? "right" : "left");
        }
        WheelBase.WheelPos toWheelPos() { return toSpec().toWheelPos(); }
        FrameSpec.WheelPosSpec toSpec() { return new FrameSpec.WheelPosSpec(forward, right, scale, yaw, end, side); }
    }

    public record HitboxPoint(Vec3 origin, float width, float height, boolean hasContainer) {
        FrameSpec.HitboxSpec toSpec() { return new FrameSpec.HitboxSpec(origin, width, height, hasContainer); }
    }

    record AutomaticWheelModelSize(float radiusPx, float widthPx, float rotationY) {}
}
