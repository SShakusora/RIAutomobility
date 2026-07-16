package com.sshakusora.riautomobility.network.packet.client;

import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.editor.client.VehiclePackBuilder;
import com.sshakusora.riautomobility.editor.upload.CarPackUploadService;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.BeginCarPackUploadPacket;
import com.sshakusora.riautomobility.network.packet.CarPackUploadChunkPacket;
import com.sshakusora.riautomobility.network.packet.CarPackUploadResultPacket;
import com.sshakusora.riautomobility.network.packet.CompleteCarPackUploadPacket;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

public final class ClientCarPackUploader {
    private static final Map<UUID, Consumer<CarPackUploadResultPacket>> CALLBACKS = new ConcurrentHashMap<>();
    private static final long BYTES_PER_SECOND = 16L * 1024L * 1024L;
    private static final ExecutorService UPLOAD_IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "RIAutomobility car pack uploader");
        thread.setDaemon(true);
        return thread;
    });

    private ClientCarPackUploader() {
    }

    public static void upload(Path archive, VehiclePackBuilder.ExportRequest request,
                              Consumer<CarPackUploadResultPacket> callback) {
        UUID id = UUID.randomUUID();
        CALLBACKS.put(id, callback);
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
        Consumer<CarPackUploadResultPacket> callback = CALLBACKS.remove(result.uploadId());
        if (callback != null) callback.accept(result);
    }

    private static void fail(UUID id, Throwable error) {
        Consumer<CarPackUploadResultPacket> callback = CALLBACKS.remove(id);
        if (callback == null) return;
        String detail = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        if (detail.length() > 512) detail = detail.substring(0, 512);
        String failure = detail;
        Minecraft.getInstance().execute(() -> callback.accept(new CarPackUploadResultPacket(id, false, failure)));
    }
}
