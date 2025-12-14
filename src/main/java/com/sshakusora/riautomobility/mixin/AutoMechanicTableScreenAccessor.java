package com.sshakusora.riautomobility.mixin;

import io.github.foundationgames.automobility.screen.AutoMechanicTableScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AutoMechanicTableScreen.class)
public interface AutoMechanicTableScreenAccessor {
    @Accessor("time")
    long getTime();
}
