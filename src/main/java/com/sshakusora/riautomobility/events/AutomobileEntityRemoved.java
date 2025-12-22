package com.sshakusora.riautomobility.events;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.util.RIAutomobileHitboxRegistry;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID)
public class AutomobileEntityRemoved {
    @SubscribeEvent
    public static void onRemovedFromWorld(EntityLeaveLevelEvent event) {
        Entity e = event.getEntity();
        if(!(e instanceof AutomobileEntity ae)) return;
        if(!RIAutomobileFrame.isRIAutomobileFrame(ae.getFrame())) return;

        Level world = event.getLevel();
        if(world.isClientSide()) return;

        RIAutomobileHitboxRegistry.removeAll(ae);
    }
}
