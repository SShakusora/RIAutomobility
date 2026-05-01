package com.sshakusora.riautomobility.mixin.accessor;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(EntityModelSet.class)
public interface EntityModelSetAccessor {
    @Accessor("roots")
    Map<ModelLayerLocation, LayerDefinition> riautomobility$getRoots();

    @Accessor("roots")
    void riautomobility$setRoots(Map<ModelLayerLocation, LayerDefinition> roots);
}
