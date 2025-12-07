package com.sshakusora.riautomobility.mixin;

import io.github.foundationgames.automobility.entity.AutomobileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AutomobileEntity.class)
public interface AutomobileEntityAccessor {
    @Invoker("inLockedViewMode")
    static boolean inLockedViewMode() {
        throw new AssertionError();
    }
}
