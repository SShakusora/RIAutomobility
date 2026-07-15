package com.sshakusora.riautomobility;

import com.mojang.logging.LogUtils;
import com.sshakusora.riautomobility.client.RIAutomobilityKeyBindings;
import com.sshakusora.riautomobility.creative.RIAutomobilityCreativeTabs;
import com.sshakusora.riautomobility.editor.VehicleImportRegistries;
import com.sshakusora.riautomobility.editor.client.VehicleImportScreen;
import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import com.sshakusora.riautomobility.entity.RIAutomobilityEntities;
import com.sshakusora.riautomobility.entity.render.RendererRegistry;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.model.RIAutomobileModels;
import com.sshakusora.riautomobility.model.bbmodel.BbInstancedRenderer;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.wheel.RIAutomobileWheel;
import io.github.foundationgames.automobility.screen.AutomobileHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.io.IOException;

@Mod(RIAutomobility.MODID)
public class RIAutomobility
{
    public static final String MODID = "riautomobility";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RIAutomobility()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        RIAutomobilityCreativeTabs.TABS.register(modEventBus);
        registerAll(modEventBus);
    }

    public static ResourceLocation rl(String path){
        return new ResourceLocation(MODID, path);
    }

    private void registerAll(IEventBus bus){
        RIAutomobileFrame.init();
        RIAutomobileWheel.init();
        RIAutomobilityEntities.ENTITIES.register(bus);
        VehicleImportRegistries.BLOCKS.register(bus);
        VehicleImportRegistries.ITEMS.register(bus);
        VehicleImportRegistries.MENUS.register(bus);
        RIAutomobilityNetwork.register();
    }

    @Mod.EventBusSubscriber(modid = RIAutomobility.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class RIAutomobilityClient{
        @SubscribeEvent
        public static void onClientSetup(final FMLClientSetupEvent event){
            RIAutomobileModels.init();
            MinecraftForge.EVENT_BUS.addListener(BbInstancedRenderer::onRenderLevelStage);
            event.enqueueWork(() -> MenuScreens.register(
                    VehicleImportRegistries.VEHICLE_IMPORT_MENU.get(),
                    VehicleImportScreen::new));
            MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent evt) -> {
                LocalPlayer player = Minecraft.getInstance().player;
                Entity vehicle = null;
                if (player != null) {
                    vehicle = player.getVehicle();
                }
                if (vehicle instanceof RIAutomobileEntity auto) {
                    AutomobileHud.render(evt.getGuiGraphics(), player, auto, evt.getPartialTick());
                }
            });
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event){
            RendererRegistry.init(event);
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event){
            RIAutomobilityKeyBindings.init(event);
        }

        @SubscribeEvent
        public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
            BbInstancedRenderer.registerShader(event);
        }

    }
}
