package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.util.RIAutomobileSeatRegistry;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AutomobileEntity.class)
public class AutomobileEntityMixin {
    @Inject(method = "positionRider", at = @At("HEAD"), cancellable = true)
    public void positionRiderMixin(Entity passenger, Entity.MoveFunction moveFunc, CallbackInfo ci) {
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
        ci.cancel();
    }

    @Inject(method = "hasSpaceForPassengers", at = @At("HEAD"), cancellable = true, remap = false)
    public void hasSpaceForPassengersMixin(CallbackInfoReturnable<Boolean> cir) {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;
        List<Entity> passengers = self.getPassengers();
        List<Vec3> seats = RIAutomobileSeatRegistry.getSeats(self.getFrame());
        cir.setReturnValue(passengers.size() < seats.size());
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 1)
    private void onTickRotatePassengers(CallbackInfo ci) {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        if (!self.level().isClientSide()) return;

        if (!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;

        float yawDelta = self.getYRot() - self.yRotO;
        if (Math.abs(yawDelta) < 0.01f) return;

        for (Entity passenger : self.getPassengers()) {
            if (passenger instanceof Player player) {
                if (passenger == self.getFirstPassenger()) continue;

                float newYaw = Mth.wrapDegrees(player.getYRot() + yawDelta);
                player.setYRot(newYaw);
                player.setYBodyRot(newYaw);
            }
        }
    }

    @Inject(method = "provideClientInput", at = @At("HEAD"), cancellable = true, remap = false)
    public void refuseNotDriverInput(CallbackInfo ci) {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        if (!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;

        List<Entity> passengers = self.getPassengers();
        if(passengers.isEmpty() || passengers.get(0) != player) ci.cancel();
    }
}
