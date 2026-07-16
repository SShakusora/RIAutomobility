package com.sshakusora.riautomobility.editor;

import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class AutomobileItemTooltips {
    private static final String AUTOMOBILE_TAG = "Automobile";

    private AutomobileItemTooltips() {
    }

    public static void replaceKnownComponentNames(ItemStack stack, List<Component> tooltip) {
        CompoundTag automobile = stack.getTagElement(AUTOMOBILE_TAG);
        if (automobile == null) return;

        replaceComponentName(tooltip, "tooltip.automobility.frameLabel", automobile.getString("frame"));
        replaceComponentName(tooltip, "tooltip.automobility.wheelLabel", automobile.getString("wheels"));
        replaceComponentName(tooltip, "tooltip.automobility.engineLabel", automobile.getString("engine"));
    }

    private static void replaceComponentName(List<Component> tooltip, String labelKey, String componentValue) {
        ResourceLocation componentId = ResourceLocation.tryParse(componentValue);
        if (componentId == null) return;
        CarPackArchiveStore.ItemMetadata metadata = CarPackArchiveStore.findComponentMetadata(componentId);
        if (metadata == null) return;

        for (int index = 0; index < tooltip.size(); index++) {
            Component line = tooltip.get(index);
            if (line.getContents() instanceof TranslatableContents translatable
                    && labelKey.equals(translatable.getKey())) {
                tooltip.set(index, Component.translatable(labelKey)
                        .withStyle(ChatFormatting.BLUE)
                        .append(Component.literal(metadata.displayName())
                                .withStyle(ChatFormatting.DARK_GREEN)));
                return;
            }
        }
    }
}
