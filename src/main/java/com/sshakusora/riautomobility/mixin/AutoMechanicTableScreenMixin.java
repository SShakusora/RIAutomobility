package com.sshakusora.riautomobility.mixin;

import io.github.foundationgames.automobility.screen.AutoMechanicTableScreen;
import io.github.foundationgames.automobility.screen.AutoMechanicTableScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;

@Mixin(AutoMechanicTableScreen.class)
public class AutoMechanicTableScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void renderMissingIngredientTooltip(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AutoMechanicTableScreen self = (AutoMechanicTableScreen)(Object)this;
        renderMissingIngredientsTooltip(self, graphics, mouseX, mouseY);
    }

    @Unique
    private static void renderMissingIngredientsTooltip(AutoMechanicTableScreen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        AutoMechanicTableScreenHandler handler = screen.getMenu();

        SimpleContainer inputInv = handler.inputInv;
        ArrayDeque<Ingredient> missing =
                new ArrayDeque<>(handler.missingIngredients);

        for (int i = 0; i < inputInv.getContainerSize(); ++i) {
            if (missing.isEmpty()) break;

            if (inputInv.getItem(i).isEmpty()) {
                int x = screen.getGuiLeft() + 8 + i * 18 - 1;
                int y = screen.getGuiTop() + 88 - 1;

                if (isMouseOver(mouseX, mouseY, x, y, 18, 18)) {
                    Ingredient ing = missing.peekFirst();
                    renderIngredientTooltip(graphics, ing, mouseX, mouseY, screen);
                    return;
                }

                missing.removeFirst();
            }
        }
    }

    @Unique
    private static void renderIngredientTooltip(GuiGraphics graphics, Ingredient ing, int mouseX, int mouseY, AutoMechanicTableScreen screen) {
        ItemStack[] stacks = ing.getItems();
        if (stacks.length == 0) return;

        AutoMechanicTableScreenAccessor accessor = (AutoMechanicTableScreenAccessor)screen;
        long time = accessor.getTime();
        ItemStack stack = stacks[(int)((time / 30) % stacks.length)];
        ScreenAccessor screenAccessor = (ScreenAccessor)screen;

        graphics.renderTooltip(screenAccessor.getFont(), stack, mouseX, mouseY);
    }

    @Unique
    private static boolean isMouseOver(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
