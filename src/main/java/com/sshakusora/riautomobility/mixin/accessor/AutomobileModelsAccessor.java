package com.sshakusora.riautomobility.mixin.accessor;

import io.github.foundationgames.automobility.automobile.render.AutomobileModels;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.function.Function;

@Mixin(value = AutomobileModels.class, remap = false)
public interface AutomobileModelsAccessor {
    @Accessor("modelProviders")
    static Map<ResourceLocation, Function<EntityRendererProvider.Context, Model>> riautomobility$getModelProviders() {
        throw new AssertionError();
    }

    @Accessor("models")
    static Map<ResourceLocation, Model> riautomobility$getModels() {
        throw new AssertionError();
    }
}
