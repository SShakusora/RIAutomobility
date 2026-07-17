package com.sshakusora.riautomobility.carpack;

import com.sshakusora.riautomobility.content.RIAutomobilityComponentManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the data side of RIAuto archives without touching the datapack repository.
 */
public final class CarPackRuntime {
    private CarPackRuntime() {
    }

    static CarPackComponentDataLoader.LoadedContent loadServerContent(List<CarPackManager.CarPack> packs) {
        try (CloseableResourceManager resources = open(PackType.SERVER_DATA, packs)) {
            return CarPackComponentDataLoader.load(resources);
        }
    }

    public static void reloadServer() {
        reloadServerAndCaptureState();
    }

    static CarPackSharedDirectoryMonitor.DirectoryState reloadServerAndCaptureState() {
        try {
            Path directory = CarPackManager.getServerCarPackDirectory();
            return CarPackDirectoryLock.withExclusive(directory, () -> {
                apply(prepare());
                return CarPackSharedDirectoryMonitor.DirectoryState.capture(directory);
            });
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to reload the shared RIAutomobility car pack directory", exception);
        }
    }

    /** Called from a background I/O thread while the caller holds the shared-directory lock. */
    public static void reloadServerAndSync(MinecraftServer server) throws Exception {
        PreparedReload prepared = prepare();
        server.submit(() -> {
            PreparedReload previous = current();
            try {
                apply(prepared);
                CarPackEvents.CommonEvents.syncAll(server);
            } catch (RuntimeException failure) {
                try {
                    apply(previous);
                    CarPackEvents.CommonEvents.syncAll(server);
                } catch (RuntimeException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        }).get();
    }

    static PreparedReload prepare() {
        List<CarPackManager.CarPack> packs = CarPackManager.discoverServerCarPacks();
        CarPackComponentDataLoader.LoadedContent content = loadServerContent(packs);
        CarPackArchiveStore.PreparedCatalog catalog = CarPackArchiveStore.prepareCatalog(packs);
        return new PreparedReload(content, catalog);
    }

    static void apply(PreparedReload prepared) {
        CarPackComponentDataLoader.apply(prepared.content());
        CarPackArchiveStore.installPreparedCatalog(prepared.catalog());
    }

    private static PreparedReload current() {
        return new PreparedReload(new CarPackComponentDataLoader.LoadedContent(
                RIAutomobilityComponentManager.getCustomFrames(),
                RIAutomobilityComponentManager.getCustomWheels(),
                RIAutomobilityComponentManager.getCustomEngines()),
                CarPackArchiveStore.currentCatalog());
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

    record PreparedReload(CarPackComponentDataLoader.LoadedContent content,
                          CarPackArchiveStore.PreparedCatalog catalog) {
    }
}
