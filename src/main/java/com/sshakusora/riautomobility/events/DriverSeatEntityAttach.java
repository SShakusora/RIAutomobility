package com.sshakusora.riautomobility.events;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.entity.DriverSeatEntity;
import com.sshakusora.riautomobility.entity.RIAutomobilityEntities;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID)
public class DriverSeatEntityAttach {
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if(!(e instanceof AutomobileEntity ae)) return;
        if(e.getFirstPassenger() instanceof DriverSeatEntity) return;
        if(!RIAutomobileFrame.isRIAutomobileFrame(ae.getFrame())) return;

        Level world = event.getLevel();
        if(world.isClientSide()) return;

        DriverSeatEntity seat = RIAutomobilityEntities.DRIVER_SEAT.get().create(world);
        if (seat != null) {
            seat.moveTo(ae.position());
            world.addFreshEntity(seat);

            seat.startRiding(ae, true);
        }
    }
}
