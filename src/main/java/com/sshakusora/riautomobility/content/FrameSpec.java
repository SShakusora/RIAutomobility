package com.sshakusora.riautomobility.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sshakusora.riautomobility.definition.RIAutomobileDefinition;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.WheelBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public record FrameSpec(
        ResourceLocation id,
        float weight,
        ModelSpec model,
        WheelBaseSpec wheelBase,
        float lengthPx,
        float seatHeight,
        float enginePosBack,
        float enginePosUp,
        boolean hideEngine,
        float rearAttachmentPos,
        float frontAttachmentPos,
        float widthBlocks,
        float heightBlocks,
        List<Vec3> seats,
        List<Vec3> cameraPositions,
        List<HitboxSpec> hitboxes,
        boolean frontAttachmentEnabled,
        boolean rearAttachmentEnabled,
        List<ResourceLocation> frontAttachmentWhitelist,
        List<ResourceLocation> frontAttachmentBlacklist,
        List<ResourceLocation> rearAttachmentWhitelist,
        List<ResourceLocation> rearAttachmentBlacklist,
        boolean showInCreativeTab
) {
    public AutomobileFrame toFrame() {
        return new AutomobileFrame(
                this.id,
                this.weight,
                new AutomobileFrame.FrameModel(
                        this.model.texture(),
                        this.model.modelId(),
                        this.wheelBase.toWheelBase(),
                        this.lengthPx,
                        this.seatHeight,
                        this.enginePosBack,
                        this.enginePosUp,
                        this.rearAttachmentPos,
                        this.frontAttachmentPos
                )
        );
    }

    public RIAutomobileDefinition toDefinition() {
        return RIAutomobileDefinition.builder()
                .dimensions(EntityDimensions.scalable(this.widthBlocks, this.heightBlocks))
                .seats(this.seats.stream().map(pos -> new RIAutomobileDefinition.SeatPos(pos.x, pos.y, pos.z)).toList())
                .cameraPositions(this.cameraPositions)
                .hitboxes(this.hitboxes.stream().map(HitboxSpec::toHitbox).toList())
                .hideEngine(this.hideEngine)
                .frontAttachmentEnabled(this.frontAttachmentEnabled)
                .rearAttachmentEnabled(this.rearAttachmentEnabled)
                .frontAttachmentWhitelist(this.frontAttachmentWhitelist)
                .frontAttachmentBlacklist(this.frontAttachmentBlacklist)
                .rearAttachmentWhitelist(this.rearAttachmentWhitelist)
                .rearAttachmentBlacklist(this.rearAttachmentBlacklist)
                .build();
    }

    public static FrameSpec fromJson(ResourceLocation id, JsonObject json) {
        JsonObject modelObject = GsonHelper.getAsJsonObject(json, "model");
        JsonObject dimensionsObject = GsonHelper.getAsJsonObject(json, "dimensions");

        return new FrameSpec(
                id,
                GsonHelper.getAsFloat(json, "weight"),
                ModelSpec.fromJson(modelObject),
                WheelBaseSpec.fromJson(GsonHelper.getAsJsonObject(json, "wheel_base")),
                GsonHelper.getAsFloat(json, "length_px"),
                GsonHelper.getAsFloat(json, "seat_height"),
                GsonHelper.getAsFloat(json, "engine_pos_back"),
                GsonHelper.getAsFloat(json, "engine_pos_up"),
                GsonHelper.getAsBoolean(json, "hide_engine", false),
                GsonHelper.getAsFloat(json, "rear_attachment_pos"),
                GsonHelper.getAsFloat(json, "front_attachment_pos"),
                GsonHelper.getAsFloat(dimensionsObject, "width"),
                GsonHelper.getAsFloat(dimensionsObject, "height"),
                readVec3List(json, "seats"),
                readVec3List(json, "camera_positions"),
                readHitboxes(json),
                GsonHelper.getAsBoolean(json, "front_attachment_enabled", true),
                GsonHelper.getAsBoolean(json, "rear_attachment_enabled", true),
                readIdList(json, "front_attachment_whitelist"),
                readIdList(json, "front_attachment_blacklist"),
                readIdList(json, "rear_attachment_whitelist"),
                readIdList(json, "rear_attachment_blacklist"),
                GsonHelper.getAsBoolean(json, "show_in_creative_tab", true)
        );
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("weight", this.weight);
        json.add("model", this.model.toJson());
        json.add("wheel_base", this.wheelBase.toJson());
        json.addProperty("length_px", this.lengthPx);
        json.addProperty("seat_height", this.seatHeight);
        json.addProperty("engine_pos_back", this.enginePosBack);
        json.addProperty("engine_pos_up", this.enginePosUp);
        json.addProperty("hide_engine", this.hideEngine);
        json.addProperty("rear_attachment_pos", this.rearAttachmentPos);
        json.addProperty("front_attachment_pos", this.frontAttachmentPos);

        JsonObject dimensions = new JsonObject();
        dimensions.addProperty("width", this.widthBlocks);
        dimensions.addProperty("height", this.heightBlocks);
        json.add("dimensions", dimensions);

        json.add("seats", writeVec3List(this.seats));
        json.add("camera_positions", writeVec3List(this.cameraPositions));

        JsonArray hitboxArray = new JsonArray();
        for (HitboxSpec hitbox : this.hitboxes) {
            hitboxArray.add(hitbox.toJson());
        }
        json.add("hitboxes", hitboxArray);
        json.addProperty("front_attachment_enabled", this.frontAttachmentEnabled);
        json.addProperty("rear_attachment_enabled", this.rearAttachmentEnabled);
        json.add("front_attachment_whitelist", writeIdList(this.frontAttachmentWhitelist));
        json.add("front_attachment_blacklist", writeIdList(this.frontAttachmentBlacklist));
        json.add("rear_attachment_whitelist", writeIdList(this.rearAttachmentWhitelist));
        json.add("rear_attachment_blacklist", writeIdList(this.rearAttachmentBlacklist));
        json.addProperty("show_in_creative_tab", this.showInCreativeTab);
        return json;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.id);
        buf.writeUtf(this.toJson().toString());
    }

    public static FrameSpec read(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        return fromJson(id, GsonHelper.parse(buf.readUtf()));
    }

    private static List<Vec3> readVec3List(JsonObject json, String key) {
        List<Vec3> values = new ArrayList<>();
        if (!json.has(key)) {
            return values;
        }
        for (JsonElement element : GsonHelper.getAsJsonArray(json, key)) {
            JsonObject obj = element.getAsJsonObject();
            values.add(new Vec3(
                    GsonHelper.getAsDouble(obj, "x"),
                    GsonHelper.getAsDouble(obj, "y", 0.0D),
                    GsonHelper.getAsDouble(obj, "z")
            ));
        }
        return values;
    }

    private static JsonArray writeVec3List(List<Vec3> values) {
        JsonArray array = new JsonArray();
        for (Vec3 value : values) {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", value.x);
            obj.addProperty("y", value.y);
            obj.addProperty("z", value.z);
            array.add(obj);
        }
        return array;
    }

    private static List<HitboxSpec> readHitboxes(JsonObject json) {
        List<HitboxSpec> values = new ArrayList<>();
        if (!json.has("hitboxes")) {
            return values;
        }
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "hitboxes")) {
            values.add(HitboxSpec.fromJson(element.getAsJsonObject()));
        }
        return values;
    }

    private static List<ResourceLocation> readIdList(JsonObject json, String key) {
        List<ResourceLocation> values = new ArrayList<>();
        if (!json.has(key)) {
            return values;
        }
        for (JsonElement element : GsonHelper.getAsJsonArray(json, key)) {
            values.add(parseId(element.getAsString()));
        }
        return values;
    }

    private static JsonArray writeIdList(List<ResourceLocation> ids) {
        JsonArray array = new JsonArray();
        for (ResourceLocation id : ids) {
            array.add(id.toString());
        }
        return array;
    }

    public record ModelSpec(
            String type,
            ResourceLocation texture,
            ResourceLocation modelId,
            ResourceLocation layerLocation,
            String renderType,
            float rotationY,
            ResourceLocation geoModel,
            ResourceLocation animation
    ) {
        public static ModelSpec fromJson(JsonObject json) {
            String type = GsonHelper.getAsString(json, "type", "jsonem");
            return new ModelSpec(
                    type,
                    parseId(GsonHelper.getAsString(json, "texture")),
                    parseId(GsonHelper.getAsString(json, "model_id")),
                    json.has("layer_location") ? parseId(GsonHelper.getAsString(json, "layer_location")) : null,
                    GsonHelper.getAsString(json, "render_type", "entity_cutout"),
                    GsonHelper.getAsFloat(json, "rotation_y", 0.0F),
                    json.has("geo_model") ? parseId(GsonHelper.getAsString(json, "geo_model")) : null,
                    json.has("animation") ? parseId(GsonHelper.getAsString(json, "animation")) : null
            );
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", this.type);
            json.addProperty("texture", this.texture.toString());
            json.addProperty("model_id", this.modelId.toString());
            if (this.layerLocation != null) {
                json.addProperty("layer_location", this.layerLocation.toString());
            }
            json.addProperty("render_type", this.renderType);
            json.addProperty("rotation_y", this.rotationY);
            if (this.geoModel != null) {
                json.addProperty("geo_model", this.geoModel.toString());
            }
            if (this.animation != null) {
                json.addProperty("animation", this.animation.toString());
            }
            return json;
        }

        public boolean isGeckoLib() {
            return "geckolib".equalsIgnoreCase(this.type);
        }
    }

    public record WheelBaseSpec(Float forwardSeparation, Float sideSeparation, List<WheelPosSpec> wheels) {
        public WheelBase toWheelBase() {
            if (this.forwardSeparation != null && this.sideSeparation != null) {
                return WheelBase.basic(this.forwardSeparation, this.sideSeparation);
            }
            return new WheelBase(this.wheels.stream().map(WheelPosSpec::toWheelPos).toArray(WheelBase.WheelPos[]::new));
        }

        public static WheelBaseSpec fromJson(JsonObject json) {
            if (json.has("forward_separation") && json.has("side_separation")) {
                return new WheelBaseSpec(
                        GsonHelper.getAsFloat(json, "forward_separation"),
                        GsonHelper.getAsFloat(json, "side_separation"),
                        List.of()
                );
            }

            List<WheelPosSpec> wheels = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "wheels")) {
                wheels.add(WheelPosSpec.fromJson(element.getAsJsonObject()));
            }
            return new WheelBaseSpec(null, null, wheels);
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            if (this.forwardSeparation != null && this.sideSeparation != null) {
                json.addProperty("forward_separation", this.forwardSeparation);
                json.addProperty("side_separation", this.sideSeparation);
                return json;
            }

            JsonArray wheelsJson = new JsonArray();
            for (WheelPosSpec wheel : this.wheels) {
                wheelsJson.add(wheel.toJson());
            }
            json.add("wheels", wheelsJson);
            return json;
        }
    }

    public record WheelPosSpec(float forward, float right, float scale, float yaw, String end, String side) {
        public WheelBase.WheelPos toWheelPos() {
            return new WheelBase.WheelPos(
                    this.forward,
                    this.right,
                    this.scale,
                    this.yaw,
                    WheelBase.WheelEnd.valueOf(this.end.toUpperCase()),
                    WheelBase.WheelSide.valueOf(this.side.toUpperCase())
            );
        }

        public static WheelPosSpec fromJson(JsonObject json) {
            return new WheelPosSpec(
                    GsonHelper.getAsFloat(json, "forward"),
                    GsonHelper.getAsFloat(json, "right"),
                    GsonHelper.getAsFloat(json, "scale", 1.0F),
                    GsonHelper.getAsFloat(json, "yaw", 0.0F),
                    GsonHelper.getAsString(json, "end"),
                    GsonHelper.getAsString(json, "side")
            );
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("forward", this.forward);
            json.addProperty("right", this.right);
            json.addProperty("scale", this.scale);
            json.addProperty("yaw", this.yaw);
            json.addProperty("end", this.end);
            json.addProperty("side", this.side);
            return json;
        }
    }

    public record HitboxSpec(Vec3 origin, float width, float height, boolean hasContainer) {
        public RIAutomobileDefinition.Hitbox toHitbox() {
            return new RIAutomobileDefinition.Hitbox(this.origin, this.width, this.height, this.hasContainer);
        }

        public static HitboxSpec fromJson(JsonObject json) {
            return new HitboxSpec(
                    new Vec3(
                            GsonHelper.getAsDouble(json, "x"),
                            GsonHelper.getAsDouble(json, "y", 0.0D),
                            GsonHelper.getAsDouble(json, "z")
                    ),
                    GsonHelper.getAsFloat(json, "width"),
                    GsonHelper.getAsFloat(json, "height"),
                    GsonHelper.getAsBoolean(json, "container", false)
            );
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("x", this.origin.x);
            json.addProperty("y", this.origin.y);
            json.addProperty("z", this.origin.z);
            json.addProperty("width", this.width);
            json.addProperty("height", this.height);
            json.addProperty("container", this.hasContainer);
            return json;
        }
    }

    private static ResourceLocation parseId(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid resource location: " + id);
        }
        return parsed;
    }
}
