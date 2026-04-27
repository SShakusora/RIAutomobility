package com.sshakusora.riautomobility.mixin;

import com.google.common.collect.ImmutableList;
import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(Entity.class)
public class EntityMixin {
    @Shadow private ImmutableList<Entity> passengers;

    @Inject(method = "addPassenger", at = @At("HEAD"), cancellable = true)
    private void patchPlayerInsert(Entity passenger, CallbackInfo ci) {
        Entity e = (Entity) (Object) this;
        if(!(e instanceof RIAutomobileEntity self)) return;
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;

        if (passenger.getVehicle() != e) {
            throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
        }

        if (this.passengers.isEmpty()) {
            this.passengers = ImmutableList.of(passenger);
        } else {
            List<Entity> list = new ArrayList<>(this.passengers);
            if (!e.level().isClientSide && passenger instanceof Player && !(self.getFirstPassenger() instanceof Player)) {
                list.add(0, passenger);
            } else {
                list.add(passenger);
            }
            this.passengers = ImmutableList.copyOf(list);
        }

        self.assignSeatForPassenger(passenger);
        self.snapPassengerToSeat(passenger);
        e.gameEvent(GameEvent.ENTITY_MOUNT, passenger);
        ci.cancel();
    }
}
