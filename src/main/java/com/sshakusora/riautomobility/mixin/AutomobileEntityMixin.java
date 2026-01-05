package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.entity.HitboxEntity;
import com.sshakusora.riautomobility.entity.SeatEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.util.RIAutomobileEntityDimensionsRegistry;
import com.sshakusora.riautomobility.util.RIAutomobileHitboxRegistry;
import com.sshakusora.riautomobility.util.RIAutomobileSeatRegistry;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import io.github.foundationgames.automobility.entity.AutomobilityEntities;
import io.github.foundationgames.automobility.sound.AutomobilitySounds;
import io.github.foundationgames.automobility.util.duck.CollisionArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@Mixin(AutomobileEntity.class)
public abstract class AutomobileEntityMixin extends Entity implements Container {
    @Unique private AutomobileEntity self = (AutomobileEntity) (Object) this;
    @Unique private final TagKey<Item> FORGE_WRENCH = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/wrench"));
    @Unique private float prevYawForRotate = 0.0F;
    @Unique private boolean preAccelerating = false;
    @Unique private boolean changed = false;
    @Unique private int driftedReadyBoostCounter = Integer.MAX_VALUE;
    @Unique private int hadVehicleCollision = 0;
    @Unique private AABB cullingBox = new AABB(0, 0, 0, 0, 0, 0);
    @Unique public final List<HitboxEntity> hitboxes = new ArrayList<>();
    @Unique private final int INVENTORY_SIZE = 54;
    @Unique private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    @Shadow private float hSpeed;
    @Shadow private int turboCharge;
    @Shadow private boolean accelerating;
    @Shadow private boolean holdingDrift;
    @Shadow private boolean prevHoldDrift;
    @Shadow private boolean decorative;
    @Shadow private float engineSpeed;
    @Shadow private Vec3 addedVelocity;
    @Shadow private Vec3 lastPosForDisplacement;

    public AutomobileEntityMixin(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        if(RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) {
            return this.cullingBox;
        } else return super.getBoundingBoxForCulling();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return calDismountLocation(self, passenger);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            Containers.dropContents(level(), blockPosition(), this);
            this.removeAll();
        }
        super.remove(reason);
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
    public Entity getFirstPassenger(){
        List<Entity> passengers = this.getPassengers();
        if(passengers.isEmpty()) return null;
        else if(passengers.get(0) instanceof SeatEntity seat) return seat.getPassengers().isEmpty() ? null : seat.getPassengers().get(0);
        else return passengers.get(0);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void readAdditionalSaveContainerData(CompoundTag tag, CallbackInfo ci) {
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;
        ContainerHelper.loadAllItems(tag, items);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    public void addAdditionalSaveContainerData(CompoundTag tag, CallbackInfo ci) {
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;
        ContainerHelper.saveAllItems(tag, items);
    }

    @Inject(method = "positionRider", at = @At("HEAD"), cancellable = true)
    public void positionPassenger(Entity passenger, Entity.MoveFunction moveFunc, CallbackInfo ci) {
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;

        RIAutomobileSeatRegistry.SeatPos local = RIAutomobileSeatRegistry.getSeat(self, passenger);
        float pitch = self.getDisplacement().getAngularX(1.0F);
        float roll = self.getDisplacement().getAngularZ(1.0F);
        float vert = self.getDisplacement().getVertical(1.0F);

        Vec3 pos = self.position()
                .add(0.0F, (double)vert + passenger.getMyRidingOffset(), 0.0F)
                .add((new Vec3(local.pos.x, self.getPassengersRidingOffset() + local.pos.y, local.pos.z))
                        .yRot(-self.getYRot() * Mth.DEG_TO_RAD)
                        .xRot(-pitch * Mth.DEG_TO_RAD)
                        .zRot(-roll * Mth.DEG_TO_RAD));

        moveFunc.accept(passenger, pos.x, pos.y, pos.z);

        if(passenger instanceof SeatEntity){
            if(passenger.getFirstPassenger() != null && passenger.getFirstPassenger() != self.getFirstPassenger())
                whenRotated(self.getYRot() - prevYawForRotate, passenger.getFirstPassenger());
        }
        ci.cancel();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lio/github/foundationgames/automobility/entity/AutomobileEntity;position()Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
    public void tick(CallbackInfo ci) {
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;
        receiveVehicleCollisions();

        //set custom entity collision box in tick. I don't think it's a good idea, and I don't have a good idea :(
        EntityAccessor accessor = (EntityAccessor) self;
        EntityDimensions dimensions = RIAutomobileEntityDimensionsRegistry.getEntityDimensions(self.getFrame());
        if(dimensions != accessor.getDimensions()){
            accessor.setDimensions(dimensions);
        }

        verifyHitboxesAndSeatFor(self.getFrame());
        prevYawForRotate = self.getYRot();
        updateCullingBox();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;startRiding(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean cancelStartRiding(Entity entity, Entity vehicle) {
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return entity.startRiding(vehicle);
        return false;
    }

    @Inject(method = "hasSpaceForPassengers", at = @At("HEAD"), cancellable = true, remap = false)
    public void extendSpaceForPassengers(CallbackInfoReturnable<Boolean> cir) {
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;
        List<Entity> passengers = new ArrayList<>();
        for(Entity e : this.getPassengers()) {
            if(e.getFirstPassenger() != null) passengers.add(e.getFirstPassenger());
        }
        List<RIAutomobileSeatRegistry.SeatPos> seats = RIAutomobileSeatRegistry.getSeats(self.getFrame());
        cir.setReturnValue(passengers.size() < seats.size());
    }

    @Inject(method = "accumulateCollisionAreas", at = @At("HEAD"), cancellable = true, remap = false)
    public void accumulateCollisionAreasFix(Collection<CollisionArea> areas, CallbackInfo ci) {
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;

        self.level().getEntitiesOfClass(
                Entity.class,
                self.getBoundingBox().inflate(3.0F, 3.0F, 3.0F),
                (e) -> {
                    if (e == self) return false;
                    if (e.getVehicle() == self) return false;
                    if (e instanceof SeatEntity) return false;
                    if (e instanceof HitboxEntity hb) return hb.getAutomobile() != self;
                    return !(e.getVehicle() instanceof SeatEntity s) || s.getVehicle() != self;
                }
        ).forEach((e) -> areas.add(CollisionArea.entity(e)));

        ci.cancel();
    }

    @Inject(method = "engineRunning", at = @At("HEAD"), cancellable = true, remap = false)
    public void RIAutomobileEngineRunning(CallbackInfoReturnable<Boolean> cir) {
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;

        Entity driver = self.getFirstPassenger();
        if(driver == null) cir.setReturnValue(self.getBoostTimer() > 0);
        else cir.setReturnValue(true);
    }

    @Inject(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;startRiding(Lnet/minecraft/world/entity/Entity;)Z"), cancellable = true)
    private void RIAutomobileInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;

        for(Entity e : this.getPassengers()) {
            if(!e.isVehicle()) {
                player.startRiding(e, true);
                break;
            }
        }

        cir.setReturnValue(InteractionResult.sidedSuccess(this.level().isClientSide()));
        cir.cancel();
    }

    //After drifting, flying and landing, quickly press the accelerate button to boost.
    @Inject(method = "driftingTick", at = @At("HEAD"), remap = false)
    public void specialTurbo(CallbackInfo ci) {
        driftedReadyBoost(self);

        this.preAccelerating = this.accelerating;
    }

    @Inject(method = "runOverEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;", shift = At.Shift.AFTER), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private void redirectGetFirstPassengerE(Vec3 velocity, CallbackInfo ci, AABB frontBox) {
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;

        for(Entity entity : self.level().getEntities(EntityTypeTest.forClass(Entity.class), frontBox, (entityx) -> entityx != self && !isOnRIAutomobile(entityx))) {
            if (!entity.isInvulnerable() && entity instanceof LivingEntity living) {
                if (entity.getVehicle() != self) {
                    AutomobilityEntities.automobileDamageSource(self.level()).ifPresent((dmg) -> living.hurt(dmg, this.hSpeed * 10.0F));
                    entity.push(velocity.x, velocity.y, velocity.z);
                }
            }
        }

        ci.cancel();
    }

    @Redirect(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean allowForgeWrench(ItemStack stack, Item item) {
        return stack.is(item) || stack.is(FORGE_WRENCH);
    }

    @ModifyVariable(method = "collisionStateTick", at = @At("STORE"), name = "start", remap = false)
    private BlockPos driftingFix(BlockPos original) {
        double y = self.getY();

        if(y <= 0){
            return new BlockPos(original.getX(), original.getY() - 1, original.getZ());
        } else {
            return original;
        }
    }

    @Unique private void driftedReadyBoost(AutomobileEntity self) {
        if(self.isDrifting() && this.prevHoldDrift && !this.holdingDrift){
            driftedReadyBoostCounter = 0;
        }

        if(!self.isDrifting() && !this.prevHoldDrift && !this.holdingDrift){
            if(driftedReadyBoostCounter >= 3) return;
            if(this.turboCharge > 35 || this.turboCharge == 0) return;
            driftedReadyBoostCounter++;
            if(!this.preAccelerating && this.accelerating){
                self.boost(0.38F, 12);
            }
        }
    }

    @Unique
    private boolean isOnRIAutomobile(Entity entity){
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return false;

        List<Entity> passengerListPre = self.getPassengers();
        List<Entity> passengerListPost = new ArrayList<>();

        for (Entity e : passengerListPre) {
            if(e.getFirstPassenger() != null) passengerListPost.add(e.getFirstPassenger());
        }

        return passengerListPost.contains(entity);
    }

    @Unique
    public void whenRotated(float dYaw, Entity e) {
        e.setYRot(Mth.wrapDegrees(e.getYRot() + dYaw));
        e.setYBodyRot(Mth.wrapDegrees(e.getYRot() + dYaw));
    }

    @Unique
    public void receiveVehicleCollisions() {
        if (this.decorative) {
            return;
        }

        var collisions = new HashMap<AutomobileEntity, RIAutomobileHitboxRegistry.IncomingCollision>();

        for (var box : this.hitboxes) {
            var bbox = box.getBoundingBox().inflate(0.15);
            for (var hitbox : self.level().getEntities(EntityTypeTest.forClass(HitboxEntity.class), bbox, h -> h.getAutomobile() != self)) {
                var auto = hitbox.getAutomobile();
                var intersect = hitbox.getBoundingBox().inflate(0.15).intersect(bbox);

                var collDepth = new Vec3(intersect.getXsize(), 0, intersect.getZsize());
                if (auto == null || collisions.containsKey(auto) && collisions.get(auto).depth().lengthSqr() > collDepth.lengthSqr()) {
                    continue;
                }

                var momentum = this.getMeasuredMovement();
                var origin = intersect.getCenter();

                collisions.put(auto, new RIAutomobileHitboxRegistry.IncomingCollision(collDepth, momentum, origin, auto.getFrame().weight()));
            }
        }

        hadVehicleCollision = Math.max(0, hadVehicleCollision - 1);
        for (var col : collisions.values()) {
            var meToCollision = col.origin().subtract(self.position()).multiply(1, 0, 1);
            double hitScale = hadVehicleCollision <= 0 ? 0.15 : 0.07;
            hitScale *= (1 + col.inertia() / self.getFrame().weight()) * 0.5;
            this.addedVelocity = this.addedVelocity.add(
                    meToCollision.reverse().normalize().scale(hitScale * (1 + 0.1 * Math.sqrt(col.velocity().length())) * col.depth().lengthSqr())
                            .multiply(1, 0, 1));

            if (hadVehicleCollision <= 0) {
                self.level().playLocalSound(self.getX(), self.getY(), self.getZ(), AutomobilitySounds.COLLISION.require(), SoundSource.AMBIENT, 0.22f, 0.7f + (0.06f * (self.level().random.nextFloat() - 0.5f)), false);
                this.engineSpeed *= 0.6f;
                hadVehicleCollision = 12;
            }
        }
    }

    @Unique
    public Vec3 getMeasuredMovement() {
        return self.position().subtract(this.lastPosForDisplacement);
    }

    @Unique
    public void updateCullingBox() {
        this.cullingBox = super.getBoundingBoxForCulling();
        for (var hitbox : this.hitboxes) {
            this.cullingBox = this.cullingBox.minmax(hitbox.getBoundingBox());
        }
    }

    @Unique
    private Vec3 calDismountLocation(AutomobileEntity auto, Entity passenger) {
        AABB box = auto.getBoundingBox();
        double sideOffset = box.getXsize() / 2 + 1.0;

        float yawRad = auto.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = auto.position().add(-Math.cos(yawRad) * sideOffset, 0, -Math.sin(yawRad) * sideOffset);
        Vec3 left = auto.position().add(Math.cos(yawRad) * sideOffset, 0, Math.sin(yawRad) * sideOffset);

        Level level = auto.level();
        boolean leftSafe = level.noCollision(passenger, passenger.getBoundingBox().move(left.subtract(passenger.position())));
        boolean rightSafe = level.noCollision(passenger, passenger.getBoundingBox().move(right.subtract(passenger.position())));

        Vec3 result;
        if (leftSafe && rightSafe) {
            double distLeft = passenger.position().distanceTo(left);
            double distRight = passenger.position().distanceTo(right);

            result = (distLeft <= distRight) ? left : right;
        }
        else if (leftSafe) {
            result = left;
        }
        else if (rightSafe) {
            result = right;
        }
        else {
            result = new Vec3(auto.getX(), box.maxY, auto.getZ());
        }

        return result;
    }

    @Unique
    private void removeAll() {
        for (HitboxEntity hb : this.hitboxes) {
            if (!hb.isRemoved()) {
                hb.discard();
            }
        }
    }

    @Unique
    public void verifyHitboxesAndSeatFor(AutomobileFrame frame) {
        this.hitboxes.removeIf(Entity::isRemoved);

        if (this.level().isClientSide()) {
            return;
        }

        var boxes = RIAutomobileHitboxRegistry.getHitboxes(frame);
        var seats = RIAutomobileSeatRegistry.getSeats(frame);

        if (this.hitboxes.size() != boxes.size()) {
            this.hitboxes.forEach(e -> e.remove(RemovalReason.DISCARDED));
            this.hitboxes.clear();

            for (var box : boxes) {
                var boxEntity = new HitboxEntity(this.level(), self, box);
                boxEntity.setPos(this.position());
                this.hitboxes.add(boxEntity);

                this.level().addFreshEntity(boxEntity);
            }
        }

        if(self.getPassengers().size() != seats.size()) {
            self.getPassengers().forEach(e -> e.remove(RemovalReason.DISCARDED));

            for (var seat : seats) {
                var seatEntity = new SeatEntity(this.level(), self);
                seatEntity.setPos(this.position());
                seatEntity.startRiding(self, true);

                this.level().addFreshEntity(seatEntity);
            }
        }
    }
}
