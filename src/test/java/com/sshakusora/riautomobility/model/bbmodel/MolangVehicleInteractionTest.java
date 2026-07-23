package com.sshakusora.riautomobility.model.bbmodel;

import com.google.gson.JsonParser;
import com.sshakusora.riautomobility.interaction.VehicleInteractionStateProvider;
import io.github.foundationgames.automobility.automobile.render.RenderableAutomobile;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MolangVehicleInteractionTest {
    @Test
    void routesInteractionQueriesThroughRuntimeVariables() {
        MolangExpression.Expression expression = MolangExpression.compile(
                "q.vehicle_interaction(2) + q.vehicle_interaction_time(2)");

        assertEquals(1.75D, expression.evaluate(name -> switch (name) {
            case "query.vehicle_interaction(2.0)" -> 0.75D;
            case "query.vehicle_interaction_time(2.0)" -> 1.0D;
            default -> 0.0D;
        }), 1.0E-8D);
    }

    @Test
    void samplesInteractionQueriesFromNonEntityPreviewProvider() {
        BbModelData.Document document = BbModelParser.parse(JsonParser.parseString("""
                {
                  "meta":{"format_version":"5.0","model_format":"modded_entity"},
                  "elements":[],
                  "animations":[{"name":"interaction","animators":{"bone":{"type":"bone","keyframes":[
                    {"channel":"position","time":0,"data_points":[
                      {"x":"q.vehicle_interaction(2) + q.vehicle_interaction_time(2)","y":0,"z":0}
                    ]}
                  ]}}}]
                }
                """).getAsJsonObject());
        RenderableAutomobile preview = (RenderableAutomobile) Proxy.newProxyInstance(
                RenderableAutomobile.class.getClassLoader(),
                new Class<?>[]{RenderableAutomobile.class, VehicleInteractionStateProvider.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getInteractionValue" -> 0.75F;
                    case "getInteractionTime" -> 1.0F;
                    default -> defaultValue(method.getReturnType());
                });

        BbRenderContext.begin(null, preview, 0.0F);
        BbAnimationPlayer.Transform transform;
        try {
            transform = BbAnimationPlayer.sample(
                    document, "interaction", BbRenderContext.current()).get("bone");
        } finally {
            BbRenderContext.end();
        }

        assertEquals(1.75F, transform.position().x, 0.0001F);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0F;
        if (type == double.class) return 0.0D;
        if (type == char.class) return '\0';
        return null;
    }
}
