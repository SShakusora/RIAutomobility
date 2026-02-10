package com.sshakusora.riautomobility.mixin;

import io.github.foundationgames.automobility.entity.AutomobileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AutomobileEntity.class)
public interface AutomobileEntityAccessor {
    @Accessor("hSpeed")
    float getHSpeed();
}
