package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.carpack.CarPackManifestEntry;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CarPackPacketCodecTest {
    private static final String DIGEST = "a".repeat(64);

    @Test
    void manifestEntryRoundTrips() {
        CarPackManifestEntry expected = new CarPackManifestEntry(
                "riautomobility/example", "Example", DIGEST, "b".repeat(64), 12345
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
    }
}
