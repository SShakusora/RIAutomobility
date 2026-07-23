package com.sshakusora.riautomobility.events;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.creative.RIAutomobilityCreativeTabs;
import io.github.foundationgames.automobility.Automobility;
import io.github.foundationgames.automobility.item.AutomobileEngineItem;
import io.github.foundationgames.automobility.item.AutomobileFrameItem;
import io.github.foundationgames.automobility.item.AutomobileWheelItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AutomobileComponentCreativeTabEvents {
    private AutomobileComponentCreativeTabEvents() {
    }

    @SubscribeEvent
    public static void filterRIAutomobilityComponents(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().location().equals(Automobility.TAB.location)) {
            return;
        }

        var entries = event.getEntries().iterator();
        while (entries.hasNext()) {
            var stack = entries.next().getKey();
            if ((stack.getItem() instanceof AutomobileFrameItem frameItem
                    && RIAutomobilityCreativeTabs.isRIAutomobilityComponent(frameItem.getComponent(stack)))
                    || (stack.getItem() instanceof AutomobileWheelItem wheelItem
                    && RIAutomobilityCreativeTabs.isRIAutomobilityComponent(wheelItem.getComponent(stack)))
                    || (stack.getItem() instanceof AutomobileEngineItem engineItem
                    && RIAutomobilityCreativeTabs.isRIAutomobilityComponent(engineItem.getComponent(stack)))) {
                entries.remove();
            }
        }
    }
}
