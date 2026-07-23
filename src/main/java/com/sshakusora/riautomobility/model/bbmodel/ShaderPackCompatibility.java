package com.sshakusora.riautomobility.model.bbmodel;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

final class ShaderPackCompatibility {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String IRIS_API_CLASS = "net.irisshaders.iris.api.v0.IrisApi";
    private static final ShaderPackProbe PROBE = discoverProbe();
    private static boolean warnedProbeFailure;

    private ShaderPackCompatibility() {
    }

    static boolean isShaderPackInUse() {
        return PROBE.isShaderPackInUse();
    }

    static boolean isRenderingShadowPass() {
        return PROBE.isRenderingShadowPass();
    }

    static boolean allowsCustomInstancing(boolean shaderPackInUse) {
        return !shaderPackInUse;
    }

    private static ShaderPackProbe discoverProbe() {
        try {
            Class<?> apiType = Class.forName(IRIS_API_CLASS, false,
                    ShaderPackCompatibility.class.getClassLoader());
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            MethodHandle getInstance = lookup.findStatic(apiType, "getInstance",
                    MethodType.methodType(apiType));
            Object api = getInstance.invoke();
            MethodHandle isShaderPackInUse = lookup.findVirtual(apiType, "isShaderPackInUse",
                    MethodType.methodType(boolean.class)).bindTo(api);
            MethodHandle isRenderingShadowPass = lookup.findVirtual(apiType, "isRenderingShadowPass",
                    MethodType.methodType(boolean.class)).bindTo(api);
            return new ShaderPackProbe() {
                @Override
                public boolean isShaderPackInUse() {
                    try {
                        return (boolean) isShaderPackInUse.invokeExact();
                    } catch (Throwable throwable) {
                        warnProbeFailure(throwable);
                        return true;
                    }
                }

                @Override
                public boolean isRenderingShadowPass() {
                    try {
                        return (boolean) isRenderingShadowPass.invokeExact();
                    } catch (Throwable throwable) {
                        warnProbeFailure(throwable);
                        return false;
                    }
                }
            };
        } catch (ClassNotFoundException ignored) {
            return ShaderPackProbe.NONE;
        } catch (Throwable throwable) {
            warnProbeFailure(throwable);
            return ShaderPackProbe.FAILED;
        }
    }

    private static synchronized void warnProbeFailure(Throwable throwable) {
        if (warnedProbeFailure) return;
        warnedProbeFailure = true;
        LOGGER.warn("Unable to query Iris/Oculus shader-pack state; disabling BBModel instancing for compatibility",
                throwable);
    }

    private interface ShaderPackProbe {
        ShaderPackProbe NONE = fixed(false);
        ShaderPackProbe FAILED = fixed(true);

        boolean isShaderPackInUse();

        boolean isRenderingShadowPass();

        private static ShaderPackProbe fixed(boolean shaderPackInUse) {
            return new ShaderPackProbe() {
                @Override
                public boolean isShaderPackInUse() {
                    return shaderPackInUse;
                }

                @Override
                public boolean isRenderingShadowPass() {
                    return false;
                }
            };
        }
    }
}
