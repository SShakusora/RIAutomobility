package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.creative.RIAutomobilityCreativeTabs;
import com.sshakusora.riautomobility.editor.VehicleComponentItemData;
import io.github.foundationgames.automobility.automobile.AutomobileComponent;
import io.github.foundationgames.automobility.item.AutomobileComponentItem;
import io.github.foundationgames.automobility.item.AutomobileFrameItem;
import io.github.foundationgames.automobility.item.AutomobileWheelItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AutomobileComponentItem.class)
public class AutomobileComponentItemMixin {
    @Inject(method = "createStack", at = @At("RETURN"), remap = false)
    private void restoreCarPackMetadata(AutomobileComponent<?> component,
                                        CallbackInfoReturnable<ItemStack> cir) {
        VehicleComponentItemData.applyKnownMetadata(cir.getReturnValue(), component.getId());
    }

    @Inject(method = "addToCreative", at = @At("HEAD"), cancellable = true, remap = false)
    private void filterRIAutomobilityComponents(AutomobileComponent<?> component, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof AutomobileFrameItem || (Object) this instanceof AutomobileWheelItem) {
            if (RIAutomobilityCreativeTabs.isRIAutomobilityComponent(component)) {
                cir.setReturnValue(false);
            }
        }
    }
}
