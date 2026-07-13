package com.sshakusora.riautomobility.carpack.client;

import com.sshakusora.riautomobility.carpack.CarPackManager;
import com.sshakusora.riautomobility.carpack.CarPackRuntime;
import com.sshakusora.riautomobility.carpack.OverlayCloseableResourceManager;
import com.sshakusora.riautomobility.mixin.accessor.ReloadableResourceManagerAccessor;
import com.sshakusora.riautomobility.model.DynamicJsonModelLoader;
import com.sshakusora.riautomobility.model.RIAutomobileModels;
import com.sshakusora.riautomobility.model.bbmodel.BbModelRepository;
import com.sshakusora.riautomobility.model.gecko.CarPackGeckoReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Installs RIAuto assets as a silent overlay on Minecraft's existing resource view.
 */
public final class ClientCarPackResources {
    private static Mount mount;

    private ClientCarPackResources() {
    }

    public static void install(List<CarPackManager.CarPack> packs) {
        Minecraft minecraft = Minecraft.getInstance();
        ReloadableResourceManager manager = (ReloadableResourceManager) minecraft.getResourceManager();
        ReloadableResourceManagerAccessor accessor = (ReloadableResourceManagerAccessor) manager;
        String fingerprint = fingerprint(packs);
        if (mount != null && mount.fingerprint.equals(fingerprint) && accessor.riautomobility$getResources() == mount.resources) {
            return;
        }

        Set<ResourceLocation> staleTextures = mount == null ? Set.of() : mount.textures;
        CloseableResourceManager base = detach(accessor);
        CloseableResourceManager carResources = CarPackRuntime.open(PackType.CLIENT_RESOURCES, packs);
        OverlayCloseableResourceManager combined = new OverlayCloseableResourceManager(base, carResources);
        Set<ResourceLocation> textures = textureIds(carResources);
        mount = new Mount(fingerprint, combined, carResources, textures);
        accessor.riautomobility$setResources(combined);

        Set<ResourceLocation> reset = new HashSet<>(staleTextures);
        reset.addAll(textures);
        reset.forEach(minecraft.getTextureManager()::release);
        CarPackGeckoReloader.reload(carResources, manager);
    }

    public static void uninstall() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.getResourceManager() instanceof ReloadableResourceManager manager)) return;
        ReloadableResourceManagerAccessor accessor = (ReloadableResourceManagerAccessor) manager;
        if (mount == null) return;
        Set<ResourceLocation> textures = mount.textures;
        detach(accessor);
        mount = null;
        textures.forEach(minecraft.getTextureManager()::release);
        CarPackGeckoReloader.clear();
    }

    public static void refreshDiscoveredPacks() {
        install(CarPackManager.discoverClientResourcePacks());
        refreshDynamicModels();
    }

    public static void refreshDynamicModels() {
        Minecraft minecraft = Minecraft.getInstance();
        BbModelRepository.reload(minecraft.getResourceManager());
        DynamicJsonModelLoader.loadIntoEntityModelSet(minecraft.getEntityModels(), minecraft.getResourceManager());
        RIAutomobileModels.rebuildDynamicModelsNow();
    }

    private static CloseableResourceManager detach(ReloadableResourceManagerAccessor accessor) {
        CloseableResourceManager current = accessor.riautomobility$getResources();
        if (mount == null || current != mount.resources) return current;
        CloseableResourceManager base = mount.resources.detachBase();
        accessor.riautomobility$setResources(base);
        mount.resources.close();
        return base;
    }

    private static Set<ResourceLocation> textureIds(CloseableResourceManager resources) {
        return Set.copyOf(resources.listResources("textures", id -> id.getPath().endsWith(".png")).keySet());
    }

    private static String fingerprint(List<CarPackManager.CarPack> packs) {
        return packs.stream().map(pack -> pack.id() + "=" + pack.digest()).collect(Collectors.joining("\n"));
    }

    private record Mount(String fingerprint, OverlayCloseableResourceManager resources,
                         CloseableResourceManager carResources, Set<ResourceLocation> textures) {
    }
}
