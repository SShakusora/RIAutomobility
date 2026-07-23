package com.sshakusora.riautomobility.recipe;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class RIAutomobilityRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, RIAutomobility.MODID);

    public static final RegistryObject<RecipeSerializer<VehicleKeyResetRecipe>> VEHICLE_KEY_RESET =
            SERIALIZERS.register(
                    "vehicle_key_reset",
                    () -> new SimpleCraftingRecipeSerializer<>(VehicleKeyResetRecipe::new)
            );

    private RIAutomobilityRecipes() {
    }
}
