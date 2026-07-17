package com.sshakusora.riautomobility.item;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

import java.util.Optional;
import java.util.UUID;

final class VehicleKeyData {
    private static final String VEHICLE_ID_TAG = "VehicleId";
    private static final String VEHICLE_NAME_TAG = "VehicleName";

    private VehicleKeyData() {
    }

    static Optional<UUID> getVehicleId(CompoundTag tag) {
        return tag.hasUUID(VEHICLE_ID_TAG)
                ? Optional.of(tag.getUUID(VEHICLE_ID_TAG))
                : Optional.empty();
    }

    static void bind(CompoundTag tag, UUID automobileId) {
        tag.putUUID(VEHICLE_ID_TAG, automobileId);
    }

    static Optional<String> getVehicleName(CompoundTag tag) {
        String name = tag.getString(VEHICLE_NAME_TAG);
        return name.isEmpty() ? Optional.empty() : Optional.of(name);
    }

    static void setVehicleName(CompoundTag tag, @Nullable String name) {
        if (name == null || name.isEmpty()) {
            tag.remove(VEHICLE_NAME_TAG);
        } else {
            tag.putString(VEHICLE_NAME_TAG, name);
        }
    }

    static void clear(CompoundTag tag) {
        tag.remove(VEHICLE_ID_TAG);
        tag.remove(VEHICLE_NAME_TAG);
    }
}
