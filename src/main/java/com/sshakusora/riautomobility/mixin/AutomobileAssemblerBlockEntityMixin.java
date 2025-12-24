package com.sshakusora.riautomobility.mixin;

import io.github.foundationgames.automobility.block.entity.AutomobileAssemblerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AutomobileAssemblerBlockEntity.class)
public class AutomobileAssemblerBlockEntityMixin {
    @Unique private final TagKey<Item> FORGE_WRENCH = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/wrench"));

    @Redirect(method = "handleItemInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z", ordinal = 0))
    private boolean allowForgeWrench(ItemStack stack, Item item) {
        return stack.is(item) || stack.is(FORGE_WRENCH);
    }
}
