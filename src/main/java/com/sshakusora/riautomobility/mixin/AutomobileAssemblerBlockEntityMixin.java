package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import io.github.foundationgames.automobility.block.entity.AutomobileAssemblerBlockEntity;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

    @Redirect(method = "tryConstructAutomobile", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/Level;)Lio/github/foundationgames/automobility/entity/AutomobileEntity;"), remap = false)
    private AutomobileEntity redirectAutomobileConstruction(Level level) {
        AutomobileAssemblerBlockEntity self = (AutomobileAssemblerBlockEntity) (Object) this;
        if (RIAutomobileFrame.isRIAutomobileFrame(self.getFrame())) {
            return new RIAutomobileEntity(level);
        }

        return new AutomobileEntity(level);
    }
}
