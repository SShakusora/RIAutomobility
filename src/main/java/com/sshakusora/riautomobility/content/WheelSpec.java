package com.sshakusora.riautomobility.content;

import com.google.gson.JsonObject;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public record WheelSpec(
        ResourceLocation id,
        float size,
        float grip,
        float radius,
        float width,
        FrameSpec.ModelSpec model,
        boolean showInCreativeTab
) {
    public AutomobileWheel toWheel() {
        return new AutomobileWheel(
                this.id,
                this.size,
                this.grip,
                new AutomobileWheel.WheelModel(this.radius, this.width, this.model.texture(), this.model.modelId())
        );
    }

    public static WheelSpec fromJson(ResourceLocation id, JsonObject json) {
        return new WheelSpec(
                id,
                GsonHelper.getAsFloat(json, "size"),
                GsonHelper.getAsFloat(json, "grip"),
                GsonHelper.getAsFloat(json, "radius"),
                GsonHelper.getAsFloat(json, "width"),
                FrameSpec.ModelSpec.fromJson(GsonHelper.getAsJsonObject(json, "model")),
                GsonHelper.getAsBoolean(json, "show_in_creative_tab", true)
        );
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("size", this.size);
        json.addProperty("grip", this.grip);
        json.addProperty("radius", this.radius);
        json.addProperty("width", this.width);
        json.add("model", this.model.toJson());
        json.addProperty("show_in_creative_tab", this.showInCreativeTab);
        return json;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.id);
        buf.writeUtf(this.toJson().toString());
    }

    public static WheelSpec read(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        return fromJson(id, GsonHelper.parse(buf.readUtf()));
    }
}
