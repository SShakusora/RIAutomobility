package com.sshakusora.riautomobility.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VehicleLocatorSavedDataTest {
    @Test
    void locationsSurviveSavedDataRoundTrip() {
        UUID automobileId = UUID.randomUUID();
        VehicleLocatorSavedData data = new VehicleLocatorSavedData();
        VehicleLocatorSavedData.Location expected = new VehicleLocatorSavedData.Location(
                Level.NETHER,
                12.5D,
                64.0D,
                -9.25D,
                "Roadster"
        );
        data.update(automobileId, expected);

        CompoundTag saved = data.save(new CompoundTag());
        VehicleLocatorSavedData restored = VehicleLocatorSavedData.load(saved);

        assertEquals(expected, restored.find(automobileId));
    }

    @Test
    void removingLocationDoesNotMarkVehicleAsDestroyed() {
        UUID automobileId = UUID.randomUUID();
        VehicleLocatorSavedData data = new VehicleLocatorSavedData();
        data.update(automobileId, new VehicleLocatorSavedData.Location(
                Level.OVERWORLD, 0.0D, 70.0D, 0.0D, null
        ));

        data.removeLocation(automobileId);

        assertNull(data.find(automobileId));
        assertFalse(data.isDestroyed(automobileId));
    }

    @Test
    void destroyedVehicleMarkerSurvivesSavedDataRoundTrip() {
        UUID automobileId = UUID.randomUUID();
        VehicleLocatorSavedData data = new VehicleLocatorSavedData();
        data.update(automobileId, new VehicleLocatorSavedData.Location(
                Level.OVERWORLD, 4.0D, 70.0D, 8.0D, "Roadster"
        ));

        data.markDestroyed(automobileId);

        CompoundTag saved = data.save(new CompoundTag());
        VehicleLocatorSavedData restored = VehicleLocatorSavedData.load(saved);
        assertNull(restored.find(automobileId));
        assertTrue(restored.isDestroyed(automobileId));
    }

    @Test
    void locationUpdateClearsStaleDestroyedMarker() {
        UUID automobileId = UUID.randomUUID();
        VehicleLocatorSavedData data = new VehicleLocatorSavedData();
        VehicleLocatorSavedData.Location location = new VehicleLocatorSavedData.Location(
                Level.NETHER, 1.0D, 2.0D, 3.0D, null
        );
        data.markDestroyed(automobileId);

        data.update(automobileId, location);

        assertEquals(location, data.find(automobileId));
        assertFalse(data.isDestroyed(automobileId));
    }

    @Test
    void destroyedMarkerExpiresAfterRetentionPeriod() {
        UUID automobileId = UUID.randomUUID();
        VehicleLocatorSavedData data = new VehicleLocatorSavedData();
        long destroyedAt = 1_000L;
        data.markDestroyed(automobileId, destroyedAt);

        assertTrue(data.isDestroyed(
                automobileId,
                destroyedAt + VehicleLocatorSavedData.DESTROYED_RETENTION_MILLIS - 1L
        ));
        assertFalse(data.isDestroyed(
                automobileId,
                destroyedAt + VehicleLocatorSavedData.DESTROYED_RETENTION_MILLIS
        ));
    }

    @Test
    void destroyedMarkerHistoryIsCapacityBounded() {
        VehicleLocatorSavedData data = new VehicleLocatorSavedData();
        long now = System.currentTimeMillis();
        UUID oldest = new UUID(0L, 0L);
        data.markDestroyed(oldest, now);
        UUID newest = oldest;
        for (int i = 1; i <= VehicleLocatorSavedData.MAX_DESTROYED_VEHICLES; i++) {
            newest = new UUID(0L, i);
            data.markDestroyed(newest, now + i);
        }

        assertFalse(data.isDestroyed(oldest, now + VehicleLocatorSavedData.MAX_DESTROYED_VEHICLES));
        assertTrue(data.isDestroyed(newest, now + VehicleLocatorSavedData.MAX_DESTROYED_VEHICLES));
        ListTag savedMarkers = data.save(new CompoundTag())
                .getList("DestroyedVehicles", Tag.TAG_COMPOUND);
        assertEquals(VehicleLocatorSavedData.MAX_DESTROYED_VEHICLES, savedMarkers.size());
    }

    @Test
    void legacyDestroyedMarkerWithoutTimestampIsMigrated() {
        UUID automobileId = UUID.randomUUID();
        CompoundTag legacyData = new CompoundTag();
        ListTag destroyedVehicles = new ListTag();
        CompoundTag destroyedVehicle = new CompoundTag();
        destroyedVehicle.putUUID("Id", automobileId);
        destroyedVehicles.add(destroyedVehicle);
        legacyData.put("DestroyedVehicles", destroyedVehicles);

        VehicleLocatorSavedData restored = VehicleLocatorSavedData.load(legacyData);

        assertTrue(restored.isDestroyed(automobileId));
        CompoundTag migratedEntry = restored.save(new CompoundTag())
                .getList("DestroyedVehicles", Tag.TAG_COMPOUND)
                .getCompound(0);
        assertTrue(migratedEntry.contains("DestroyedAt", Tag.TAG_LONG));
    }
}
