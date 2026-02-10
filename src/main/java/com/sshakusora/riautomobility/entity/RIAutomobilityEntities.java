package com.sshakusora.riautomobility.entity;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RIAutomobilityEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RIAutomobility.MODID);

    public static final RegistryObject<EntityType<RIAutomobileEntity>> RIAUTOMOBILE = ENTITIES.register(
            "riautomobile",
            () -> EntityType.Builder.<RIAutomobileEntity>of(RIAutomobileEntity::new, MobCategory.MISC)
                    .clientTrackingRange(20)
                    .sized(1F, 0.66F)
                    .build("riautomobile")
    );

    public static final RegistryObject<EntityType<SeatEntity>> SEAT = ENTITIES.register(
            "seat",
            () -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                    .clientTrackingRange(20)
                    .sized(0.0F, 0.0F)
                    .build("seat")
    );

    public static final RegistryObject<EntityType<HitboxEntity>> HITBOX = ENTITIES.register(
            "hitbox",
            () -> EntityType.Builder.<HitboxEntity>of(HitboxEntity::new, MobCategory.MISC)
                    .clientTrackingRange(20)
                    .sized(1.0F, 0.66F)
                    .build("hitbox")
    );
}
