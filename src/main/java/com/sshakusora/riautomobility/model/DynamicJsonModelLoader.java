package com.sshakusora.riautomobility.model;

import io.github.foundationgames.automobility.forge.vendored.jsonem.util.JsonEntityModelUtil;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class DynamicJsonModelLoader {
    private static final Set<ModelLayerLocation> REGISTERED_LAYERS = new LinkedHashSet<>();

    private DynamicJsonModelLoader() {}

    public static void register(ModelLayerLocation layer) {
        REGISTERED_LAYERS.add(layer);
    }

    public static void unregister(ModelLayerLocation layer) {
        REGISTERED_LAYERS.remove(layer);
    }

    public static LayerDefinition loadRequiredModel(ResourceManager manager, ModelLayerLocation layer) {
        ResourceLocation modelLocation = getModelLocation(layer);
        var resource = manager.getResource(modelLocation)
                .orElseThrow(() -> new IllegalStateException("Missing entity model " + modelLocation));
        try (var in = resource.open()) {
            return JsonEntityModelUtil.readJson(in)
                    .orElseThrow(() -> new IllegalStateException("Invalid entity model " + modelLocation));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load entity model " + modelLocation, exception);
        }
    }

    public static void loadModels(ResourceManager manager, Map<ModelLayerLocation, LayerDefinition> roots) {
        for (ModelLayerLocation layer : REGISTERED_LAYERS) {
            ResourceLocation modelLocation = getModelLocation(layer);

            manager.getResource(modelLocation).ifPresent(resource -> {
                try (var in = resource.open()) {
                    JsonEntityModelUtil.readJson(in).ifPresent(model -> roots.put(layer, model));
                } catch (IOException exception) {
                    throw new IllegalStateException("Failed to load dynamic automobile model " + modelLocation, exception);
                }
            });
        }
    }

    public static void loadIntoEntityModelSet(EntityModelSet entityModels, ResourceManager manager) {
        Map<ModelLayerLocation, LayerDefinition> roots = new HashMap<>(entityModels.roots);
        loadModels(manager, roots);
        entityModels.roots = roots;
    }

    private static ResourceLocation getModelLocation(ModelLayerLocation layer) {
        return new ResourceLocation(
                layer.getModel().getNamespace(),
                "models/entity/" + layer.getModel().getPath() + "/" + layer.getLayer() + ".json"
        );
    }
}
