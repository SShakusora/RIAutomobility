package com.sshakusora.riautomobility.model.bbmodel;

import io.github.foundationgames.automobility.automobile.render.RenderableAutomobile;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.*;

public final class BbRenderContext {
    private static final ThreadLocal<Deque<BbRenderContext>> CURRENT =
            ThreadLocal.withInitial(ArrayDeque::new);

    private final MultiBufferSource buffers;
    private final RenderableAutomobile automobile;
    private final float tickDelta;
    private final Map<BbModelData.Document, Map<String, Map<String, BbAnimationPlayer.Transform>>> animationSamples =
            new IdentityHashMap<>();

    private BbRenderContext(MultiBufferSource buffers, RenderableAutomobile automobile, float tickDelta) {
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
        Map<String, Map<String, BbAnimationPlayer.Transform>> byAnimation =
                animationSamples.computeIfAbsent(document, ignored -> new HashMap<>());
        String key = requestedAnimation == null ? "" : requestedAnimation;
        Map<String, BbAnimationPlayer.Transform> cached = byAnimation.get(key);
        if (cached != null) return cached;
        Map<String, BbAnimationPlayer.Transform> sampled =
                BbAnimationPlayer.sampleUncached(document, requestedAnimation, this);
        byAnimation.put(key, sampled);
        return sampled;
    }

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
