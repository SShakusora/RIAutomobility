package com.sshakusora.riautomobility.model.bbmodel;

import io.github.foundationgames.automobility.automobile.render.RenderableAutomobile;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.ArrayDeque;
import java.util.Deque;

public record BbRenderContext(MultiBufferSource buffers, RenderableAutomobile automobile, float tickDelta) {
    private static final ThreadLocal<Deque<BbRenderContext>> CURRENT =
            ThreadLocal.withInitial(ArrayDeque::new);

    public static void begin(MultiBufferSource buffers, RenderableAutomobile automobile, float tickDelta) {
        CURRENT.get().push(new BbRenderContext(buffers, automobile, tickDelta));
    }

    public static BbRenderContext current() {
        return CURRENT.get().peek();
    }

    public static void end() {
        Deque<BbRenderContext> contexts = CURRENT.get();
        if (!contexts.isEmpty()) contexts.pop();
        if (contexts.isEmpty()) CURRENT.remove();
    }
}
