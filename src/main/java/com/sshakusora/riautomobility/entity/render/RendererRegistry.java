package com.sshakusora.riautomobility.entity.render;

import com.sshakusora.riautomobility.entity.RIAutomobilityEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;

public class RendererRegistry {
    public static void init(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RIAutomobilityEntities.RI_AUTOMOBILE.get(), RIAutomobileEntityRenderer::new);
        event.registerEntityRenderer(RIAutomobilityEntities.HITBOX.get(), HitboxEntityRenderer::new);
    }
}
