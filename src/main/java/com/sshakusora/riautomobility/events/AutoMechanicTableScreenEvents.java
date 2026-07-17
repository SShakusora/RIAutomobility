package com.sshakusora.riautomobility.events;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.mixin.accessor.AutoMechanicTableScreenAccessor;
import io.github.foundationgames.automobility.screen.AutoMechanicTableScreen;
import io.github.foundationgames.automobility.screen.AutoMechanicTableScreenHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID, value = Dist.CLIENT)
public final class AutoMechanicTableScreenEvents {
    private AutoMechanicTableScreenEvents() {
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AutoMechanicTableScreen screen)) {
            return;
        }

        AutoMechanicTableScreenHandler handler = screen.getMenu();
        SimpleContainer inputInv = handler.inputInv;
        ArrayDeque<Ingredient> missing = new ArrayDeque<>(handler.missingIngredients);
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();

        for (int i = 0; i < inputInv.getContainerSize(); ++i) {
            if (missing.isEmpty()) {
                return;
            }

            if (inputInv.getItem(i).isEmpty()) {
                int x = screen.getGuiLeft() + 8 + i * 18 - 1;
                int y = screen.getGuiTop() + 88 - 1;

                if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                    ItemStack[] stacks = missing.getFirst().getItems();
                    if (stacks.length == 0) {
                        return;
                    }

                    Minecraft minecraft = Minecraft.getInstance();
                    long time = ((AutoMechanicTableScreenAccessor) screen).riautomobility$getTime();
                    ItemStack stack = stacks[Mth.floor((float) time / 30.0F) % stacks.length];
                    event.getGuiGraphics().renderTooltip(minecraft.font, stack, mouseX, mouseY);
                    return;
                }

                missing.removeFirst();
            }
        }
    }
}
