package com.sshakusora.riautomobility.model.bbmodel;

import com.google.gson.JsonElement;
import io.github.foundationgames.automobility.automobile.render.RenderableAutomobile;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BbAnimationPlayer {
    private static final Map<String, MolangExpression.Expression> EXPRESSIONS = new ConcurrentHashMap<>();
    private static final Map<BbModelData.Animation, List<PreparedAnimator>> PREPARED = new IdentityHashMap<>();
    private static final Map<RenderableAutomobile, SampleCache> SAMPLE_CACHES = new WeakHashMap<>();
    private static final QueryState QUERY_STATE = new QueryState();
    private static final ThreadLocal<AnimationScratch> SAMPLE_SCRATCH = ThreadLocal.withInitial(AnimationScratch::new);
    private static final ThreadLocal<SampleCache> FALLBACK_SAMPLE_CACHE = ThreadLocal.withInitial(SampleCache::new);

    private BbAnimationPlayer() {
    }

    public static Map<String, Transform> sample(BbModelData.Document document, String requestedAnimation, BbRenderContext context) {
        if (document.animations().isEmpty()) {
            return Map.of();
        }
        if (context != null) {
            return context.animationSample(document, requestedAnimation);
        }
        return sampleUncached(document, requestedAnimation, null);
    }

    static Map<String, Transform> sampleUncached(BbModelData.Document document, String requestedAnimation,
                                                 BbRenderContext context) {
        BbModelData.Animation animation = selectAnimation(document, requestedAnimation);
        if (animation == null) {
            return Map.of();
        }

        RenderableAutomobile automobile = context == null ? null : context.automobile();
        float tickDelta = context == null ? 0.0F : context.tickDelta();
        float absoluteTime = automobile == null ? 0.0F : (automobile.getTime() + tickDelta) / 20.0F;
        float time = animationTime(animation, absoluteTime);
        configureQueries(automobile, time, absoluteTime, tickDelta);

        List<PreparedAnimator> animators = prepared(animation);
        SampleCache cache;
        if (automobile == null) {
            cache = FALLBACK_SAMPLE_CACHE.get();
        } else {
            synchronized (SAMPLE_CACHES) {
                cache = SAMPLE_CACHES.computeIfAbsent(automobile, ignored -> new SampleCache());
            }
        }
        return cache.sample(document, requestedAnimation, animation, animators, time, absoluteTime, tickDelta);
    }

    static void clearCache() {
        synchronized (PREPARED) {
            PREPARED.clear();
        }
        synchronized (SAMPLE_CACHES) {
            SAMPLE_CACHES.clear();
        }
        FALLBACK_SAMPLE_CACHE.remove();
    }

    private static List<PreparedAnimator> prepared(BbModelData.Animation animation) {
        synchronized (PREPARED) {
            return PREPARED.computeIfAbsent(animation, BbAnimationPlayer::prepare);
        }
    }

    private static List<PreparedAnimator> prepare(BbModelData.Animation animation) {
        List<PreparedAnimator> prepared = new ArrayList<>();
        animation.animators().forEach((target, animator) -> {
            if (!"bone".equals(animator.type()) && !animator.type().isBlank()) return;
            Map<String, List<BbModelData.Keyframe>> channels = new HashMap<>();
            for (BbModelData.Keyframe keyframe : animator.keyframes()) {
                channels.computeIfAbsent(keyframe.channel(), ignored -> new ArrayList<>()).add(keyframe);
            }
            prepared.add(new PreparedAnimator(target,
                    sorted(channels.get("position")),
                    sorted(channels.get("rotation")),
                    sorted(channels.get("scale"))));
        });
        return List.copyOf(prepared);
    }

    private static List<BbModelData.Keyframe> sorted(List<BbModelData.Keyframe> keyframes) {
        if (keyframes == null || keyframes.isEmpty()) return List.of();
        return keyframes.stream().sorted(Comparator.comparingDouble(BbModelData.Keyframe::time)).toList();
    }

    private static BbModelData.Animation selectAnimation(BbModelData.Document document, String requested) {
        if (requested != null && !requested.isBlank()) {
            for (BbModelData.Animation animation : document.animations()) {
                if (requested.equals(animation.name()) || requested.equals(animation.uuid())) {
                    return animation;
                }
            }
            return null;
        }
        return document.animations().get(0);
    }

    private static float animationTime(BbModelData.Animation animation, float time) {
        if (animation.length() <= 0.0F) {
            return time;
        }
        if ("loop".equalsIgnoreCase(animation.loop()) || "true".equalsIgnoreCase(animation.loop())) {
            return time % animation.length();
        }
        return Math.min(time, animation.length());
    }

    private static void configureQueries(RenderableAutomobile automobile, float animationTime, float lifeTime, float tickDelta) {
        QUERY_STATE.automobile = automobile;
        QUERY_STATE.animationTime = animationTime;
        QUERY_STATE.lifeTime = lifeTime;
        QUERY_STATE.tickDelta = tickDelta;
    }

    static Vector3f sampleChannel(List<BbModelData.Keyframe> keyframes, float time, Vector3f defaultValue) {
        return sampleChannelInto(keyframes, time, defaultValue, SAMPLE_SCRATCH.get());
    }

    private static Vector3f sampleChannelInto(List<BbModelData.Keyframe> keyframes, float time,
                                              Vector3f output, AnimationScratch scratch) {
        if (keyframes.isEmpty()) {
            return output;
        }
        if (time <= keyframes.get(0).time()) {
            return evaluateInto(keyframes.get(0), 0, output);
        }
        if (time >= keyframes.get(keyframes.size() - 1).time()) {
            BbModelData.Keyframe last = keyframes.get(keyframes.size() - 1);
            return evaluateInto(last, last.dataPoints().size() - 1, output);
        }

        int nextIndex = 1;
        while (nextIndex < keyframes.size() && keyframes.get(nextIndex).time() < time) {
            nextIndex++;
        }
        int previousIndex = nextIndex - 1;
        BbModelData.Keyframe previous = keyframes.get(previousIndex);
        BbModelData.Keyframe next = keyframes.get(nextIndex);
        float span = next.time() - previous.time();
        float delta = span <= 0 ? 0 : (time - previous.time()) / span;
        Vector3f a = evaluateInto(previous, Math.min(1, previous.dataPoints().size() - 1), scratch.a);
        Vector3f b = evaluateInto(next, 0, scratch.b);
        return switch (previous.interpolation().toLowerCase()) {
            case "step" -> output.set(a);
            case "catmullrom" -> catmullRomInto(
                    previousIndex > 0
                            ? evaluateInto(keyframes.get(previousIndex - 1),
                            Math.min(1, keyframes.get(previousIndex - 1).dataPoints().size() - 1), scratch.c)
                            : a,
                    a, b,
                    nextIndex + 1 < keyframes.size()
                            ? evaluateInto(keyframes.get(nextIndex + 1), 0, scratch.d)
                            : b,
                    delta, output);
            case "bezier" -> bezierInto(previous, next, a, b, time, output);
            default -> output.set(a).lerp(b, delta);
        };
    }

    private static Vector3f evaluateInto(BbModelData.Keyframe keyframe, int pointIndex, Vector3f output) {
        BbModelData.DataPoint point = keyframe.dataPoints().get(Math.max(0, Math.min(pointIndex, keyframe.dataPoints().size() - 1)));
        return output.set((float) evaluate(point.x()), (float) evaluate(point.y()), (float) evaluate(point.z()));
    }

    private static double evaluate(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return 0;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return value.getAsDouble();
        }
        String expression = value.getAsString();
        try {
            return EXPRESSIONS.computeIfAbsent(expression, MolangExpression::compile)
                    .evaluate(BbAnimationPlayer::queryValue);
        } catch (IllegalArgumentException exception) {
            throw new BbModelFormatException("Invalid Blockbench Molang expression '" + expression + "'", exception);
        }
    }

    private static double queryValue(String name) {
        return switch (name) {
            case "query.anim_time" -> QUERY_STATE.animationTime;
            case "query.life_time" -> QUERY_STATE.lifeTime;
            case "query.is_on_ground" -> QUERY_STATE.automobile != null && QUERY_STATE.automobile.automobileOnGround() ? 1 : 0;
            case "query.vehicle_steering" -> QUERY_STATE.automobile == null ? 0 : QUERY_STATE.automobile.getSteering(QUERY_STATE.tickDelta);
            case "query.vehicle_wheel_angle" -> QUERY_STATE.automobile == null ? 0 : QUERY_STATE.automobile.getWheelAngle(QUERY_STATE.tickDelta);
            case "query.vehicle_engine_running" -> QUERY_STATE.automobile != null && QUERY_STATE.automobile.engineRunning() ? 1 : 0;
            case "query.vehicle_turbo_charge" -> QUERY_STATE.automobile == null ? 0 : QUERY_STATE.automobile.getTurboCharge();
            case "query.vehicle_boost_timer" -> QUERY_STATE.automobile == null ? 0 : QUERY_STATE.automobile.getBoostTimer();
            default -> 0.0D;
        };
    }

    private static Vector3f catmullRomInto(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3,
                                           float t, Vector3f output) {
        float t2 = t * t;
        float t3 = t2 * t;
        return output.set(
                0.5F * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3),
                0.5F * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3),
                0.5F * ((2 * p1.z) + (-p0.z + p2.z) * t + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3)
        );
    }

    private static Vector3f bezierInto(BbModelData.Keyframe previous, BbModelData.Keyframe next,
                                       Vector3f a, Vector3f b, float time, Vector3f output) {
        return output.set(
                bezierAxis(previous, next, a.x, b.x, time, 0),
                bezierAxis(previous, next, a.y, b.y, time, 1),
                bezierAxis(previous, next, a.z, b.z, time, 2)
        );
    }

    static float bezierAxis(BbModelData.Keyframe previous, BbModelData.Keyframe next, float beforeValue, float afterValue, float time, int axis) {
        float span = Math.max(0.0F, next.time() - previous.time());
        if (span == 0.0F) {
            return afterValue;
        }
        float rightTime = clamp(component(previous.bezierRightTime(), axis, 0.1F), 0.0F, span);
        float leftTime = clamp(component(next.bezierLeftTime(), axis, -0.1F), -span, 0.0F);
        float rightValue = component(previous.bezierRightValue(), axis, 0.0F);
        float leftValue = component(next.bezierLeftValue(), axis, 0.0F);

        float x0 = previous.time();
        float x1 = previous.time() + rightTime;
        float x2 = next.time() + leftTime;
        float x3 = next.time();
        float targetTime = clamp(time, x0, x3);

        float low = 0.0F;
        float high = 1.0F;
        for (int iteration = 0; iteration < 20; iteration++) {
            float parameter = (low + high) * 0.5F;
            if (cubic(x0, x1, x2, x3, parameter) < targetTime) {
                low = parameter;
            } else {
                high = parameter;
            }
        }
        float parameter = (low + high) * 0.5F;
        return cubic(beforeValue, beforeValue + rightValue, afterValue + leftValue, afterValue, parameter);
    }

    private static float cubic(float p0, float p1, float p2, float p3, float t) {
        float inverse = 1.0F - t;
        return inverse * inverse * inverse * p0
                + 3.0F * inverse * inverse * t * p1
                + 3.0F * inverse * t * t * p2
                + t * t * t * p3;
    }

    private static float component(float[] values, int axis, float fallback) {
        return values.length > axis ? values[axis] : fallback;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Transform(Vector3f position, Vector3f rotation, Vector3f scale) {
        public static final Transform IDENTITY = new Transform(new Vector3f(), new Vector3f(), new Vector3f(1, 1, 1));
    }

    private record PreparedAnimator(String target,
                                    List<BbModelData.Keyframe> position,
                                    List<BbModelData.Keyframe> rotation,
                                    List<BbModelData.Keyframe> scale) {
    }

    private static final class QueryState {
        private RenderableAutomobile automobile;
        private float animationTime;
        private float lifeTime;
        private float tickDelta;
    }

    private static final class AnimationScratch {
        private final Vector3f a = new Vector3f();
        private final Vector3f b = new Vector3f();
        private final Vector3f c = new Vector3f();
        private final Vector3f d = new Vector3f();
    }

    private static final class SampleCache {
        private final Map<BbModelData.Document, Map<String, SampledAnimation>> documents = new IdentityHashMap<>();

        Map<String, Transform> sample(BbModelData.Document document, String requestedAnimation,
                                      BbModelData.Animation animation, List<PreparedAnimator> animators,
                                      float time, float absoluteTime, float tickDelta) {
            String key = requestedAnimation == null ? "" : requestedAnimation;
            Map<String, SampledAnimation> animations = documents.computeIfAbsent(document, ignored -> new HashMap<>());
            SampledAnimation sampled = animations.computeIfAbsent(key, ignored -> new SampledAnimation());
            return sampled.sample(animation, animators, time, absoluteTime, tickDelta);
        }
    }

    private static final class SampledAnimation {
        private BbModelData.Animation animation;
        private final Map<String, Transform> mutable = new HashMap<>();
        private Map<String, Transform> view = Map.of();
        private int timeBits;
        private int absoluteTimeBits;
        private int tickDeltaBits;
        private boolean sampled;

        Map<String, Transform> sample(BbModelData.Animation animation, List<PreparedAnimator> animators,
                                      float time, float absoluteTime, float tickDelta) {
            int nextTimeBits = Float.floatToIntBits(time);
            int nextAbsoluteTimeBits = Float.floatToIntBits(absoluteTime);
            int nextTickDeltaBits = Float.floatToIntBits(tickDelta);
            if (sampled && this.animation == animation && timeBits == nextTimeBits
                    && absoluteTimeBits == nextAbsoluteTimeBits && tickDeltaBits == nextTickDeltaBits) {
                return view;
            }
            if (this.animation != animation) {
                this.animation = animation;
                mutable.clear();
                for (PreparedAnimator animator : animators) {
                    mutable.put(animator.target(), new Transform(
                            new Vector3f(), new Vector3f(), new Vector3f(1, 1, 1)));
                }
                view = Collections.unmodifiableMap(mutable);
            }
            for (PreparedAnimator animator : animators) {
                Transform transform = mutable.get(animator.target());
                sampleChannel(animator.position(), time, transform.position().zero());
                sampleChannel(animator.rotation(), time, transform.rotation().zero());
                sampleChannel(animator.scale(), time, transform.scale().set(1, 1, 1));
            }
            timeBits = nextTimeBits;
            absoluteTimeBits = nextAbsoluteTimeBits;
            tickDeltaBits = nextTickDeltaBits;
            sampled = true;
            return view;
        }
    }

}
