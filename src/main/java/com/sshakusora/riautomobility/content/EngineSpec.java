package com.sshakusora.riautomobility.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public record EngineSpec(
        ResourceLocation id,
        float torque,
        float speed,
        FrameSpec.ModelSpec model,
        List<ExhaustSpec> exhausts,
        boolean showInCreativeTab
) {
    public EngineSpec {
        exhausts = List.copyOf(exhausts);
    }

    public AutomobileEngine toEngine() {
        return new AutomobileEngine(
                this.id, this.torque, this.speed, () -> SoundEvents.MINECART_INSIDE,
                new AutomobileEngine.EngineModel(this.model.texture(), this.model.modelId(),
                        this.exhausts.stream().map(ExhaustSpec::toExhaust).toArray(AutomobileEngine.ExhaustPos[]::new))
        );
    }

    public static EngineSpec fromJson(ResourceLocation id, JsonObject json) {
        List<ExhaustSpec> exhausts = new ArrayList<>();
        JsonArray array = GsonHelper.getAsJsonArray(json, "exhausts", new JsonArray());
        for (var element : array) exhausts.add(ExhaustSpec.fromJson(element.getAsJsonObject()));
        return new EngineSpec(
                id,
                GsonHelper.getAsFloat(json, "torque"),
                GsonHelper.getAsFloat(json, "speed"),
                FrameSpec.ModelSpec.fromComponentJson(json.get("model"), id, "engine"),
                exhausts,
                GsonHelper.getAsBoolean(json, "show_in_creative_tab", true)
        );
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("torque", this.torque);
        json.addProperty("speed", this.speed);
        json.add("model", this.model.toJson());
        JsonArray exhaustArray = new JsonArray();
        this.exhausts.forEach(exhaust -> exhaustArray.add(exhaust.toJson()));
        json.add("exhausts", exhaustArray);
        json.addProperty("show_in_creative_tab", this.showInCreativeTab);
        return json;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.id);
        buf.writeUtf(this.toJson().toString());
    }

    public static EngineSpec read(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        return fromJson(id, GsonHelper.parse(buf.readUtf()));
    }

    public record ExhaustSpec(float x, float y, float z, float pitch, float yaw) {
        public AutomobileEngine.ExhaustPos toExhaust() {
            return new AutomobileEngine.ExhaustPos(x, y, z, pitch, yaw);
        }

        static ExhaustSpec fromJson(JsonObject json) {
            return new ExhaustSpec(
                    GsonHelper.getAsFloat(json, "x"), GsonHelper.getAsFloat(json, "y"),
                    GsonHelper.getAsFloat(json, "z"), GsonHelper.getAsFloat(json, "pitch", 0),
                    GsonHelper.getAsFloat(json, "yaw", 0));
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("x", x);
            json.addProperty("y", y);
            json.addProperty("z", z);
            json.addProperty("pitch", pitch);
            json.addProperty("yaw", yaw);
            return json;
        }
    }
}
