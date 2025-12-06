package com.shakusora.riautomobility;

import com.mojang.logging.LogUtils;
import com.shakusora.riautomobility.frame.RIAutomobileFrame;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RIAutomobility.MODID)
public class RIAutomobility
{
    public static final String MODID = "riautomobility";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RIAutomobility()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        registerAll(modEventBus);

    }

    public static ResourceLocation rl(String path){
        return new ResourceLocation(MODID, path);
    }

    private void registerAll(IEventBus bus){
        RIAutomobileFrame.init();
    }
}
