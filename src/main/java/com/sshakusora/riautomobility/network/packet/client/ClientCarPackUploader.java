package com.sshakusora.riautomobility.network.packet.client;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.editor.client.VehiclePackBuilder;
import com.sshakusora.riautomobility.editor.upload.CarPackUploadService;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.BeginCarPackUploadPacket;
import com.sshakusora.riautomobility.network.packet.CarPackUploadChunkPacket;
import com.sshakusora.riautomobility.network.packet.CarPackUploadResultPacket;
import com.sshakusora.riautomobility.network.packet.CompleteCarPackUploadPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID, value = Dist.CLIENT)
public final class ClientCarPackUploader {
    private static final Map<UUID, PendingUpload> CALLBACKS = new ConcurrentHashMap<>();
    private static final long BYTES_PER_SECOND = 16L * 1024L * 1024L;
    private static final long UPLOAD_TIMEOUT_SECONDS = 90L;
    private static final ExecutorService UPLOAD_IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "RIAutomobility car pack uploader");
        thread.setDaemon(true);
        return thread;
    });
    private static final ScheduledThreadPoolExecutor TIMEOUTS = createTimeoutExecutor();

    private ClientCarPackUploader() {
    }

    private static ScheduledThreadPoolExecutor createTimeoutExecutor() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "RIAutomobility car pack upload timeout");
            thread.setDaemon(true);
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    public static void upload(Path archive, VehiclePackBuilder.ExportRequest request,
                              Consumer<CarPackUploadResultPacket> callback) {
        UUID id = UUID.randomUUID();
        PendingUpload pending = new PendingUpload(callback);
        CALLBACKS.put(id, pending);
        pending.timeout = TIMEOUTS.schedule(
                () -> fail(id, new IOException("Car pack upload timed out")),
                UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        UPLOAD_IO.execute(() -> upload(id, archive, request));
    }

    private static void upload(UUID id, Path archive, VehiclePackBuilder.ExportRequest request) {
        try {
            long archiveSize = Files.size(archive);
            if (archiveSize <= 0 || archiveSize > CarPackUploadService.MAX_UPLOAD_SIZE) {
                throw new IOException("Car pack is too large to upload");
            }
            RIAutomobilityNetwork.CHANNEL.sendToServer(new BeginCarPackUploadPacket(
                    id, request.packName(), request.namespace(), request.componentPath(), request.target().path,
                    request.overwrite(), archiveSize, CarPackArchiveStore.sha256(archive)));
            byte[] buffer = new byte[CarPackUploadChunkPacket.MAX_CHUNK_SIZE];
            try (InputStream input = Files.newInputStream(archive)) {
                int index = 0;
                int read;
                long sent = 0;
                long started = System.nanoTime();
                while ((read = input.read(buffer)) != -1) {
                    byte[] chunk = read == buffer.length ? buffer.clone() : Arrays.copyOf(buffer, read);
                    RIAutomobilityNetwork.CHANNEL.sendToServer(new CarPackUploadChunkPacket(id, index++, chunk));
                    sent += read;
                    long expectedElapsed = sent * 1_000_000_000L / BYTES_PER_SECOND;
                    long waitNanos = expectedElapsed - (System.nanoTime() - started);
                    if (waitNanos > 0) LockSupport.parkNanos(waitNanos);
                }
            }
            RIAutomobilityNetwork.CHANNEL.sendToServer(new CompleteCarPackUploadPacket(id));
        } catch (IOException | RuntimeException exception) {
            fail(id, exception);
        }
    }

    public static void acceptResult(CarPackUploadResultPacket result) {
        PendingUpload pending = take(result.uploadId());
        if (pending != null) pending.callback.accept(result);
    }

    private static void fail(UUID id, Throwable error) {
        PendingUpload pending = take(id);
        if (pending == null) return;
        String detail = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        if (detail.length() > 512) detail = detail.substring(0, 512);
        String failure = detail;
        Minecraft.getInstance().execute(() -> pending.callback.accept(new CarPackUploadResultPacket(id, false, failure)));
    }

    private static PendingUpload take(UUID id) {
        PendingUpload pending = CALLBACKS.remove(id);
        if (pending != null) pending.cancelTimeout();
        return pending;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        IOException failure = new IOException("Disconnected before the car pack upload completed");
        CALLBACKS.keySet().forEach(id -> fail(id, failure));
    }

    private static final class PendingUpload {
        private final Consumer<CarPackUploadResultPacket> callback;
        private volatile ScheduledFuture<?> timeout;

        private PendingUpload(Consumer<CarPackUploadResultPacket> callback) {
            this.callback = callback;
        }

        private void cancelTimeout() {
            ScheduledFuture<?> scheduled = this.timeout;
            if (scheduled != null) scheduled.cancel(false);
        }
    }
}
