package com.sshakusora.riautomobility.entity;

import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.mixin.AutomobileEntityAccessor;
import com.sshakusora.riautomobility.util.RIAutomobileEntityDimensionsRegistry;
import com.sshakusora.riautomobility.util.RIAutomobileHitboxRegistry;
import com.sshakusora.riautomobility.util.RIAutomobileSeatRegistry;
import io.github.foundationgames.automobility.automobile.render.RenderableAutomobile;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import io.github.foundationgames.automobility.entity.AutomobilityEntities;
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
import java.util.List;

public class RIAutomobileEntity extends AutomobileEntity implements RenderableAutomobile{
    private AABB cullingBox = new AABB(0, 0, 0, 0, 0, 0);
    public final List<HitboxEntity> hitboxes = new ArrayList<>();
    public final List<SeatEntity> seats = new ArrayList<>();

    public RIAutomobileEntity(EntityType<?> type, Level world) {
        super(type, world);
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

//        TODO: 后续再处理额外逻辑
//        this.receiveVehicleCollisions();

        this.refreshSeats();
        this.refreshHitboxes();
        EntityDimensions targetDim = RIAutomobileEntityDimensionsRegistry.getEntityDimensions(this.getFrame());
        if (this.getDimensions(this.getPose()) != targetDim) {
            this.refreshDimensions();
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
        if (!this.level().isClientSide) {
            SeatEntity emptySeat = this.getAvailableSeat();
            if (emptySeat != null) {
                player.startRiding(emptySeat);
                return InteractionResult.SUCCESS;
            }
        }
        return super.interact(player, hand);
    }

    @Override
    public boolean hasSpaceForPassengers() {
        if (this.seats.isEmpty()) {
            this.refreshSeats();
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

    private boolean isOnRIAutomobile(Entity entity) {
        if (entity == this || this.hasPassenger(entity)) {
            return true;
        }

        if (entity instanceof HitboxEntity hitbox) {
            return hitbox.getAutomobile() == this;
        }

        for (SeatEntity seat : this.seats) {
            if (entity == seat) return true;
            if (seat.hasPassenger(entity)) return true;
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
        if (this.level().isClientSide()) return;

        List<RIAutomobileSeatRegistry.SeatPos> seatPositions = RIAutomobileSeatRegistry.getSeats(this.getFrame());

        if (this.seats.size() != seatPositions.size()) {
            this.seats.forEach(Entity::discard);
            this.seats.clear();

            for (int i = 0; i < seatPositions.size(); i++) {
                SeatEntity seat = new SeatEntity(this.level(), this);

                if (seat.startRiding(this, true)) {
                    this.seats.add(seat);
                    this.level().addFreshEntity(seat);
                }
            }
        }
    }

    private void refreshHitboxes() {
        if (this.level().isClientSide()) return;

        List<RIAutomobileFrame.Hitbox> boxes = RIAutomobileHitboxRegistry.getHitboxes(this.getFrame());

        if (this.hitboxes.size() != boxes.size()) {
            this.hitboxes.forEach(Entity::discard);
            this.hitboxes.clear();

            for (RIAutomobileFrame.Hitbox box : boxes) {
                HitboxEntity hitbox = new HitboxEntity(this.level(), this, box);

                this.hitboxes.add(hitbox);
                this.level().addFreshEntity(hitbox);
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

        this.cullingBox = mergedBox;
    }
}
