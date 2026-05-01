package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.model.RIAutomobileModels;
import io.github.foundationgames.automobility.automobile.AutomobileComponent;
import io.github.foundationgames.automobility.item.AutomobileComponentItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AutomobileComponentItem.class)
public abstract class AutomobileComponentItemTooltipMixin<T extends AutomobileComponent<T>> {
    @Shadow public abstract T getComponent(ItemStack stack);

    @Inject(method = "appendHoverText", at = @At("TAIL"), remap = false)
    private void appendMissingResourceHint(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context, CallbackInfo ci) {
        T component = this.getComponent(stack);
        if (RIAutomobileModels.isMissingComponent(component.getId())) {
            tooltip.add(Component.translatable("tooltip.riautomobility.missing_resource_pack").withStyle(ChatFormatting.RED));
        }
    }
}
