package com.sshakusora.riautomobility.integration.jei;

import com.sshakusora.riautomobility.mixin.accessor.AutoMechanicTableRecipeAccessor;
import io.github.foundationgames.automobility.Automobility;
import io.github.foundationgames.automobility.recipe.AutoMechanicTableRecipe;
import io.github.foundationgames.automobility.screen.AutoMechanicTableScreenHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AutoMechanicTableRecipeTransferHandler implements IRecipeTransferHandler<AutoMechanicTableScreenHandler, AutoMechanicTableRecipe> {

    private final IRecipeTransferHandlerHelper helper;

    public AutoMechanicTableRecipeTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<AutoMechanicTableScreenHandler> getContainerClass() {
        return AutoMechanicTableScreenHandler.class;
    }

    @Override
    public Optional<MenuType<AutoMechanicTableScreenHandler>> getMenuType() {
        return Optional.of(Automobility.AUTO_MECHANIC_SCREEN.require());
    }

    @Override
    public RecipeType<AutoMechanicTableRecipe> getRecipeType() {
        return AutoMechanicTableRecipeCategory.RECIPE_TYPE;
    }

    @Override
    public IRecipeTransferError transferRecipe(
            AutoMechanicTableScreenHandler container,
            AutoMechanicTableRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            Player player,
            boolean maxTransfer,
            boolean doTransfer
    ) {
        // Find recipe index in the container
        int recipeId = -1;
        for (int i = 0; i < container.recipes.size(); i++) {
            if (container.recipes.get(i).getId().equals(recipe.getId())) {
                recipeId = i;
                break;
            }
        }

        if (recipeId == -1) {
            return helper.createInternalError();
        }

        AutoMechanicTableRecipeAccessor accessor = (AutoMechanicTableRecipeAccessor) recipe;
        List<Ingredient> ingredients = new ArrayList<>(accessor.getIngredients());

        List<IRecipeSlotView> missingSlots = getMissingSlots(container, ingredients, recipeSlotsView);

        if (!doTransfer) {
            if (!missingSlots.isEmpty()) {
                return helper.createUserErrorForMissingSlots(
                        Component.translatable("jei.tooltip.error.recipe.transfer.missing"),
                        missingSlots
                );
            }
            return null;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) {
            return helper.createInternalError();
        }

        // 1. Select the recipe
        container.clickMenuButton(player, recipeId);
        mc.gameMode.handleInventoryButtonClick(container.containerId, recipeId);

        // 2. Transfer items
        transferItems(container, ingredients, mc, player, maxTransfer);

        return null;
    }

    private static boolean ingredientHasNbt(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return items.length > 0 && items[0].hasTag();
    }

    private static boolean matchesIngredient(ItemStack stack, Ingredient ingredient) {
        if (stack.isEmpty()) return false;
        if (ingredientHasNbt(ingredient)) {
            return ItemStack.isSameItemSameTags(stack, ingredient.getItems()[0]);
        }
        return ingredient.test(stack);
    }

    private static List<IRecipeSlotView> getMissingSlots(AutoMechanicTableScreenHandler container, List<Ingredient> ingredients, IRecipeSlotsView recipeSlotsView) {
        List<ItemStack> available = new ArrayList<>();

        // Input slots (0-8)
        for (int i = 0; i < 9; i++) {
            available.add(container.getSlot(i).getItem().copy());
        }
        // Player inventory (10-45)
        for (int i = 10; i < 46; i++) {
            available.add(container.getSlot(i).getItem().copy());
        }

        List<IRecipeSlotView> missing = new ArrayList<>();
        List<IRecipeSlotView> inputSlots = recipeSlotsView.getSlotViews(RecipeIngredientRole.INPUT);

        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            boolean found = false;
            for (ItemStack stack : available) {
                if (matchesIngredient(stack, ingredient)) {
                    stack.shrink(1);
                    found = true;
                    break;
                }
            }
            if (!found && i < inputSlots.size()) {
                missing.add(inputSlots.get(i));
            }
        }
        return missing;
    }

    /**
     * Calculates which ingredients are missing from the input slots,
     * taking into account that multiple ingredients may require the same item type.
     */
    private static List<Ingredient> getMissingIngredients(AutoMechanicTableScreenHandler container, List<Ingredient> ingredients) {
        // Copy current input slot contents
        List<ItemStack> available = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            available.add(container.getSlot(i).getItem().copy());
        }

        List<Ingredient> missing = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (int j = 0; j < available.size(); j++) {
                ItemStack stack = available.get(j);
                if (matchesIngredient(stack, ingredient)) {
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        available.remove(j);
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                missing.add(ingredient);
            }
        }
        return missing;
    }

    private static void transferItems(AutoMechanicTableScreenHandler container, List<Ingredient> ingredients, Minecraft mc, Player player, boolean maxTransfer) {
        List<Ingredient> missing = getMissingIngredients(container, ingredients);

        for (Ingredient ingredient : missing) {
            int targetSlot = findSlotForIngredient(container, ingredient);
            if (targetSlot == -1) continue;

            if (!transferOneToSlot(container, ingredient, targetSlot, mc, player)) {
                continue;
            }

            if (maxTransfer) {
                fillSlot(container, ingredient, targetSlot, mc, player);
            }
        }
    }

    private static int findSlotForIngredient(AutoMechanicTableScreenHandler container, Ingredient ingredient) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = container.getSlot(i).getItem();
            if (matchesIngredient(stack, ingredient) && stack.getCount() < stack.getMaxStackSize()) {
                return i;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (container.getSlot(i).getItem().isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static boolean transferOneToSlot(AutoMechanicTableScreenHandler container, Ingredient ingredient, int targetSlot, Minecraft mc, Player player) {
        for (int j = 10; j < 46; j++) {
            ItemStack stack = container.getSlot(j).getItem();
            if (!matchesIngredient(stack, ingredient)) continue;

            if (stack.getCount() == 1) {
                mc.gameMode.handleInventoryMouseClick(container.containerId, j, 0, ClickType.PICKUP, player);
                mc.gameMode.handleInventoryMouseClick(container.containerId, targetSlot, 0, ClickType.PICKUP, player);
            } else {
                mc.gameMode.handleInventoryMouseClick(container.containerId, j, 0, ClickType.PICKUP, player);
                mc.gameMode.handleInventoryMouseClick(container.containerId, targetSlot, 1, ClickType.PICKUP, player);
                mc.gameMode.handleInventoryMouseClick(container.containerId, j, 0, ClickType.PICKUP, player);
            }
            return true;
        }
        return false;
    }

    private static void fillSlot(AutoMechanicTableScreenHandler container, Ingredient ingredient, int targetSlot, Minecraft mc, Player player) {
        ItemStack target = container.getSlot(targetSlot).getItem();
        while (!target.isEmpty() && target.getCount() < target.getMaxStackSize()) {
            boolean transferred = false;
            for (int j = 10; j < 46; j++) {
                ItemStack stack = container.getSlot(j).getItem();
                if (!matchesIngredient(stack, ingredient)) continue;

                if (stack.getCount() == 1) {
                    mc.gameMode.handleInventoryMouseClick(container.containerId, j, 0, ClickType.PICKUP, player);
                    mc.gameMode.handleInventoryMouseClick(container.containerId, targetSlot, 0, ClickType.PICKUP, player);
                } else {
                    mc.gameMode.handleInventoryMouseClick(container.containerId, j, 0, ClickType.PICKUP, player);
                    mc.gameMode.handleInventoryMouseClick(container.containerId, targetSlot, 1, ClickType.PICKUP, player);
                    mc.gameMode.handleInventoryMouseClick(container.containerId, j, 0, ClickType.PICKUP, player);
                }
                transferred = true;
                break;
            }

            if (!transferred) break;
            target = container.getSlot(targetSlot).getItem();
        }
    }
}
