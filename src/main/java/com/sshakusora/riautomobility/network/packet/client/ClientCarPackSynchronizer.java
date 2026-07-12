package com.sshakusora.riautomobility.network.packet.client;

import com.mojang.logging.LogUtils;
import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.carpack.CarPackManager;
import com.sshakusora.riautomobility.carpack.CarPackManifestEntry;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.CarPackSyncStatusPacket;
import com.sshakusora.riautomobility.network.packet.RequestCarPacksPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID, value = Dist.CLIENT)
public final class ClientCarPackSynchronizer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ExecutorService CACHE_IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "RIAutomobility car pack cache");
        thread.setDaemon(true);
        return thread;
    });
    private static final ScheduledExecutorService TIMEOUT_CHECKER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "RIAutomobility car pack timeout");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicLong GENERATION = new AtomicLong();
    private static final long MAX_CACHE_SIZE = 2L * 1024L * 1024L * 1024L;
    private static final long TRANSFER_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(60);
    private static volatile Session session;

    static {
        TIMEOUT_CHECKER.scheduleAtFixedRate(ClientCarPackSynchronizer::checkTimeout, 15, 15, TimeUnit.SECONDS);
    }

    private ClientCarPackSynchronizer() {}

    public static void begin(
            Map<ResourceLocation, FrameSpec> frames,
            Map<ResourceLocation, WheelSpec> wheels,
            List<CarPackManifestEntry> manifest
    ) {
        long generation = GENERATION.incrementAndGet();
        Session next;
        try {
            next = new Session(generation, frames, wheels, validateManifest(manifest));
        } catch (RuntimeException exception) {
            failWithoutSession("Invalid server car pack manifest", exception);
            return;
        }
        Session previous = session;
        session = next;
        CACHE_IO.execute(() -> {
            cleanup(previous);
            try {
                resolveAvailablePacks(next);
                next.lastActivityNanos = System.nanoTime();
                if (!isCurrent(next)) {
                    return;
                }
                if (next.missing.isEmpty()) {
                    finish(next);
                } else {
                    List<RequestCarPacksPacket.Request> requests = next.missing.values().stream()
                            .map(entry -> new RequestCarPacksPacket.Request(entry.id(), entry.archiveDigest()))
                            .toList();
                    Minecraft.getInstance().execute(() -> {
                        if (isCurrent(next)) {
                            RIAutomobilityNetwork.CHANNEL.sendToServer(new RequestCarPacksPacket(requests));
                        }
                    });
                }
            } catch (Exception exception) {
                fail(next, "Unable to inspect the local car pack cache", exception);
            }
        });
    }

    public static void startTransfer(CarPackManifestEntry manifest) {
        enqueue(current -> {
            CarPackManifestEntry expected = current.missing.get(manifest.id());
            if (expected == null || !expected.equals(manifest)) {
                throw new IOException("Server started an unexpected car pack transfer: " + manifest.id());
            }
            if (current.transfers.containsKey(manifest.archiveDigest())) {
                throw new IOException("Duplicate car pack transfer: " + manifest.id());
            }
            Files.createDirectories(CarPackManager.getClientPackCacheDirectory());
            Path target = cachePath(manifest);
            Path temporary = target.resolveSibling(target.getFileName() + "." + UUID.randomUUID() + ".part");
            current.transfers.put(manifest.archiveDigest(), new Transfer(manifest, target, temporary, Files.newOutputStream(temporary)));
        });
    }

    public static void acceptChunk(String archiveDigest, int index, byte[] data) {
        enqueue(current -> {
            Transfer transfer = current.transfers.get(archiveDigest);
            if (transfer == null) {
                throw new IOException("Received a car pack chunk before its transfer started");
            }
            if (index != transfer.nextIndex) {
                throw new IOException("Out-of-order car pack chunk: expected " + transfer.nextIndex + ", got " + index);
            }
            long nextSize = transfer.received + data.length;
            if (nextSize > transfer.manifest.archiveSize()) {
                throw new IOException("Car pack transfer exceeded its declared size");
            }
            transfer.output.write(data);
            transfer.received = nextSize;
            transfer.nextIndex++;
        });
    }

    public static void completeTransfer(String archiveDigest) {
        enqueue(current -> {
            Transfer transfer = current.transfers.remove(archiveDigest);
            if (transfer == null) {
                throw new IOException("Completed an unknown car pack transfer");
            }
            transfer.close();
            if (transfer.received != transfer.manifest.archiveSize()) {
                throw new IOException("Car pack transfer size mismatch for " + transfer.manifest.id());
            }
            if (!CarPackArchiveStore.sha256(transfer.temporary).equals(transfer.manifest.archiveDigest())) {
                throw new IOException("Car pack checksum mismatch for " + transfer.manifest.id());
            }
            CarPackArchiveStore.validateArchive(transfer.temporary);
            moveAtomically(transfer.temporary, transfer.target);
            Files.setLastModifiedTime(transfer.target, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
            current.resolved.put(transfer.manifest.id(), CarPackManager.cachedCarPack(transfer.manifest, transfer.target));
            current.missing.remove(transfer.manifest.id());
            if (current.missing.isEmpty()) {
                finish(current);
            }
        });
    }

    public static void failFromServer(String reason) {
        Session current = session;
        if (current == null) {
            failWithoutSession(reason, null);
        } else {
            CACHE_IO.execute(() -> fail(current, reason, null));
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        GENERATION.incrementAndGet();
        Session previous = session;
        session = null;
        CarPackManager.clearClientResourcePacks();
        CACHE_IO.execute(() -> cleanup(previous));
    }

    private static void resolveAvailablePacks(Session current) throws IOException {
        Map<String, CarPackManager.CarPack> local = new HashMap<>();
        for (CarPackManager.CarPack pack : CarPackManager.discoverCarPacks()) {
            local.put(pack.id(), pack);
        }
        Files.createDirectories(CarPackManager.getClientPackCacheDirectory());
        cleanPartialDownloads();
        for (CarPackManifestEntry entry : current.manifest) {
            CarPackManager.CarPack localPack = local.get(entry.id());
            if (localPack != null && localPack.digest().equals(entry.contentDigest())) {
                current.resolved.put(entry.id(), localPack);
                continue;
            }
            Path cached = cachePath(entry);
            if (isValidCacheEntry(cached, entry)) {
                current.resolved.put(entry.id(), CarPackManager.cachedCarPack(entry, cached));
            } else {
                Files.deleteIfExists(cached);
                current.missing.put(entry.id(), entry);
            }
        }
    }

    private static boolean isValidCacheEntry(Path path, CarPackManifestEntry entry) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try {
            return Files.size(path) == entry.archiveSize()
                    && CarPackArchiveStore.sha256(path).equals(entry.archiveDigest())
                    && validArchive(path);
        } catch (IOException exception) {
            LOGGER.warn("Discarding invalid cached RIAutomobility car pack {}", path, exception);
            return false;
        }
    }

    private static boolean validArchive(Path path) {
        try {
            CarPackArchiveStore.validateArchive(path);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private static void cleanPartialDownloads() throws IOException {
        long staleBefore = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        try (var files = Files.list(CarPackManager.getClientPackCacheDirectory())) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".part"))
                    .filter(path -> lastModified(path) < staleBefore)
                    .toList()) {
                Files.deleteIfExists(file);
            }
        }
    }

    private static void pruneCache(Session current) {
        Set<String> retained = current.manifest.stream()
                .map(entry -> entry.archiveDigest() + ".zip")
                .collect(java.util.stream.Collectors.toSet());
        try (var files = Files.list(CarPackManager.getClientPackCacheDirectory())) {
            List<Path> cached = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .sorted(java.util.Comparator.comparingLong(ClientCarPackSynchronizer::lastModified))
                    .toList();
            long total = 0;
            for (Path file : cached) {
                total = Math.addExact(total, Files.size(file));
            }
            for (Path file : cached) {
                if (total <= MAX_CACHE_SIZE) {
                    break;
                }
                if (!retained.contains(file.getFileName().toString())) {
                    long size = Files.size(file);
                    Files.deleteIfExists(file);
                    total -= size;
                }
            }
        } catch (IOException | ArithmeticException exception) {
            LOGGER.debug("Unable to prune the RIAutomobility car pack cache", exception);
        }
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return Long.MIN_VALUE;
        }
    }

    private static List<CarPackManifestEntry> validateManifest(List<CarPackManifestEntry> entries) {
        if (entries.size() > CarPackManifestEntry.MAX_PACKS) {
            throw new IllegalArgumentException("Too many server car packs");
        }
        Set<String> ids = new HashSet<>();
        long totalSize = 0;
        for (CarPackManifestEntry entry : entries) {
            if (!ids.add(entry.id())) {
                throw new IllegalArgumentException("Duplicate server car pack id: " + entry.id());
            }
            totalSize = Math.addExact(totalSize, entry.archiveSize());
            if (totalSize > CarPackManifestEntry.MAX_TOTAL_ARCHIVE_SIZE) {
                throw new IllegalArgumentException("Server car packs exceed the total cache safety limit");
            }
        }
        return List.copyOf(entries);
    }

    private static void finish(Session current) {
        if (!isCurrent(current) || current.finishing) {
            return;
        }
        pruneCache(current);
        current.finishing = true;
        List<CarPackManager.CarPack> selected = new ArrayList<>(current.manifest.size());
        for (CarPackManifestEntry entry : current.manifest) {
            CarPackManager.CarPack pack = current.resolved.get(entry.id());
            if (pack == null) {
                fail(current, "Car pack was not resolved: " + entry.id(), null);
                return;
            }
            selected.add(pack);
        }

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (!isCurrent(current)) {
                return;
            }
            CarPackManager.setClientResourcePacks(selected);
            minecraft.getResourcePackRepository().reload();
            minecraft.reloadResourcePacks().whenComplete((unused, error) -> minecraft.execute(() -> {
                if (!isCurrent(current)) {
                    return;
                }
                if (error != null) {
                    fail(current, "Unable to reload downloaded car pack resources", error);
                    return;
                }
                try {
                    SyncCustomComponentsClientHandler.applyComponents(minecraft, current.frames, current.wheels);
                    RIAutomobilityNetwork.CHANNEL.sendToServer(new CarPackSyncStatusPacket(
                            true,
                            current.manifest.size() + " car pack(s) ready"
                    ));
                    session = null;
                } catch (RuntimeException exception) {
                    fail(current, "Unable to apply synchronized car components", exception);
                }
            }));
        });
    }

    private static void enqueue(IoAction action) {
        Session current = session;
        if (current == null) {
            return;
        }
        CACHE_IO.execute(() -> {
            if (!isCurrent(current)) {
                return;
            }
            try {
                action.run(current);
                current.lastActivityNanos = System.nanoTime();
            } catch (Exception exception) {
                fail(current, "Car pack transfer failed", exception);
            }
        });
    }

    private static void checkTimeout() {
        Session current = session;
        if (current == null) {
            return;
        }
        CACHE_IO.execute(() -> {
            if (isCurrent(current) && !current.finishing
                    && System.nanoTime() - current.lastActivityNanos > TRANSFER_TIMEOUT_NANOS) {
                fail(current, "Timed out while waiting for server car pack data", null);
            }
        });
    }

    private static void fail(Session current, String reason, Throwable error) {
        if (!isCurrent(current) || current.failed) {
            return;
        }
        current.failed = true;
        if (error == null) {
            LOGGER.error("RIAutomobility car pack synchronization failed: {}", reason);
        } else {
            LOGGER.error("RIAutomobility car pack synchronization failed: {}", reason, error);
        }
        cleanup(current);
        String detail = error == null || error.getMessage() == null ? reason : reason + ": " + error.getMessage();
        Minecraft.getInstance().execute(() -> {
            try {
                RIAutomobilityNetwork.CHANNEL.sendToServer(new CarPackSyncStatusPacket(false, truncateStatus(detail)));
            } catch (RuntimeException ignored) {
            }
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.getConnection().disconnect(Component.literal("RIAutomobility car pack synchronization failed: " + detail));
            }
        });
    }

    private static void failWithoutSession(String reason, Throwable error) {
        LOGGER.error("RIAutomobility car pack synchronization failed: {}", reason, error);
        Minecraft.getInstance().execute(() -> {
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.getConnection().disconnect(Component.literal("RIAutomobility car pack synchronization failed: " + reason));
            }
        });
    }

    private static void cleanup(Session current) {
        if (current == null) {
            return;
        }
        for (Transfer transfer : current.transfers.values()) {
            try {
                transfer.close();
                Files.deleteIfExists(transfer.temporary);
            } catch (IOException exception) {
                LOGGER.debug("Unable to clean up partial car pack transfer {}", transfer.temporary, exception);
            }
        }
        current.transfers.clear();
    }

    private static boolean isCurrent(Session current) {
        return current != null && session == current && GENERATION.get() == current.generation && !current.failed;
    }

    private static Path cachePath(CarPackManifestEntry manifest) {
        return CarPackManager.getClientPackCacheDirectory().resolve(manifest.archiveDigest() + ".zip");
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String truncateStatus(String value) {
        return value.length() <= 512 ? value : value.substring(0, 512);
    }

    private interface IoAction {
        void run(Session session) throws Exception;
    }

    private static final class Session {
        private final long generation;
        private final Map<ResourceLocation, FrameSpec> frames;
        private final Map<ResourceLocation, WheelSpec> wheels;
        private final List<CarPackManifestEntry> manifest;
        private final Map<String, CarPackManager.CarPack> resolved = new LinkedHashMap<>();
        private final Map<String, CarPackManifestEntry> missing = new LinkedHashMap<>();
        private final Map<String, Transfer> transfers = new HashMap<>();
        private boolean finishing;
        private boolean failed;
        private long lastActivityNanos = System.nanoTime();

        private Session(long generation, Map<ResourceLocation, FrameSpec> frames, Map<ResourceLocation, WheelSpec> wheels,
                        List<CarPackManifestEntry> manifest) {
            this.generation = generation;
            this.frames = Map.copyOf(frames);
            this.wheels = Map.copyOf(wheels);
            this.manifest = manifest;
        }
    }

    private static final class Transfer {
        private final CarPackManifestEntry manifest;
        private final Path target;
        private final Path temporary;
        private final OutputStream output;
        private int nextIndex;
        private long received;
        private boolean closed;

        private Transfer(CarPackManifestEntry manifest, Path target, Path temporary, OutputStream output) {
            this.manifest = manifest;
            this.target = target;
            this.temporary = temporary;
            this.output = output;
        }

        private void close() throws IOException {
            if (!closed) {
                closed = true;
                output.close();
            }
        }
    }
}
