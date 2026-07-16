package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.editor.AutomobileItemTooltips;
import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import io.github.foundationgames.automobility.automobile.AutomobileData;
import io.github.foundationgames.automobility.item.AutomobileItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AutomobileItem.class)
public class AutomobileItemMixin {
    @Unique
    private static final AutomobileData riautomobility$data = new AutomobileData();

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true, remap = false)
    private void spawnRIAutomobile(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        var stack = context.getItemInHand();
        riautomobility$data.read(stack.getOrCreateTagElement("Automobile"));

        RIAutomobileEntity entity = new RIAutomobileEntity(context.getLevel());
        var pos = context.getClickLocation();
        entity.moveTo(pos.x, pos.y, pos.z, context.getHorizontalDirection().toYRot(), 0);
        entity.setComponents(riautomobility$data.getFrame(), riautomobility$data.getWheel(), riautomobility$data.getEngine());
        context.getLevel().addFreshEntity(entity);
        stack.shrink(1);
        cir.setReturnValue(InteractionResult.PASS);
    }

    @Inject(method = "appendHoverText", at = @At("RETURN"), remap = false)
    private void useKnownComponentNames(ItemStack stack, Level world, List<Component> tooltip,
                                        TooltipFlag context, CallbackInfo ci) {
        AutomobileItemTooltips.replaceKnownComponentNames(stack, tooltip);
    }
}
