package com.sshakusora.riautomobility.entity;

import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class HitboxEntity extends Entity{
    public static final EntityDataAccessor<Integer> AUTOMOBILE = SynchedEntityData.defineId(HitboxEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Vector3f> ORIGIN = SynchedEntityData.defineId(HitboxEntity.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(HitboxEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(HitboxEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> HAS_CONTAINER = SynchedEntityData.defineId(HitboxEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int INVENTORY_SIZE = 54;
    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private EntityDimensions size;

    public HitboxEntity(Level level, AutomobileEntity automobile, RIAutomobileFrame.Hitbox hitbox) {
        super(RIAutomobilityEntities.HITBOX.get(), level);

        this.entityData.set(AUTOMOBILE, automobile.getId());
        this.entityData.set(ORIGIN, new Vector3f((float) hitbox.origin().x(), (float) hitbox.origin().y(), (float) hitbox.origin().z()));
        this.entityData.set(WIDTH, hitbox.width());
        this.entityData.set(HEIGHT, hitbox.height());
        this.entityData.set(HAS_CONTAINER, hitbox.hasContainer());

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
        AutomobileEntity automobile = getAutomobile();

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
        if (this.hasContainer()) {
            if (!this.level().isClientSide() && this.entityData.get(HAS_CONTAINER)) {
                this.level().playSound(
                        null,
                        this.blockPosition(),
                        net.minecraft.sounds.SoundEvents.BARREL_OPEN,
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F,
                        1.0F
                        );
                player.openMenu(new SimpleMenuProvider(
                        (syncId, inv, p) -> ChestMenu.sixRows(syncId, inv, (Container) this.getAutomobile()),
                        Component.translatable("container.riautomobility.hitbox")
                ));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        var automobile = getAutomobile();
        if (automobile == null) return super.interact(player, hand);
        if (automobile.getPassengers().contains(player)) return InteractionResult.PASS;

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
        if (other == this) return false;
        AutomobileEntity auto = this.getAutomobile();
        if (auto == null) return false;
        if (other == auto) return false;
        if (other instanceof HitboxEntity hitbox) {
            if (hitbox.getAutomobile() == auto) {
                return false;
            }
        }
        if (auto.hasPassenger(other)) {
            return false;
        }
        Entity firstPassenger = auto.getFirstPassenger();
        if (firstPassenger instanceof SeatEntity seat) {
            if (other == seat) {
                return false;
            }
            if (seat.hasPassenger(other)) {
                return false;
            }
        }
        return Boat.canVehicleCollide(this, other);
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
        this.entityData.define(HAS_CONTAINER, false);

    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> dataAccessor) {
        super.onSyncedDataUpdated(dataAccessor);

        if (WIDTH.equals(dataAccessor) || HEIGHT.equals(dataAccessor)) {
            this.size = EntityDimensions.scalable(this.entityData.get(WIDTH), this.entityData.get(HEIGHT));
            this.refreshDimensions();
        }
    }

    public boolean hasContainer() {
        return this.entityData.get(HAS_CONTAINER);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        ContainerHelper.loadAllItems(tag, items);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        ContainerHelper.saveAllItems(tag, items);
    }

//    @Override
//    public Vec3 getPosition(float partialTicks) {
//        AutomobileEntity auto = this.getAutomobile();
//        if (auto == null) return super.getPosition(partialTicks);
//
//        double x = Mth.lerp(partialTicks, auto.xo, auto.getX());
//        double y = Mth.lerp(partialTicks, auto.yo, auto.getY());
//        double z = Mth.lerp(partialTicks, auto.zo, auto.getZ());
//
//        float yaw = Mth.lerp(partialTicks, auto.yRotO, auto.getYRot());
//
//        float vert = auto.getDisplacement().getVertical(partialTicks);
//
//        float pitch = auto.getDisplacement().getAngularX(partialTicks);
//        float roll = auto.getDisplacement().getAngularZ(partialTicks);
//
//        Vec3 origin = this.boxOrigin();
//        Vec3 rotatedPos = origin
//                .yRot(-yaw * Mth.DEG_TO_RAD)
//                .xRot(-pitch * Mth.DEG_TO_RAD)
//                .zRot(-roll * Mth.DEG_TO_RAD);
//
//        return new Vec3(x, y + vert, z).add(rotatedPos);
//    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return super.shouldRenderAtSqrDistance(distance);
    }

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
