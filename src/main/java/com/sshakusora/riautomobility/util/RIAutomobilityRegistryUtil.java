package com.sshakusora.riautomobility.util;

import com.sshakusora.riautomobility.mixin.accessor.SimpleMapContentRegistryAccessor;
import io.github.foundationgames.automobility.util.SimpleMapContentRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class RIAutomobilityRegistryUtil {
    private RIAutomobilityRegistryUtil() {
    }

    public static <V extends SimpleMapContentRegistry.Identifiable> V registerOrReplace(SimpleMapContentRegistry<V> registry, V entry) {
        SimpleMapContentRegistryAccessor<V> accessor = (SimpleMapContentRegistryAccessor<V>) registry;
        Map<ResourceLocation, V> entries = accessor.riautomobility$getEntries();
        List<ResourceLocation> orderedKeys = accessor.riautomobility$getOrderedKeys();
        ResourceLocation id = entry.getId();

        if (!entries.containsKey(id)) {
            orderedKeys.add(id);
        }
        entries.put(id, entry);
        return entry;
    }

    public static <V extends SimpleMapContentRegistry.Identifiable> void removeNamespace(SimpleMapContentRegistry<V> registry, String namespace) {
        SimpleMapContentRegistryAccessor<V> accessor = (SimpleMapContentRegistryAccessor<V>) registry;
        Map<ResourceLocation, V> entries = accessor.riautomobility$getEntries();
        List<ResourceLocation> orderedKeys = accessor.riautomobility$getOrderedKeys();

        Iterator<ResourceLocation> iterator = orderedKeys.iterator();
        while (iterator.hasNext()) {
            ResourceLocation id = iterator.next();
            if (namespace.equals(id.getNamespace())) {
                iterator.remove();
                entries.remove(id);
            }
        }
    }

    public static <V extends SimpleMapContentRegistry.Identifiable> void remove(SimpleMapContentRegistry<V> registry, ResourceLocation id) {
        SimpleMapContentRegistryAccessor<V> accessor = (SimpleMapContentRegistryAccessor<V>) registry;
        accessor.riautomobility$getEntries().remove(id);
        accessor.riautomobility$getOrderedKeys().remove(id);
    }
}
