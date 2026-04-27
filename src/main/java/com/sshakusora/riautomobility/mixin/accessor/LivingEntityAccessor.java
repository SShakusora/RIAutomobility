package com.sshakusora.riautomobility.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("yBodyRotO")
    void setYBodyRotOld(float yBodyRotOld);

    @Accessor("yHeadRotO")
    void setYHeadRotOld(float yHeadRotOld);
}
