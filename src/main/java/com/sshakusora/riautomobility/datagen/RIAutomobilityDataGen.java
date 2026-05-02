package com.sshakusora.riautomobility.datagen;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RIAutomobilityDataGen {
    private RIAutomobilityDataGen() {}

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        PackOutput output = event.getGenerator().getPackOutput();
        event.getGenerator().addProvider(event.includeClient(), new RIAutomobilityLangProvider.EnUs(output));
        event.getGenerator().addProvider(event.includeClient(), new RIAutomobilityLangProvider.ZhCn(output));
    }
}
