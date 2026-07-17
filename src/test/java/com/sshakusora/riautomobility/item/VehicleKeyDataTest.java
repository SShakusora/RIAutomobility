package com.sshakusora.riautomobility.item;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VehicleKeyDataTest {
    @Test
    void blankDataCanBeBoundAndCleared() {
        CompoundTag tag = new CompoundTag();
        UUID vehicleId = UUID.randomUUID();

        assertTrue(VehicleKeyData.getVehicleId(tag).isEmpty());
        VehicleKeyData.bind(tag, vehicleId);
        VehicleKeyData.setVehicleName(tag, "Roadster");

        assertEquals(vehicleId, VehicleKeyData.getVehicleId(tag).orElseThrow());
        assertEquals("Roadster", VehicleKeyData.getVehicleName(tag).orElseThrow());

        VehicleKeyData.clear(tag);
        assertTrue(VehicleKeyData.getVehicleId(tag).isEmpty());
        assertTrue(VehicleKeyData.getVehicleName(tag).isEmpty());
    }

    @Test
    void clearingBindingPreservesOtherData() {
        CompoundTag tag = new CompoundTag();
        tag.putString("OwnerNote", "keep");
        VehicleKeyData.bind(tag, UUID.randomUUID());

        VehicleKeyData.clear(tag);

        assertEquals("keep", tag.getString("OwnerNote"));
    }

    @Test
    void copiedBindingOnlyMatchesItsVehicleId() {
        UUID vehicleId = UUID.randomUUID();
        CompoundTag tag = new CompoundTag();
        VehicleKeyData.bind(tag, vehicleId);

        assertEquals(vehicleId, VehicleKeyData.getVehicleId(tag).orElseThrow());
        assertNotEquals(UUID.randomUUID(), VehicleKeyData.getVehicleId(tag).orElseThrow());
    }

    @Test
    void clearingVehicleNamePreservesBinding() {
        UUID vehicleId = UUID.randomUUID();
        CompoundTag tag = new CompoundTag();
        VehicleKeyData.bind(tag, vehicleId);
        VehicleKeyData.setVehicleName(tag, "Roadster");

        VehicleKeyData.setVehicleName(tag, null);

        assertEquals(vehicleId, VehicleKeyData.getVehicleId(tag).orElseThrow());
        assertTrue(VehicleKeyData.getVehicleName(tag).isEmpty());
    }
}
