package com.sshakusora.riautomobility.model.gecko;

import com.sshakusora.riautomobility.mixin.accessor.GeckoLibCacheAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.loading.FileLoader;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;

import java.util.*;

/**
 * Replaces only GeckoLib entries supplied by the currently mounted RIAuto packs.
 */
public final class CarPackGeckoReloader {
    private static Set<ResourceLocation> modelIds = Set.of();
    private static Set<ResourceLocation> animationIds = Set.of();

    private CarPackGeckoReloader() {
    }

    public static void reload(ResourceManager carPacks, ResourceManager combined) {
        Map<ResourceLocation, BakedGeoModel> models = new HashMap<>(GeckoLibCacheAccessor.riautomobility$getModels());
        modelIds.forEach(models::remove);
        Set<ResourceLocation> nextModels = new HashSet<>();
        for (ResourceLocation id : carPacks.listResources("geo", location -> location.getPath().endsWith(".json")).keySet()) {
            try {
                Model model = FileLoader.loadModelFile(id, combined);
                models.put(id, BakedModelFactory.getForNamespace(id.getNamespace()).constructGeoModel(GeometryTree.fromModel(model)));
                nextModels.add(id);
            } catch (RuntimeException exception) {
                throw new IllegalStateException("Failed to load RIAuto GeckoLib model " + id, exception);
            }
        }

        Map<ResourceLocation, BakedAnimations> animations = new HashMap<>(GeckoLibCacheAccessor.riautomobility$getAnimations());
        animationIds.forEach(animations::remove);
        Set<ResourceLocation> nextAnimations = new HashSet<>();
        for (ResourceLocation id : carPacks.listResources("animations", location -> location.getPath().endsWith(".json")).keySet()) {
            try {
                animations.put(id, FileLoader.loadAnimationsFile(id, combined));
                nextAnimations.add(id);
            } catch (RuntimeException exception) {
                throw new IllegalStateException("Failed to load RIAuto GeckoLib animation " + id, exception);
            }
        }

        GeckoLibCacheAccessor.riautomobility$setModels(Map.copyOf(models));
        GeckoLibCacheAccessor.riautomobility$setAnimations(Map.copyOf(animations));
        modelIds = Set.copyOf(nextModels);
        animationIds = Set.copyOf(nextAnimations);
    }

    public static void refresh(ResourceManager combined, Collection<ResourceLocation> changedModels,
                               Collection<ResourceLocation> changedAnimations) {
        if (!changedModels.isEmpty()) {
            Map<ResourceLocation, BakedGeoModel> models = new HashMap<>(GeckoLibCacheAccessor.riautomobility$getModels());
            Set<ResourceLocation> nextModels = new HashSet<>(modelIds);
            for (ResourceLocation id : changedModels) {
                models.remove(id);
                nextModels.remove(id);
                if (combined.getResource(id).isEmpty()) continue;
                try {
                    Model model = FileLoader.loadModelFile(id, combined);
                    models.put(id, BakedModelFactory.getForNamespace(id.getNamespace()).constructGeoModel(GeometryTree.fromModel(model)));
                    nextModels.add(id);
                } catch (RuntimeException exception) {
                    throw new IllegalStateException("Failed to refresh RIAuto GeckoLib model " + id, exception);
                }
            }
            GeckoLibCacheAccessor.riautomobility$setModels(Map.copyOf(models));
            modelIds = Set.copyOf(nextModels);
        }

        if (!changedAnimations.isEmpty()) {
            Map<ResourceLocation, BakedAnimations> animations = new HashMap<>(GeckoLibCacheAccessor.riautomobility$getAnimations());
            Set<ResourceLocation> nextAnimations = new HashSet<>(animationIds);
            for (ResourceLocation id : changedAnimations) {
                animations.remove(id);
                nextAnimations.remove(id);
                if (combined.getResource(id).isEmpty()) continue;
                try {
                    animations.put(id, FileLoader.loadAnimationsFile(id, combined));
                    nextAnimations.add(id);
                } catch (RuntimeException exception) {
                    throw new IllegalStateException("Failed to refresh RIAuto GeckoLib animation " + id, exception);
                }
            }
            GeckoLibCacheAccessor.riautomobility$setAnimations(Map.copyOf(animations));
            animationIds = Set.copyOf(nextAnimations);
        }
    }

    public static void clear() {
        Map<ResourceLocation, BakedGeoModel> models = new HashMap<>(GeckoLibCacheAccessor.riautomobility$getModels());
        modelIds.forEach(models::remove);
        Map<ResourceLocation, BakedAnimations> animations = new HashMap<>(GeckoLibCacheAccessor.riautomobility$getAnimations());
        animationIds.forEach(animations::remove);
        GeckoLibCacheAccessor.riautomobility$setModels(Map.copyOf(models));
        GeckoLibCacheAccessor.riautomobility$setAnimations(Map.copyOf(animations));
        modelIds = Set.of();
        animationIds = Set.of();
    }
}
