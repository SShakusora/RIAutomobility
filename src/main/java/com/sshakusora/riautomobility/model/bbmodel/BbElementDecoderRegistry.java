package com.sshakusora.riautomobility.model.bbmodel;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BbElementDecoderRegistry {
    private static final Map<String, Decoder> DECODERS = new LinkedHashMap<>();

    private BbElementDecoderRegistry() {}

    public static synchronized void register(String type, Decoder decoder) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("BBModel element type cannot be blank");
        }
        DECODERS.put(type, decoder);
    }

    public static synchronized Decoder get(String type) {
        return DECODERS.get(type);
    }

    @FunctionalInterface
    public interface Decoder {
        BbModelData.Geometry decode(JsonObject element, BbModelParser.Context context);
    }
}
