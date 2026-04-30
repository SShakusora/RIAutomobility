package com.sshakusora.riautomobility.entity;

import com.sshakusora.riautomobility.definition.RIAutomobileDefinition;
import com.sshakusora.riautomobility.definition.RIAutomobileRegistry;
import com.sshakusora.riautomobility.mixin.accessor.AutomobileEntityAccessor;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import io.github.foundationgames.automobility.entity.AutomobilityEntities;
import io.github.foundationgames.automobility.item.AutomobileInteractable;
import io.github.foundationgames.automobility.item.AutomobilityItems;
import io.github.foundationgames.automobility.sound.AutomobilitySounds;
import io.github.foundationgames.automobility.util.duck.CollisionArea;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RIAutomobileEntity extends AutomobileEntity implements Container {
    private static final int INVENTORY_SIZE = 54;
    private static final int MAX_TRACKED_SEATS = 8;
    private static final List<EntityDataAccessor<Integer>> TRACKED_SEATS = List.of(
            SynchedEntityData.defineId(RIAutomobileEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(RIAutomobileEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(RIAutomobileEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(RIAutomobileEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(RIAutomobileEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(RIAutomobileEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(RIAutomobileEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(RIAutomobileEntity.class, EntityDataSerializers.INT)
    );

    private final TagKey<Item> forgeWrench = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/wrench"));
    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final List<HitboxEntity> hitboxes = new ArrayList<>();
    private final UUID[] persistedSeatPassengers = new UUID[MAX_TRACKED_SEATS];

    private boolean changed = false;
    private float prevYawForRotate = 0.0F;
    private float clientPassengerYawDelta = 0.0F;
    private boolean preAccelerating = false;
    private int driftedReadyBoostCounter = Integer.MAX_VALUE;
    private int hadVehicleCollision = 0;
    private boolean dimensionsNeedRefresh = false;
    private AABB cullingBox = new AABB(0, 0, 0, 0, 0, 0);

    public RIAutomobileEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public RIAutomobileEntity(Level level) {
        this(RIAutomobilityEntities.RIAUTOMOBILE.get(), level);
    }

    private boolean usesRIASeats() {
        return RIAutomobileRegistry.isRegistered(this.getFrame());
    }

    private RIAutomobileDefinition getDefinition() {
        return RIAutomobileRegistry.get(this.getFrame());
    }

    private RIAutomobileDefinition.SeatPos getSeat(int seatIndex) {
        List<RIAutomobileDefinition.SeatPos> seats = getDefinition().seats();
        return seatIndex >= 0 && seatIndex < seats.size() ? seats.get(seatIndex) : RIAutomobileDefinition.SeatPos.zero();
    }

    @Override
    public void onAddedToWorld() {
        if (usesRIASeats() && this.hitboxes.isEmpty()) {
            spawnHitboxes();
        }
        super.onAddedToWorld();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && usesRIASeats()) {
            Containers.dropContents(level(), blockPosition(), this);
            removeHitboxes();
        }
        super.remove(reason);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (usesRIASeats()) {
            ContainerHelper.loadAllItems(tag, items);
            for (int i = 0; i < MAX_TRACKED_SEATS; i++) {
                String key = "SeatPassenger" + i;
                persistedSeatPassengers[i] = tag.hasUUID(key) ? tag.getUUID(key) : null;
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (usesRIASeats()) {
            ContainerHelper.saveAllItems(tag, items);
            for (int i = 0; i < getSeatCount(); i++) {
                Entity passenger = getSeatPassenger(i);
                if (passenger != null) {
                    tag.putUUID("SeatPassenger" + i, passenger.getUUID());
                }
            }
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.dimensions;
    }

    @Override
    public void setComponents(AutomobileFrame frame, AutomobileWheel wheel, AutomobileEngine engine) {
        super.setComponents(frame, wheel, engine);
        if (usesRIASeats()) {
            EntityDimensions dimensions = getDefinition().dimensions();
            if (dimensions.width != this.dimensions.width || dimensions.height != this.dimensions.height) {
                this.dimensions = dimensions;
                dimensionsNeedRefresh = true;
            }
        }
    }

    @Override
    public void tick() {
        if (!usesRIASeats()) {
            super.tick();
            return;
        }

        receiveVehicleCollisions();
        updateDimensionsForFrame();
        updateCullingBox();
        driftedReadyBoost();
        preAccelerating = isAccelerating();
        prevYawForRotate = this.getYRot();
        if (!level().isClientSide()) {
            reconcileSeatAssignments();
        }

        super.tick();
        if (level().isClientSide()) {
            clientPassengerYawDelta = Mth.wrapDegrees(this.getYRot() - prevYawForRotate);
            this.yRotO = prevYawForRotate;
        }
        if (dimensionsNeedRefresh) {
            this.refreshDimensions();
            dimensionsNeedRefresh = false;
        }

        for (HitboxEntity hitbox : this.hitboxes) {
            if (hitbox.isAlive()) {
                hitbox.syncPosition(this);
            }
        }
    }

    @Override
    public void forNearbyPlayers(int radius, boolean ignoreDriver, java.util.function.Consumer<ServerPlayer> action) {
        if (!usesRIASeats()) {
            super.forNearbyPlayers(radius, ignoreDriver, action);
            return;
        }

        Entity driver = getControllingPassenger();
        for (Player p : level().players()) {
            if (ignoreDriver && p == driver) {
                continue;
            }
            if (p.position().distanceTo(position()) < radius && p instanceof ServerPlayer player) {
                action.accept(player);
            }
        }
    }

    @Override
    public boolean hasSpaceForPassengers() {
        if (!usesRIASeats()) {
            return super.hasSpaceForPassengers();
        }
        return this.getPassengers().size() < getDefinition().seats().size();
    }

    @Override
    public boolean engineRunning() {
        if (!usesRIASeats()) {
            return super.engineRunning();
        }
        return this.getBoostTimer() > 0 || this.getControllingPassenger() != null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (!usesRIASeats()) {
            return super.canAddPassenger(passenger);
        }
        return hasSpaceForPassengers();
    }

    @Override
    public void provideClientInput(boolean fwd, boolean back, boolean left, boolean right, boolean space) {
        if (!usesRIASeats()) {
            super.provideClientInput(fwd, back, left, right, space);
            return;
        }

        if (!(getControllingPassenger() instanceof Player driver) || !driver.isLocalPlayer() || getVisualSeatIndex(driver) != 0) {
            return;
        }

        super.provideClientInput(fwd, back, left, right, space);
    }

    @Override
    public void accumulateCollisionAreas(Collection<CollisionArea> areas) {
        if (!usesRIASeats()) {
            super.accumulateCollisionAreas(areas);
            return;
        }

        this.level().getEntitiesOfClass(
                Entity.class,
                this.getBoundingBox().inflate(3.0F, 3.0F, 3.0F),
                entity -> {
                    if (entity == this) {
                        return false;
                    }
                    if (entity.getVehicle() == this) {
                        return false;
                    }
                    if (entity instanceof HitboxEntity hb) {
                        return hb.getAutomobile() != this;
                    }
                    return !this.hasPassenger(entity);
                }
        ).forEach(entity -> areas.add(CollisionArea.entity(entity)));
    }

    @Override
    public void runOverEntities(Vec3 velocity) {
        if (!usesRIASeats()) {
            super.runOverEntities(velocity);
            return;
        }

        AABB frontBox = getBoundingBox().move(velocity.scale(0.5));
        Vec3 velAdd = velocity.add(0, 0.1, 0).scale(3);

        for (Entity entity : level().getEntities(EntityTypeTest.forClass(Entity.class), frontBox, entity -> entity != this && !isOnRIAutomobile(entity))) {
            if (!entity.isInvulnerable() && entity instanceof LivingEntity living && entity.getVehicle() != this) {
                AutomobilityEntities.automobileDamageSource(level()).ifPresent(dmg -> living.hurt(dmg, getHorizontalSpeed() * 10.0F));
                entity.push(velAdd.x, velAdd.y, velAdd.z);
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!usesRIASeats()) {
            return super.interact(player, hand);
        }

        if (player.isShiftKeyDown() && this.hasInventory()) {
            if (!level().isClientSide()) {
                openInventory(player);
                return InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);
        if ((!isDecorative() || player.isCreative()) && (stack.is(AutomobilityItems.CROWBAR.require()) || stack.is(forgeWrench))) {
            double playerAngle = Math.toDegrees(Math.atan2(player.getZ() - this.getZ(), player.getX() - this.getX()));
            double angleDiff = Mth.wrapDegrees(this.getYRot() - playerAngle);

            if (angleDiff < 0 && !this.getFrontAttachmentType().isEmpty()) {
                this.destroyFrontAttachment(!player.isCreative());
                this.playHitSound(this.getHeadPos());
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (!this.getRearAttachmentType().isEmpty()) {
                this.destroyRearAttachment(!player.isCreative());
                this.playHitSound(this.getRearAttachment().pos());
                return InteractionResult.sidedSuccess(level().isClientSide);
            }

            this.destroyAutomobile(!player.isCreative(), RemovalReason.KILLED);
            this.playHitSound(this.position());
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (isDecorative()) {
            return InteractionResult.PASS;
        }

        if (stack.getItem() instanceof AutomobileInteractable interactable) {
            return interactable.interactAutomobile(stack, player, hand, this);
        }

        if (this.hasPassenger(player)) {
            return InteractionResult.PASS;
        }

        if (!this.hasSpaceForPassengers()) {
            Entity removable = this.getPassengers().stream()
                    .filter(entity -> !(entity instanceof Player))
                    .findFirst()
                    .orElse(null);
            if (removable != null) {
                if (!level().isClientSide()) {
                    removable.stopRiding();
                    player.startRiding(this, true);
                }
                return InteractionResult.sidedSuccess(level().isClientSide());
            }
            return InteractionResult.PASS;
        }

        if (!level().isClientSide()) {
            player.startRiding(this, true);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public void positionRider(Entity passenger, MoveFunction moveFunc) {
        if (!usesRIASeats()) {
            super.positionRider(passenger, moveFunc);
            return;
        }

        int seatIndex = getVisualSeatIndex(passenger);
        RIAutomobileDefinition.SeatPos local = getSeat(seatIndex);
        float pitch = this.getDisplacement().getAngularX(1.0F);
        float roll = this.getDisplacement().getAngularZ(1.0F);
        float vert = this.getDisplacement().getVertical(1.0F);

        Vec3 seatPos = local.pos();
        Vec3 pos = this.position()
                .add(0.0F, vert + passenger.getMyRidingOffset(), 0.0F)
                .add(new Vec3(seatPos.x, this.getPassengersRidingOffset() + seatPos.y, seatPos.z)
                        .yRot(-this.getYRot() * Mth.DEG_TO_RAD)
                        .xRot(-pitch * Mth.DEG_TO_RAD)
                        .zRot(-roll * Mth.DEG_TO_RAD));

        moveFunc.accept(passenger, pos.x, pos.y, pos.z);

    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (!usesRIASeats()) {
            return super.getDismountLocationForPassenger(passenger);
        }
        return calculateDismountLocation(passenger);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        if (!usesRIASeats()) {
            return super.getBoundingBoxForCulling();
        }
        return this.cullingBox;
    }

    @Override
    @Nullable
    public Entity getFirstPassenger() {
        if (!usesRIASeats()) {
            return super.getFirstPassenger();
        }
        Entity driver = getSeatPassenger(0);
        return driver != null ? driver : super.getFirstPassenger();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity driver = getFirstPassenger();
        return driver instanceof LivingEntity living ? living : null;
    }

    @Override
    public boolean isControlledByLocalInstance() {
        if (!usesRIASeats()) {
            return super.isControlledByLocalInstance();
        }

        return getControllingPassenger() instanceof Player driver
                && driver.isLocalPlayer()
                && getVisualSeatIndex(driver) == 0;
    }

    public boolean cycleSeat(Player player) {
        if (!usesRIASeats()) {
            return false;
        }
        int currentSeat = getSeatIndex(player);
        if (currentSeat < 0) {
            return false;
        }
        int seatCount = getSeatCount();
        for (int offset = 1; offset < seatCount; offset++) {
            int targetSeat = (currentSeat + offset) % seatCount;
            if (getSeatPassenger(targetSeat) == null) {
                setSeatPassenger(currentSeat, null);
                setSeatPassenger(targetSeat, player);
                return true;
            }
        }
        return false;
    }

    public void assignSeatForPassenger(Entity passenger) {
        if (!usesRIASeats() || passenger == null || getSeatIndex(passenger) >= 0) {
            return;
        }

        int seatCount = getSeatCount();
        int restoredSeat = getPersistedSeatIndex(passenger.getUUID(), seatCount);
        if (restoredSeat >= 0 && getSeatPassenger(restoredSeat) == null) {
            setSeatPassenger(restoredSeat, passenger);
            return;
        }

        int freeSeat = findFirstEmptySeat(0);
        if (freeSeat >= 0) {
            setSeatPassenger(freeSeat, passenger);
        }
    }

    public void snapPassengerToSeat(Entity passenger) {
        if (!usesRIASeats() || passenger == null || passenger.getVehicle() != this) {
            return;
        }
        positionRider(passenger, Entity::setPos);
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
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
    protected void defineSynchedData() {
        super.defineSynchedData();
        for (EntityDataAccessor<Integer> trackedSeat : TRACKED_SEATS) {
            this.entityData.define(trackedSeat, -1);
        }
    }

    private void driftedReadyBoost() {
        if (this.isDrifting() && wasHoldingDrift() && !isHoldingDrift()) {
            driftedReadyBoostCounter = 0;
        }

        if (!this.isDrifting() && !wasHoldingDrift() && !isHoldingDrift()) {
            if (driftedReadyBoostCounter >= 3) {
                return;
            }
            if (getTurboCharge() > 35 || getTurboCharge() == 0) {
                return;
            }
            driftedReadyBoostCounter++;
            if (!this.preAccelerating && isAccelerating()) {
                this.boost(0.38F, 12);
            }
        }
    }

    private void updateDimensionsForFrame() {
        EntityDimensions dimensions = getDefinition().dimensions();
        if (dimensions.width != this.dimensions.width || dimensions.height != this.dimensions.height) {
            this.dimensions = dimensions;
            this.refreshDimensions();
        }
    }

    private void receiveVehicleCollisions() {
        if (isDecorative()) {
            return;
        }

        var collisions = new HashMap<AutomobileEntity, IncomingCollision>();

        for (HitboxEntity box : this.hitboxes) {
            AABB bbox = box.getBoundingBox().inflate(0.15);
            for (HitboxEntity hitbox : this.level().getEntities(EntityTypeTest.forClass(HitboxEntity.class), bbox, h -> h.getAutomobile() != this)) {
                AutomobileEntity auto = hitbox.getAutomobile();
                AABB intersect = hitbox.getBoundingBox().inflate(0.15).intersect(bbox);
                Vec3 collDepth = new Vec3(intersect.getXsize(), 0, intersect.getZsize());

                if (auto == null || collisions.containsKey(auto) && collisions.get(auto).depth().lengthSqr() > collDepth.lengthSqr()) {
                    continue;
                }

                collisions.put(auto, new IncomingCollision(
                        collDepth,
                        getMeasuredMovement(),
                        intersect.getCenter(),
                        auto.getFrame().weight()
                ));
            }
        }

        hadVehicleCollision = Math.max(0, hadVehicleCollision - 1);
        for (IncomingCollision col : collisions.values()) {
            Vec3 meToCollision = col.origin().subtract(this.position()).multiply(1, 0, 1);
            double hitScale = hadVehicleCollision <= 0 ? 0.15 : 0.07;
            hitScale *= (1 + col.inertia() / this.getFrame().weight()) * 0.5;

            setAddedVelocity(getAddedVelocity().add(
                    meToCollision.reverse().normalize().scale(hitScale * (1 + 0.1 * Math.sqrt(col.velocity().length())) * col.depth().lengthSqr())
                            .multiply(1, 0, 1)
            ));

            if (hadVehicleCollision <= 0) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), AutomobilitySounds.COLLISION.require(), SoundSource.AMBIENT, 0.22f, 0.7f + (0.06f * (this.level().random.nextFloat() - 0.5f)), false);
                setEngineSpeed(getEngineSpeed() * 0.6f);
                hadVehicleCollision = 12;
            }
        }
    }

    private Vec3 getMeasuredMovement() {
        return this.position().subtract(getLastPosForDisplacement());
    }

    private void updateCullingBox() {
        this.cullingBox = super.getBoundingBoxForCulling();
        for (HitboxEntity hitbox : this.hitboxes) {
            this.cullingBox = this.cullingBox.minmax(hitbox.getBoundingBox());
        }
    }

    public int getSeatIndex(Entity passenger) {
        if (passenger == null) {
            return -1;
        }
        int seatCount = getSeatCount();
        for (int i = 0; i < seatCount; i++) {
            if (getTrackedSeatPassengerId(i) == passenger.getId()) {
                return i;
            }
        }
        return -1;
    }

    public int getVisualSeatIndex(@Nullable Entity passenger) {
        int trackedSeatIndex = getSeatIndex(passenger);
        if (trackedSeatIndex >= 0) {
            return trackedSeatIndex;
        }
        if (!this.level().isClientSide() || passenger == null || passenger.getVehicle() != this) {
            return -1;
        }

        int passengerIndex = this.getPassengers().indexOf(passenger);
        return passengerIndex >= 0 && passengerIndex < getSeatCount() ? passengerIndex : -1;
    }

    public int getSeatCount() {
        return Math.min(MAX_TRACKED_SEATS, getDefinition().seats().size());
    }

    @Nullable
    public Entity getSeatPassenger(int seatIndex) {
        if (seatIndex < 0 || seatIndex >= getSeatCount()) {
            return null;
        }
        int passengerId = getTrackedSeatPassengerId(seatIndex);
        if (passengerId < 0) {
            return null;
        }
        Entity passenger = level().getEntity(passengerId);
        return passenger != null && passenger.getVehicle() == this ? passenger : null;
    }

    private void reconcileSeatAssignments() {
        int seatCount = getSeatCount();
        Set<Integer> assignedPassengers = new HashSet<>();

        for (int i = 0; i < MAX_TRACKED_SEATS; i++) {
            if (i >= seatCount) {
                setTrackedSeatPassengerId(i, -1);
                continue;
            }

            Entity passenger = getSeatPassenger(i);
            if (passenger == null || !assignedPassengers.add(passenger.getId())) {
                setTrackedSeatPassengerId(i, -1);
            }
        }

        for (Entity passenger : this.getPassengers()) {
            assignSeatForPassenger(passenger);
        }
    }

    private int getPersistedSeatIndex(UUID uuid, int seatCount) {
        for (int i = 0; i < seatCount; i++) {
            if (uuid.equals(persistedSeatPassengers[i])) {
                return i;
            }
        }
        return -1;
    }

    private int findFirstEmptySeat(int start) {
        int seatCount = getSeatCount();
        for (int i = start; i < seatCount; i++) {
            if (getSeatPassenger(i) == null) {
                return i;
            }
        }
        return -1;
    }

    private void setSeatPassenger(int seatIndex, @Nullable Entity passenger) {
        if (seatIndex < 0 || seatIndex >= getSeatCount()) {
            return;
        }
        setTrackedSeatPassengerId(seatIndex, passenger == null ? -1 : passenger.getId());
    }

    private int getTrackedSeatPassengerId(int seatIndex) {
        return this.entityData.get(TRACKED_SEATS.get(seatIndex));
    }

    private void setTrackedSeatPassengerId(int seatIndex, int passengerId) {
        this.entityData.set(TRACKED_SEATS.get(seatIndex), passengerId);
    }

    private Vec3 calculateDismountLocation(Entity passenger) {
        AABB box = this.getBoundingBox();
        double sideOffset = box.getXsize() / 2 + 1.0;
        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;

        Vec3 right = this.position().add(-Math.cos(yawRad) * sideOffset, 0, -Math.sin(yawRad) * sideOffset);
        Vec3 left = this.position().add(Math.cos(yawRad) * sideOffset, 0, Math.sin(yawRad) * sideOffset);

        boolean leftSafe = level().noCollision(passenger, passenger.getBoundingBox().move(left.subtract(passenger.position())));
        boolean rightSafe = level().noCollision(passenger, passenger.getBoundingBox().move(right.subtract(passenger.position())));

        if (leftSafe && rightSafe) {
            return passenger.position().distanceTo(left) <= passenger.position().distanceTo(right) ? left : right;
        }
        if (leftSafe) {
            return left;
        }
        if (rightSafe) {
            return right;
        }
        return new Vec3(this.getX(), box.maxY, this.getZ());
    }

    private void spawnHitboxes() {
        for (RIAutomobileDefinition.Hitbox def : getDefinition().hitboxes()) {
            HitboxEntity hitbox = new HitboxEntity(level(), this, def);
            this.level().addFreshEntity(hitbox);
            this.hitboxes.add(hitbox);
        }
    }

    private void removeHitboxes() {
        for (HitboxEntity hitbox : this.hitboxes) {
            if (!hitbox.isRemoved()) {
                hitbox.discard();
            }
        }
    }

    private boolean isOnRIAutomobile(Entity entity) {
        return this == entity.getVehicle() || this.hasPassenger(entity);
    }

    private void rotatePassenger(float dYaw, Entity passenger) {
        if (dYaw == 0.0F) {
            return;
        }

        float prevYaw = passenger.getYRot();
        float newYaw = Mth.wrapDegrees(passenger.getYRot() + dYaw);
        passenger.yRotO = unwrapInterpolationYaw(prevYaw, newYaw);
        passenger.setYRot(newYaw);
        passenger.setYBodyRot(newYaw);
        if (passenger instanceof LivingEntity living) {
            living.yBodyRotO = unwrapInterpolationYaw(prevYaw, newYaw);
            living.yHeadRotO = unwrapInterpolationYaw(prevYaw, newYaw);
            living.setYHeadRot(newYaw);
        }
    }

    private float unwrapInterpolationYaw(float previousYaw, float currentYaw) {
        return currentYaw - Mth.wrapDegrees(currentYaw - previousYaw);
    }

    public float getClientPassengerYawDelta() {
        return clientPassengerYawDelta;
    }

    public void rotateLocalPassengerWithVehicle(Entity passenger) {
        if (!(passenger instanceof Player player) || !player.isLocalPlayer()) {
            return;
        }
        if (player.getVehicle() != this || getVisualSeatIndex(player) == 0) {
            return;
        }

        rotatePassenger(clientPassengerYawDelta, player);
    }

    private boolean isDecorative() {
        return ((AutomobileEntityAccessor) this).isDecorative();
    }

    private float getHorizontalSpeed() {
        return ((AutomobileEntityAccessor) this).getHSpeed();
    }

    private Vec3 getLastPosForDisplacement() {
        return ((AutomobileEntityAccessor) this).getLastPosForDisplacement();
    }

    private Vec3 getAddedVelocity() {
        return ((AutomobileEntityAccessor) this).getAddedVelocity();
    }

    private void setAddedVelocity(Vec3 velocity) {
        ((AutomobileEntityAccessor) this).setAddedVelocity(velocity);
    }

    private float getEngineSpeed() {
        return ((AutomobileEntityAccessor) this).getEngineSpeed();
    }

    private void setEngineSpeed(float engineSpeed) {
        ((AutomobileEntityAccessor) this).setEngineSpeed(engineSpeed);
    }

    private boolean isAccelerating() {
        return ((AutomobileEntityAccessor) this).isAccelerating();
    }

    private boolean isHoldingDrift() {
        return ((AutomobileEntityAccessor) this).isHoldingDrift();
    }

    private boolean wasHoldingDrift() {
        return ((AutomobileEntityAccessor) this).wasHoldingDrift();
    }

    private record IncomingCollision(Vec3 depth, Vec3 velocity, Vec3 origin, float inertia) {
    }
}
