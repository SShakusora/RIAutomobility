package com.sshakusora.riautomobility.mixin.accessor;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.loading.object.BakedAnimations;

import java.util.Map;

@Mixin(value = GeckoLibCache.class, remap = false)
public interface GeckoLibCacheAccessor {
    @Accessor("MODELS")
    static Map<ResourceLocation, BakedGeoModel> riautomobility$getModels() {
        throw new AssertionError();
    }

    @Accessor("MODELS")
    static void riautomobility$setModels(Map<ResourceLocation, BakedGeoModel> models) {
        throw new AssertionError();
    }

    @Accessor("ANIMATIONS")
    static Map<ResourceLocation, BakedAnimations> riautomobility$getAnimations() {
        throw new AssertionError();
    }

    @Accessor("ANIMATIONS")
    static void riautomobility$setAnimations(Map<ResourceLocation, BakedAnimations> animations) {
        throw new AssertionError();
    }
}
