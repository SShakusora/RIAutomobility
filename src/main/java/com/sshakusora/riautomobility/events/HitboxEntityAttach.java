package com.sshakusora.riautomobility.events;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.entity.HitboxEntity;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.util.RIAutomobileHitboxRegistry;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID)
public class HitboxEntityAttach {
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if(!(e instanceof AutomobileEntity ae)) return;
        if(!RIAutomobileFrame.isRIAutomobileFrame(ae.getFrame())) return;

        Level world = event.getLevel();
        if(world.isClientSide()) return;

        spawnHitboxesForAutomobile(ae);
    }

    private static void spawnHitboxesForAutomobile(AutomobileEntity automobile) {
        var frame = automobile.getFrame();
        var hitboxes = RIAutomobileHitboxRegistry.getHitboxes(frame);

        if(hitboxes.isEmpty()) return;
        for (var hb : hitboxes) {
            HitboxEntity hitbox = new HitboxEntity(automobile.level(), automobile, hb);

            Vec3 worldPos = automobile.position().add(hb.origin());
            hitbox.setPos(worldPos.x, worldPos.y, worldPos.z);

            automobile.level().addFreshEntity(hitbox);
            RIAutomobileHitboxRegistry.addHitbox(automobile, hitbox);
        }
    }
}
