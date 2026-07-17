package com.sshakusora.riautomobility.network.packet.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VehicleHighlightClientHandlerTest {
    @AfterEach
    void clearHighlights() {
        VehicleHighlightClientHandler.clear();
    }

    @Test
    void periodicPurgeRemovesExpiredHighlightsWithoutEntityLookup() {
        UUID expired = UUID.randomUUID();
        UUID active = UUID.randomUUID();
        VehicleHighlightClientHandler.handle(expired, 10, 1_000L);
        VehicleHighlightClientHandler.handle(active, 20, 1_000L);

        VehicleHighlightClientHandler.purgeExpired(1_500L);

        assertFalse(VehicleHighlightClientHandler.shouldHighlight(expired, 1_500L));
        assertTrue(VehicleHighlightClientHandler.shouldHighlight(active, 1_500L));
        assertEquals(1, VehicleHighlightClientHandler.trackedHighlightCount());
    }

    @Test
    void disconnectCleanupRemovesAllHighlights() {
        VehicleHighlightClientHandler.handle(UUID.randomUUID(), 200, 1_000L);
        VehicleHighlightClientHandler.handle(UUID.randomUUID(), 200, 1_000L);

        VehicleHighlightClientHandler.clear();

        assertEquals(0, VehicleHighlightClientHandler.trackedHighlightCount());
    }
}
