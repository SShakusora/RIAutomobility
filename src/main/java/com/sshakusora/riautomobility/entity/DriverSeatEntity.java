package com.sshakusora.riautomobility.entity;

import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DriverSeatEntity extends Entity {
    public DriverSeatEntity(EntityType<?> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {}

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

    public void positionRider(Entity passenger, Entity.MoveFunction moveFunc){
        Vec3 pos = this.position();
        moveFunc.accept(passenger, pos.x, pos.y, pos.z);
    }

    public void tick(){
        Entity seat = this.getVehicle();
        if(seat instanceof AutomobileEntity) return;

        this.discard();
    }
}
