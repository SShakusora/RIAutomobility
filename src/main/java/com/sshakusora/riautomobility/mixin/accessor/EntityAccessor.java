package com.sshakusora.riautomobility.mixin.accessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("dimensions")
    EntityDimensions getDimensions();

    @Accessor("dimensions")
    void setDimensions(EntityDimensions dimensions);
}