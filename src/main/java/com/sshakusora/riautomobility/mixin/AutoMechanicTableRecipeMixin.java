package com.sshakusora.riautomobility.mixin;

import io.github.foundationgames.automobility.recipe.AutoMechanicTableRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

//TODO: fix extend recipes
@Mixin(AutoMechanicTableRecipe.class)
public class AutoMechanicTableRecipeMixin {
//    @Redirect(
//            method = "forMissingIngredients",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Ljava/util/stream/Stream;noneMatch(Ljava/util/function/Predicate;)Z"
//            ),
//            remap = false
//    )
//    private boolean redirectNoneMatchToAllMatch(Stream<ItemStack> stream, Predicate<ItemStack> predicate) {
//        return stream.anyMatch(stack -> {
//            Ingredient ingredient = (Ingredient) predicate;
//            System.out.println("Ingredient: " + Arrays.toString(ingredient.getItems()) + "stack: " + stack);
//            return !ItemStack.isSameItemSameTags(stack, ingredient.getItems()[0]);
//        });
//    }
}