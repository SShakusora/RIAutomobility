package com.sshakusora.riautomobility.editor;

import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.carpack.CarPackManifestEntry;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AutomobileItemTooltipsTest {
    private static final String DIGEST = "a".repeat(64);

    @AfterEach
    void clearMetadata() {
        CarPackArchiveStore.installComponentMetadata(List.of());
    }

    @Test
    void replacesGeneratedTranslationKeysWithManifestNames() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ResourceLocation frame = new ResourceLocation("riautomobility", "auto_1234");
        CarPackArchiveStore.installComponentMetadata(List.of(new CarPackManifestEntry(
                "riautomobility/test", "机动车xx", "Test Author", DIGEST, DIGEST, 1024, frame)));

        ItemStack stack = new ItemStack(Items.STICK);
        var automobile = stack.getOrCreateTagElement("Automobile");
        automobile.putString("frame", frame.toString());
        automobile.putString("wheels", "automobility:standard");
        automobile.putString("engine", "automobility:stone");
        String generatedFrameKey = "frame." + frame.getNamespace() + "." + frame.getPath();
        List<Component> tooltip = new ArrayList<>(List.of(
                Component.translatable("tooltip.automobility.frameLabel")
                        .append(Component.translatable(generatedFrameKey)),
                Component.translatable("tooltip.automobility.wheelLabel")
                        .append(Component.translatable("wheel.automobility.standard"))
        ));

        AutomobileItemTooltips.replaceKnownComponentNames(stack, tooltip);

        Component frameName = tooltip.get(0).getSiblings().get(0);
        LiteralContents literal = assertInstanceOf(LiteralContents.class, frameName.getContents());
        assertEquals("机动车xx", literal.text());
        assertEquals("wheel.automobility.standard", tooltip.get(1).getSiblings().get(0).getString());
    }
}
