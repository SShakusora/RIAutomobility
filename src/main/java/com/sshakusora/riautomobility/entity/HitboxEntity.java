package com.sshakusora.riautomobility.entity;

import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.util.RIAutomobileHitboxRegistry;
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
import java.util.Objects;

public class HitboxEntity extends Entity implements Container {
    public static final EntityDataAccessor<Integer> AUTOMOBILE = SynchedEntityData.defineId(HitboxEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Vector3f> ORIGIN = SynchedEntityData.defineId(HitboxEntity.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(HitboxEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(HitboxEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> HAS_CONTAINER = SynchedEntityData.defineId(HitboxEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int INVENTORY_SIZE = 54;
    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private EntityDimensions size;
    private boolean changed = false;

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
        } else {
            if (!RIAutomobileHitboxRegistry.getHitboxEntities(automobile).contains(this)) {
                RIAutomobileHitboxRegistry.addHitbox(automobile, this);
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
            if (!this.level().isClientSide()) {
                this.level().playSound(
                        null,
                        this.blockPosition(),
                        net.minecraft.sounds.SoundEvents.BARREL_OPEN,
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F,
                        1.0F
                        );
                player.openMenu(new SimpleMenuProvider(
                        (syncId, inv, p) -> ChestMenu.sixRows(syncId, inv, this),
                        Component.translatable("container.riautomobility.hitbox")
                ));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        var automobile = getAutomobile();
        if (automobile == null) return super.interact(player, hand);
        if (automobile.getPassengers().contains(player)) return InteractionResult.PASS;
        if (Objects.requireNonNull(automobile.getFirstPassenger()).isVehicle() && player == automobile.getFirstPassenger().getFirstPassenger()) return InteractionResult.PASS;

        return automobile.interact(player, hand);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide()) {
            if(this.getAutomobile() != null) {
                Containers.dropContents(this.level(), this.getAutomobile().blockPosition(), this);
            } else {
                Containers.dropContents(this.level(), this.blockPosition(), this);
            }
        }

        super.remove(reason);
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
        if(this.getAutomobile().getFirstPassenger() instanceof DriverSeatEntity seat) {
            Entity pa = seat.getFirstPassenger();
            if(pa == other) return false;
        }
        return !(other instanceof HitboxEntity) && !(other instanceof DriverSeatEntity)  && !(other instanceof AutomobileEntity) && Boat.canVehicleCollide(this, other);
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

    public void setHasContainer(boolean value) {
        this.entityData.set(HAS_CONTAINER, value);
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(items, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        this.changed = true;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.isAlive() && player.distanceTo(this) < 8.0;
    }

    @Override
    public void clearContent() {
        items.clear();
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
