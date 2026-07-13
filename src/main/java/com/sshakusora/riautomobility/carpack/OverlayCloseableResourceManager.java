package com.sshakusora.riautomobility.carpack;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.Resource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A live resource view which gives RIAuto packs priority over the resource
 * manager that Minecraft has already loaded. Replacing the delegate with this
 * view does not run Minecraft's global reload listeners.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class OverlayCloseableResourceManager implements CloseableResourceManager {
    private final CloseableResourceManager base;
    private final CloseableResourceManager overlay;
    private boolean ownsBase = true;
    private boolean closed;

    public OverlayCloseableResourceManager(CloseableResourceManager base, CloseableResourceManager overlay) {
        this.base = base;
        this.overlay = overlay;
    }

    public CloseableResourceManager detachBase() {
        this.ownsBase = false;
        return this.base;
    }

    @Override
    public Set<String> getNamespaces() {
        var namespaces = new HashSet<>(this.base.getNamespaces());
        namespaces.addAll(this.overlay.getNamespaces());
        return Set.copyOf(namespaces);
    }

    @Override
    public Optional<Resource> getResource(ResourceLocation location) {
        Optional<Resource> resource = this.overlay.getResource(location);
        return resource.isPresent() ? resource : this.base.getResource(location);
    }

    @Override
    public List<Resource> getResourceStack(ResourceLocation location) {
        List<Resource> resources = new ArrayList<>(this.base.getResourceStack(location));
        resources.addAll(this.overlay.getResourceStack(location));
        return List.copyOf(resources);
    }

    @Override
    public Map<ResourceLocation, Resource> listResources(String path, Predicate<ResourceLocation> filter) {
        Map<ResourceLocation, Resource> resources = new LinkedHashMap<>(this.base.listResources(path, filter));
        resources.putAll(this.overlay.listResources(path, filter));
        return Map.copyOf(resources);
    }

    @Override
    public Map<ResourceLocation, List<Resource>> listResourceStacks(String path, Predicate<ResourceLocation> filter) {
        Map<ResourceLocation, List<Resource>> resources = new LinkedHashMap<>();
        this.base.listResourceStacks(path, filter).forEach((id, stack) -> resources.put(id, new ArrayList<>(stack)));
        this.overlay.listResourceStacks(path, filter).forEach((id, stack) ->
                resources.computeIfAbsent(id, ignored -> new ArrayList<>()).addAll(stack));
        resources.replaceAll((id, stack) -> List.copyOf(stack));
        return Map.copyOf(resources);
    }

    @Override
    public Stream<PackResources> listPacks() {
        return Stream.concat(this.base.listPacks(), this.overlay.listPacks());
    }

    @Override
    public void close() {
        if (this.closed) return;
        this.closed = true;
        try {
            this.overlay.close();
        } finally {
            if (this.ownsBase) this.base.close();
        }
    }
}
