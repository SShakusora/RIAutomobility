package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.entity.DriverSeatEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.util.RIAutomobileSeatRegistry;
import io.github.foundationgames.automobility.automobile.AutomobileStats;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import io.github.foundationgames.automobility.entity.AutomobilityEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
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
import java.util.List;
import java.util.Objects;

@Mixin(AutomobileEntity.class)
public class AutomobileEntityMixin {
    @Unique private float prevYawForRotate = 0.0F;

    @Shadow private float engineSpeed;
    @Final
    @Shadow private AutomobileStats stats;
    @Shadow private float hSpeed;

    @Inject(method = "positionRider", at = @At("HEAD"), cancellable = true)
    public void positionPassenger(Entity passenger, Entity.MoveFunction moveFunc, CallbackInfo ci) {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;
        List<Entity> passengers = self.getPassengers();
        int index = passengers.indexOf(passenger);
        if (index == -1) return;

        List<Vec3> seats = RIAutomobileSeatRegistry.getSeats(self.getFrame());
        if (index >= seats.size()) return;
        Vec3 local = seats.get(index);

        float pitch = self.getDisplacement().getAngularX(1.0F);
        float roll = self.getDisplacement().getAngularZ(1.0F);
        float vert = self.getDisplacement().getVertical(1.0F);

        Vec3 pos = self.position().add(local.yRot(-self.getYRot() * Mth.DEG_TO_RAD)
                .xRot(-pitch * Mth.DEG_TO_RAD)
                .zRot(-roll * Mth.DEG_TO_RAD));
        pos = pos.add(0, vert, 0);

        moveFunc.accept(passenger, pos.x, pos.y, pos.z);

        if(passenger != self.getFirstPassenger()){
            whenRotated(self.getYRot() - prevYawForRotate, passenger);
        }
        ci.cancel();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lio/github/foundationgames/automobility/entity/AutomobileEntity;position()Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
    public void tick(CallbackInfo ci) {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;
        prevYawForRotate = self.getYRot();
    }

    @Inject(method = "hasSpaceForPassengers", at = @At("HEAD"), cancellable = true, remap = false)
    public void extendSpaceForPassengers(CallbackInfoReturnable<Boolean> cir) {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;
        List<Entity> passengers = self.getPassengers();
        List<Vec3> seats = RIAutomobileSeatRegistry.getSeats(self.getFrame());
        cir.setReturnValue(passengers.size() < seats.size());
    }

    @Inject(method = "provideClientInput", at = @At("HEAD"), cancellable = true, remap = false)
    public void disableNotDriverInput(boolean fwd, boolean back, boolean left, boolean right, boolean space, CallbackInfo ci) {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;

        List<Entity> passengers = self.getPassengers();
        if(passengers.isEmpty() || passengers.get(0).getFirstPassenger() != player) ci.cancel();
    }

    @Inject(method = "boost", at = @At(value = "INVOKE", target = "Lio/github/foundationgames/automobility/entity/AutomobileEntity;isControlledByLocalInstance()Z"), cancellable = true)
    public void passengerBoostFix(CallbackInfo ci) {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;
        this.engineSpeed = Math.max(this.engineSpeed, this.stats.getComfortableSpeed() * 0.5F);
        ci.cancel();
    }

    @Inject(method = "engineRunning", at = @At("HEAD"), cancellable = true, remap = false)
    public void RIAutomobileEngineRunning(CallbackInfoReturnable<Boolean> cir) {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;

        if(self.isVehicle()){
            Entity driver = self.getFirstPassenger();
            if(!(driver instanceof DriverSeatEntity seat)) return;

            Entity realDriver = seat.getFirstPassenger();
            if(realDriver == null) cir.setReturnValue(self.getBoostTimer() > 0);
            else cir.setReturnValue(true);
        }
    }

    @Inject(method = "interact", at = @At(value = "INVOKE", target = "Lio/github/foundationgames/automobility/entity/AutomobileEntity;hasSpaceForPassengers()Z"), cancellable = true)
    public void RIAutomobileInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;

        Entity seat = self.getFirstPassenger();
        Entity driver = null;
        if (seat != null) {
            driver = seat.getFirstPassenger();
        }
        if(!self.hasSpaceForPassengers()) {
            if(!Objects.requireNonNull(self.getFirstPassenger()).isVehicle()){
                if (!self.level().isClientSide()){
                    player.startRiding(self.getFirstPassenger(), true);
                }
                cir.setReturnValue(InteractionResult.sidedSuccess(self.level().isClientSide()));
                cir.cancel();
            } else {
                for(Entity e : self.getPassengers()){
                    if(e == self.getFirstPassenger()) continue;
                    if(!(e instanceof Player)) {
                        if (!self.level().isClientSide()){
                            e.stopRiding();
                        }
                        cir.setReturnValue(InteractionResult.sidedSuccess(self.level().isClientSide()));
                        cir.cancel();
                    }
                }
                cir.setReturnValue(InteractionResult.PASS);
                cir.cancel();
            }
        } else {
            if (!self.level().isClientSide()) {
                if(driver == null) {
                    player.startRiding(seat);
                } else {
                    player.startRiding(self);
                }
            }

            cir.setReturnValue(InteractionResult.sidedSuccess(self.level().isClientSide()));
        }
    }

    /*
    Redirect FirstPassenger to player sitting on DriverSeatEntity.
     */
    @Redirect(method = "forNearbyPlayers", at = @At(value = "INVOKE", target = "Lio/github/foundationgames/automobility/entity/AutomobileEntity;getFirstPassenger()Lnet/minecraft/world/entity/Entity;"))
    private Entity redirectGetFirstPassengerA(AutomobileEntity ae){
        return redirectDriver(ae);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lio/github/foundationgames/automobility/entity/AutomobileEntity;getFirstPassenger()Lnet/minecraft/world/entity/Entity;"))
    private Entity redirectGetFirstPassengerB(AutomobileEntity ae){
        return redirectDriver(ae);
    }

    @Redirect(method = "postMovementTick", at = @At(value = "INVOKE", target = "Lio/github/foundationgames/automobility/entity/AutomobileEntity;getFirstPassenger()Lnet/minecraft/world/entity/Entity;"))
    private Entity redirectGetFirstPassengerC(AutomobileEntity ae){
        return redirectDriver(ae);
    }

    @Redirect(method = "getControllingPassenger", at = @At(value = "INVOKE", target = "Lio/github/foundationgames/automobility/entity/AutomobileEntity;getFirstPassenger()Lnet/minecraft/world/entity/Entity;"))
    private Entity redirectGetFirstPassengerD(AutomobileEntity ae){
        return redirectDriver(ae);
    }

    @Inject(method = "runOverEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;", shift = At.Shift.AFTER), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private void redirectGetFirstPassengerE(Vec3 velocity, CallbackInfo ci, AABB frontBox) {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
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

    @ModifyVariable(method = "collisionStateTick", at = @At("STORE"), name = "start", remap = false)
    private BlockPos driftingFix(BlockPos original) {
        return new BlockPos(original.getX(), original.getY() - 1, original.getZ());
    }

    @Unique
    private Entity redirectDriver(AutomobileEntity ae){
        Entity first = ae.getFirstPassenger();
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return first;

        if(first instanceof DriverSeatEntity seat){
            Entity real = seat.getFirstPassenger();
            if (real instanceof Player) return real;
        }
        return first;
    }

    @Unique
    private boolean isOnRIAutomobile(Entity entity){
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return false;

        List<Entity> passengerListPre = self.getPassengers();
        List<Entity> passengerListPost = new ArrayList<>(passengerListPre);

        passengerListPost.set(0, passengerListPost.get(0).getFirstPassenger());

        return passengerListPost.contains(entity);
    }

    @Unique
    public void whenRotated(float dYaw, Entity e) {
        e.setYRot(Mth.wrapDegrees(e.getYRot() + dYaw));
        e.setYBodyRot(Mth.wrapDegrees(e.getYRot() + dYaw));
    }
}
