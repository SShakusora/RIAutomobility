package com.sshakusora.riautomobility.events;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.editor.VehicleComponentItemData;
import com.sshakusora.riautomobility.model.RIAutomobileModels;
import io.github.foundationgames.automobility.item.AutomobileComponentItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID, value = Dist.CLIENT)
public final class AutomobileComponentTooltipEvents {
    private AutomobileComponentTooltipEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (!(stack.getItem() instanceof AutomobileComponentItem<?> componentItem)) {
            return;
        }

        var component = componentItem.getComponent(stack);
        String displayName = VehicleComponentItemData.getDisplayName(stack);
        if (displayName.isBlank() && stack.hasCustomHoverName()
                && isGeneratedComponent(component.getId())) {
            displayName = stack.getHoverName().getString();
        }
        if (!displayName.isBlank()) {
            replaceItemTypeName(event.getToolTip(), stack.getItem().getName(stack));
            replaceComponentName(event.getToolTip(), component.getId(), displayName);
        }
        String author = VehicleComponentItemData.getAuthor(stack);
        if (!author.isBlank()) {
            insertAuthor(event.getToolTip(), author);
        }
        if (RIAutomobileModels.isMissingComponent(component.getId())) {
            event.getToolTip().add(Component.translatable(
                    "tooltip.riautomobility.missing_car_pack_resources").withStyle(ChatFormatting.RED));
        }
    }

    static void replaceItemTypeName(List<Component> tooltip, Component itemTypeName) {
        if (tooltip.isEmpty()) return;
        tooltip.set(0, itemTypeName.copy().withStyle(tooltip.get(0).getStyle().withItalic(false)));
    }

    static void replaceComponentName(List<Component> tooltip, ResourceLocation componentId, String displayName) {
        String translationSuffix = "." + componentId.getNamespace() + "." + componentId.getPath();
        tooltip.removeIf(line -> line.getContents() instanceof TranslatableContents translatable
                && translatable.getKey().endsWith(translationSuffix));
        tooltip.add(Math.min(1, tooltip.size()),
                Component.literal(displayName).withStyle(ChatFormatting.BLUE));
    }

    static boolean isGeneratedComponent(ResourceLocation componentId) {
        return "riautomobility".equals(componentId.getNamespace())
                && componentId.getPath().matches("auto_[0-9a-f]{32}");
    }

    static void insertAuthor(List<Component> tooltip, String author) {
        tooltip.add(Math.min(2, tooltip.size()), Component.translatable(
                "tooltip.riautomobility.component_author", author).withStyle(ChatFormatting.GRAY));
    }
}
