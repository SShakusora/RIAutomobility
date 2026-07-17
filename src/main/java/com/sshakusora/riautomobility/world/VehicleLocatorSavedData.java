package com.sshakusora.riautomobility.world;

import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.*;

public final class VehicleLocatorSavedData extends SavedData {
    private static final String DATA_NAME = "riautomobility_vehicle_locations";
    private static final String VEHICLES_TAG = "Vehicles";
    private static final String DESTROYED_VEHICLES_TAG = "DestroyedVehicles";
    static final long DESTROYED_RETENTION_MILLIS = 30L * 24L * 60L * 60L * 1_000L;
    static final int MAX_DESTROYED_VEHICLES = 16_384;

    private final Map<UUID, Location> locations = new HashMap<>();
    private final Map<UUID, Long> destroyedVehicles = new LinkedHashMap<>();

    public static VehicleLocatorSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                VehicleLocatorSavedData::load,
                VehicleLocatorSavedData::new,
                DATA_NAME
        );
    }

    static VehicleLocatorSavedData load(CompoundTag tag) {
        VehicleLocatorSavedData data = new VehicleLocatorSavedData();
        ListTag vehicles = tag.getList(VEHICLES_TAG, Tag.TAG_COMPOUND);
        for (Tag rawEntry : vehicles) {
            CompoundTag entry = (CompoundTag) rawEntry;
            if (!entry.hasUUID("Id")) {
                continue;
            }
            ResourceLocation dimensionId = ResourceLocation.tryParse(entry.getString("Dimension"));
            if (dimensionId == null) {
                continue;
            }
            UUID id = entry.getUUID("Id");
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            data.locations.put(id, new Location(
                    dimension,
                    entry.getDouble("X"),
                    entry.getDouble("Y"),
                    entry.getDouble("Z"),
                    entry.contains("Name", Tag.TAG_STRING) ? entry.getString("Name") : null
            ));
        }
        long now = System.currentTimeMillis();
        boolean migratedDestroyedVehicle = false;
        ListTag destroyedVehiclesTag = tag.getList(DESTROYED_VEHICLES_TAG, Tag.TAG_COMPOUND);
        for (Tag rawEntry : destroyedVehiclesTag) {
            CompoundTag entry = (CompoundTag) rawEntry;
            if (entry.hasUUID("Id")) {
                boolean hasDestroyedAt = entry.contains("DestroyedAt", Tag.TAG_LONG);
                data.destroyedVehicles.put(
                        entry.getUUID("Id"),
                        hasDestroyedAt ? entry.getLong("DestroyedAt") : now
                );
                migratedDestroyedVehicle |= !hasDestroyedAt;
            }
        }
        if (data.pruneDestroyedVehicles(now) || migratedDestroyedVehicle) {
            data.setDirty();
        }
        return data;
    }

    public void update(RIAutomobileEntity automobile) {
        if (!(automobile.level() instanceof ServerLevel serverLevel) || !automobile.isKeyed()) {
            return;
        }
        Location next = new Location(
                serverLevel.dimension(),
                automobile.getX(),
                automobile.getY(),
                automobile.getZ(),
                automobile.hasCustomName() ? automobile.getCustomName().getString() : null
        );
        update(automobile.getUUID(), next);
    }

    void update(UUID automobileId, Location next) {
        boolean changed = destroyedVehicles.remove(automobileId) != null;
        if (!next.equals(locations.put(automobileId, next)) || changed) {
            setDirty();
        }
    }

    public void removeLocation(UUID automobileId) {
        boolean changed = locations.remove(automobileId) != null;
        if (destroyedVehicles.remove(automobileId) != null || changed) {
            setDirty();
        }
    }

    public void markDestroyed(UUID automobileId) {
        markDestroyed(automobileId, System.currentTimeMillis());
    }

    void markDestroyed(UUID automobileId, long now) {
        boolean changed = locations.remove(automobileId) != null;
        Long previousDestroyedAt = destroyedVehicles.remove(automobileId);
        destroyedVehicles.put(automobileId, now);
        changed |= !Objects.equals(previousDestroyedAt, now);
        changed |= pruneDestroyedVehicles(now);
        if (changed) {
            setDirty();
        }
    }

    public boolean isDestroyed(UUID automobileId) {
        return isDestroyed(automobileId, System.currentTimeMillis());
    }

    boolean isDestroyed(UUID automobileId, long now) {
        if (pruneDestroyedVehicles(now)) {
            setDirty();
        }
        return destroyedVehicles.containsKey(automobileId);
    }

    private boolean pruneDestroyedVehicles(long now) {
        boolean changed = destroyedVehicles.entrySet().removeIf(entry ->
                entry.getValue() <= now - DESTROYED_RETENTION_MILLIS
        );
        Iterator<UUID> oldestFirst = destroyedVehicles.keySet().iterator();
        while (destroyedVehicles.size() > MAX_DESTROYED_VEHICLES && oldestFirst.hasNext()) {
            oldestFirst.next();
            oldestFirst.remove();
            changed = true;
        }
        return changed;
    }

    @Nullable
    public Location find(UUID automobileId) {
        return locations.get(automobileId);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        pruneDestroyedVehicles(System.currentTimeMillis());
        ListTag vehicles = new ListTag();
        locations.forEach((id, location) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", id);
            entry.putString("Dimension", location.dimension().location().toString());
            entry.putDouble("X", location.x());
            entry.putDouble("Y", location.y());
            entry.putDouble("Z", location.z());
            if (location.name() != null) {
                entry.putString("Name", location.name());
            }
            vehicles.add(entry);
        });
        tag.put(VEHICLES_TAG, vehicles);

        ListTag destroyedVehiclesTag = new ListTag();
        this.destroyedVehicles.forEach((id, destroyedAt) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", id);
            entry.putLong("DestroyedAt", destroyedAt);
            destroyedVehiclesTag.add(entry);
        });
        tag.put(DESTROYED_VEHICLES_TAG, destroyedVehiclesTag);
        return tag;
    }

    public record Location(ResourceKey<Level> dimension, double x, double y, double z,
                           @Nullable String name) {
    }
}
