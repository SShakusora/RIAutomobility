package com.sshakusora.riautomobility.carpack;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads the data side of RIAuto archives without touching the datapack repository.
 */
public final class CarPackRuntime {
    private CarPackRuntime() {
    }

    public static CarPackComponentDataLoader.LoadedContent loadServerContent() {
        List<CarPackManager.CarPack> packs = CarPackManager.discoverCarPacks();
        try (CloseableResourceManager resources = open(PackType.SERVER_DATA, packs)) {
            return CarPackComponentDataLoader.load(resources);
        }
    }

    public static void reloadServer() {
        CarPackComponentDataLoader.LoadedContent content = loadServerContent();
        CarPackComponentDataLoader.apply(content);
        CarPackArchiveStore.prepareManifest();
    }

    public static CloseableResourceManager open(PackType type, List<CarPackManager.CarPack> packs) {
        List<PackResources> opened = new ArrayList<>(packs.size());
        try {
            for (CarPackManager.CarPack pack : packs) {
                opened.add(pack.resources().open(pack.id()));
            }
            return new MultiPackResourceManager(type, opened);
        } catch (RuntimeException exception) {
            opened.forEach(PackResources::close);
            throw exception;
        }
    }
}
