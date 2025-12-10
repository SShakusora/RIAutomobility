package com.sshakusora.riautomobility;

import com.mojang.logging.LogUtils;
import com.sshakusora.riautomobility.entity.EntityRegistry;
import com.sshakusora.riautomobility.entity.render.RendererRegistry;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.model.RIAutomobileModels;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
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
        EntityRegistry.ENTITIES.register(bus);
    }

    @Mod.EventBusSubscriber(modid = RIAutomobility.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class RIAutomobilityClient{
        @SubscribeEvent
        public static void onClientSetup(final FMLClientSetupEvent event){
            RIAutomobileModels.init();
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event){
            RendererRegistry.init(event);
        }
    }
}
