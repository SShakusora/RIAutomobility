package com.sshakusora.riautomobility.model;

import io.github.foundationgames.automobility.forge.vendored.jsonem.util.JsonEntityModelUtil;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;

final class BuiltinJsonModelLoader {
    private BuiltinJsonModelLoader() {
    }

    static LayerDefinition loadRequiredModel(ResourceManager manager, ModelLayerLocation layer) {
        ResourceLocation modelLocation = new ResourceLocation(
                layer.getModel().getNamespace(),
                "models/entity/" + layer.getModel().getPath() + "/" + layer.getLayer() + ".json"
        );
        var resource = manager.getResource(modelLocation)
                .orElseThrow(() -> new IllegalStateException("Missing entity model " + modelLocation));
        try (var in = resource.open()) {
            return JsonEntityModelUtil.readJson(in)
                    .orElseThrow(() -> new IllegalStateException("Invalid entity model " + modelLocation));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load entity model " + modelLocation, exception);
        }
    }
}
