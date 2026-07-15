package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.carpack.CarPackManifestEntry;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CarPackPacketCodecTest {
    private static final String DIGEST = "a".repeat(64);

    @Test
    void manifestEntryRoundTrips() {
        CarPackManifestEntry expected = new CarPackManifestEntry(
                "riautomobility/example", "Example", DIGEST, "b".repeat(64), 12345,
                new ResourceLocation("example", "buggy")
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            expected.write(buffer);
            assertEquals(expected, CarPackManifestEntry.read(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void requestPacketRoundTrips() {
        RequestCarPacksPacket expected = new RequestCarPacksPacket(List.of(
                new RequestCarPacksPacket.Request("riautomobility/example", DIGEST)
        ));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            RequestCarPacksPacket.encode(expected, buffer);
            assertEquals(expected, RequestCarPacksPacket.decode(buffer));
        } finally {
            buffer.release();
        }

        assertThrows(IllegalArgumentException.class, () -> new RequestCarPacksPacket(List.of()));
    }

    @Test
    void chunkPacketRoundTripsAndEnforcesItsLimit() {
        byte[] data = new byte[] {1, 2, 3, 4};
        CarPackChunkPacket expected = new CarPackChunkPacket(DIGEST, 7, data);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            CarPackChunkPacket.encode(expected, buffer);
            CarPackChunkPacket decoded = CarPackChunkPacket.decode(buffer);
            assertEquals(expected.archiveDigest(), decoded.archiveDigest());
            assertEquals(expected.index(), decoded.index());
            assertArrayEquals(data, decoded.data());
        } finally {
            buffer.release();
        }

        assertThrows(IllegalArgumentException.class, () -> new CarPackChunkPacket(
                DIGEST,
                0,
                new byte[CarPackChunkPacket.MAX_CHUNK_SIZE + 1]
        ));

        byte[] maximum = new byte[CarPackChunkPacket.MAX_CHUNK_SIZE];
        FriendlyByteBuf maximumBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            CarPackChunkPacket.encode(new CarPackChunkPacket(DIGEST, Integer.MAX_VALUE, maximum), maximumBuffer);
            assertTrue(maximumBuffer.readableBytes() < 32767);
        } finally {
            maximumBuffer.release();
        }
    }

    @Test
    void uploadPacketsRoundTripAndEnforceChunkLimit() {
        UUID uploadId = UUID.randomUUID();
        BeginCarPackUploadPacket begin = new BeginCarPackUploadPacket(
                uploadId, "custom-car-frame", "custom", "car/frame", "frame", false, 4096, DIGEST);
        FriendlyByteBuf beginBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BeginCarPackUploadPacket.encode(begin, beginBuffer);
            assertEquals(begin, BeginCarPackUploadPacket.decode(beginBuffer));
        } finally {
            beginBuffer.release();
        }

        byte[] data = {9, 8, 7};
        CarPackUploadChunkPacket chunk = new CarPackUploadChunkPacket(uploadId, 2, data);
        FriendlyByteBuf chunkBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            CarPackUploadChunkPacket.encode(chunk, chunkBuffer);
            CarPackUploadChunkPacket decoded = CarPackUploadChunkPacket.decode(chunkBuffer);
            assertEquals(uploadId, decoded.uploadId());
            assertEquals(2, decoded.index());
            assertArrayEquals(data, decoded.data());
        } finally {
            chunkBuffer.release();
        }
        assertThrows(IllegalArgumentException.class, () -> new CarPackUploadChunkPacket(
                uploadId, 0, new byte[CarPackUploadChunkPacket.MAX_CHUNK_SIZE + 1]));

        FriendlyByteBuf maximumUploadBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            CarPackUploadChunkPacket.encode(new CarPackUploadChunkPacket(
                    uploadId, Integer.MAX_VALUE, new byte[CarPackUploadChunkPacket.MAX_CHUNK_SIZE]), maximumUploadBuffer);
            assertTrue(maximumUploadBuffer.readableBytes() < 32767);
        } finally {
            maximumUploadBuffer.release();
        }

        CompleteCarPackUploadPacket complete = new CompleteCarPackUploadPacket(uploadId);
        FriendlyByteBuf completeBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            CompleteCarPackUploadPacket.encode(complete, completeBuffer);
            assertEquals(complete, CompleteCarPackUploadPacket.decode(completeBuffer));
        } finally {
            completeBuffer.release();
        }

        CarPackUploadResultPacket result = new CarPackUploadResultPacket(uploadId, true, "installed");
        FriendlyByteBuf resultBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            CarPackUploadResultPacket.encode(result, resultBuffer);
            assertEquals(result, CarPackUploadResultPacket.decode(resultBuffer));
        } finally {
            resultBuffer.release();
        }
    }
}
