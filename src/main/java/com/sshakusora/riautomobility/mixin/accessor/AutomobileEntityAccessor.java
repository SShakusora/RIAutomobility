package com.sshakusora.riautomobility.mixin.accessor;

import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AutomobileEntity.class)
public interface AutomobileEntityAccessor {
    @Accessor("hSpeed")
    float getHSpeed();

    @Accessor("lastPosForDisplacement")
    Vec3 getLastPosForDisplacement();

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

    @Accessor("steering")
    float getSteeringRaw();

    @Accessor("steering")
    void setSteeringRaw(float steering);

    @Accessor("wheelAngle")
    float getWheelAngleRaw();

    @Accessor("wheelAngle")
    void setWheelAngleRaw(float wheelAngle);

    @Accessor("lastWheelAngle")
    void setLastWheelAngleRaw(float lastWheelAngle);

    @Accessor("accelerating")
    boolean isAccelerating();

    @Accessor("holdingDrift")
    boolean isHoldingDrift();

    @Accessor("prevHoldDrift")
    boolean wasHoldingDrift();
}
