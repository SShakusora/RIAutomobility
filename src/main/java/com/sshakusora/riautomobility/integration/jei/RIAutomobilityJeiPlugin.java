package com.sshakusora.riautomobility.integration.jei;

import com.sshakusora.riautomobility.RIAutomobility;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import io.github.foundationgames.automobility.automobile.attachment.FrontAttachmentType;
import io.github.foundationgames.automobility.automobile.attachment.RearAttachmentType;
import io.github.foundationgames.automobility.block.AutomobilityBlocks;
import io.github.foundationgames.automobility.item.AutomobileComponentItem;
import io.github.foundationgames.automobility.item.AutomobilityItems;
import io.github.foundationgames.automobility.recipe.AutoMechanicTableRecipe;
import io.github.foundationgames.automobility.screen.AutoMechanicTableScreenHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class RIAutomobilityJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return RIAutomobility.rl("jei_plugin");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        IIngredientSubtypeInterpreter<ItemStack> interpreter = (stack, context) -> {
            if (stack.getItem() instanceof AutomobileComponentItem<?> item) {
                var component = item.getComponent(stack);
                if (component != null && !component.isEmpty()) {
                    return component.getId().toString();
                }
            }
            return IIngredientSubtypeInterpreter.NONE;
        };

        registration.registerSubtypeInterpreter(AutomobilityItems.AUTOMOBILE_FRAME.require(), interpreter);
        registration.registerSubtypeInterpreter(AutomobilityItems.AUTOMOBILE_ENGINE.require(), interpreter);
        registration.registerSubtypeInterpreter(AutomobilityItems.AUTOMOBILE_WHEEL.require(), interpreter);
        registration.registerSubtypeInterpreter(AutomobilityItems.FRONT_ATTACHMENT.require(), interpreter);
        registration.registerSubtypeInterpreter(AutomobilityItems.REAR_ATTACHMENT.require(), interpreter);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AutoMechanicTableRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        IIngredientManager ingredientManager = registration.getIngredientManager();

        List<ItemStack> frameStacks = new ArrayList<>();
        AutomobileFrame.REGISTRY.forEach(frame -> {
            if (!frame.isEmpty()) {
                frameStacks.add(AutomobilityItems.AUTOMOBILE_FRAME.require().createStack(frame));
            }
        });
        ingredientManager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, frameStacks);

        List<ItemStack> engineStacks = new ArrayList<>();
        AutomobileEngine.REGISTRY.forEach(engine -> {
            if (!engine.isEmpty()) {
                engineStacks.add(AutomobilityItems.AUTOMOBILE_ENGINE.require().createStack(engine));
            }
        });
        ingredientManager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, engineStacks);

        List<ItemStack> wheelStacks = new ArrayList<>();
        AutomobileWheel.REGISTRY.forEach(wheel -> {
            if (!wheel.isEmpty()) {
                wheelStacks.add(AutomobilityItems.AUTOMOBILE_WHEEL.require().createStack(wheel));
            }
        });
        ingredientManager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, wheelStacks);

        List<ItemStack> frontAttachmentStacks = new ArrayList<>();
        FrontAttachmentType.REGISTRY.forEach(attachment -> {
            if (!attachment.isEmpty()) {
                frontAttachmentStacks.add(AutomobilityItems.FRONT_ATTACHMENT.require().createStack(attachment));
            }
        });
        ingredientManager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, frontAttachmentStacks);

        List<ItemStack> rearAttachmentStacks = new ArrayList<>();
        RearAttachmentType.REGISTRY.forEach(attachment -> {
            if (!attachment.isEmpty()) {
                rearAttachmentStacks.add(AutomobilityItems.REAR_ATTACHMENT.require().createStack(attachment));
            }
        });
        ingredientManager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, rearAttachmentStacks);

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        RecipeManager recipeManager = level.getRecipeManager();
        List<AutoMechanicTableRecipe> recipes = recipeManager.getAllRecipesFor(AutoMechanicTableRecipe.TYPE);
        registration.addRecipes(AutoMechanicTableRecipeCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                AutoMechanicTableScreenHandler.class,
                io.github.foundationgames.automobility.Automobility.AUTO_MECHANIC_SCREEN.require(),
                AutoMechanicTableRecipeCategory.RECIPE_TYPE,
                0,   // recipeSlotStart (input slots 0-8)
                9,   // recipeSlotCount
                10,  // inventorySlotStart
                36   // inventorySlotCount
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(AutomobilityBlocks.AUTO_MECHANIC_TABLE.require()), AutoMechanicTableRecipeCategory.RECIPE_TYPE);
    }
}
