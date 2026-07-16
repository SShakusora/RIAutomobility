package com.sshakusora.riautomobility.editor;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleComponentItemDataTest {
    @Test
    void displayNameUsesPersistentLiteralHoverName() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ItemStack stack = new ItemStack(Items.STICK);

        VehicleComponentItemData.setDisplayName(stack, "Imported Frame");
        VehicleComponentItemData.setAuthor(stack, "Test Author");

        assertTrue(stack.hasCustomHoverName());
        assertFalse(stack.getHoverName().getStyle().isItalic());
        assertEquals("Imported Frame", stack.getHoverName().getString());
        assertEquals("Imported Frame", VehicleComponentItemData.getDisplayName(stack));
        assertEquals("Test Author", VehicleComponentItemData.getAuthor(stack));
    }
}
