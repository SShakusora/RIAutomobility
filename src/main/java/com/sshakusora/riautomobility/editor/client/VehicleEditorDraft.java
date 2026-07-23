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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
    public boolean overwrite;
    public boolean showInCreativeTab = true;
    public String modelError = "";

    private final EnumMap<Target, Path> modelFiles = new EnumMap<>(Target.class);
    private final EnumMap<Target, String> displayNames = new EnumMap<>(Target.class);
    private final EnumMap<Target, String> authors = new EnumMap<>(Target.class);
    private final EnumMap<Target, Boolean> previewReady = new EnumMap<>(Target.class);
    private final EnumMap<Target, String> previewKeys = new EnumMap<>(Target.class);
    private final EnumMap<Target, String> componentPaths = new EnumMap<>(Target.class);
    private final EnumSet<Target> visibleParts = EnumSet.noneOf(Target.class);
    private Vec3 automaticCameraOffset;
    private boolean automaticFrameModelSize;
    private boolean automaticWheelModelSize;

    private AutomobileFrame cachedPreviewFrame;
    private AutomobileFrame cachedPreviewSupportFrame;
    private AutomobileFrame cachedFrameSource;
    private boolean cachedFrameReady;
    private float cachedWeight;
    private float cachedLengthPx;
    private float cachedEnginePosBack;
    private float cachedEnginePosUp;
    private float cachedRearAttachmentPos;
    private float cachedFrontAttachmentPos;
    private List<WheelPoint> cachedWheelPoints = List.of();

    private AutomobileWheel cachedPreviewWheel;
    private AutomobileWheel cachedWheelSource;
    private boolean cachedWheelReady;
    private float cachedWheelSize;
    private float cachedWheelGrip;
    private float cachedWheelRadius;
    private float cachedWheelWidth;
    private float cachedWheelRotationY;

    private AutomobileEngine cachedPreviewEngine;
    private AutomobileEngine cachedEngineSource;
    private boolean cachedEngineReady;
    private float cachedEngineTorque;
    private float cachedEngineSpeed;
    private float cachedEngineRotationY;
    private List<EngineSpec.ExhaustSpec> cachedExhausts = List.of();

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
            displayNames.put(value, VehicleImportText.string("default_name." + value.path));
            authors.put(value, "");
            previewReady.put(value, false);
            previewKeys.put(value, UUID.randomUUID().toString().replace("-", ""));
            componentPaths.put(value, generateComponentPath());
        }
        loadFrame(frame);
        loadWheel(wheel);
        loadEngine(engine);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Target", target.name());
        if (previewTarget != null) tag.putString("PreviewTarget", previewTarget.name());
        tag.putBoolean("Overwrite", overwrite);
        tag.putBoolean("ShowInCreativeTab", showInCreativeTab);
        tag.putBoolean("AutomaticFrameModelSize", automaticFrameModelSize);
        tag.putBoolean("AutomaticWheelModelSize", automaticWheelModelSize);
        if (automaticCameraOffset != null) tag.put("AutomaticCameraOffset", saveVec3(automaticCameraOffset));

        CompoundTag parts = new CompoundTag();
        for (Target part : Target.values()) {
            CompoundTag partTag = new CompoundTag();
            Path modelFile = modelFiles.get(part);
            if (modelFile != null && modelFile.getFileName() != null) {
                partTag.putString("ModelFile", modelFile.getFileName().toString());
            }
            partTag.putString("DisplayName", displayNames.getOrDefault(part, ""));
            partTag.putString("Author", authors.getOrDefault(part, ""));
            partTag.putString("PreviewKey", previewKeys.get(part));
            partTag.putString("ComponentPath", componentPaths.get(part));
            partTag.putBoolean("Visible", visibleParts.contains(part));
            parts.put(part.name(), partTag);
        }
        tag.put("Parts", parts);
        tag.putString("SelectedFrame", selectedFrame.getId().toString());
        tag.putString("SelectedWheel", selectedWheel.getId().toString());
        tag.putString("SelectedEngine", selectedEngine.getId().toString());

        tag.putFloat("Weight", weight);
        tag.putFloat("LengthPx", lengthPx);
        tag.putFloat("EnginePosBack", enginePosBack);
        tag.putFloat("EnginePosUp", enginePosUp);
        tag.putFloat("RearAttachmentPos", rearAttachmentPos);
        tag.putFloat("FrontAttachmentPos", frontAttachmentPos);
        tag.putFloat("WidthBlocks", widthBlocks);
        tag.putFloat("HeightBlocks", heightBlocks);
        tag.putBoolean("HideEngine", hideEngine);
        tag.putBoolean("FrontAttachmentEnabled", frontAttachmentEnabled);
        tag.putBoolean("RearAttachmentEnabled", rearAttachmentEnabled);
        tag.putBoolean("FrontAttachmentWhitelistMode", frontAttachmentWhitelistMode);
        tag.putBoolean("RearAttachmentWhitelistMode", rearAttachmentWhitelistMode);
        tag.putString("FrontAttachmentList", frontAttachmentListText);
        tag.putString("RearAttachmentList", rearAttachmentListText);
        tag.put("WheelPoints", saveWheelPoints());
        tag.put("Seats", saveVec3List(seats));
        tag.put("CameraPositions", saveVec3List(cameraPositions));
        tag.put("Hitboxes", saveHitboxes());

        tag.putFloat("WheelSize", wheelSize);
        tag.putFloat("WheelGrip", wheelGrip);
        tag.putFloat("WheelRadius", wheelRadius);
        tag.putFloat("WheelWidth", wheelWidth);
        tag.putFloat("WheelRotationY", wheelRotationY);
        tag.putFloat("EngineRotationY", engineRotationY);
        tag.putFloat("EngineTorque", engineTorque);
        tag.putFloat("EngineSpeed", engineSpeed);
        tag.put("Exhausts", saveExhausts());
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return;
        target = readTarget(tag.getString("Target"), Target.FRAME);
        previewTarget = tag.contains("PreviewTarget", Tag.TAG_STRING)
                ? readTarget(tag.getString("PreviewTarget"), null) : null;
        overwrite = tag.getBoolean("Overwrite");
        showInCreativeTab = !tag.contains("ShowInCreativeTab") || tag.getBoolean("ShowInCreativeTab");
        automaticFrameModelSize = tag.getBoolean("AutomaticFrameModelSize");
        automaticWheelModelSize = tag.getBoolean("AutomaticWheelModelSize");
        automaticCameraOffset = tag.contains("AutomaticCameraOffset", Tag.TAG_COMPOUND)
                ? loadVec3(tag.getCompound("AutomaticCameraOffset")) : null;

        CompoundTag parts = tag.getCompound("Parts");
        visibleParts.clear();
        for (Target part : Target.values()) {
            CompoundTag partTag = parts.getCompound(part.name());
            if (partTag.contains("ModelFile", Tag.TAG_STRING)) {
                try {
                    modelFiles.put(part, Path.of(partTag.getString("ModelFile")));
                } catch (RuntimeException ignored) {
                    modelFiles.remove(part);
                }
            } else {
                modelFiles.remove(part);
            }
            displayNames.put(part, limited(partTag.getString("DisplayName"), 1024));
            authors.put(part, limited(partTag.getString("Author"), 256));
            String previewKey = partTag.getString("PreviewKey");
            if (previewKey.matches("[0-9a-f]{32}")) previewKeys.put(part, previewKey);
            String componentPath = partTag.getString("ComponentPath");
            if (componentPath.matches(GENERATED_COMPONENT_PREFIX + "[0-9a-f]{32}")) {
                componentPaths.put(part, componentPath);
            }
            previewReady.put(part, false);
            if (partTag.getBoolean("Visible")) visibleParts.add(part);
        }

        selectedFrame = AutomobileFrame.REGISTRY.getOrDefault(readId(tag.getString("SelectedFrame"), selectedFrame.getId()));
        selectedWheel = AutomobileWheel.REGISTRY.getOrDefault(readId(tag.getString("SelectedWheel"), selectedWheel.getId()));
        selectedEngine = AutomobileEngine.REGISTRY.getOrDefault(readId(tag.getString("SelectedEngine"), selectedEngine.getId()));

        weight = tag.getFloat("Weight");
        lengthPx = tag.getFloat("LengthPx");
        enginePosBack = tag.getFloat("EnginePosBack");
        enginePosUp = tag.getFloat("EnginePosUp");
        rearAttachmentPos = tag.getFloat("RearAttachmentPos");
        frontAttachmentPos = tag.getFloat("FrontAttachmentPos");
        widthBlocks = tag.getFloat("WidthBlocks");
        heightBlocks = tag.getFloat("HeightBlocks");
        hideEngine = tag.getBoolean("HideEngine");
        frontAttachmentEnabled = tag.getBoolean("FrontAttachmentEnabled");
        rearAttachmentEnabled = tag.getBoolean("RearAttachmentEnabled");
        frontAttachmentWhitelistMode = tag.getBoolean("FrontAttachmentWhitelistMode");
        rearAttachmentWhitelistMode = tag.getBoolean("RearAttachmentWhitelistMode");
        frontAttachmentListText = limited(tag.getString("FrontAttachmentList"), 4096);
        rearAttachmentListText = limited(tag.getString("RearAttachmentList"), 4096);
        loadWheelPoints(tag.getList("WheelPoints", Tag.TAG_COMPOUND));
        loadVec3List(tag.getList("Seats", Tag.TAG_COMPOUND), seats);
        if (seats.isEmpty()) seats.add(defaultSeatPosition());
        loadVec3List(tag.getList("CameraPositions", Tag.TAG_COMPOUND), cameraPositions);
        if (cameraPositions.isEmpty()) cameraPositions.add(Vec3.ZERO);
        loadHitboxes(tag.getList("Hitboxes", Tag.TAG_COMPOUND));

        wheelSize = tag.getFloat("WheelSize");
        wheelGrip = tag.getFloat("WheelGrip");
        wheelRadius = tag.getFloat("WheelRadius");
        wheelWidth = tag.getFloat("WheelWidth");
        wheelRotationY = tag.getFloat("WheelRotationY");
        engineRotationY = tag.getFloat("EngineRotationY");
        engineTorque = tag.getFloat("EngineTorque");
        engineSpeed = tag.getFloat("EngineSpeed");
        loadExhausts(tag.getList("Exhausts", Tag.TAG_COMPOUND));
        modelError = "";
        clearPreviewCaches();
    }

    private ListTag saveWheelPoints() {
        ListTag list = new ListTag();
        wheelPoints.forEach(point -> {
            CompoundTag value = new CompoundTag();
            value.putFloat("Forward", point.forward);
            value.putFloat("Right", point.right);
            value.putFloat("Scale", point.scale);
            value.putFloat("Yaw", point.yaw);
            value.putString("End", point.end);
            value.putString("Side", point.side);
            list.add(value);
        });
        return list;
    }

    private void loadWheelPoints(ListTag list) {
        wheelPoints.clear();
        for (int index = 0; index < Math.min(list.size(), 256); index++) {
            CompoundTag value = list.getCompound(index);
            wheelPoints.add(new WheelPoint(value.getFloat("Forward"), value.getFloat("Right"),
                    value.getFloat("Scale"), value.getFloat("Yaw"),
                    limited(value.getString("End"), 16), limited(value.getString("Side"), 16)));
        }
    }

    private static ListTag saveVec3List(List<Vec3> values) {
        ListTag list = new ListTag();
        values.forEach(value -> list.add(saveVec3(value)));
        return list;
    }

    private static CompoundTag saveVec3(Vec3 value) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("X", value.x);
        tag.putDouble("Y", value.y);
        tag.putDouble("Z", value.z);
        return tag;
    }

    private static Vec3 loadVec3(CompoundTag tag) {
        return new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
    }

    private static void loadVec3List(ListTag list, List<Vec3> target) {
        target.clear();
        for (int index = 0; index < Math.min(list.size(), 256); index++) {
            target.add(loadVec3(list.getCompound(index)));
        }
    }

    private ListTag saveHitboxes() {
        ListTag list = new ListTag();
        hitboxes.forEach(hitbox -> {
            CompoundTag value = saveVec3(hitbox.origin);
            value.putFloat("Width", hitbox.width);
            value.putFloat("Height", hitbox.height);
            value.putBoolean("HasContainer", hitbox.hasContainer);
            list.add(value);
        });
        return list;
    }

    private void loadHitboxes(ListTag list) {
        hitboxes.clear();
        for (int index = 0; index < Math.min(list.size(), 256); index++) {
            CompoundTag value = list.getCompound(index);
            hitboxes.add(new HitboxPoint(loadVec3(value), value.getFloat("Width"),
                    value.getFloat("Height"), value.getBoolean("HasContainer")));
        }
    }

    private ListTag saveExhausts() {
        ListTag list = new ListTag();
        exhausts.forEach(exhaust -> {
            CompoundTag value = new CompoundTag();
            value.putFloat("X", exhaust.x());
            value.putFloat("Y", exhaust.y());
            value.putFloat("Z", exhaust.z());
            value.putFloat("Pitch", exhaust.pitch());
            value.putFloat("Yaw", exhaust.yaw());
            list.add(value);
        });
        return list;
    }

    private void loadExhausts(ListTag list) {
        exhausts.clear();
        for (int index = 0; index < Math.min(list.size(), 256); index++) {
            CompoundTag value = list.getCompound(index);
            exhausts.add(new EngineSpec.ExhaustSpec(value.getFloat("X"), value.getFloat("Y"),
                    value.getFloat("Z"), value.getFloat("Pitch"), value.getFloat("Yaw")));
        }
    }

    private void clearPreviewCaches() {
        cachedPreviewFrame = null;
        cachedPreviewSupportFrame = null;
        cachedPreviewWheel = null;
        cachedPreviewEngine = null;
    }

    private static Target readTarget(String value, Target fallback) {
        try {
            return Target.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static ResourceLocation readId(String value, ResourceLocation fallback) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        return id == null ? fallback : id;
    }

    private static String limited(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public Path modelFile() {
        return modelFiles.get(target);
    }

    public String displayName() {
        return displayNames.get(target);
    }

    public void setDisplayName(String displayName) {
        displayNames.put(target, displayName);
    }

    public String author() {
        return authors.getOrDefault(target, "");
    }

    public Path modelFile(Target part) {
        return modelFiles.get(part);
    }

    public void setModelFile(Target part, Path path) {
        modelFiles.put(part, path);
        authors.put(part, "");
        previewReady.put(part, false);
        if (part == Target.FRAME) automaticFrameModelSize = true;
        if (part == Target.WHEEL) automaticWheelModelSize = true;
    }

    void restoreModelFile(Target part, Path path) {
        if (path == null) modelFiles.remove(part);
        else modelFiles.put(part, path);
        previewReady.put(part, false);
    }

    private void setImportedModelFile(Target part, Path path) {
        modelFiles.put(part, path);
        previewReady.put(part, false);
        if (part == Target.FRAME) automaticFrameModelSize = false;
        if (part == Target.WHEEL) automaticWheelModelSize = false;
    }

    void setImportedAuthor(Target part, String author) {
        authors.put(part, author == null ? "" : author);
    }

    public boolean previewReady(Target part) {
        return previewReady.getOrDefault(part, false);
    }

    public void setPreviewReady(Target part, boolean ready) {
        previewReady.put(part, ready);
    }

    public String previewKey(Target part) {
        return previewKeys.get(part);
    }

    public void showPart(Target part) {
        visibleParts.add(part);
        previewTarget = part;
    }

    public boolean isPartVisible(Target part) {
        return visibleParts.contains(part);
    }

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
        wheelSize = wheel.size();
        wheelGrip = wheel.grip();
        wheelRadius = wheel.model().radius();
        wheelWidth = wheel.model().width();
        wheelRotationY = 0.0F;
        automaticWheelModelSize = false;
    }

    public void loadEngine(AutomobileEngine engine) {
        selectedEngine = engine;
        engineTorque = engine.torque();
        engineSpeed = engine.speed();
        engineRotationY = 0.0F;
        exhausts.clear();
        for (AutomobileEngine.ExhaustPos e : engine.model().exhausts())
            exhausts.add(new EngineSpec.ExhaustSpec(e.x(), e.y(), e.z(), e.pitch(), e.yaw()));
    }

    void importFrame(FrameSpec spec, String displayName, Path modelFile) {
        selectedFrame = spec.toFrame();
        displayNames.put(Target.FRAME, displayName);
        weight = spec.weight();
        lengthPx = spec.lengthPx();
        enginePosBack = spec.enginePosBack();
        enginePosUp = spec.enginePosUp();
        rearAttachmentPos = spec.rearAttachmentPos();
        frontAttachmentPos = spec.frontAttachmentPos();
        widthBlocks = spec.widthBlocks();
        heightBlocks = spec.heightBlocks();
        hideEngine = spec.hideEngine();
        frontAttachmentEnabled = spec.frontAttachmentEnabled();
        rearAttachmentEnabled = spec.rearAttachmentEnabled();
        frontAttachmentWhitelistMode = !spec.frontAttachmentWhitelist().isEmpty()
                || spec.frontAttachmentBlacklist().isEmpty();
        rearAttachmentWhitelistMode = !spec.rearAttachmentWhitelist().isEmpty()
                || spec.rearAttachmentBlacklist().isEmpty();
        frontAttachmentListText = formatResourceLocations(frontAttachmentWhitelistMode
                ? spec.frontAttachmentWhitelist() : spec.frontAttachmentBlacklist());
        rearAttachmentListText = formatResourceLocations(rearAttachmentWhitelistMode
                ? spec.rearAttachmentWhitelist() : spec.rearAttachmentBlacklist());
        wheelPoints.clear();
        for (WheelBase.WheelPos point : spec.wheelBase().toWheelBase().wheels) wheelPoints.add(WheelPoint.from(point));
        double legacySeatYOffset = normalizedSeatYOffset(spec.seatHeight());
        seats.clear();
        spec.seats().forEach(seat -> seats.add(seat.add(0.0D, legacySeatYOffset, 0.0D)));
        if (seats.isEmpty()) seats.add(defaultSeatPosition());
        cameraPositions.clear();
        cameraPositions.addAll(spec.cameraPositions());
        if (cameraPositions.isEmpty()) cameraPositions.add(Vec3.ZERO);
        hitboxes.clear();
        spec.hitboxes().forEach(hitbox -> hitboxes.add(new HitboxPoint(
                hitbox.origin(), hitbox.width(), hitbox.height(), hitbox.hasContainer())));
        showInCreativeTab = spec.showInCreativeTab();
        automaticCameraOffset = null;
        modelError = "";
        setImportedModelFile(Target.FRAME, modelFile);
    }

    void importWheel(WheelSpec spec, String displayName, Path modelFile) {
        selectedWheel = spec.toWheel();
        displayNames.put(Target.WHEEL, displayName);
        wheelSize = spec.size();
        wheelGrip = spec.grip();
        wheelRadius = spec.radius();
        wheelWidth = spec.width();
        wheelRotationY = spec.model().rotationY();
        showInCreativeTab = spec.showInCreativeTab();
        modelError = "";
        setImportedModelFile(Target.WHEEL, modelFile);
    }

    void importEngine(EngineSpec spec, String displayName, Path modelFile) {
        selectedEngine = spec.toEngine();
        displayNames.put(Target.ENGINE, displayName);
        engineTorque = spec.torque();
        engineSpeed = spec.speed();
        engineRotationY = spec.model().rotationY();
        exhausts.clear();
        exhausts.addAll(spec.exhausts());
        showInCreativeTab = spec.showInCreativeTab();
        modelError = "";
        setImportedModelFile(Target.ENGINE, modelFile);
    }

    public ResourceLocation componentId() {
        return componentId(target);
    }

    private ResourceLocation componentId(Target part) {
        return new ResourceLocation(GENERATED_NAMESPACE, componentPath(part));
    }

    public String namespace() {
        return GENERATED_NAMESPACE;
    }

    public String componentPath() {
        return componentPath(target);
    }

    private String componentPath(Target part) {
        return componentPaths.get(part);
    }

    static String generateComponentPath() {
        return GENERATED_COMPONENT_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }

    public String validationError() {
        String displayName = displayName();
        if (displayName.isBlank() || displayName.length() > 80)
            return VehicleImportText.string("validation.display_name_length");
        if (target == Target.FRAME) {
            if (!Float.isFinite(weight) || weight <= 0.0F) return VehicleImportText.string("validation.frame_weight");
            if (!Float.isFinite(lengthPx) || lengthPx <= 0.0F)
                return VehicleImportText.string("validation.item_length");
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
        if (file == null)
            return VehicleImportText.string("validation.choose_model", VehicleImportText.string("page." + target.path));
        if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".bbmodel"))
            return VehicleImportText.string("validation.bbmodel_only");
        if (target == Target.FRAME && wheelPoints.isEmpty()) return VehicleImportText.string("validation.frame_wheels");
        return modelError;
    }

    public FrameSpec.ModelSpec modelSpec(Target part, boolean preview) {
        ResourceLocation id = preview ? new ResourceLocation(PREVIEW_NAMESPACE, previewKey(part)) : componentId(part);
        ResourceLocation modelId = new ResourceLocation(id.getNamespace(), "riautomobility/" + part.path + "/" + id.getPath());
        ResourceLocation texture = new ResourceLocation(id.getNamespace(), "textures/entity/automobile/" + part.path + "/" + id.getPath() + ".png");
        float rotationY = switch (part) {
            case FRAME -> 0.0F;
            case WHEEL -> wheelRotationY;
            case ENGINE -> engineRotationY;
        };
        return new FrameSpec.ModelSpec("bbmodel", texture, modelId, "entity_cutout", rotationY,
                new ResourceLocation(id.getNamespace(), "models/entity/automobile/" + part.path + "/" + id.getPath() + ".bbmodel"), Map.of(), "");
    }

    public FrameSpec.ModelSpec modelSpec(boolean preview) {
        return modelSpec(target, preview);
    }

    public AutomobileFrame previewFrame() {
        refreshFramePreviewCache();
        return cachedPreviewFrame;
    }

    public AutomobileFrame previewSupportFrame() {
        refreshFramePreviewCache();
        return cachedPreviewSupportFrame;
    }

    private AutomobileFrame frameWithModel(ResourceLocation texture, ResourceLocation modelId, WheelBase wheelBase) {
        return new AutomobileFrame(new ResourceLocation(PREVIEW_NAMESPACE, "frame/" + previewKey(Target.FRAME)), weight,
                new AutomobileFrame.FrameModel(texture, modelId, wheelBase, lengthPx, NORMALIZED_SEAT_HEIGHT_PX, enginePosBack, enginePosUp,
                        rearAttachmentPos, frontAttachmentPos));
    }

    public AutomobileWheel previewWheel() {
        boolean ready = previewReady(Target.WHEEL);
        if (cachedPreviewWheel != null
                && cachedWheelSource == selectedWheel
                && cachedWheelReady == ready
                && same(cachedWheelSize, wheelSize)
                && same(cachedWheelGrip, wheelGrip)
                && same(cachedWheelRadius, wheelRadius)
                && same(cachedWheelWidth, wheelWidth)
                && same(cachedWheelRotationY, wheelRotationY)) {
            return cachedPreviewWheel;
        }
        AutomobileWheel.WheelModel base = selectedWheel.model();
        FrameSpec.ModelSpec model = ready ? modelSpec(Target.WHEEL, true) : null;
        cachedPreviewWheel = new AutomobileWheel(new ResourceLocation(PREVIEW_NAMESPACE, "wheel/" + previewKey(Target.WHEEL)), wheelSize, wheelGrip,
                new AutomobileWheel.WheelModel(wheelRadius, wheelWidth, model == null ? base.texture() : model.texture(),
                        model == null ? base.modelId() : model.modelId()));
        cachedWheelSource = selectedWheel;
        cachedWheelReady = ready;
        cachedWheelSize = wheelSize;
        cachedWheelGrip = wheelGrip;
        cachedWheelRadius = wheelRadius;
        cachedWheelWidth = wheelWidth;
        cachedWheelRotationY = wheelRotationY;
        return cachedPreviewWheel;
    }

    public AutomobileEngine previewEngine() {
        boolean ready = previewReady(Target.ENGINE);
        if (cachedPreviewEngine != null
                && cachedEngineSource == selectedEngine
                && cachedEngineReady == ready
                && same(cachedEngineTorque, engineTorque)
                && same(cachedEngineSpeed, engineSpeed)
                && same(cachedEngineRotationY, engineRotationY)
                && cachedExhausts.equals(exhausts)) {
            return cachedPreviewEngine;
        }
        AutomobileEngine.EngineModel base = selectedEngine.model();
        FrameSpec.ModelSpec model = ready ? modelSpec(Target.ENGINE, true) : null;
        cachedPreviewEngine = new AutomobileEngine(new ResourceLocation(PREVIEW_NAMESPACE, "engine/" + previewKey(Target.ENGINE)), engineTorque, engineSpeed,
                selectedEngine.sound(), new AutomobileEngine.EngineModel(model == null ? base.texture() : model.texture(),
                model == null ? base.modelId() : model.modelId(), exhausts.stream().map(EngineSpec.ExhaustSpec::toExhaust).toArray(AutomobileEngine.ExhaustPos[]::new)));
        cachedEngineSource = selectedEngine;
        cachedEngineReady = ready;
        cachedEngineTorque = engineTorque;
        cachedEngineSpeed = engineSpeed;
        cachedEngineRotationY = engineRotationY;
        cachedExhausts = List.copyOf(exhausts);
        return cachedPreviewEngine;
    }

    private void refreshFramePreviewCache() {
        boolean ready = previewReady(Target.FRAME);
        if (cachedPreviewFrame != null
                && cachedFrameSource == selectedFrame
                && cachedFrameReady == ready
                && same(cachedWeight, weight)
                && same(cachedLengthPx, lengthPx)
                && same(cachedEnginePosBack, enginePosBack)
                && same(cachedEnginePosUp, enginePosUp)
                && same(cachedRearAttachmentPos, rearAttachmentPos)
                && same(cachedFrontAttachmentPos, frontAttachmentPos)
                && cachedWheelPoints.equals(wheelPoints)) {
            return;
        }
        AutomobileFrame.FrameModel base = selectedFrame.model();
        FrameSpec.ModelSpec model = ready ? modelSpec(Target.FRAME, true) : null;
        WheelBase wheelBase = wheelBase();
        cachedPreviewFrame = frameWithModel(
                model == null ? base.texture() : model.texture(),
                model == null ? base.modelId() : model.modelId(),
                wheelBase);
        cachedPreviewSupportFrame = frameWithModel(
                AutomobileFrame.EMPTY.model().texture(), AutomobileFrame.EMPTY.model().modelId(), wheelBase);
        cachedFrameSource = selectedFrame;
        cachedFrameReady = ready;
        cachedWeight = weight;
        cachedLengthPx = lengthPx;
        cachedEnginePosBack = enginePosBack;
        cachedEnginePosUp = enginePosUp;
        cachedRearAttachmentPos = rearAttachmentPos;
        cachedFrontAttachmentPos = frontAttachmentPos;
        cachedWheelPoints = List.copyOf(wheelPoints);
    }

    private static boolean same(float first, float second) {
        return Float.floatToIntBits(first) == Float.floatToIntBits(second);
    }

    public FrameSpec frameSpec(boolean preview) {
        syncAutomaticCameraPositions();
        ResourceLocation id = preview ? new ResourceLocation(PREVIEW_NAMESPACE, previewKey(Target.FRAME)) : componentId(Target.FRAME);
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
        ResourceLocation id = preview ? new ResourceLocation(PREVIEW_NAMESPACE, previewKey(Target.WHEEL)) : componentId(Target.WHEEL);
        return new WheelSpec(id, wheelSize, wheelGrip, wheelRadius, wheelWidth, modelSpec(Target.WHEEL, preview), showInCreativeTab);
    }

    public EngineSpec engineSpec(boolean preview) {
        ResourceLocation id = preview ? new ResourceLocation(PREVIEW_NAMESPACE, previewKey(Target.ENGINE)) : componentId(Target.ENGINE);
        return new EngineSpec(id, engineTorque, engineSpeed, modelSpec(Target.ENGINE, preview), List.copyOf(exhausts), showInCreativeTab);
    }

    public WheelBase wheelBase() {
        return new WheelBase(wheelPoints.stream().map(WheelPoint::toWheelPos).toArray(WheelBase.WheelPos[]::new));
    }

    static Vec3 defaultSeatPosition() {
        return Vec3.ZERO;
    }

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
        if (!automaticFrameModelSize) return;
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

    boolean usesAutomaticFrameModelSize() {
        return automaticFrameModelSize;
    }

    boolean usesAutomaticWheelModelSize() {
        return automaticWheelModelSize;
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

    public String packName() {
        return GENERATED_NAMESPACE + "-" + componentPath() + "-" + target.path;
    }

    public enum Target {
        FRAME("frame"), WHEEL("wheel"), ENGINE("engine");
        public final String path;

        Target(String path) {
            this.path = path;
        }
    }

    public record WheelPoint(float forward, float right, float scale, float yaw, String end, String side) {
        static WheelPoint from(WheelBase.WheelPos p) {
            return new WheelPoint(p.forward(), p.right(), p.scale(), p.yaw(), p.end().name().toLowerCase(), p.side().name().toLowerCase());
        }

        WheelPoint mirrored() {
            float mirroredYaw = (yaw + 180.0F) % 360.0F;
            if (mirroredYaw < 0.0F) mirroredYaw += 360.0F;
            return new WheelPoint(forward, -right, scale, mirroredYaw, end, side.equals("left") ? "right" : "left");
        }

        WheelBase.WheelPos toWheelPos() {
            return toSpec().toWheelPos();
        }

        FrameSpec.WheelPosSpec toSpec() {
            return new FrameSpec.WheelPosSpec(forward, right, scale, yaw, end, side);
        }
    }

    public record HitboxPoint(Vec3 origin, float width, float height, boolean hasContainer) {
        FrameSpec.HitboxSpec toSpec() {
            return new FrameSpec.HitboxSpec(origin, width, height, hasContainer);
        }
    }

    record AutomaticWheelModelSize(float radiusPx, float widthPx, float rotationY) {
    }
}
