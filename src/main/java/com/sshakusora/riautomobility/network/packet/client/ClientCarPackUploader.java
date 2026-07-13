package com.sshakusora.riautomobility.network.packet.client;

import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.editor.client.VehicleEditorDraft;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.BeginCarPackUploadPacket;
import com.sshakusora.riautomobility.network.packet.CarPackUploadChunkPacket;
import com.sshakusora.riautomobility.network.packet.CarPackUploadResultPacket;
import com.sshakusora.riautomobility.network.packet.CompleteCarPackUploadPacket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ClientCarPackUploader {
    private static final Map<UUID, Consumer<CarPackUploadResultPacket>> CALLBACKS = new ConcurrentHashMap<>();
    private ClientCarPackUploader() {}

    public static UUID upload(Path archive, VehicleEditorDraft draft, Consumer<CarPackUploadResultPacket> callback) throws IOException {
        UUID id = UUID.randomUUID();
        byte[] bytes = Files.readAllBytes(archive);
        CALLBACKS.put(id, callback);
        RIAutomobilityNetwork.CHANNEL.sendToServer(new BeginCarPackUploadPacket(id, draft.packName(), draft.namespace(),
                draft.componentPath(), draft.target.path, draft.overwrite, bytes.length, CarPackArchiveStore.sha256(archive)));
        int chunkSize = CarPackUploadChunkPacket.MAX_CHUNK_SIZE;
        for (int offset = 0, index = 0; offset < bytes.length; offset += chunkSize, index++) {
            int length = Math.min(chunkSize, bytes.length - offset);
            byte[] chunk = new byte[length];
            System.arraycopy(bytes, offset, chunk, 0, length);
            RIAutomobilityNetwork.CHANNEL.sendToServer(new CarPackUploadChunkPacket(id, index, chunk));
        }
        RIAutomobilityNetwork.CHANNEL.sendToServer(new CompleteCarPackUploadPacket(id));
        return id;
    }

    public static void acceptResult(CarPackUploadResultPacket result) {
        Consumer<CarPackUploadResultPacket> callback = CALLBACKS.remove(result.uploadId());
        if (callback != null) callback.accept(result);
    }
}
