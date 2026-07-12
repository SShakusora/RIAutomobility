package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.creative.RIAutomobilityCreativeTabs;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.common.CreativeModeTabRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {
    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/common/CreativeModeTabRegistry;getSortedCreativeModeTabs()Ljava/util/List;",
                    remap = false
            )
    )
    private List<CreativeModeTab> removeEmptyCustomComponentsTabFromPages() {
        CreativeModeTab customComponentsTab = RIAutomobilityCreativeTabs.CUSTOM_COMPONENTS.get();
        return CreativeModeTabRegistry.getSortedCreativeModeTabs().stream()
                .filter(tab -> tab != customComponentsTab || tab.shouldDisplay())
                .toList();
    }
}
