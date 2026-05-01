package com.sshakusora.riautomobility.mixin.accessor;

import io.github.foundationgames.automobility.util.SimpleMapContentRegistry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(value = SimpleMapContentRegistry.class, remap = false)
public interface SimpleMapContentRegistryAccessor<V extends SimpleMapContentRegistry.Identifiable> {
    @Accessor("entries")
    Map<ResourceLocation, V> riautomobility$getEntries();

    @Accessor("orderedKeys")
    List<ResourceLocation> riautomobility$getOrderedKeys();
}
