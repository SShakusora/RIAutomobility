package com.sshakusora.riautomobility.network.packet.client;

import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.editor.client.VehicleEditorDraft;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.BeginCarPackUploadPacket;
import com.sshakusora.riautomobility.network.packet.CarPackUploadChunkPacket;
import com.sshakusora.riautomobility.network.packet.CarPackUploadResultPacket;
import com.sshakusora.riautomobility.network.packet.CompleteCarPackUploadPacket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ClientCarPackUploader {
    private static final Map<UUID, Consumer<CarPackUploadResultPacket>> CALLBACKS = new ConcurrentHashMap<>();
    private ClientCarPackUploader() {}

    public static UUID upload(Path archive, VehicleEditorDraft draft, Consumer<CarPackUploadResultPacket> callback) throws IOException {
        UUID id = UUID.randomUUID();
        long archiveSize = Files.size(archive);
        if (archiveSize > Integer.MAX_VALUE) {
            throw new IOException("Car pack is too large to upload");
        }
        CALLBACKS.put(id, callback);
        try {
            RIAutomobilityNetwork.CHANNEL.sendToServer(new BeginCarPackUploadPacket(id, draft.packName(), draft.namespace(),
                    draft.componentPath(), draft.target.path, draft.overwrite, archiveSize, CarPackArchiveStore.sha256(archive)));
            byte[] buffer = new byte[CarPackUploadChunkPacket.MAX_CHUNK_SIZE];
            try (InputStream input = Files.newInputStream(archive)) {
                int index = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    byte[] chunk = read == buffer.length ? buffer.clone() : Arrays.copyOf(buffer, read);
                    RIAutomobilityNetwork.CHANNEL.sendToServer(new CarPackUploadChunkPacket(id, index++, chunk));
                }
            }
            RIAutomobilityNetwork.CHANNEL.sendToServer(new CompleteCarPackUploadPacket(id));
        } catch (IOException | RuntimeException exception) {
            CALLBACKS.remove(id);
            throw exception;
        }
        return id;
    }

    public static void acceptResult(CarPackUploadResultPacket result) {
        Consumer<CarPackUploadResultPacket> callback = CALLBACKS.remove(result.uploadId());
        if (callback != null) callback.accept(result);
    }
}
