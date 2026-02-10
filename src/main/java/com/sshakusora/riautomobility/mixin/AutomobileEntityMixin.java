package com.sshakusora.riautomobility.mixin;

import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AutomobileEntity.class)
public class AutomobileEntityMixin {
    @ModifyVariable(method = "collisionStateTick", at = @At("STORE"), name = "start", remap = false)
    private BlockPos driftingFix(BlockPos original) {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        double y = self.getY();

        if(y <= 0){
            return new BlockPos(original.getX(), original.getY() - 1, original.getZ());
        } else {
            return original;
        }
    }
}
