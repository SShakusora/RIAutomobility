package com.sshakusora.riautomobility.events;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomobileComponentTooltipEventsTest {
    @Test
    void importedComponentNameReplacesTranslationAndStaysOnSecondLine() {
        List<Component> tooltip = new ArrayList<>(List.of(
                Component.literal("Automobile Frame"),
                Component.translatable("item.automobility.automobile_frame.riautomobility.auto_123"),
                Component.literal("Weight: 0.9")
        ));

        AutomobileComponentTooltipEvents.replaceComponentName(
                tooltip, new ResourceLocation("riautomobility", "auto_123"), "Imported Frame");

        assertEquals("Automobile Frame", tooltip.get(0).getString());
        assertEquals("Imported Frame", tooltip.get(1).getString());
        assertEquals(ChatFormatting.BLUE.getColor(), tooltip.get(1).getStyle().getColor().getValue());
        assertEquals("Weight: 0.9", tooltip.get(2).getString());
    }

    @Test
    void insertsComponentNameWhenTheOriginalTranslationLineIsAbsent() {
        List<Component> tooltip = new ArrayList<>(List.of(
                Component.literal("Automobile Wheel"),
                Component.literal("Grip: 0.5")
        ));

        AutomobileComponentTooltipEvents.replaceComponentName(
                tooltip, new ResourceLocation("riautomobility", "auto_456"), "Imported Wheel");

        assertEquals(List.of("Automobile Wheel", "Imported Wheel", "Grip: 0.5"),
                tooltip.stream().map(Component::getString).toList());
    }

    @Test
    void legacyCustomNameFallbackOnlyAppliesToGeneratedComponents() {
        assertTrue(AutomobileComponentTooltipEvents.isGeneratedComponent(new ResourceLocation(
                "riautomobility", "auto_0123456789abcdef0123456789abcdef")));
        assertFalse(AutomobileComponentTooltipEvents.isGeneratedComponent(
                new ResourceLocation("automobility", "standard")));
        assertFalse(AutomobileComponentTooltipEvents.isGeneratedComponent(
                new ResourceLocation("riautomobility", "custom_handwritten_component")));
    }

    @Test
    void authorIsInsertedAfterComponentNameAndBeforeStats() {
        List<Component> tooltip = new ArrayList<>(List.of(
                Component.literal("Automobile Frame"),
                Component.literal("Imported Frame"),
                Component.literal("Weight: 0.9")
        ));

        AutomobileComponentTooltipEvents.insertAuthor(tooltip, "Test Author");

        assertEquals("Automobile Frame", tooltip.get(0).getString());
        assertEquals("Imported Frame", tooltip.get(1).getString());
        TranslatableContents author = (TranslatableContents) tooltip.get(2).getContents();
        assertEquals("tooltip.riautomobility.component_author", author.getKey());
        assertEquals("Test Author", author.getArgs()[0]);
        assertEquals(ChatFormatting.GRAY.getColor(), tooltip.get(2).getStyle().getColor().getValue());
        assertEquals("Weight: 0.9", tooltip.get(3).getString());
    }
}
