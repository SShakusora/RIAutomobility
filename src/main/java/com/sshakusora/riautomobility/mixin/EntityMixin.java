package com.sshakusora.riautomobility.mixin;

import com.google.common.collect.ImmutableList;
import com.sshakusora.riautomobility.entity.DriverSeatEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Entity.class)
public class EntityMixin {
    @Shadow private ImmutableList<Entity> passengers;

    @Inject(method = "addPassenger", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;copyOf(Ljava/util/Collection;)Lcom/google/common/collect/ImmutableList;"),cancellable = true)
    private void patchPlayerInsert(Entity passenger, CallbackInfo ci) {
        Entity e = (Entity) (Object) this;
        if(!(e instanceof AutomobileEntity self)) return;

        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;

        List<Entity> list = new ArrayList<>(this.passengers);
        list.add(passenger);
        this.passengers = ImmutableList.copyOf(list);
        e.gameEvent(GameEvent.ENTITY_MOUNT, passenger);
        ci.cancel();
    }

    @Inject(method = "getDismountLocationForPassenger", at = @At("HEAD"), cancellable = true)
    private void fixAutomobileDismountLocation(LivingEntity passenger, CallbackInfoReturnable<Vec3> cir) {
        Entity e = (Entity) (Object) this;
        if(e instanceof AutomobileEntity self && RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) {
            cir.setReturnValue(calDismountLocation(self, passenger));
        } else if (e instanceof DriverSeatEntity seat) {
            AutomobileEntity auto = (AutomobileEntity) seat.getVehicle();
            if(auto != null) {
                cir.setReturnValue(calDismountLocation(auto, seat));
            }
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
}
