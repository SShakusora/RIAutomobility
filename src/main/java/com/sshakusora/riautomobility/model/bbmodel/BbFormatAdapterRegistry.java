package com.sshakusora.riautomobility.model.bbmodel;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BbFormatAdapterRegistry {
    private static final Map<String, Adapter> ADAPTERS = new LinkedHashMap<>();

    static {
        register("modded_entity", model -> model);
        register("free", model -> model);
        register("java_block", model -> model);
        register("bedrock", model -> model);
        register("bedrock_old", model -> model);
    }

    private BbFormatAdapterRegistry() {}

    public static synchronized void register(String modelFormat, Adapter adapter) {
        ADAPTERS.put(modelFormat, adapter);
    }

    public static synchronized JsonObject adapt(String modelFormat, JsonObject model) {
        Adapter adapter = ADAPTERS.get(modelFormat);
        if (adapter == null) {
            throw new BbModelFormatException("Unsupported Blockbench model format '" + modelFormat + "'. Install a format adapter for it.");
        }
        return adapter.adapt(model);
    }

    @FunctionalInterface
    public interface Adapter {
        JsonObject adapt(JsonObject model);
    }
}
