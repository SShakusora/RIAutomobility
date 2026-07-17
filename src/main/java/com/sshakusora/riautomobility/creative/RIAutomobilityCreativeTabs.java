package com.sshakusora.riautomobility.creative;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.content.RIAutomobilityComponentManager;
import com.sshakusora.riautomobility.editor.VehicleImportRegistries;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.item.RIAutomobilityItems;
import io.github.foundationgames.automobility.automobile.AutomobileComponent;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
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
                output.accept(RIAutomobilityItems.VEHICLE_KEY.get());
                output.accept(VehicleImportRegistries.VEHICLE_IMPORT_TABLE_ITEM.get());
                AutomobileFrame.REGISTRY.forEach(frame -> {
                    if (isBuiltInRIAComponent(frame) && !frame.isEmpty()) {
                        output.accept(AutomobilityItems.AUTOMOBILE_FRAME.require().createStack(frame));
                    }
                });
                AutomobileWheel.REGISTRY.forEach(wheel -> {
                    if (isBuiltInRIAComponent(wheel) && !wheel.isEmpty()) {
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
        if (component instanceof AutomobileEngine engine) {
            return engine.getId().getNamespace().equals(RIAutomobility.MODID) || RIAutomobilityComponentManager.isManagedEngine(engine);
        }
        return component.getId().getNamespace().equals(RIAutomobility.MODID);
    }

    private static boolean isBuiltInRIAComponent(AutomobileComponent<?> component) {
        return component.getId().getNamespace().equals(RIAutomobility.MODID)
                && !isManagedComponent(component);
    }

    private static boolean isManagedComponent(AutomobileComponent<?> component) {
        return component instanceof AutomobileFrame frame && RIAutomobilityComponentManager.isManagedFrame(frame)
                || component instanceof AutomobileWheel wheel && RIAutomobilityComponentManager.isManagedWheel(wheel)
                || component instanceof AutomobileEngine engine && RIAutomobilityComponentManager.isManagedEngine(engine);
    }
}
