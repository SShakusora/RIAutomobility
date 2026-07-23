package com.sshakusora.riautomobility.integration.jei;

import com.sshakusora.riautomobility.item.RIAutomobilityItems;
import com.sshakusora.riautomobility.item.VehicleKeyItem;
import com.sshakusora.riautomobility.recipe.VehicleKeyResetRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public final class VehicleKeyResetRecipeJeiExtension implements ICraftingCategoryExtension {
    private static final UUID DISPLAY_VEHICLE_ID = new UUID(0L, 0L);

    private final VehicleKeyResetRecipe recipe;

    public VehicleKeyResetRecipeJeiExtension(VehicleKeyResetRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            ICraftingGridHelper craftingGridHelper,
            IFocusGroup focuses
    ) {
        ItemStack taggedKey = VehicleKeyItem.createBound(DISPLAY_VEHICLE_ID);
        ItemStack blankKey = new ItemStack(RIAutomobilityItems.VEHICLE_KEY.get());

        craftingGridHelper.createAndSetInputs(builder, List.of(List.of(taggedKey)), 0, 0);
        craftingGridHelper.createAndSetOutputs(builder, List.of(blankKey));
    }

    @Override
    public ResourceLocation getRegistryName() {
        return recipe.getId();
    }
}
