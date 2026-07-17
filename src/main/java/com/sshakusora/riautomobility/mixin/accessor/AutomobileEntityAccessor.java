package com.sshakusora.riautomobility.mixin.accessor;

import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AutomobileEntity.class)
public interface AutomobileEntityAccessor {
    @Accessor("decorative")
    boolean isDecorative();

    @Accessor("addedVelocity")
    Vec3 getAddedVelocity();

    @Accessor("addedVelocity")
    void setAddedVelocity(Vec3 addedVelocity);

    @Accessor("engineSpeed")
    float getEngineSpeed();

    @Accessor("engineSpeed")
    void setEngineSpeed(float engineSpeed);
}
