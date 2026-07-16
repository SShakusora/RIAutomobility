package com.sshakusora.riautomobility.carpack.client;

import com.sshakusora.riautomobility.carpack.CarPackManager;
import com.sshakusora.riautomobility.carpack.CarPackRuntime;
import com.sshakusora.riautomobility.carpack.OverlayCloseableResourceManager;
import com.sshakusora.riautomobility.model.RIAutomobileModels;
import com.sshakusora.riautomobility.model.bbmodel.BbModelRepository;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Installs RIAuto assets as a silent overlay on Minecraft's existing resource view.
 */
public final class ClientCarPackResources {
    private static Mount mount;

    private ClientCarPackResources() {
    }

    public static void install(List<CarPackManager.CarPack> packs) {
        install(packs, true);
    }

    public static void installIncremental(List<CarPackManager.CarPack> packs) {
        install(packs, false);
    }

    private static void install(List<CarPackManager.CarPack> packs, boolean releaseAllTextures) {
        Minecraft minecraft = Minecraft.getInstance();
        ReloadableResourceManager manager = (ReloadableResourceManager) minecraft.getResourceManager();
        String fingerprint = fingerprint(packs);
        if (mount != null && mount.fingerprint.equals(fingerprint) && manager.resources == mount.resources) {
            return;
        }

        Map<ResourceLocation, String> staleTextures = mount == null ? Map.of() : mount.textures;
        CloseableResourceManager base = detach(manager);
        CloseableResourceManager carResources = CarPackRuntime.open(PackType.CLIENT_RESOURCES, packs);
        OverlayCloseableResourceManager combined = new OverlayCloseableResourceManager(base, carResources);
        Map<ResourceLocation, String> textures = textureSources(carResources, packs);
        mount = new Mount(fingerprint, combined, carResources, textures);
        manager.resources = combined;

        if (releaseAllTextures) {
            Set<ResourceLocation> reset = new HashSet<>(staleTextures.keySet());
            reset.addAll(textures.keySet());
            reset.forEach(minecraft.getTextureManager()::release);
        } else {
            Set<ResourceLocation> candidates = new HashSet<>(staleTextures.keySet());
            candidates.addAll(textures.keySet());
            candidates.stream()
                    .filter(id -> !Objects.equals(staleTextures.get(id), textures.get(id)))
                    .forEach(minecraft.getTextureManager()::release);
        }
    }

    public static void uninstall() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.getResourceManager() instanceof ReloadableResourceManager manager)) return;
        if (mount == null) return;
        Set<ResourceLocation> textures = mount.textures.keySet();
        detach(manager);
        mount = null;
        textures.forEach(minecraft.getTextureManager()::release);
    }

    public static void refreshDiscoveredPacks() {
        install(CarPackManager.discoverClientResourcePacks());
        refreshDynamicModels();
    }

    public static void refreshDynamicModels() {
        Minecraft minecraft = Minecraft.getInstance();
        BbModelRepository.reload(minecraft.getResourceManager());
        RIAutomobileModels.rebuildDynamicModelsNow();
    }

    private static CloseableResourceManager detach(ReloadableResourceManager manager) {
        CloseableResourceManager current = manager.resources;
        if (mount == null || current != mount.resources) return current;
        CloseableResourceManager base = mount.resources.detachBase();
        manager.resources = base;
        mount.resources.close();
        return base;
    }

    private static Map<ResourceLocation, String> textureSources(CloseableResourceManager resources,
                                                                List<CarPackManager.CarPack> packs) {
        Map<String, String> digests = packs.stream().collect(Collectors.toMap(
                CarPackManager.CarPack::id, CarPackManager.CarPack::digest));
        Map<ResourceLocation, String> sources = new HashMap<>();
        resources.listResources("textures", id -> id.getPath().endsWith(".png"))
                .forEach((id, resource) -> {
                    String source = resource.sourcePackId();
                    sources.put(id, source + "=" + digests.getOrDefault(source, ""));
                });
        return Map.copyOf(sources);
    }

    private static String fingerprint(List<CarPackManager.CarPack> packs) {
        return packs.stream().map(pack -> pack.id() + "=" + pack.digest()).collect(Collectors.joining("\n"));
    }

    private record Mount(String fingerprint, OverlayCloseableResourceManager resources,
                         CloseableResourceManager carResources, Map<ResourceLocation, String> textures) {
    }
}
