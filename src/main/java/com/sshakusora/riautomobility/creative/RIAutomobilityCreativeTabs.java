package com.sshakusora.riautomobility.creative;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.content.RIAutomobilityComponentManager;
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
                    if (isBuiltInRIAComponent(frame) && !frame.isEmpty() && isVisible(frame)) {
                        output.accept(AutomobilityItems.AUTOMOBILE_FRAME.require().createStack(frame));
                    }
                });
                AutomobileWheel.REGISTRY.forEach(wheel -> {
                    if (isBuiltInRIAComponent(wheel) && !wheel.isEmpty() && isVisible(wheel)) {
                        output.accept(AutomobilityItems.AUTOMOBILE_WHEEL.require().createStack(wheel));
                    }
                });
            })
            .build());

    public static final RegistryObject<CreativeModeTab> CUSTOM_COMPONENTS = TABS.register("custom_components", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.riautomobility.custom_components"))
            .icon(() -> AutomobilityItems.AUTOMOBILE_FRAME.require().createStack(RIAutomobileFrame.DMC12))
            .withTabsBefore(COMPONENTS.getKey())
            .withTabFactory(CustomComponentsTab::new)
            .displayItems((params, output) -> {
                AutomobileFrame.REGISTRY.forEach(frame -> {
                    if (RIAutomobilityComponentManager.isManagedFrame(frame) && !frame.isEmpty() && isVisible(frame)) {
                        output.accept(AutomobilityItems.AUTOMOBILE_FRAME.require().createStack(frame));
                    }
                });
                AutomobileWheel.REGISTRY.forEach(wheel -> {
                    if (RIAutomobilityComponentManager.isManagedWheel(wheel) && !wheel.isEmpty() && isVisible(wheel)) {
                        output.accept(AutomobilityItems.AUTOMOBILE_WHEEL.require().createStack(wheel));
                    }
                });
            })
            .build());

    private RIAutomobilityCreativeTabs() {}

    public static boolean isRIAutomobilityComponent(AutomobileComponent<?> component) {
        if (component instanceof AutomobileFrame frame) {
            return frame.getId().getNamespace().equals(RIAutomobility.MODID) || RIAutomobilityComponentManager.isManagedFrame(frame);
        }
        if (component instanceof AutomobileWheel wheel) {
            return wheel.getId().getNamespace().equals(RIAutomobility.MODID) || RIAutomobilityComponentManager.isManagedWheel(wheel);
        }
        return component.getId().getNamespace().equals(RIAutomobility.MODID);
    }

    private static boolean isBuiltInRIAComponent(AutomobileComponent<?> component) {
        return component.getId().getNamespace().equals(RIAutomobility.MODID);
    }

    private static boolean isVisible(AutomobileFrame frame) {
        var spec = RIAutomobilityComponentManager.getCustomFrames().get(frame.getId());
        return spec == null || spec.showInCreativeTab();
    }

    private static boolean isVisible(AutomobileWheel wheel) {
        var spec = RIAutomobilityComponentManager.getCustomWheels().get(wheel.getId());
        return spec == null || spec.showInCreativeTab();
    }

    private static boolean hasVisibleCustomComponents() {
        return RIAutomobilityComponentManager.getCustomFrameSpecs().stream().anyMatch(spec -> spec.showInCreativeTab())
                || RIAutomobilityComponentManager.getCustomWheelSpecs().stream().anyMatch(spec -> spec.showInCreativeTab());
    }

    private static final class CustomComponentsTab extends CreativeModeTab {
        private CustomComponentsTab(Builder builder) {
            super(builder);
        }

        @Override
        public boolean shouldDisplay() {
            return hasVisibleCustomComponents() && super.shouldDisplay();
        }
    }
}
