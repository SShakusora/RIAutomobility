package com.sshakusora.riautomobility.entity;

import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SeatEntity extends Entity {
    public static final EntityDataAccessor<Integer> AUTOMOBILE = SynchedEntityData.defineId(SeatEntity.class, EntityDataSerializers.INT);

    public SeatEntity(Level level, AutomobileEntity automobile) {
        super(RIAutomobilityEntities.SEAT.get(), level);
        this.entityData.set(AUTOMOBILE, automobile.getId());
        this.noPhysics = true;
    }

    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(AUTOMOBILE, -1);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {}

    @Override
    public boolean isInvisible() {
        return true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(this.position(), this.position());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        AutomobileEntity automobile = getAutomobile();
        return automobile.getDismountLocationForPassenger(passenger);
    }

    public void positionRider(Entity passenger, Entity.MoveFunction moveFunc){
        Vec3 pos = this.position();
        moveFunc.accept(passenger, pos.x, pos.y -0.35, pos.z);
    }

    public void tick(){
        AutomobileEntity automobile = getAutomobile();

        if (automobile == null) {
            if (!this.level().isClientSide()) this.discard();
            return;
        }

        if (!this.level().isClientSide()) {
            if (!automobile.isAlive()) this.discard();
        }
    }

    public AutomobileEntity getAutomobile() {
        Entity entity = this.level().getEntity(this.entityData.get(AUTOMOBILE));
        if (entity instanceof AutomobileEntity auto) return auto;
        return null;
    }
}