package com.sshakusora.riautomobility.model.bbmodel;

import io.github.foundationgames.automobility.automobile.render.RenderableAutomobile;
import net.minecraft.client.renderer.MultiBufferSource;

public record BbRenderContext(MultiBufferSource buffers, RenderableAutomobile automobile, float tickDelta) {
    private static final ThreadLocal<BbRenderContext> CURRENT = new ThreadLocal<>();

    public static void begin(MultiBufferSource buffers, RenderableAutomobile automobile, float tickDelta) {
        CURRENT.set(new BbRenderContext(buffers, automobile, tickDelta));
    }

    public static BbRenderContext current() {
        return CURRENT.get();
    }

    public static void end() {
        CURRENT.remove();
    }
}
