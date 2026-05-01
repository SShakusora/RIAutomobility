package com.sshakusora.riautomobility.integration.jei;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.mixin.accessor.AutoMechanicTableRecipeAccessor;
import io.github.foundationgames.automobility.block.AutomobilityBlocks;
import io.github.foundationgames.automobility.recipe.AutoMechanicTableRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("removal")
public class AutoMechanicTableRecipeCategory implements IRecipeCategory<AutoMechanicTableRecipe> {
    public static final RecipeType<AutoMechanicTableRecipe> RECIPE_TYPE = RecipeType.create("automobility", "auto_mechanic_table", AutoMechanicTableRecipe.class);

    private static final ResourceLocation TEXTURE = RIAutomobility.rl("textures/gui/auto_mechanic_table_jei.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 112;

    // Output item: centered in the upper display frame
    private static final int OUTPUT_X = 80;
    private static final int OUTPUT_Y = 38;

    // Input slots: aligned with the 9-slot row at the bottom of the texture
    private static final int INPUT_START_X = 8;
    private static final int INPUT_START_Y = 88;
    private static final int INPUT_SLOTS_PER_ROW = 9;
    private static final int INPUT_SLOT_SIZE = 18;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slotDrawable;

    public AutoMechanicTableRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(AutomobilityBlocks.AUTO_MECHANIC_TABLE.require()));
        this.slotDrawable = guiHelper.getSlotDrawable();
    }

    @Override
    public RecipeType<AutoMechanicTableRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.automobility.auto_mechanic_table");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AutoMechanicTableRecipe recipe, IFocusGroup focuses) {
        AutoMechanicTableRecipeAccessor accessor = (AutoMechanicTableRecipeAccessor) recipe;
        List<Ingredient> ingredients = new ArrayList<>(accessor.getIngredients());

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(recipe.getResultItem());

        for (int i = 0; i < ingredients.size(); i++) {
            int x = INPUT_START_X + (i % INPUT_SLOTS_PER_ROW) * INPUT_SLOT_SIZE;
            int y = INPUT_START_Y + (i / INPUT_SLOTS_PER_ROW) * INPUT_SLOT_SIZE;
            builder
                    .addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addIngredients(ingredients.get(i));
        }
    }

    @Override
    public void draw(AutoMechanicTableRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        IRecipeCategory.super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);

        var category = recipe.getCategory();
        String categoryText = I18n.get("part_category." + category.getNamespace() + "." + category.getPath());
        var font = Minecraft.getInstance().font;
        int textWidth = font.width(categoryText);
        int boxWidth = 57;
        float scale = textWidth > boxWidth ? (float) boxWidth / textWidth : 1.0f;
        int x = (WIDTH - (int) (textWidth * scale)) / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 200);
        if (scale < 1.0f) {
            guiGraphics.pose().translate(x, 8, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.drawString(font, categoryText, 0, 0, 0xFFFFFFFF, false);
        } else {
            guiGraphics.drawString(font, categoryText, x, 8, 0xFFFFFFFF, false);
        }
        guiGraphics.pose().popPose();
    }
}
