package com.sshakusora.riautomobility.mixin;

import io.github.foundationgames.automobility.recipe.AutoMechanicTableRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(AutoMechanicTableRecipe.class)
public class AutoMechanicTableRecipeMixin {
    @Final
    @Shadow protected Set<Ingredient> ingredients;

    @Inject(method = "forMissingIngredients", at = @At("HEAD"), remap = false, cancellable = true)
    private void forMissingIngredientsHead(Container inv, Consumer<Ingredient> action, CallbackInfo ci) {
        boolean isIngredientHasTag = false;
        for(Ingredient ingredient : this.ingredients) {
            for(ItemStack is : ingredient.getItems()) {
                if(is.hasTag()){
                    isIngredientHasTag = true;
                    break;
                }
            }
            if(isIngredientHasTag) break;
        }
        if(!isIngredientHasTag) return;

        ArrayList<ItemStack> invCopy = new ArrayList<>();
        for(int i = 0; i < inv.getContainerSize(); ++i) {
            invCopy.add(inv.getItem(i));
        }

        for(Ingredient ingredient : this.ingredients) {
            boolean flag = false;
            int removeIdx = 0;
            for(int i = 0; i < invCopy.size(); i++) {
                if(ItemStack.isSameItemSameTags(ingredient.getItems()[0], invCopy.get(i))){
                    removeIdx = i;
                    flag = true;
                    break;
                }
            }
            if(!flag){
                action.accept(ingredient);
            } else {
                invCopy.remove(removeIdx);
            }
        }
        ci.cancel();
    }
}