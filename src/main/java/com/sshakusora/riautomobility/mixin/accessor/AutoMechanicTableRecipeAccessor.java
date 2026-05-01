package com.sshakusora.riautomobility.mixin.accessor;

import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(io.github.foundationgames.automobility.recipe.AutoMechanicTableRecipe.class)
public interface AutoMechanicTableRecipeAccessor {
    @Accessor("ingredients")
    Set<Ingredient> getIngredients();
}
