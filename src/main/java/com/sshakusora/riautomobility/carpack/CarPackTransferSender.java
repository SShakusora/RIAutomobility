package com.sshakusora.riautomobility.carpack;

import com.mojang.logging.LogUtils;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.LockSupport;

public final class CarPackTransferSender {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<UUID> ACTIVE_TRANSFERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, ArrayDeque<List<CarPackArchiveStore.TransferPack>>> QUEUED_TRANSFERS = new HashMap<>();
    private static final Semaphore TRANSFER_SLOTS = new Semaphore(4);
    private static final int MAX_QUEUED_BATCHES_PER_PLAYER = 32;
    private static final long BYTES_PER_SECOND = 16L * 1024L * 1024L;

    private CarPackTransferSender() {}

    public static void send(ServerPlayer player, List<RequestCarPacksPacket.Request> requests) {
        List<CarPackArchiveStore.TransferPack> packs;
        try {
            Set<String> uniqueIds = new HashSet<>();
            packs = requests.stream().map(request -> {
                if (!uniqueIds.add(request.id())) {
                    throw new IllegalArgumentException("Duplicate car pack request: " + request.id());
                }
                CarPackArchiveStore.TransferPack pack = CarPackArchiveStore.find(request.id(), request.archiveDigest());
                if (pack == null) {
                    throw new IllegalArgumentException("Unknown or stale car pack request: " + request.id());
                }
                return pack;
            }).toList();
        } catch (RuntimeException exception) {
            sendFailure(player, exception.getMessage());
            return;
        }

        UUID playerId = player.getUUID();
        synchronized (QUEUED_TRANSFERS) {
            ArrayDeque<List<CarPackArchiveStore.TransferPack>> queue =
                    QUEUED_TRANSFERS.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
            if (queue.size() >= MAX_QUEUED_BATCHES_PER_PLAYER) {
                sendFailure(player, "Too many queued car pack requests");
                return;
            }
            queue.addLast(packs);
            if (!ACTIVE_TRANSFERS.add(playerId)) return;
        }
        CompletableFuture.runAsync(() -> drain(player, playerId));
    }

    private static void drain(ServerPlayer player, UUID playerId) {
        boolean acquired = false;
        boolean completed = false;
        try {
            TRANSFER_SLOTS.acquire();
            acquired = true;
            while (true) {
                List<CarPackArchiveStore.TransferPack> packs;
                synchronized (QUEUED_TRANSFERS) {
                    ArrayDeque<List<CarPackArchiveStore.TransferPack>> queue = QUEUED_TRANSFERS.get(playerId);
                    packs = queue == null ? null : queue.pollFirst();
                    if (packs == null) {
                        QUEUED_TRANSFERS.remove(playerId);
                        ACTIVE_TRANSFERS.remove(playerId);
                        completed = true;
                        return;
                    }
                }
                for (CarPackArchiveStore.TransferPack pack : packs) sendPack(player, pack);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            sendFailure(player, "Car pack transfer was interrupted");
        } catch (Exception exception) {
            LOGGER.warn("Failed to send RIAutomobility car packs to {}", player.getGameProfile().getName(), exception);
            sendFailure(player, "Car pack transfer failed: " + exception.getMessage());
        } finally {
            if (acquired) TRANSFER_SLOTS.release();
            if (!completed) {
                synchronized (QUEUED_TRANSFERS) {
                    QUEUED_TRANSFERS.remove(playerId);
                    ACTIVE_TRANSFERS.remove(playerId);
                }
            }
        }
    }

    private static void sendPack(ServerPlayer player, CarPackArchiveStore.TransferPack pack) throws IOException {
        send(player, new CarPackTransferStartPacket(pack.manifest()));
        byte[] buffer = new byte[CarPackChunkPacket.MAX_CHUNK_SIZE];
        int index = 0;
        long sent = 0;
        long startedAt = System.nanoTime();
        try (InputStream input = Files.newInputStream(pack.archive())) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (!player.connection.connection.isConnected()) {
                    throw new IOException("Player disconnected during car pack transfer");
                }
                byte[] data = read == buffer.length ? buffer.clone() : Arrays.copyOf(buffer, read);
                send(player, new CarPackChunkPacket(pack.manifest().archiveDigest(), index++, data));
                sent += read;
                if (sent > pack.manifest().archiveSize()) {
                    throw new IOException("Car pack changed while it was being transferred");
                }
                throttle(sent, startedAt);
            }
        }
        if (sent != pack.manifest().archiveSize()) {
            throw new IOException("Car pack changed while it was being transferred");
        }
        send(player, new CarPackTransferCompletePacket(pack.manifest().archiveDigest()));
    }

    private static void throttle(long sent, long startedAt) {
        long expectedNanos = sent * 1_000_000_000L / BYTES_PER_SECOND;
        long remaining = expectedNanos - (System.nanoTime() - startedAt);
        if (remaining > 0) {
            LockSupport.parkNanos(remaining);
        }
    }

    private static void sendFailure(ServerPlayer player, String reason) {
        send(player, new CarPackTransferFailedPacket(reason == null ? "Unknown transfer error" : reason));
    }

    private static void send(ServerPlayer player, Object packet) {
        RIAutomobilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
