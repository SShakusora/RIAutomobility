package com.sshakusora.riautomobility.network.packet.client;

import com.mojang.logging.LogUtils;
import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.carpack.CarPackManager;
import com.sshakusora.riautomobility.carpack.CarPackManifestEntry;
import com.sshakusora.riautomobility.carpack.client.ClientCarPackResources;
import com.sshakusora.riautomobility.content.EngineSpec;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.CarPackSyncStatusPacket;
import com.sshakusora.riautomobility.network.packet.RequestCarPacksPacket;
import io.github.foundationgames.automobility.automobile.AutomobileData;
import io.github.foundationgames.automobility.item.AutomobileComponentItem;
import io.github.foundationgames.automobility.item.AutomobileItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps the server car-pack catalog for the whole connection and mounts package assets only when a
 * component is encountered. Component definitions are installed immediately so missing assets can
 * use the normal placeholder model while their package is downloaded.
 */
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
    private static final long MOUNT_IDLE_NANOS = TimeUnit.MINUTES.toNanos(5);
    private static volatile Session session;
    private static int scanTicker;

    static {
        TIMEOUT_CHECKER.scheduleAtFixedRate(ClientCarPackSynchronizer::checkTimeout, 15, 15, TimeUnit.SECONDS);
    }

    private ClientCarPackSynchronizer() {
    }

    public static void runWhenReady(Runnable action) {
        Session current = session;
        if (current == null) {
            Minecraft.getInstance().execute(action);
            return;
        }
        current.ready.thenRunAsync(action, Minecraft.getInstance()::execute);
    }

    public static void begin(Map<ResourceLocation, FrameSpec> frames,
                             Map<ResourceLocation, WheelSpec> wheels,
                             Map<ResourceLocation, EngineSpec> engines,
                             List<CarPackManifestEntry> manifest) {
        long generation = GENERATION.incrementAndGet();
        Session next;
        try {
            next = new Session(generation, frames, wheels, engines, validateManifest(manifest));
        } catch (RuntimeException exception) {
            failCatalog(null, "Invalid server car pack catalog", exception);
            return;
        }

        Session previous = session;
        session = next;
        CarPackArchiveStore.installComponentMetadata(next.manifest);
        if (previous != null && !previous.ready.isDone()) {
            previous.ready.completeExceptionally(new IllegalStateException("Car pack catalog was superseded"));
        }

        CACHE_IO.execute(() -> initializeCatalog(next, previous));
    }

    public static void requestComponent(ResourceLocation componentId) {
        requestComponents(List.of(componentId));
    }

    public static void requestComponents(Collection<ResourceLocation> componentIds) {
        Session current = session;
        if (current == null || !current.ready.isDone()) return;
        LinkedHashSet<String> packIds = new LinkedHashSet<>();
        for (ResourceLocation componentId : componentIds) {
            String packId = current.componentPacks.get(componentId);
            if (packId != null) {
                packIds.add(packId);
                current.lastNeededNanos.put(packId, System.nanoTime());
            }
        }
        if (!packIds.isEmpty()) {
            CACHE_IO.execute(() -> resolveAndRequest(current, packIds));
        }
    }

    public static void startTransfer(CarPackManifestEntry manifest) {
        enqueue(current -> {
            CarPackManifestEntry expected = current.catalog.get(manifest.id());
            if (expected == null || !expected.equals(manifest) || !current.requested.contains(manifest.id())) {
                throw new IOException("Server started an unexpected car pack transfer: " + manifest.id());
            }
            if (current.transfers.containsKey(manifest.archiveDigest())) {
                throw new IOException("Duplicate car pack transfer: " + manifest.id());
            }
            Files.createDirectories(CarPackManager.getClientPackCacheDirectory());
            Path target = cachePath(manifest);
            Path temporary = target.resolveSibling(target.getFileName() + "." + UUID.randomUUID() + ".part");
            current.transfers.put(manifest.archiveDigest(),
                    new Transfer(manifest, target, temporary, Files.newOutputStream(temporary)));
        });
    }

    public static void acceptChunk(String archiveDigest, int index, byte[] data) {
        enqueue(current -> {
            Transfer transfer = current.transfers.get(archiveDigest);
            if (transfer == null) throw new IOException("Received a car pack chunk before its transfer started");
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
            if (transfer == null) throw new IOException("Completed an unknown car pack transfer");
            transfer.close();
            if (transfer.received != transfer.manifest.archiveSize()) {
                throw new IOException("Car pack transfer size mismatch for " + transfer.manifest.id());
            }
            if (!CarPackArchiveStore.sha256(transfer.temporary).equals(transfer.manifest.archiveDigest())) {
                throw new IOException("Car pack checksum mismatch for " + transfer.manifest.id());
            }
            CarPackArchiveStore.validateArchive(transfer.temporary);
            moveAtomically(transfer.temporary, transfer.target);
            Files.setLastModifiedTime(transfer.target, FileTime.fromMillis(System.currentTimeMillis()));
            CarPackManager.CarPack pack = CarPackManager.cachedCarPack(transfer.manifest, transfer.target);
            current.resolved.put(transfer.manifest.id(), pack);
            current.requested.remove(transfer.manifest.id());
            mount(current, pack);
        });
    }

    public static void failFromServer(String reason) {
        Session current = session;
        if (current == null) {
            LOGGER.warn("RIAutomobility car pack download failed: {}", reason);
            return;
        }
        CACHE_IO.execute(() -> failDownloads(current, reason, null));
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof RIAutomobileEntity automobile) {
            requestAutomobile(automobile);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++scanTicker < 20) return;
        scanTicker = 0;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        LinkedHashSet<ResourceLocation> components = new LinkedHashSet<>();
        minecraft.level.entitiesForRendering().forEach(entity -> {
            if (entity instanceof RIAutomobileEntity automobile) collectAutomobile(automobile, components);
            else if (entity instanceof ItemEntity itemEntity) collectItem(itemEntity.getItem(), components);
            else if (entity instanceof LivingEntity living) {
                living.getHandSlots().forEach(stack -> collectItem(stack, components));
            }
        });
        minecraft.player.containerMenu.slots.forEach(slot -> collectItem(slot.getItem(), components));
        requestComponents(components);
        CACHE_IO.execute(() -> expireUnusedMounts(session));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        GENERATION.incrementAndGet();
        Session previous = session;
        session = null;
        if (previous != null && !previous.ready.isDone()) {
            previous.ready.completeExceptionally(new IllegalStateException("Client disconnected"));
        }
        CarPackManager.clearClientResourcePacks();
        ClientCarPackResources.uninstall();
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.hasSingleplayerServer()) {
            SyncCustomComponentsClientHandler.applyComponents(minecraft, Map.of(), Map.of(), Map.of());
        }
        CACHE_IO.execute(() -> cleanupTransfers(previous));
    }

    private static void initializeCatalog(Session current, Session previous) {
        try {
            Files.createDirectories(CarPackManager.getClientPackCacheDirectory());
            cleanPartialDownloads();
            Map<String, CarPackManager.CarPack> local = new HashMap<>();
            for (CarPackManager.CarPack pack : CarPackManager.discoverCarPacks()) local.put(pack.id(), pack);
            for (CarPackManifestEntry entry : current.manifest) {
                CarPackManager.CarPack localPack = local.get(entry.id());
                if (localPack != null && localPack.digest().equals(entry.contentDigest())) {
                    current.resolved.put(entry.id(), localPack);
                }
            }

            if (previous != null) {
                for (CarPackManager.CarPack pack : previous.mounted.values()) {
                    CarPackManifestEntry entry = current.catalog.get(pack.id());
                    if (entry != null && entry.contentDigest().equals(pack.digest())) {
                        current.resolved.put(pack.id(), pack);
                        current.initialMounted.put(pack.id(), pack);
                        current.lastNeededNanos.put(pack.id(), System.nanoTime());
                    }
                }
            }
            cleanupTransfers(previous);

            Minecraft.getInstance().execute(() -> applyCatalog(current));
        } catch (Exception exception) {
            failCatalog(current, "Unable to initialize the car pack catalog", exception);
        }
    }

    private static void applyCatalog(Session current) {
        if (!isCurrent(current)) return;
        try {
            current.mounted.putAll(current.initialMounted);
            installMounted(current, true);
            SyncCustomComponentsClientHandler.applyComponents(
                    Minecraft.getInstance(), current.frames, current.wheels, current.engines);
            current.ready.complete(null);
            LOGGER.info("Indexed {} server car pack(s) for on-demand loading; retained {} mounted pack(s)",
                    current.manifest.size(), current.mounted.size());
            RIAutomobilityNetwork.CHANNEL.sendToServer(new CarPackSyncStatusPacket(
                    true, current.manifest.size() + " car pack(s) indexed for on-demand loading"));
            CACHE_IO.execute(() -> pruneCache(current));
        } catch (RuntimeException exception) {
            failCatalog(current, "Unable to apply synchronized car components", exception);
        }
    }

    private static void resolveAndRequest(Session current, Collection<String> packIds) {
        if (!isCurrent(current)) return;
        List<RequestCarPacksPacket.Request> downloads = new ArrayList<>();
        List<CarPackManager.CarPack> mounts = new ArrayList<>();
        for (String packId : packIds) {
            if (current.mounted.containsKey(packId) || current.requested.contains(packId)) continue;
            CarPackManifestEntry entry = current.catalog.get(packId);
            if (entry == null) continue;

            CarPackManager.CarPack pack = current.resolved.get(packId);
            if (pack == null) {
                Path cached = cachePath(entry);
                if (isValidCacheEntry(cached, entry)) {
                    try {
                        Files.setLastModifiedTime(cached, FileTime.fromMillis(System.currentTimeMillis()));
                        pack = CarPackManager.cachedCarPack(entry, cached);
                        current.resolved.put(packId, pack);
                    } catch (IOException exception) {
                        LOGGER.debug("Unable to reuse cached car pack {}", packId, exception);
                    }
                } else {
                    try {
                        Files.deleteIfExists(cached);
                    } catch (IOException exception) {
                        LOGGER.debug("Unable to remove invalid cached car pack {}", cached, exception);
                    }
                }
            }

            if (pack != null) {
                mounts.add(pack);
            } else if (current.requested.add(packId)) {
                downloads.add(new RequestCarPacksPacket.Request(entry.id(), entry.archiveDigest()));
            }
        }
        if (!mounts.isEmpty()) mountAll(current, mounts);
        if (!downloads.isEmpty()) {
            LOGGER.info("Requesting {} on-demand car pack(s)", downloads.size());
            current.lastActivityNanos = System.nanoTime();
            Minecraft.getInstance().execute(() -> {
                if (isCurrent(current)) {
                    for (int start = 0; start < downloads.size(); start += RequestCarPacksPacket.MAX_REQUESTS) {
                        int end = Math.min(downloads.size(), start + RequestCarPacksPacket.MAX_REQUESTS);
                        RIAutomobilityNetwork.CHANNEL.sendToServer(
                                new RequestCarPacksPacket(downloads.subList(start, end)));
                    }
                }
            });
        }
    }

    private static void mount(Session current, CarPackManager.CarPack pack) {
        mountAll(current, List.of(pack));
    }

    private static void mountAll(Session current, Collection<CarPackManager.CarPack> packs) {
        Minecraft.getInstance().execute(() -> {
            if (!isCurrent(current)) return;
            List<CarPackManager.CarPack> added = packs.stream()
                    .filter(pack -> current.mounted.putIfAbsent(pack.id(), pack) == null).toList();
            if (added.isEmpty()) return;
            Set<ResourceLocation> affectedComponents = componentsForPacks(current, added);
            long now = System.nanoTime();
            added.forEach(pack -> current.lastNeededNanos.put(pack.id(), now));
            try {
                installMounted(current, false);
                SyncCustomComponentsClientHandler.refreshMountedComponents(
                        Minecraft.getInstance(), affectedComponents, current.frames, current.wheels, current.engines);
                LOGGER.info("Mounted {} on-demand car pack(s); {} currently mounted", added.size(), current.mounted.size());
                CACHE_IO.execute(() -> pruneCache(current));
            } catch (RuntimeException exception) {
                added.forEach(pack -> current.mounted.remove(pack.id(), pack));
                LOGGER.error("Unable to mount {} on-demand car pack(s)", added.size(), exception);
                try {
                    installMounted(current, false);
                    SyncCustomComponentsClientHandler.refreshMountedComponents(
                            Minecraft.getInstance(), affectedComponents, current.frames, current.wheels, current.engines);
                } catch (RuntimeException rollbackException) {
                    exception.addSuppressed(rollbackException);
                    LOGGER.error("Unable to restore the previous car pack mount", rollbackException);
                }
            }
        });
    }

    private static void expireUnusedMounts(Session current) {
        if (!isCurrent(current) || !current.ready.isDone()) return;
        long expiredBefore = System.nanoTime() - MOUNT_IDLE_NANOS;
        Set<String> expired = new HashSet<>();
        current.mounted.keySet().forEach(id -> {
            if (current.lastNeededNanos.getOrDefault(id, 0L) < expiredBefore) expired.add(id);
        });
        if (expired.isEmpty()) return;

        Minecraft.getInstance().execute(() -> {
            if (!isCurrent(current)) return;
            Map<String, CarPackManager.CarPack> removed = new HashMap<>();
            long checkBefore = System.nanoTime() - MOUNT_IDLE_NANOS;
            for (String id : expired) {
                if (current.lastNeededNanos.getOrDefault(id, 0L) < checkBefore) {
                    CarPackManager.CarPack pack = current.mounted.remove(id);
                    if (pack != null) removed.put(id, pack);
                }
            }
            if (removed.isEmpty()) return;
            Set<ResourceLocation> affectedComponents = componentsForPackIds(current, removed.keySet());
            try {
                installMounted(current, false);
                SyncCustomComponentsClientHandler.refreshMountedComponents(
                        Minecraft.getInstance(), affectedComponents, current.frames, current.wheels, current.engines);
                removed.keySet().forEach(current.lastNeededNanos::remove);
                LOGGER.info("Unmounted {} idle car pack(s); {} currently mounted", removed.size(), current.mounted.size());
                CACHE_IO.execute(() -> pruneCache(current));
            } catch (RuntimeException exception) {
                current.mounted.putAll(removed);
                LOGGER.error("Unable to unmount idle car packs; restoring the previous mount", exception);
                try {
                    installMounted(current, false);
                    SyncCustomComponentsClientHandler.refreshMountedComponents(
                            Minecraft.getInstance(), affectedComponents, current.frames, current.wheels, current.engines);
                } catch (RuntimeException rollbackException) {
                    exception.addSuppressed(rollbackException);
                    LOGGER.error("Unable to restore idle car pack mounts", rollbackException);
                }
            }
        });
    }

    private static void installMounted(Session current, boolean fullReload) {
        List<CarPackManager.CarPack> selected = current.manifest.stream()
                .map(entry -> current.mounted.get(entry.id()))
                .filter(Objects::nonNull)
                .toList();
        CarPackManager.setClientResourcePacks(selected);
        if (fullReload) {
            ClientCarPackResources.install(CarPackManager.discoverClientResourcePacks());
        } else {
            ClientCarPackResources.installIncremental(CarPackManager.discoverClientResourcePacks());
        }
    }

    private static Set<ResourceLocation> componentsForPacks(Session current,
                                                            Collection<CarPackManager.CarPack> packs) {
        Set<String> ids = new HashSet<>();
        packs.forEach(pack -> ids.add(pack.id()));
        return componentsForPackIds(current, ids);
    }

    private static Set<ResourceLocation> componentsForPackIds(Session current, Collection<String> packIds) {
        Set<ResourceLocation> components = new HashSet<>();
        for (String packId : packIds) {
            CarPackManifestEntry entry = current.catalog.get(packId);
            if (entry != null) components.add(entry.component());
        }
        return components;
    }

    private static void requestAutomobile(RIAutomobileEntity automobile) {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
        collectAutomobile(automobile, ids);
        requestComponents(ids);
    }

    private static void collectAutomobile(RIAutomobileEntity automobile, Collection<ResourceLocation> result) {
        result.add(automobile.getFrame().getId());
        result.add(automobile.getWheels().getId());
        result.add(automobile.getEngine().getId());
    }

    private static void collectItem(ItemStack stack, Collection<ResourceLocation> result) {
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof AutomobileComponentItem<?> componentItem) {
            result.add(componentItem.getComponent(stack).getId());
            return;
        }
        if (stack.getItem() instanceof AutomobileItem) {
            var tag = stack.getTagElement("Automobile");
            if (tag == null) return;
            AutomobileData data = new AutomobileData();
            data.read(tag);
            result.add(data.getFrame().getId());
            result.add(data.getWheel().getId());
            result.add(data.getEngine().getId());
        }
    }

    private static boolean isValidCacheEntry(Path path, CarPackManifestEntry entry) {
        if (!Files.isRegularFile(path)) return false;
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
                    .filter(path -> lastModified(path) < staleBefore).toList()) {
                Files.deleteIfExists(file);
            }
        }
    }

    private static void pruneCache(Session current) {
        Set<String> retained = new HashSet<>();
        current.mounted.keySet().forEach(id -> retained.add(current.catalog.get(id).archiveDigest()
                + CarPackManager.CAR_PACK_EXTENSION));
        current.requested.forEach(id -> retained.add(current.catalog.get(id).archiveDigest()
                + CarPackManager.CAR_PACK_EXTENSION));
        try (var files = Files.list(CarPackManager.getClientPackCacheDirectory())) {
            List<Path> cached = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(CarPackManager.CAR_PACK_EXTENSION))
                    .sorted(Comparator.comparingLong(ClientCarPackSynchronizer::lastModified)).toList();
            long total = 0;
            for (Path file : cached) total = Math.addExact(total, Files.size(file));
            for (Path file : cached) {
                if (total <= MAX_CACHE_SIZE) break;
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
        Set<ResourceLocation> components = new HashSet<>();
        for (CarPackManifestEntry entry : entries) {
            if (!ids.add(entry.id())) throw new IllegalArgumentException("Duplicate server car pack id: " + entry.id());
            if (!components.add(entry.component())) {
                throw new IllegalArgumentException("Component is declared by multiple server car packs: " + entry.component());
            }
        }
        return List.copyOf(entries);
    }

    private static void enqueue(IoAction action) {
        Session current = session;
        if (current == null) return;
        CACHE_IO.execute(() -> {
            if (!isCurrent(current)) return;
            try {
                action.run(current);
                current.lastActivityNanos = System.nanoTime();
            } catch (Exception exception) {
                failDownloads(current, "Car pack transfer failed", exception);
            }
        });
    }

    private static void checkTimeout() {
        Session current = session;
        if (current == null || current.requested.isEmpty()) return;
        CACHE_IO.execute(() -> {
            if (isCurrent(current) && !current.requested.isEmpty()
                    && System.nanoTime() - current.lastActivityNanos > TRANSFER_TIMEOUT_NANOS) {
                failDownloads(current, "Timed out while waiting for on-demand car pack data", null);
            }
        });
    }

    private static void failDownloads(Session current, String reason, Throwable error) {
        if (!isCurrent(current)) return;
        if (error == null) LOGGER.warn("RIAutomobility on-demand car pack download failed: {}", reason);
        else LOGGER.warn("RIAutomobility on-demand car pack download failed: {}", reason, error);
        cleanupTransfers(current);
        current.requested.clear();
        Minecraft.getInstance().execute(() -> {
            try {
                RIAutomobilityNetwork.CHANNEL.sendToServer(new CarPackSyncStatusPacket(false, truncateStatus(reason)));
            } catch (RuntimeException ignored) {
            }
        });
    }

    private static void failCatalog(Session current, String reason, Throwable error) {
        if (current != null && !isCurrent(current)) return;
        LOGGER.error("RIAutomobility car pack catalog failed: {}", reason, error);
        if (current != null)
            current.ready.completeExceptionally(error == null ? new IllegalStateException(reason) : error);
        Minecraft.getInstance().execute(() -> {
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                String detail = error == null || error.getMessage() == null ? reason : reason + ": " + error.getMessage();
                connection.getConnection().disconnect(Component.literal("RIAutomobility car pack catalog failed: " + detail));
            }
        });
    }

    private static void cleanupTransfers(Session current) {
        if (current == null) return;
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
        return current != null && session == current && GENERATION.get() == current.generation;
    }

    private static Path cachePath(CarPackManifestEntry manifest) {
        return CarPackManager.getClientPackCacheDirectory()
                .resolve(manifest.archiveDigest() + CarPackManager.CAR_PACK_EXTENSION);
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
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
        private final Map<ResourceLocation, EngineSpec> engines;
        private final List<CarPackManifestEntry> manifest;
        private final Map<String, CarPackManifestEntry> catalog = new LinkedHashMap<>();
        private final Map<ResourceLocation, String> componentPacks = new HashMap<>();
        private final Map<String, CarPackManager.CarPack> resolved = new ConcurrentHashMap<>();
        private final Map<String, CarPackManager.CarPack> mounted = new ConcurrentHashMap<>();
        private final Map<String, CarPackManager.CarPack> initialMounted = new LinkedHashMap<>();
        private final Set<String> requested = ConcurrentHashMap.newKeySet();
        private final Map<String, Long> lastNeededNanos = new ConcurrentHashMap<>();
        private final Map<String, Transfer> transfers = new HashMap<>();
        private final CompletableFuture<Void> ready = new CompletableFuture<>();
        private volatile long lastActivityNanos = System.nanoTime();

        private Session(long generation, Map<ResourceLocation, FrameSpec> frames,
                        Map<ResourceLocation, WheelSpec> wheels, Map<ResourceLocation, EngineSpec> engines,
                        List<CarPackManifestEntry> manifest) {
            this.generation = generation;
            this.frames = Map.copyOf(frames);
            this.wheels = Map.copyOf(wheels);
            this.engines = Map.copyOf(engines);
            this.manifest = manifest;
            for (CarPackManifestEntry entry : manifest) {
                catalog.put(entry.id(), entry);
                componentPacks.put(entry.component(), entry.id());
            }
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
