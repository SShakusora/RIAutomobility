package com.sshakusora.riautomobility.events;

import com.sshakusora.riautomobility.client.RIAutomobilityKeyBindings;
import com.sshakusora.riautomobility.entity.SeatEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.PassengerSwitchPacket;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientKeyHandler {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        if (RIAutomobilityKeyBindings.BOARDING_AS_PASSENGER.consumeClick()) {
            Entity vehicle = player.getVehicle();
            if(vehicle instanceof AutomobileEntity auto && RIAutomobileFrame.isRIAutomobileFrame(auto.getFrame()) || vehicle instanceof SeatEntity) {
                RIAutomobilityNetwork.CHANNEL.sendToServer(new PassengerSwitchPacket());
            }
        }
    }
}
