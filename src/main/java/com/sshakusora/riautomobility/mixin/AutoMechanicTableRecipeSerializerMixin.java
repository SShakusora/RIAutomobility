package com.sshakusora.riautomobility.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.foundationgames.automobility.recipe.AutoMechanicTableRecipeSerializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AutoMechanicTableRecipeSerializer.class)
public class AutoMechanicTableRecipeSerializerMixin {
    @Redirect(method = "fromJson(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonObject;)Lio/github/foundationgames/automobility/recipe/AutoMechanicTableRecipe;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/Ingredient;fromJson(Lcom/google/gson/JsonElement;)Lnet/minecraft/world/item/crafting/Ingredient;"))
    private Ingredient redirectIngredientFromJson(JsonElement ele) {
        try {
            if (ele.isJsonObject()) {
                JsonObject obj = ele.getAsJsonObject();
                if (obj.has("item")) {
                    ItemStack stack = AutoMechanicTableRecipeSerializer.autoComponentStackFromJson(obj);
                    return Ingredient.of(stack);
                }
            }
            return Ingredient.fromJson(ele);
        } catch (Exception e) {
            return Ingredient.fromJson(ele);
        }
    }
}
