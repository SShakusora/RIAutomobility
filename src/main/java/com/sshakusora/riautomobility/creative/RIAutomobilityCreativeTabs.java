package com.sshakusora.riautomobility.creative;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileComponent;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import io.github.foundationgames.automobility.item.AutomobilityItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class RIAutomobilityCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RIAutomobility.MODID);

    public static final RegistryObject<CreativeModeTab> COMPONENTS = TABS.register("components", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.riautomobility.components"))
            .icon(() -> AutomobilityItems.AUTOMOBILE_FRAME.require().createStack(RIAutomobileFrame.BEJEWELED_DOUBLEMOTORCAR))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .displayItems((params, output) -> {
                AutomobileFrame.REGISTRY.forEach(frame -> {
                    if (isRIAutomobilityComponent(frame) && !frame.isEmpty()) {
                        output.accept(AutomobilityItems.AUTOMOBILE_FRAME.require().createStack(frame));
                    }
                });
                AutomobileWheel.REGISTRY.forEach(wheel -> {
                    if (isRIAutomobilityComponent(wheel) && !wheel.isEmpty()) {
                        output.accept(AutomobilityItems.AUTOMOBILE_WHEEL.require().createStack(wheel));
                    }
                });
            })
            .build());

    private RIAutomobilityCreativeTabs() {}

    public static boolean isRIAutomobilityComponent(AutomobileComponent<?> component) {
        return component.getId().getNamespace().equals(RIAutomobility.MODID);
    }
}
