package com.sshakusora.riautomobility.editor;

import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class VehicleComponentItemData {
    private static final String DISPLAY_NAME_TAG = "RIAutomobilityComponentDisplayName";
    private static final String AUTHOR_TAG = "RIAutomobilityComponentAuthor";

    private VehicleComponentItemData() {
    }

    public static void setDisplayName(ItemStack stack, String displayName) {
        stack.getOrCreateTag().putString(DISPLAY_NAME_TAG, displayName);
        stack.setHoverName(Component.literal(displayName).withStyle(Style.EMPTY.withItalic(false)));
    }

    public static String getDisplayName(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && tag.contains(DISPLAY_NAME_TAG, Tag.TAG_STRING)
                ? tag.getString(DISPLAY_NAME_TAG) : "";
    }

    public static void setAuthor(ItemStack stack, String author) {
        if (!author.isBlank()) stack.getOrCreateTag().putString(AUTHOR_TAG, author);
    }

    public static String getAuthor(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && tag.contains(AUTHOR_TAG, Tag.TAG_STRING)
                ? tag.getString(AUTHOR_TAG) : "";
    }

    public static void applyKnownMetadata(ItemStack stack, ResourceLocation componentId) {
        CarPackArchiveStore.ItemMetadata metadata =
                CarPackArchiveStore.findComponentMetadata(componentId);
        if (metadata == null || stack.isEmpty()) return;
        setDisplayName(stack, metadata.displayName());
        setAuthor(stack, metadata.author());
    }
}
