package com.sshakusora.riautomobility.wheel;

import com.sshakusora.riautomobility.RIAutomobility;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class RIAutomobileWheel {
    private RIAutomobileWheel() {}

    public static final AutomobileWheel DMC12 = RIAutomobileWheel.register("dmc12", wheel -> wheel
            .size(1.1F)
            .grip(0.8F)
            .wheelModel(
                    8.12F,
                    8.05F,
                    RIAutomobility.rl("textures/entity/automobile/wheel/dmc12.png"),
                    RIAutomobility.rl("wheel_dmc12")
            )
    );

    public static final AutomobileWheel STANDARD_FORMULA = RIAutomobileWheel.register("standard_formula", wheel -> wheel
            .size(1.1F)
            .grip(0.8F)
            .wheelModel(
                    10.9F,
                    12.2F,
                    RIAutomobility.rl("textures/entity/automobile/wheel/standard_formula.png"),
                    RIAutomobility.rl("wheel_standard_formula")
            )
    );

    public static Builder builder(String path) {
        return builder(RIAutomobility.rl(path));
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static AutomobileWheel register(String path, Consumer<Builder> spec) {
        return register(RIAutomobility.rl(path), spec);
    }

    public static AutomobileWheel register(ResourceLocation id, Consumer<Builder> spec) {
        Builder builder = builder(id);
        spec.accept(builder);
        return register(builder.build());
    }

    public static AutomobileWheel register(AutomobileWheel wheel) {
        return AutomobileWheel.REGISTRY.register(wheel);
    }

    public static void init() {}

    public static final class Builder {
        private final ResourceLocation id;
        private float size = 0.6F;
        private float grip = 0.5F;
        private float radius = 3.0F;
        private float width = 3.0F;
        private ResourceLocation texture;
        private ResourceLocation modelId;
        private final List<AutomobileWheel.Ability> abilities = new ArrayList<>();

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder size(float size) {
            this.size = size;
            return this;
        }

        public Builder grip(float grip) {
            this.grip = grip;
            return this;
        }

        public Builder radius(float radius) {
            this.radius = radius;
            return this;
        }

        public Builder width(float width) {
            this.width = width;
            return this;
        }

        public Builder texture(ResourceLocation texture) {
            this.texture = texture;
            return this;
        }

        public Builder texture(String path) {
            return this.texture(RIAutomobility.rl(path));
        }

        public Builder modelId(ResourceLocation modelId) {
            this.modelId = modelId;
            return this;
        }

        public Builder modelId(String path) {
            return this.modelId(RIAutomobility.rl(path));
        }

        public Builder wheelModel(float radius, float width, ResourceLocation texture, ResourceLocation modelId) {
            this.radius = radius;
            this.width = width;
            this.texture = texture;
            this.modelId = modelId;
            return this;
        }

        public Builder abilities(AutomobileWheel.Ability... abilities) {
            this.abilities.clear();
            this.abilities.addAll(List.of(abilities));
            return this;
        }

        public Builder addAbility(AutomobileWheel.Ability ability) {
            this.abilities.add(ability);
            return this;
        }

        public AutomobileWheel build() {
            ResourceLocation resolvedTexture = this.texture != null
                    ? this.texture
                    : RIAutomobility.rl("textures/entity/automobile/wheel/" + this.id.getPath() + ".png");
            ResourceLocation resolvedModelId = this.modelId != null
                    ? this.modelId
                    : RIAutomobility.rl("wheel_" + this.id.getPath());

            return new AutomobileWheel(
                    this.id,
                    this.size,
                    this.grip,
                    new AutomobileWheel.WheelModel(this.radius, this.width, resolvedTexture, resolvedModelId),
                    this.abilities.toArray(AutomobileWheel.Ability[]::new)
            );
        }
    }
}
