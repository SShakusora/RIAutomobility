package com.sshakusora.riautomobility.entity;

import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.mixin.accessor.AutomobileEntityAccessor;
import com.sshakusora.riautomobility.util.RIAutomobileEntityDimensionsRegistry;
import com.sshakusora.riautomobility.util.RIAutomobileHitboxRegistry;
import com.sshakusora.riautomobility.util.RIAutomobileSeatRegistry;
import io.github.foundationgames.automobility.automobile.render.RenderableAutomobile;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import io.github.foundationgames.automobility.entity.AutomobilityEntities;
import io.github.foundationgames.automobility.sound.AutomobilitySounds;
import io.github.foundationgames.automobility.util.duck.CollisionArea;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class RIAutomobileEntity extends AutomobileEntity implements RenderableAutomobile{
    private int hadVehicleCollision = 0;
    private AABB cullingBox = new AABB(0, 0, 0, 0, 0, 0);
    public final List<HitboxEntity> hitboxes = new ArrayList<>();
    public final List<SeatEntity> seats = new ArrayList<>();

    public RIAutomobileEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    public RIAutomobileEntity(Level world) {
        this(RIAutomobilityEntities.RIAUTOMOBILE.get(), world);
    }

    @Override
    public Entity getFirstPassenger() {
        List<Entity> passengers = this.getPassengers();
        if (passengers.isEmpty()) return null;

        Entity first = passengers.get(0);

        if (first instanceof SeatEntity seat) {
            Entity seated = seat.getFirstPassenger();
            return (seated != seat) ? seated : null;
        }

        return super.getFirstPassenger();
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.cullingBox;
    }

    @Override
    public void positionRider(Entity passenger, Entity.MoveFunction moveFunc) {
        if(!RIAutomobileFrame.isRIAutomobileFrame(this.getFrame())) {
            super.positionRider(passenger, moveFunc);
            return;
        }

        RIAutomobileSeatRegistry.SeatPos local = RIAutomobileSeatRegistry.getSeat(this, passenger);
        if (local == null) return;

        float pitch = this.getDisplacement().getAngularX(1.0F);
        float roll = this.getDisplacement().getAngularZ(1.0F);
        float vert = this.getDisplacement().getVertical(1.0F);

        Vec3 pos = this.position()
                .add(0.0F, (double)vert + passenger.getMyRidingOffset(), 0.0F)
                .add((new Vec3(local.pos.x, this.getPassengersRidingOffset() + local.pos.y, local.pos.z))
                        .yRot(-this.getYRot() * Mth.DEG_TO_RAD)
                        .xRot(-pitch * Mth.DEG_TO_RAD)
                        .zRot(-roll * Mth.DEG_TO_RAD));

        moveFunc.accept(passenger, pos.x, pos.y, pos.z);
    }

    @Override
    public void tick() {
        super.tick();

//        if(!RIAutomobileFrame.isRIAutomobileFrame(this.getFrame())) {
//            return;
//        }

        this.receiveVehicleCollisions();
        this.refreshSeats();
        this.refreshHitboxes();
        EntityDimensions targetDim = RIAutomobileEntityDimensionsRegistry.getEntityDimensions(this.getFrame());
        if (!this.getDimensions(this.getPose()).equals(targetDim)) {
            System.out.println("targetDim: " + this.getDimensions(this.getPose()));
            this.refreshDimensions();
        }

        for (HitboxEntity hitbox : this.hitboxes) {
            hitbox.updateRelativePos(this);
        }

//        this.prevYawForRotate = this.getYRot();
        this.updateCullingBox();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (RIAutomobileFrame.isRIAutomobileFrame(this.getFrame())) {
            return RIAutomobileEntityDimensionsRegistry.getEntityDimensions(this.getFrame());
        }
        return super.getDimensions(pose);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        SeatEntity emptySeat = this.getAvailableSeat();

        if (emptySeat != null) {
            if (!this.level().isClientSide()) {
                player.startRiding(emptySeat);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.interact(player, hand);
    }

    @Override
    public boolean hasSpaceForPassengers() {
        if (this.seats.isEmpty()) {
            return false;
        }

        for (SeatEntity seat : this.seats) {
            if (!seat.isVehicle()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean engineRunning() {
        if(!RIAutomobileFrame.isRIAutomobileFrame(this.getFrame())) return super.engineRunning();

        Entity driver = this.getFirstPassenger();
        return driver != null;
    }

    @Override
    public void accumulateCollisionAreas(Collection<CollisionArea> areas) {
        this.level().getEntitiesOfClass(
                Entity.class,
                this.getBoundingBox().inflate(3.0),
                (e) -> e != this && !this.isOnRIAutomobile(e)
        ).forEach((e) -> areas.add(CollisionArea.entity(e)));
    }

    @Override
    public void runOverEntities(Vec3 velocity) {
        AABB frontBox = this.getBoundingBox().move(velocity.scale(0.5D));
        Vec3 pushVel = velocity.add(0.0D, 0.1D, 0.0D).scale(3.0D);

        List<Entity> targets = this.level().getEntities(
                EntityTypeTest.forClass(Entity.class),
                frontBox,
                (entityx) -> entityx != this && !this.isOnRIAutomobile(entityx)
        );

        for (Entity entity : targets) {
            if (!entity.isInvulnerable() && entity instanceof LivingEntity living) {
                if (entity.getVehicle() != this) {
                    AutomobilityEntities.automobileDamageSource(this.level())
                            .ifPresent((dmg) -> living.hurt(dmg, ((AutomobileEntityAccessor) this).getHSpeed() * 10.0F));

                    entity.push(pushVel.x, pushVel.y, pushVel.z);
                }
            }
        }
    }

    public boolean isOnRIAutomobile(Entity entity) {
        if (entity == this || this.hasPassenger(entity)) return true;

        Entity vehicle = entity.getVehicle();
        if (vehicle instanceof SeatEntity seat) {
            if (seat.getVehicle() == this) return true;
        }

        if (entity instanceof HitboxEntity hitbox) {
            return hitbox.getAutomobile() == this;
        }

        return false;
    }

    private @Nullable SeatEntity getAvailableSeat() {
        for (SeatEntity seat : this.seats) {
            if (!seat.isVehicle()) {
                return seat;
            }
        }
        return null;
    }

    private void refreshSeats() {
        List<RIAutomobileSeatRegistry.SeatPos> seatPositions = RIAutomobileSeatRegistry.getSeats(this.getFrame());

        if (this.seats.size() != seatPositions.size()) {
            if (!this.level().isClientSide()) {
                this.seats.forEach(Entity::discard);
                this.seats.clear();
                for (int i = 0; i < seatPositions.size(); i++) {
                    SeatEntity seat = new SeatEntity(this.level(), this);
                    if (seat.startRiding(this, true)) {
                        this.seats.add(seat);
                        this.level().addFreshEntity(seat);
                    }
                }
            } else {
                this.seats.clear();
                for (Entity passenger : this.getPassengers()) {
                    if (passenger instanceof SeatEntity seat) {
                        this.seats.add(seat);
                    }
                }
            }
        }
    }

    private void refreshHitboxes() {
        List<RIAutomobileFrame.Hitbox> boxes = RIAutomobileHitboxRegistry.getHitboxes(this.getFrame());

        if (this.hitboxes.size() != boxes.size()) {
            if (!this.level().isClientSide()) {
                this.hitboxes.forEach(Entity::discard);
                this.hitboxes.clear();

                for (RIAutomobileFrame.Hitbox box : boxes) {
                    HitboxEntity hitbox = new HitboxEntity(this.level(), this, box);
                    this.hitboxes.add(hitbox);
                    this.level().addFreshEntity(hitbox);
                }
            } else {
                this.hitboxes.clear();
                for (Entity passenger : this.getPassengers()) {
                    if (passenger instanceof HitboxEntity hitbox) {
                        this.hitboxes.add(hitbox);
                    }
                }
            }
        }
    }

    private void updateCullingBox() {
        List<RIAutomobileFrame.Hitbox> hitboxDefs = RIAutomobileHitboxRegistry.getHitboxes(this.getFrame());

        if (hitboxDefs == null || hitboxDefs.isEmpty()) {
            this.cullingBox = this.getBoundingBox();
            return;
        }

        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        AABB mergedBox = this.getBoundingBox();

        for (RIAutomobileFrame.Hitbox def : hitboxDefs) {
            float halfWidth = def.width() / 2.0F;
            double minX = x + def.origin().x - halfWidth;
            double minY = y + def.origin().y;
            double minZ = z + def.origin().z - halfWidth;

            double maxX = x + def.origin().x + halfWidth;
            double maxY = y + def.origin().y + def.height();
            double maxZ = z + def.origin().z + halfWidth;

            AABB box = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
            mergedBox = mergedBox.minmax(box);
        }

        this.cullingBox = mergedBox.inflate(3.0);
    }

    public Vec3 getMeasuredMovement() {
        return this.position().subtract(((AutomobileEntityAccessor) this).getLastPosForDisplacement());
    }

    private void receiveVehicleCollisions() {
        if (((AutomobileEntityAccessor) this).isDecorative()) {
            return;
        }

        var collisions = new HashMap<AutomobileEntity, RIAutomobileHitboxRegistry.IncomingCollision>();

        for (var box : this.hitboxes) {
            var bbox = box.getBoundingBox().inflate(0.15);
            for (var hitbox : this.level().getEntities(EntityTypeTest.forClass(HitboxEntity.class), bbox, h -> h.getAutomobile() != this)) {
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
            var meToCollision = col.origin().subtract(this.position()).multiply(1, 0, 1);
            double hitScale = hadVehicleCollision <= 0 ? 0.15 : 0.07;
            hitScale *= (1 + col.inertia() / this.getFrame().weight()) * 0.5;
            Vec3 newAddVelocity = ((AutomobileEntityAccessor) this).getAddedVelocity().add(
                    meToCollision.reverse().normalize().scale(hitScale * (1 + 0.1 * Math.sqrt(col.velocity().length())) * col.depth().lengthSqr())
                            .multiply(1, 0, 1));

            ((AutomobileEntityAccessor) this).setAddedVelocity(newAddVelocity);

            if (hadVehicleCollision <= 0) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), AutomobilitySounds.COLLISION.require(), SoundSource.AMBIENT, 0.22f, 0.7f + (0.06f * (this.level().random.nextFloat() - 0.5f)), false);
                float engineSpeed = ((AutomobileEntityAccessor) this).getEngineSpeed();
                ((AutomobileEntityAccessor) this).setEngineSpeed(engineSpeed * 0.6f);
                hadVehicleCollision = 12;
            }
        }
    }
}
