package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.util.RIAutomobileSeatRegistry;
import io.github.foundationgames.automobility.automobile.AutomobileStats;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AutomobileEntity.class)
public class AutomobileEntityMixin {
    @Unique private float prevYawForRotate = 0.0F;

    @Shadow private float engineSpeed;
    @Final
    @Shadow private AutomobileStats stats;

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
    public void disableNotDriverInput(CallbackInfo ci) {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;

        List<Entity> passengers = self.getPassengers();
        if(passengers.isEmpty() || passengers.get(0) != player) ci.cancel();
    }

    @Inject(method = "boost", at = @At(value = "INVOKE", target = "Lio/github/foundationgames/automobility/entity/AutomobileEntity;isControlledByLocalInstance()Z"), cancellable = true)
    public void passengerBoostFix(CallbackInfo ci) {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;
        this.engineSpeed = Math.max(this.engineSpeed, this.stats.getComfortableSpeed() * 0.5F);
        ci.cancel();
    }

    @ModifyVariable(method = "collisionStateTick", at = @At("STORE"), name = "start", remap = false)
    private BlockPos driftingFix(BlockPos original) {
        return new BlockPos(original.getX(), original.getY() - 1, original.getZ());
    }

    @Unique
    public void whenRotated(float dYaw, Entity e) {
        e.setYRot(Mth.wrapDegrees(e.getYRot() + dYaw));
        e.setYBodyRot(Mth.wrapDegrees(e.getYRot() + dYaw));
    }
}
