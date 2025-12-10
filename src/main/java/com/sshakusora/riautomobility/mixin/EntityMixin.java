package com.sshakusora.riautomobility.mixin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.world.entity.Entity;
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

    @Inject(method = "addPassenger", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;copyOf(Ljava/util/Collection;)Lcom/google/common/collect/ImmutableList;"),cancellable = true)
    private void patchPlayerInsert(Entity passenger, CallbackInfo ci) {
        Entity e = (Entity) (Object) this;
        if(!(e instanceof AutomobileEntity)) return;

        AutomobileEntity self = (AutomobileEntity) e;
        if(!RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) return;

        List<Entity> list = new ArrayList<>(this.passengers);
        list.add(passenger);
        this.passengers = ImmutableList.copyOf(list);
        e.gameEvent(GameEvent.ENTITY_MOUNT, passenger);
        ci.cancel();
    }
}
