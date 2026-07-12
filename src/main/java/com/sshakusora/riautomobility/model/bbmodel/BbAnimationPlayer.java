package com.sshakusora.riautomobility.model.bbmodel;

import com.google.gson.JsonElement;
import io.github.foundationgames.automobility.automobile.render.RenderableAutomobile;
import org.joml.Vector3f;
import software.bernie.geckolib.core.molang.MolangException;
import software.bernie.geckolib.core.molang.MolangParser;
import software.bernie.geckolib.core.molang.expressions.MolangValue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BbAnimationPlayer {
    private static final Map<String, MolangValue> EXPRESSIONS = new ConcurrentHashMap<>();

    private BbAnimationPlayer() {}

    public static Map<String, Transform> sample(BbModelData.Document document, String requestedAnimation, BbRenderContext context) {
        if (document.animations().isEmpty()) {
            return Map.of();
        }
        BbModelData.Animation animation = selectAnimation(document, requestedAnimation);
        if (animation == null) {
            return Map.of();
        }

        RenderableAutomobile automobile = context == null ? null : context.automobile();
        float tickDelta = context == null ? 0.0F : context.tickDelta();
        float absoluteTime = automobile == null ? 0.0F : (automobile.getTime() + tickDelta) / 20.0F;
        float time = animationTime(animation, absoluteTime);
        configureMolang(automobile, time, absoluteTime, tickDelta);

        Map<String, MutableTransform> sampled = new HashMap<>();
        animation.animators().forEach((target, animator) -> {
            if (!"bone".equals(animator.type()) && !animator.type().isBlank()) {
                return;
            }
            MutableTransform transform = sampled.computeIfAbsent(target, ignored -> new MutableTransform());
            Map<String, List<BbModelData.Keyframe>> channels = new HashMap<>();
            for (BbModelData.Keyframe keyframe : animator.keyframes()) {
                channels.computeIfAbsent(keyframe.channel(), ignored -> new ArrayList<>()).add(keyframe);
            }
            channels.forEach((channel, keyframes) -> {
                keyframes.sort(Comparator.comparingDouble(BbModelData.Keyframe::time));
                Vector3f value = sampleChannel(keyframes, time, "scale".equals(channel) ? new Vector3f(1, 1, 1) : new Vector3f());
                switch (channel) {
                    case "position" -> transform.position.set(value);
                    case "rotation" -> transform.rotation.set(value);
                    case "scale" -> transform.scale.set(value);
                    default -> {
                    }
                }
            });
        });

        Map<String, Transform> result = new HashMap<>();
        sampled.forEach((uuid, transform) -> result.put(uuid, transform.freeze()));
        return Map.copyOf(result);
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

    private static void configureMolang(RenderableAutomobile automobile, float animationTime, float lifeTime, float tickDelta) {
        MolangParser parser = MolangParser.INSTANCE;
        parser.setValue("query.anim_time", () -> animationTime);
        parser.setValue("query.life_time", () -> lifeTime);
        parser.setValue("query.is_on_ground", () -> automobile != null && automobile.automobileOnGround() ? 1 : 0);
        parser.setValue("query.vehicle_steering", () -> automobile == null ? 0 : automobile.getSteering(tickDelta));
        parser.setValue("query.vehicle_wheel_angle", () -> automobile == null ? 0 : automobile.getWheelAngle(tickDelta));
        parser.setValue("query.vehicle_engine_running", () -> automobile != null && automobile.engineRunning() ? 1 : 0);
        parser.setValue("query.vehicle_turbo_charge", () -> automobile == null ? 0 : automobile.getTurboCharge());
        parser.setValue("query.vehicle_boost_timer", () -> automobile == null ? 0 : automobile.getBoostTimer());
    }

    static Vector3f sampleChannel(List<BbModelData.Keyframe> keyframes, float time, Vector3f defaultValue) {
        if (keyframes.isEmpty()) {
            return defaultValue;
        }
        if (time <= keyframes.get(0).time()) {
            return evaluate(keyframes.get(0), 0);
        }
        if (time >= keyframes.get(keyframes.size() - 1).time()) {
            BbModelData.Keyframe last = keyframes.get(keyframes.size() - 1);
            return evaluate(last, last.dataPoints().size() - 1);
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
        Vector3f a = evaluate(previous, Math.min(1, previous.dataPoints().size() - 1));
        Vector3f b = evaluate(next, 0);
        return switch (previous.interpolation().toLowerCase()) {
            case "step" -> a;
            case "catmullrom" -> catmullRom(
                    previousIndex > 0 ? evaluate(keyframes.get(previousIndex - 1), Math.min(1, keyframes.get(previousIndex - 1).dataPoints().size() - 1)) : a,
                    a,
                    b,
                    nextIndex + 1 < keyframes.size() ? evaluate(keyframes.get(nextIndex + 1), 0) : b,
                    delta
            );
            case "bezier" -> bezier(previous, next, a, b, time);
            default -> a.lerp(b, delta, new Vector3f());
        };
    }

    private static Vector3f evaluate(BbModelData.Keyframe keyframe, int pointIndex) {
        BbModelData.DataPoint point = keyframe.dataPoints().get(Math.max(0, Math.min(pointIndex, keyframe.dataPoints().size() - 1)));
        return new Vector3f((float) evaluate(point.x()), (float) evaluate(point.y()), (float) evaluate(point.z()));
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
            MolangValue parsed = EXPRESSIONS.computeIfAbsent(expression, key -> {
                try {
                    return MolangParser.parseExpression(key);
                } catch (MolangException exception) {
                    throw new InvalidMolangException(exception);
                }
            });
            return parsed.get();
        } catch (InvalidMolangException exception) {
            throw new BbModelFormatException("Invalid Blockbench Molang expression '" + expression + "'", exception.getCause());
        }
    }

    private static Vector3f catmullRom(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return new Vector3f(
                0.5F * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3),
                0.5F * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3),
                0.5F * ((2 * p1.z) + (-p0.z + p2.z) * t + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3)
        );
    }

    private static Vector3f bezier(BbModelData.Keyframe previous, BbModelData.Keyframe next, Vector3f a, Vector3f b, float time) {
        return new Vector3f(
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

    private static final class MutableTransform {
        private final Vector3f position = new Vector3f();
        private final Vector3f rotation = new Vector3f();
        private final Vector3f scale = new Vector3f(1, 1, 1);

        private Transform freeze() {
            return new Transform(new Vector3f(position), new Vector3f(rotation), new Vector3f(scale));
        }
    }

    private static final class InvalidMolangException extends RuntimeException {
        private InvalidMolangException(Throwable cause) {
            super(cause);
        }
    }
}
