package com.sshakusora.riautomobility.entity.render;

import com.sshakusora.riautomobility.entity.EntityRegistry;
import net.minecraftforge.client.event.EntityRenderersEvent;

public class RendererRegistry {
    public static void init(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.DRIVER_SEAT.get(), DriverSeatEntityRenderer::new);
    }
}
