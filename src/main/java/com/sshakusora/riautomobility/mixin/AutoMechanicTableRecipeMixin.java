package com.sshakusora.riautomobility.mixin;

import io.github.foundationgames.automobility.recipe.AutoMechanicTableRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(AutoMechanicTableRecipe.class)
public class AutoMechanicTableRecipeMixin {
    @Final @Shadow protected Set<Ingredient> ingredients;
    @Final @Shadow protected ItemStack result;

    @Inject(method = "assemble(Lnet/minecraft/world/SimpleContainer;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), remap = false, cancellable = true)
    private void assembleFix(SimpleContainer inv, CallbackInfoReturnable<ItemStack> cir) {
        for(Ingredient ingredient : this.ingredients) {
            for(int i = 0; i < inv.getContainerSize(); ++i) {
                ItemStack stack = inv.getItem(i);
                if (ingredient.test(stack)) {
                    stack.shrink(1);
                    break;
                }
            }
        }
        cir.setReturnValue(this.result.copy());
    }

    @Inject(method = "forMissingIngredients", at = @At("HEAD"), remap = false, cancellable = true)
    private void forMissingIngredientsHead(Container inv, Consumer<Ingredient> action, CallbackInfo ci) {
        ArrayList<ItemStack> invCopy = new ArrayList<>();
        for(int i = 0; i < inv.getContainerSize(); ++i) {
            invCopy.add(inv.getItem(i).copy());
        }

        for(Ingredient ingredient : this.ingredients) {
            boolean found = false;
            for(int i = 0; i < invCopy.size(); i++) {
                ItemStack stack = invCopy.get(i);
                if(ingredient.test(stack)){
                    stack.shrink(1);
                    if(stack.isEmpty()){
                        invCopy.remove(i);
                    }
                    found = true;
                    break;
                }
            }
            if(!found){
                action.accept(ingredient);
            }
        }
        ci.cancel();
    }
}