package com.sshakusora.riautomobility.recipe;

import com.sshakusora.riautomobility.item.RIAutomobilityItems;
import com.sshakusora.riautomobility.item.VehicleKeyItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class VehicleKeyResetRecipe extends CustomRecipe {
    public VehicleKeyResetRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return containsOnlyTaggedKey(container);
    }

    private static boolean containsOnlyTaggedKey(CraftingContainer container) {
        boolean foundKey = false;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (foundKey || !VehicleKeyItem.isKey(stack) || !stack.hasTag()) {
                return false;
            }
            foundKey = true;
        }

        return foundKey;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        return containsOnlyTaggedKey(container)
                ? new ItemStack(RIAutomobilityItems.VEHICLE_KEY.get())
                : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RIAutomobilityRecipes.VEHICLE_KEY_RESET.get();
    }
}
