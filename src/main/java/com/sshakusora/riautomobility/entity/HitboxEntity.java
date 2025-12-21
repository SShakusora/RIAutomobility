package com.sshakusora.riautomobility.entity;

import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class HitboxEntity extends Entity {
    public static final EntityDataAccessor<Integer> AUTOMOBILE = SynchedEntityData.defineId(HitboxEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Vector3f> ORIGIN = SynchedEntityData.defineId(HitboxEntity.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(HitboxEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(HitboxEntity.class, EntityDataSerializers.FLOAT);

    private EntityDimensions size;

    public HitboxEntity(Level level, AutomobileEntity automobile, RIAutomobileFrame.Hitbox hitbox) {
        super(RIAutomobilityEntities.HITBOX.get(), level);

        this.entityData.set(AUTOMOBILE, automobile.getId());
        this.entityData.set(ORIGIN, new Vector3f((float) hitbox.origin().x(), (float) hitbox.origin().y(), (float) hitbox.origin().z()));
        this.entityData.set(WIDTH, hitbox.width());
        this.entityData.set(HEIGHT, hitbox.height());

        this.size = EntityDimensions.scalable(hitbox.width(), hitbox.height());
    }

    public HitboxEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.size = EntityDimensions.scalable(1.0f, 0.66f);
    }

    public AutomobileEntity getAutomobile() {
        Entity entity = this.level().getEntity(this.entityData.get(AUTOMOBILE));
        if (entity instanceof AutomobileEntity auto) return auto;
        return null;
    }

    public Vec3 boxOrigin() {
        var o = this.entityData.get(ORIGIN);
        return new Vec3(o.x(), o.y(), o.z());
    }

    @Override
    public void tick() {
        var automobile = getAutomobile();

        if (automobile == null) {
            if (!this.level().isClientSide()) this.discard();
            return;
        }

        if (!this.level().isClientSide()) {
            if (!automobile.isAlive()) {
                this.discard();
                return;
            }
        }

        var pos = this.boxOrigin();
        pos = localPosToWorldSpace(automobile, pos);

        this.setPos(pos.x(), pos.y(), pos.z());
        super.tick();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        var automobile = getAutomobile();
        if (automobile == null) return super.interact(player, hand);
        if(automobile.getPassengers().contains(player)) return InteractionResult.PASS;

        return automobile.interact(player, hand);
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        var automobile = getAutomobile();
        if (automobile == null) return super.getPickResult();

        return automobile.asPrefabItem();
    }

    @Override
    public Component getName() {
        var auto = getAutomobile();
        if (auto != null) return auto.getName();

        return super.getName();
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return !(other instanceof AutomobileEntity) && Boat.canVehicleCollide(this, other);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

    @Override
    public boolean canBeCollidedWith() {
        return this.level().isClientSide();
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(AUTOMOBILE, -1);
        this.entityData.define(ORIGIN, new Vector3f());
        this.entityData.define(WIDTH, 1.0f);
        this.entityData.define(HEIGHT, 0.66f);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> dataAccessor) {
        super.onSyncedDataUpdated(dataAccessor);

        if (WIDTH.equals(dataAccessor) || HEIGHT.equals(dataAccessor)) {
            this.size = EntityDimensions.scalable(this.entityData.get(WIDTH), this.entityData.get(HEIGHT));
            this.refreshDimensions();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
    }

//    @Override
//    public Container underlyingContainer() {
//        var auto = automobile();
//        if (auto != null) {
//            return auto.underlyingContainer();
//        }
//
//        return null;
//    }

    private Vec3 localPosToWorldSpace(AutomobileEntity auto, Vec3 position) {
        float pitch = auto.getDisplacement().getAngularX(1.0F);
        float roll = auto.getDisplacement().getAngularZ(1.0F);
        float vert = auto.getDisplacement().getVertical(1.0F);

        return auto.position()
                .add(0.0F, vert, 0.0F)
                .add((new Vec3(position.x, position.y, position.z))
                        .yRot(-auto.getYRot() * Mth.DEG_TO_RAD)
                        .xRot(-pitch * Mth.DEG_TO_RAD)
                        .zRot(-roll * Mth.DEG_TO_RAD));
    }
}
