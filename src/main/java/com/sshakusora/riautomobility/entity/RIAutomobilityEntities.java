package com.sshakusora.riautomobility.entity;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RIAutomobilityEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RIAutomobility.MODID);

    public static final RegistryObject<EntityType<DriverSeatEntity>> DRIVER_SEAT = ENTITIES.register(
            "driver_seat",
            () -> EntityType.Builder.<DriverSeatEntity>of(DriverSeatEntity::new, MobCategory.MISC)
                    .clientTrackingRange(20)
                    .sized(0.0F, 0.0F)
                    .build("driver_seat")
    );
}
