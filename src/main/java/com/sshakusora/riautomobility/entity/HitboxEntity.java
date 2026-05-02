package com.sshakusora.riautomobility.entity;

import com.sshakusora.riautomobility.definition.RIAutomobileDefinition;
import com.sshakusora.riautomobility.util.RIAutomobileTransformUtil;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
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

    public HitboxEntity(Level level, AutomobileEntity automobile, RIAutomobileDefinition.Hitbox hitbox) {
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
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) { }

    public void syncPosition(AutomobileEntity auto) {
        Vec3 pos = this.boxOrigin();
        pos = localPosToWorldSpace(auto, pos);

        double nx = pos.x();
        double ny = pos.y();
        double nz = pos.z();
        if (nx != this.getX() || ny != this.getY() || nz != this.getZ()) {
            this.xOld = this.getX();
            this.yOld = this.getY();
            this.zOld = this.getZ();
            this.setPos(nx, ny, nz);
        }
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

        syncPosition(automobile);
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
        if (other == this) {
            return false;
        }

        AutomobileEntity auto = this.getAutomobile();
        if (auto == null) {
            return false;
        }

        if (other == auto || auto.hasPassenger(other)) {
            return false;
        }

        if (other instanceof HitboxEntity hitbox && hitbox.getAutomobile() == auto) {
            return false;
        }

        return true;
    }


    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

    @Override
    protected AABB makeBoundingBox() {
        EntityDimensions dimensions = this.size != null ? this.size : EntityDimensions.scalable(1.0f, 0.66f);
        AABB box = dimensions.makeBoundingBox(this.getX(), this.getY(), this.getZ());
        double maxInset = Math.min(box.getXsize(), box.getZsize()) * 0.35D - 1.0E-4D;
        double inset = Math.min(0.125D, maxInset);

        if (inset <= 0.0D) {
            return box;
        }

        return new AABB(
                box.minX + inset,
                box.minY,
                box.minZ + inset,
                box.maxX - inset,
                box.maxY,
                box.maxZ - inset
        );
    }

    @Override
    public boolean canBeCollidedWith() {
        return !this.isRemoved() && this.getAutomobile() != null;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public void push(double x, double y, double z) {
        AutomobileEntity auto = this.getAutomobile();
        if (auto != null) {
            auto.push(x, y, z);
        }
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


    private Vec3 localPosToWorldSpace(AutomobileEntity auto, Vec3 position) {
        return RIAutomobileTransformUtil.localPosToWorldSpace(auto, position);
    }
}
