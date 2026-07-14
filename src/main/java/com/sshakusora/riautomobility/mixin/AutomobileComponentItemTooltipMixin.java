package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.model.RIAutomobileModels;
import io.github.foundationgames.automobility.automobile.AutomobileComponent;
import io.github.foundationgames.automobility.item.AutomobileComponentItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AutomobileComponentItem.class)
public abstract class AutomobileComponentItemTooltipMixin<T extends AutomobileComponent<T>> {
    @Final @Shadow(remap = false) protected String translationKey;
    @Shadow public abstract T getComponent(ItemStack stack);

    @Inject(method = "appendHoverText", at = @At("TAIL"), remap = false)
    private void appendMissingResourceHint(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context, CallbackInfo ci) {
        T component = this.getComponent(stack);
        if (stack.hasCustomHoverName()) {
            ResourceLocation id = component.getId();
            String componentTranslationKey = this.translationKey + "." + id.getNamespace() + "." + id.getPath();
            tooltip.removeIf(line -> line.getContents() instanceof TranslatableContents translatable
                    && translatable.getKey().equals(componentTranslationKey));
        }
        if (RIAutomobileModels.isMissingComponent(component.getId())) {
            tooltip.add(Component.translatable("tooltip.riautomobility.missing_car_pack_resources").withStyle(ChatFormatting.RED));
        }
    }
}
