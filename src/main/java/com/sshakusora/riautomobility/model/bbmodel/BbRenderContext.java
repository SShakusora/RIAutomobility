package com.sshakusora.riautomobility.model.bbmodel;

import io.github.foundationgames.automobility.automobile.render.RenderableAutomobile;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BbRenderContext {
    private static final ThreadLocal<ContextStack> CURRENT = ThreadLocal.withInitial(ContextStack::new);

    private MultiBufferSource buffers;
    private RenderableAutomobile automobile;
    private float tickDelta;

    private void set(MultiBufferSource buffers, RenderableAutomobile automobile, float tickDelta) {
        this.buffers = buffers;
        this.automobile = automobile;
        this.tickDelta = tickDelta;
    }

    public MultiBufferSource buffers() {
        return buffers;
    }

    public RenderableAutomobile automobile() {
        return automobile;
    }

    public float tickDelta() {
        return tickDelta;
    }

    Map<String, BbAnimationPlayer.Transform> animationSample(
            BbModelData.Document document, String requestedAnimation) {
        return BbAnimationPlayer.sampleUncached(document, requestedAnimation, this);
    }

    public static void begin(MultiBufferSource buffers, RenderableAutomobile automobile, float tickDelta) {
        CURRENT.get().push(buffers, automobile, tickDelta);
    }

    public static BbRenderContext current() {
        return CURRENT.get().current();
    }

    public static void end() {
        CURRENT.get().pop();
    }

    private static final class ContextStack {
        private final List<BbRenderContext> contexts = new ArrayList<>(2);
        private int depth;

        void push(MultiBufferSource buffers, RenderableAutomobile automobile, float tickDelta) {
            if (depth == contexts.size()) contexts.add(new BbRenderContext());
            contexts.get(depth++).set(buffers, automobile, tickDelta);
        }

        BbRenderContext current() {
            return depth == 0 ? null : contexts.get(depth - 1);
        }

        void pop() {
            if (depth == 0) return;
            BbRenderContext context = contexts.get(--depth);
            context.buffers = null;
            context.automobile = null;
            context.tickDelta = 0.0F;
        }
    }
}
