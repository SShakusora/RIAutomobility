package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.creative.RIAutomobilityCreativeTabs;
import io.github.foundationgames.automobility.automobile.AutomobileComponent;
import io.github.foundationgames.automobility.item.AutomobileComponentItem;
import io.github.foundationgames.automobility.item.AutomobileFrameItem;
import io.github.foundationgames.automobility.item.AutomobileWheelItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AutomobileComponentItem.class)
public class AutomobileComponentItemMixin {
    @Inject(method = "addToCreative", at = @At("HEAD"), cancellable = true, remap = false)
    private void filterRIAutomobilityComponents(AutomobileComponent<?> component, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof AutomobileFrameItem || (Object) this instanceof AutomobileWheelItem) {
            if (RIAutomobilityCreativeTabs.isRIAutomobilityComponent(component)) {
                cir.setReturnValue(false);
            }
        }
    }
}
