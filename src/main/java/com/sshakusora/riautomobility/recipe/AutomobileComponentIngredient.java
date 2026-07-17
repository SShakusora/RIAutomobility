package com.sshakusora.riautomobility.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sshakusora.riautomobility.RIAutomobility;
import io.github.foundationgames.automobility.item.AutomobileComponentItem;
import io.github.foundationgames.automobility.recipe.AutoMechanicTableRecipeSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

public final class AutomobileComponentIngredient extends AbstractIngredient {
    public static final ResourceLocation TYPE = RIAutomobility.rl("automobile_component");

    private final ItemStack stack;
    private final ResourceLocation componentId;

    private AutomobileComponentIngredient(ItemStack stack, ResourceLocation componentId) {
        super(Stream.of(new Ingredient.ItemValue(stack)));
        if (stack.isEmpty() || !(stack.getItem() instanceof AutomobileComponentItem<?>)) {
            throw new IllegalArgumentException("Automobile component ingredients require an AutomobileComponentItem");
        }
        this.stack = stack;
        this.componentId = componentId;
    }

    public static void register() {
        CraftingHelper.register(TYPE, Serializer.INSTANCE);
    }

    @Override
    public boolean test(@Nullable ItemStack input) {
        if (input == null || input.isEmpty() || input.getItem() != this.stack.getItem()) {
            return false;
        }
        AutomobileComponentItem<?> componentItem = (AutomobileComponentItem<?>) input.getItem();
        return this.componentId.equals(componentItem.getComponent(input).getId());
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", TYPE.toString());
        ResourceLocation itemId = Objects.requireNonNull(
                ForgeRegistries.ITEMS.getKey(this.stack.getItem()),
                "Automobile component ingredient contains an unregistered item"
        );
        json.addProperty("item", itemId.toString());
        json.addProperty("component", this.componentId.toString());
        return json;
    }

    public static final class Serializer implements IIngredientSerializer<AutomobileComponentIngredient> {
        private static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public AutomobileComponentIngredient parse(FriendlyByteBuf buffer) {
            return new AutomobileComponentIngredient(buffer.readItem(), buffer.readResourceLocation());
        }

        @Override
        public AutomobileComponentIngredient parse(JsonObject json) {
            GsonHelper.getAsString(json, "item");
            ResourceLocation componentId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "component"));
            if (componentId == null) {
                throw new JsonSyntaxException("Invalid automobile component id");
            }

            ItemStack stack = AutoMechanicTableRecipeSerializer.autoComponentStackFromJson(json);
            try {
                return new AutomobileComponentIngredient(stack, componentId);
            } catch (IllegalArgumentException exception) {
                throw new JsonSyntaxException(exception.getMessage(), exception);
            }
        }

        @Override
        public void write(FriendlyByteBuf buffer, AutomobileComponentIngredient ingredient) {
            buffer.writeItem(ingredient.stack);
            buffer.writeResourceLocation(ingredient.componentId);
        }
    }
}
