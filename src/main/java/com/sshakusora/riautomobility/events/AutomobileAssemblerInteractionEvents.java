package com.sshakusora.riautomobility.events;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.item.VehicleKeyItem;
import io.github.foundationgames.automobility.block.entity.AutomobileAssemblerBlockEntity;
import io.github.foundationgames.automobility.item.AutomobileEngineItem;
import io.github.foundationgames.automobility.item.AutomobileFrameItem;
import io.github.foundationgames.automobility.item.AutomobileWheelItem;
import io.github.foundationgames.automobility.item.AutomobilityItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID, value = Dist.CLIENT)
public final class AutomobileAssemblerInteractionEvents {
    private static final TagKey<Item> FORGE_WRENCH = TagKey.create(
            Registries.ITEM, new ResourceLocation("forge", "tools/wrench"));

    private AutomobileAssemblerInteractionEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !VehicleKeyItem.isKey(event.getEntity().getOffhandItem())
                || !(event.getLevel().getBlockEntity(event.getPos())
                instanceof AutomobileAssemblerBlockEntity assembler)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        boolean hasFrame = !assembler.getFrame().isEmpty();
        boolean wouldPassOnClient = stack.is(AutomobilityItems.CROWBAR.require())
                || stack.is(FORGE_WRENCH)
                || !hasFrame && item instanceof AutomobileFrameItem
                || hasFrame && (
                        assembler.getEngine().isEmpty() && item instanceof AutomobileEngineItem
                                || item instanceof AutomobileWheelItem
                );
        if (wouldPassOnClient) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
