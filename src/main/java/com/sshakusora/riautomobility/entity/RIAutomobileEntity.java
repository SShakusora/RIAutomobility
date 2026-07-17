package com.sshakusora.riautomobility.entity;

import com.sshakusora.riautomobility.definition.RIAutomobileDefinition;
import com.sshakusora.riautomobility.definition.RIAutomobileRegistry;
import com.sshakusora.riautomobility.item.VehicleKeyAccess;
import com.sshakusora.riautomobility.mixin.accessor.AutomobileEntityAccessor;
import com.sshakusora.riautomobility.network.packet.client.VehicleHighlightClientHandler;
import com.sshakusora.riautomobility.util.RIAutomobileTransformUtil;
import com.sshakusora.riautomobility.world.VehicleLocatorSavedData;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import io.github.foundationgames.automobility.entity.AutomobilityEntities;
import io.github.foundationgames.automobility.item.AutomobileInteractable;
import io.github.foundationgames.automobility.item.AutomobilityItems;
import io.github.foundationgames.automobility.item.FrontAttachmentItem;
import io.github.foundationgames.automobility.item.RearAttachmentItem;
import io.github.foundationgames.automobility.sound.AutomobilitySounds;
import io.github.foundationgames.automobility.util.SimpleMapContentRegistry;
import io.github.foundationgames.automobility.util.duck.CollisionArea;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RIAutomobileEntity extends AutomobileEntity implements WorldlyContainer {
    private static final int INVENTORY_SIZE = 54;
    private static final int MAX_TRACKED_SEATS = 8;
    private static final int[] NO_AUTOMATION_SLOTS = new int[0];
    private static final int INPUT_DRIFT_MASK = 1;
    private static final int INPUT_ACCELERATING_MASK = 1 << 4;
    private static final EntityDataAccessor<Boolean> KEYED = SynchedEntityData.defineId(
            RIAutomobileEntity.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Integer> BASE_PASSENGER = SynchedEntityData.defineId(
            RIAutomobileEntity.class,
            EntityDataSerializers.INT
    );
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
    private final int[] seatAssignmentScratch = new int[MAX_TRACKED_SEATS];

    @Nullable
    private ResourceLocation unresolvedFrameId;
    @Nullable
    private ResourceLocation unresolvedWheelId;
    @Nullable
    private ResourceLocation unresolvedEngineId;

    private boolean changed = false;
    private int collisionWarmupTicks = 0;
    private float prevYawForRotate = 0.0F;
    private float clientPassengerYawDelta = 0.0F;
    private boolean preAccelerating = false;
    private boolean previousHoldingDrift = false;
    private Vec3 movementTickStartPosition = Vec3.ZERO;
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

    @Override
    public boolean isCurrentlyGlowing() {
        return super.isCurrentlyGlowing()
                || (level().isClientSide() && VehicleHighlightClientHandler.shouldHighlight(this));
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
        collisionWarmupTicks = 15;
        super.onAddedToWorld();
        updateVehicleLocation();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            if (reason.shouldDestroy() && usesRIASeats()) {
                Containers.dropContents(level(), blockPosition(), this);
            }
            removeHitboxes();
            if (reason.shouldDestroy() && this.isKeyed() && level() instanceof ServerLevel serverLevel) {
                VehicleLocatorSavedData.get(serverLevel.getServer()).markDestroyed(this.getUUID());
            }
        }
        super.remove(reason);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        ResourceLocation savedFrameId = ResourceLocation.tryParse(tag.getString("frame"));
        ResourceLocation savedWheelId = ResourceLocation.tryParse(tag.getString("wheels"));
        ResourceLocation savedEngineId = ResourceLocation.tryParse(tag.getString("engine"));
        super.readAdditionalSaveData(tag);
        this.entityData.set(KEYED, tag.getBoolean("Keyed"));
        this.unresolvedFrameId = unresolvedId(savedFrameId, this.getFrame());
        this.unresolvedWheelId = unresolvedId(savedWheelId, this.getWheels());
        this.unresolvedEngineId = unresolvedId(savedEngineId, this.getEngine());
        if (usesRIASeats() || this.unresolvedFrameId != null) {
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
        tag.putBoolean("Keyed", this.isKeyed());
        preserveUnresolvedId(tag, "frame", this.unresolvedFrameId);
        preserveUnresolvedId(tag, "wheels", this.unresolvedWheelId);
        preserveUnresolvedId(tag, "engine", this.unresolvedEngineId);
        boolean riaSeats = usesRIASeats();
        if (riaSeats || this.unresolvedFrameId != null) {
            ContainerHelper.saveAllItems(tag, items);
            for (int i = 0; i < MAX_TRACKED_SEATS; i++) {
                Entity passenger = riaSeats && i < getSeatCount() ? getSeatPassenger(i) : null;
                if (passenger != null) {
                    tag.putUUID("SeatPassenger" + i, passenger.getUUID());
                } else if (this.unresolvedFrameId != null && persistedSeatPassengers[i] != null) {
                    tag.putUUID("SeatPassenger" + i, persistedSeatPassengers[i]);
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
        this.unresolvedFrameId = null;
        this.unresolvedWheelId = null;
        this.unresolvedEngineId = null;
        super.setComponents(frame, wheel, engine);
        if (usesRIASeats()) {
            EntityDimensions dimensions = getDefinition().dimensions();
            if (dimensions.width != this.dimensions.width || dimensions.height != this.dimensions.height) {
                this.dimensions = dimensions;
                dimensionsNeedRefresh = true;
            }
        }
    }

    public void reloadRIAutomobilityComponents() {
        AutomobileFrame frame = resolveComponent(AutomobileFrame.REGISTRY, this.unresolvedFrameId, this.getFrame());
        AutomobileWheel wheel = resolveComponent(AutomobileWheel.REGISTRY, this.unresolvedWheelId, this.getWheels());
        AutomobileEngine engine = resolveComponent(AutomobileEngine.REGISTRY, this.unresolvedEngineId, this.getEngine());
        this.unresolvedFrameId = clearIfResolved(this.unresolvedFrameId, frame);
        this.unresolvedWheelId = clearIfResolved(this.unresolvedWheelId, wheel);
        this.unresolvedEngineId = clearIfResolved(this.unresolvedEngineId, engine);
        super.setComponents(frame, wheel, engine);

        if (usesRIASeats()) {
            removeHitboxes();
            this.hitboxes.clear();
            spawnHitboxes();
            EntityDimensions dimensions = getDefinition().dimensions();
            this.dimensions = dimensions;
            this.dimensionsNeedRefresh = true;
            updateCullingBox();
        }
    }

    @Nullable
    private static ResourceLocation unresolvedId(@Nullable ResourceLocation savedId,
                                                 SimpleMapContentRegistry.Identifiable resolved) {
        return savedId != null && !savedId.equals(resolved.getId()) ? savedId : null;
    }

    private static void preserveUnresolvedId(CompoundTag tag, String key, @Nullable ResourceLocation id) {
        if (id != null) {
            tag.putString(key, id.toString());
        }
    }

    private static <T extends SimpleMapContentRegistry.Identifiable> T resolveComponent(
            SimpleMapContentRegistry<T> registry, @Nullable ResourceLocation unresolvedId, T current) {
        if (unresolvedId != null) {
            T resolved = registry.get(unresolvedId);
            if (resolved != null) {
                return resolved;
            }
        }
        return registry.getOrDefault(current.getId());
    }

    @Nullable
    private static ResourceLocation clearIfResolved(@Nullable ResourceLocation unresolvedId,
                                                    SimpleMapContentRegistry.Identifiable resolved) {
        return unresolvedId != null && unresolvedId.equals(resolved.getId()) ? null : unresolvedId;
    }

    @Override
    public void tick() {
        if (!level().isClientSide()) {
            if (this.tickCount % 20 == 0) {
                updateVehicleLocation();
            }
            enforceDriverAccess();
        }
        if (!usesRIASeats()) {
            super.tick();
            previousHoldingDrift = isHoldingDrift();
            return;
        }

        if (collisionWarmupTicks > 0) {
            collisionWarmupTicks--;
        }

        receiveVehicleCollisions();
        updateDimensionsForFrame();
        updateCullingBox();
        driftedReadyBoost();
        preAccelerating = isAccelerating();
        prevYawForRotate = this.getYRot();
        if (!level().isClientSide()) {
            reconcileSeatAssignments();
            enforceDriverAccess();
        }

        super.tick();
        previousHoldingDrift = isHoldingDrift();
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
    public void movementTick() {
        movementTickStartPosition = this.position();
        super.movementTick();
    }

    @Override
    public void forNearbyPlayers(int radius, boolean ignoreDriver, Consumer<ServerPlayer> action) {
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
            if (this.isKeyed() && isBasePassenger(super.getFirstPassenger())) {
                return this.getBoostTimer() > 0;
            }
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
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (!usesRIASeats()) {
            return;
        }

        assignSeatForPassenger(passenger);
        snapPassengerToSeat(passenger);
    }

    @Override
    public void provideClientInput(boolean fwd, boolean back, boolean left, boolean right, boolean space) {
        if (!usesRIASeats()) {
            if (this.isKeyed() && this.getControllingPassenger() == null) {
                return;
            }
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

        AABB searchBox = this.getBoundingBox().inflate(3.0F, 3.0F, 3.0F);
        for (HitboxEntity hitbox : this.hitboxes) {
            if (hitbox.isCollisionReady()) {
                searchBox = searchBox.minmax(hitbox.getBoundingBox().inflate(3.0F, 3.0F, 3.0F));
            }
        }

        this.level().getEntitiesOfClass(
                Entity.class,
                searchBox,
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
        for (HitboxEntity hitbox : this.hitboxes) {
            if (hitbox.isCollisionReady()) {
                frontBox = frontBox.minmax(hitbox.getBoundingBox().move(velocity.scale(0.5)));
            }
        }
        Vec3 velAdd = velocity.add(0, 0.1, 0).scale(3);

        for (Entity entity : level().getEntities(EntityTypeTest.forClass(Entity.class), frontBox, entity -> entity != this && !isOnRIAutomobile(entity))) {
            if (!entity.isInvulnerable() && entity instanceof LivingEntity living && entity.getVehicle() != this) {
                AutomobilityEntities.automobileDamageSource(level()).ifPresent(dmg -> living.hurt(dmg, getHSpeed() * 10.0F));
                entity.push(velAdd.x, velAdd.y, velAdd.z);
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean dismantlingTool = stack.is(AutomobilityItems.CROWBAR.require()) || stack.is(forgeWrench);
        boolean authorized = VehicleKeyAccess.canAccess(player, this);

        if (stack.is(Items.NAME_TAG) && stack.hasCustomHoverName()) {
            if (!authorized) {
                VehicleKeyAccess.deny(player);
                return InteractionResult.sidedSuccess(level().isClientSide());
            }
            if (!level().isClientSide() && level() instanceof ServerLevel serverLevel) {
                this.setCustomName(stack.getHoverName());
                updateVehicleLocation();
                VehicleKeyAccess.updateVehicleName(serverLevel.getServer(), this);
                stack.shrink(1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }

        if (!authorized && (player.isShiftKeyDown() && this.hasInventory() || dismantlingTool)) {
            VehicleKeyAccess.deny(player);
            return InteractionResult.sidedSuccess(level().isClientSide());
        }

        if (!usesRIASeats()) {
            if (!authorized && !(stack.getItem() instanceof AutomobileInteractable)) {
                if (!hasPassengerSpace()) {
                    VehicleKeyAccess.deny(player);
                    return InteractionResult.sidedSuccess(level().isClientSide());
                }
                if (!level().isClientSide()) {
                    if (player.startRiding(this, true) && super.getFirstPassenger() == player) {
                        setBasePassenger(player);
                    }
                }
                return InteractionResult.sidedSuccess(level().isClientSide());
            }
            Entity firstPassenger = super.getFirstPassenger();
            if (authorized
                    && firstPassenger instanceof Player displacedPassenger
                    && this.isKeyed()
                    && !VehicleKeyAccess.canAccess(displacedPassenger, this)
                    && this.getRearAttachment().isRideable()
                    && !(stack.getItem() instanceof AutomobileInteractable)
                    && !dismantlingTool) {
                if (!level().isClientSide()) {
                    displacedPassenger.stopRiding();
                    player.startRiding(this, true);
                    displacedPassenger.startRiding(this, true);
                    setBasePassenger(null);
                }
                return InteractionResult.sidedSuccess(level().isClientSide());
            }
            boolean mayDestroy = dismantlingTool
                    && this.getFrontAttachmentType().isEmpty()
                    && this.getRearAttachmentType().isEmpty();
            InteractionResult result = super.interact(player, hand);
            if (!level().isClientSide() && mayDestroy && this.isRemoved()) {
                VehicleKeyAccess.resetOneMatchingKey(player, this);
            }
            return result;
        }

        if (player.isShiftKeyDown() && this.hasInventory()) {
            if (!level().isClientSide()) {
                openInventory(player);
                return InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }

        if ((!isDecorative() || player.isCreative()) && dismantlingTool) {
            double playerAngle = Math.toDegrees(Math.atan2(player.getZ() - this.getZ(), player.getX() - this.getX()));
            double angleDiff = Mth.wrapDegrees(this.getYRot() - playerAngle);

            if (angleDiff < 0 && !this.getFrontAttachmentType().isEmpty()) {
                this.destroyFrontAttachment(!player.isCreative());
                this.playHitSound(this.getHeadPos());
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (!this.getRearAttachmentType().isEmpty()) {
                Vec3 rearPosition = this.getRearAttachment().pos();
                this.destroyRearAttachment(!player.isCreative());
                this.playHitSound(rearPosition);
                return InteractionResult.sidedSuccess(level().isClientSide);
            }

            if (!level().isClientSide()) {
                VehicleKeyAccess.resetOneMatchingKey(player, this);
            }
            this.destroyAutomobile(!player.isCreative(), RemovalReason.KILLED);
            this.playHitSound(this.position());
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (isDecorative()) {
            return InteractionResult.PASS;
        }

        if (stack.getItem() instanceof FrontAttachmentItem frontAttachmentItem
                && !getDefinition().allowsFrontAttachment(frontAttachmentItem.getComponent(stack))) {
            return InteractionResult.PASS;
        }

        if (stack.getItem() instanceof RearAttachmentItem rearAttachmentItem
                && !getDefinition().allowsRearAttachment(rearAttachmentItem.getComponent(stack))) {
            return InteractionResult.PASS;
        }

        if (stack.getItem() instanceof AutomobileInteractable interactable) {
            return interactable.interactAutomobile(stack, player, hand, this);
        }

        if (this.hasPassenger(player)) {
            return InteractionResult.PASS;
        }

        if (!authorized) {
            if (!hasPassengerSpace()) {
                return InteractionResult.PASS;
            }
            if (!level().isClientSide()) {
                boardAsPassenger(player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
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
            if (passenger == super.getFirstPassenger()
                    && isBasePassenger(passenger)
                    && this.getRearAttachment().isRideable()) {
                Vec3 rearSeat = this.getRearAttachment().pos().add(
                        0.0D,
                        this.getRearAttachment().getPassengerHeightOffset() + passenger.getMyRidingOffset() - 0.14D,
                        0.0D
                );
                moveFunc.accept(passenger, rearSeat.x, rearSeat.y, rearSeat.z);
                return;
            }
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
                .add(0.0F, vert, 0.0F)
                .add(RIAutomobileTransformUtil.rotateLocalOffset(
                        new Vec3(
                                seatPos.x,
                                this.getPassengersRidingOffset() + seatPos.y + passenger.getMyRidingOffset(),
                                seatPos.z
                        ),
                        this.getYRot(), pitch, roll));

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
    public boolean canCollideWith(Entity other) {
        if (!usesRIASeats()) {
            return super.canCollideWith(other);
        }

        if (other instanceof HitboxEntity hitbox && hitbox.getAutomobile() == this) {
            return false;
        }

        return super.canCollideWith(other);
    }

    @Override
    @Nullable
    public Entity getFirstPassenger() {
        if (!usesRIASeats()) {
            return super.getFirstPassenger();
        }

        return getSeatPassenger(0);
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        if (!usesRIASeats()) {
            LivingEntity controlling = super.getControllingPassenger();
            if (isBasePassenger(controlling)) {
                return null;
            }
            return controlling;
        }

        Entity driver = getSeatPassenger(0);
        return driver instanceof LivingEntity living ? living : null;
    }

    @Override
    public boolean isControlledByLocalInstance() {
        if (!usesRIASeats()) {
            if (this.isKeyed() && this.getControllingPassenger() == null) {
                return false;
            }
            return super.isControlledByLocalInstance();
        }

        if (!level().isClientSide()) {
            return true;
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
            if (targetSeat == 0 && !VehicleKeyAccess.canAccess(player, this)) {
                continue;
            }
            if (getSeatPassenger(targetSeat) == null) {
                setSeatPassenger(currentSeat, null);
                setSeatPassenger(targetSeat, player);
                return true;
            }
        }
        return false;
    }

    public boolean hasPassengerSpace() {
        if (!usesRIASeats()) {
            if (!this.getRearAttachment().isRideable()) {
                return false;
            }
            Entity firstPassenger = super.getFirstPassenger();
            if (firstPassenger == null) {
                return true;
            }
            if (isBasePassenger(firstPassenger)) {
                return false;
            }
            return this.getPassengers().size() < 2;
        }
        if (findFirstEmptySeat(1) >= 0) {
            return true;
        }
        for (int seat = 1; seat < getSeatCount(); seat++) {
            Entity passenger = getSeatPassenger(seat);
            if (passenger != null && !(passenger instanceof Player)) {
                return true;
            }
        }
        return false;
    }

    public boolean boardAsPassenger(Player player) {
        if (!usesRIASeats() || player.getVehicle() == this) {
            return false;
        }
        int targetSeat = findFirstEmptySeat(1);
        if (targetSeat < 0) {
            for (int seat = 1; seat < getSeatCount(); seat++) {
                Entity passenger = getSeatPassenger(seat);
                if (passenger != null && !(passenger instanceof Player)) {
                    passenger.stopRiding();
                    targetSeat = seat;
                    break;
                }
            }
        }
        if (targetSeat < 0 || !player.startRiding(this, true)) {
            return false;
        }
        int assignedSeat = getSeatIndex(player);
        if (assignedSeat >= 0) {
            setSeatPassenger(assignedSeat, null);
        }
        setSeatPassenger(targetSeat, player);
        snapPassengerToSeat(player);
        return true;
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

        Entity driver = getSeatPassenger(0);
        if (passenger instanceof Player && driver != null && !(driver instanceof Player)) {
            int freePassengerSeat = findFirstEmptySeat(1);
            if (freePassengerSeat >= 0) {
                setSeatPassenger(freePassengerSeat, driver);
                setSeatPassenger(0, passenger);
                return;
            }
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
        return this.isAlive()
                && player.distanceTo(this) < 8.0
                && VehicleKeyAccess.canAccess(player, this);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(KEYED, false);
        this.entityData.define(BASE_PASSENGER, -1);
        for (EntityDataAccessor<Integer> trackedSeat : TRACKED_SEATS) {
            this.entityData.define(trackedSeat, -1);
        }
    }

    public boolean isKeyed() {
        return this.entityData.get(KEYED);
    }

    public void setKeyed(boolean keyed) {
        this.entityData.set(KEYED, keyed);
        if (keyed) {
            updateVehicleLocation();
        } else if (level() instanceof ServerLevel serverLevel) {
            setBasePassenger(null);
            VehicleLocatorSavedData.get(serverLevel.getServer()).removeLocation(this.getUUID());
        }
    }

    public boolean canPlayerAccess(Player player) {
        return VehicleKeyAccess.canAccess(player, this);
    }

    private void updateVehicleLocation() {
        if (level() instanceof ServerLevel serverLevel && isKeyed()) {
            VehicleLocatorSavedData.get(serverLevel.getServer()).update(this);
        }
    }

    private void enforceDriverAccess() {
        if (this.isKeyed() && !usesRIASeats()) {
            Entity firstPassenger = super.getFirstPassenger();
            if (firstPassenger instanceof Player player && !VehicleKeyAccess.canAccess(player, this)) {
                setBasePassenger(player);
                this.setInputs(false, false, false, false, false);
                if (!this.getRearAttachment().isRideable()) {
                    player.stopRiding();
                    setBasePassenger(null);
                    VehicleKeyAccess.deny(player);
                }
                return;
            }
            setBasePassenger(null);
        }
        LivingEntity controlling = this.getControllingPassenger();
        if (!this.isKeyed()) {
            return;
        }
        if (controlling instanceof Player driver && VehicleKeyAccess.canAccess(driver, this)) {
            return;
        }
        this.setInputs(false, false, false, false, false);
        if (controlling == null) {
            return;
        }
        if (!(controlling instanceof Player driver)) {
            controlling.stopRiding();
            return;
        }
        if (!usesRIASeats()) {
            driver.stopRiding();
            VehicleKeyAccess.deny(driver);
            return;
        }
        int passengerSeat = findFirstEmptySeat(1);
        if (passengerSeat >= 0) {
            setSeatPassenger(0, null);
            setSeatPassenger(passengerSeat, driver);
            snapPassengerToSeat(driver);
        } else {
            driver.stopRiding();
        }
        VehicleKeyAccess.deny(driver);
    }

    private boolean isBasePassenger(@Nullable Entity passenger) {
        return passenger != null && this.entityData.get(BASE_PASSENGER) == passenger.getId();
    }

    private void setBasePassenger(@Nullable Entity passenger) {
        int passengerId = passenger == null ? -1 : passenger.getId();
        if (this.entityData.get(BASE_PASSENGER) != passengerId) {
            this.entityData.set(BASE_PASSENGER, passengerId);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return LazyOptional.empty();
        }
        return super.getCapability(capability, side);
    }

    private void driftedReadyBoost() {
        if (this.isDrifting() && previousHoldingDrift && !isHoldingDrift()) {
            driftedReadyBoostCounter = 0;
        }

        if (!this.isDrifting() && !previousHoldingDrift && !isHoldingDrift()) {
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
        if (isDecorative() || collisionWarmupTicks > 0
                || level().isClientSide() && !isControlledByLocalInstance()) {
            return;
        }

        AABB searchBox = null;
        for (HitboxEntity box : this.hitboxes) {
            if (!box.isCollisionReady()) continue;
            AABB candidate = box.getBoundingBox().inflate(0.15);
            searchBox = searchBox == null ? candidate : searchBox.minmax(candidate);
        }
        if (searchBox == null) return;

        List<HitboxEntity> nearby = this.level().getEntities(
                EntityTypeTest.forClass(HitboxEntity.class), searchBox,
                hitbox -> hitbox.isCollisionReady() && hitbox.getAutomobile() != this);
        if (nearby.isEmpty()) return;

        Map<AutomobileEntity, IncomingCollision> collisions = new IdentityHashMap<>();
        for (HitboxEntity box : this.hitboxes) {
            if (!box.isCollisionReady()) continue;
            AABB bbox = box.getBoundingBox().inflate(0.15);
            for (HitboxEntity hitbox : nearby) {
                if (!bbox.intersects(hitbox.getBoundingBox())) continue;
                AutomobileEntity auto = hitbox.getAutomobile();
                if (auto == null) continue;
                AABB intersect = hitbox.getBoundingBox().inflate(0.15).intersect(bbox);
                Vec3 collDepth = new Vec3(intersect.getXsize(), 0, intersect.getZsize());

                IncomingCollision previous = collisions.get(auto);
                if (previous != null && previous.depth().lengthSqr() > collDepth.lengthSqr()) {
                    continue;
                }

                Vec3 relativeMovement = getMeasuredMovement().subtract(getVehicleMeasuredMovement(auto)).multiply(1, 0, 1);

                collisions.put(auto, new IncomingCollision(
                        collDepth,
                        relativeMovement,
                        intersect.getCenter(),
                        auto.getFrame().weight()
                ));
            }
        }

        hadVehicleCollision = Math.max(0, hadVehicleCollision - 1);
        for (IncomingCollision col : collisions.values()) {
            Vec3 meToCollision = col.origin().subtract(this.position()).multiply(1, 0, 1);
            if (col.velocity().lengthSqr() < 0.0004 || meToCollision.lengthSqr() < 1.0E-6) {
                continue;
            }

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
        return this.position().subtract(movementTickStartPosition);
    }

    private void updateCullingBox() {
        this.cullingBox = super.getBoundingBoxForCulling();
        for (RIAutomobileDefinition.Hitbox hitbox : getDefinition().hitboxes()) {
            this.cullingBox = this.cullingBox.minmax(getHitboxBoundingBox(hitbox));
        }
    }

    private Vec3 getVehicleMeasuredMovement(AutomobileEntity auto) {
        if (auto instanceof RIAutomobileEntity riautomobile) {
            return riautomobile.getMeasuredMovement();
        }

        return auto.getDeltaMovement();
    }

    private AABB getHitboxBoundingBox(RIAutomobileDefinition.Hitbox hitbox) {
        Vec3 center = RIAutomobileTransformUtil.localPosToWorldSpace(this, hitbox.origin());
        double halfWidth = hitbox.width() * 0.5D;

        return new AABB(
                center.x - halfWidth,
                center.y,
                center.z - halfWidth,
                center.x + halfWidth,
                center.y + hitbox.height(),
                center.z + halfWidth
        );
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
        int assignedCount = 0;

        for (int i = 0; i < MAX_TRACKED_SEATS; i++) {
            if (i >= seatCount) {
                setTrackedSeatPassengerId(i, -1);
                continue;
            }

            Entity passenger = getSeatPassenger(i);
            int passengerId = passenger == null ? -1 : passenger.getId();
            boolean duplicate = false;
            for (int assignedIndex = 0; assignedIndex < assignedCount; assignedIndex++) {
                if (seatAssignmentScratch[assignedIndex] == passengerId) {
                    duplicate = true;
                    break;
                }
            }
            if (passenger == null || duplicate) {
                setTrackedSeatPassengerId(i, -1);
            } else {
                seatAssignmentScratch[assignedCount++] = passengerId;
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

    public List<HitboxEntity> getHitboxEntities() {
        return Collections.unmodifiableList(this.hitboxes);
    }

    private void spawnHitboxes() {
        for (RIAutomobileDefinition.Hitbox def : getDefinition().hitboxes()) {
            HitboxEntity hitbox = new HitboxEntity(level(), this, def);
            hitbox.syncPosition(this);
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
        return (compactInputData() & INPUT_ACCELERATING_MASK) != 0;
    }

    private boolean isHoldingDrift() {
        return (compactInputData() & INPUT_DRIFT_MASK) != 0;
    }

    private record IncomingCollision(Vec3 depth, Vec3 velocity, Vec3 origin, float inertia) {
    }
}
