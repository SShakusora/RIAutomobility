package com.sshakusora.riautomobility.content;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

public final class ComponentSpecNetworkCodec {
    public static final int MAX_COMPONENTS_PER_TYPE = 4096;
    public static final int MAX_JSON_LENGTH = 32767;
    // Clientbound custom payloads are capped at 1 MiB; reserve the rest for ids, manifests, and framing.
    public static final int MAX_TOTAL_ENCODED_BYTES = 512 * 1024;

    private ComponentSpecNetworkCodec() {
    }

    public static void writeJson(FriendlyByteBuf buffer, JsonObject json) {
        String encoded = json.toString();
        validateJson(encoded);
        buffer.writeUtf(encoded, MAX_JSON_LENGTH);
    }

    public static JsonObject readJson(FriendlyByteBuf buffer) {
        return GsonHelper.parse(buffer.readUtf(MAX_JSON_LENGTH));
    }

    @SafeVarargs
    public static void validateComponents(Collection<? extends NetworkComponentSpec>... componentTypes) {
        long encodedBytes = 0;
        for (Collection<? extends NetworkComponentSpec> components : componentTypes) {
            if (components.size() > MAX_COMPONENTS_PER_TYPE) {
                throw new IllegalArgumentException("Too many custom components of one type: " + components.size());
            }
            for (NetworkComponentSpec component : components) {
                String json = component.toJson().toString();
                validateJson(json);
                encodedBytes += component.id().toString().getBytes(StandardCharsets.UTF_8).length;
                encodedBytes += json.getBytes(StandardCharsets.UTF_8).length;
                encodedBytes += 10L;
                if (encodedBytes > MAX_TOTAL_ENCODED_BYTES) {
                    throw new IllegalArgumentException(
                            "Custom component definitions exceed the network payload limit of "
                                    + MAX_TOTAL_ENCODED_BYTES + " bytes");
                }
            }
        }
    }

    private static void validateJson(String json) {
        if (json.length() > MAX_JSON_LENGTH) {
            throw new IllegalArgumentException(
                    "Custom component JSON exceeds the network string limit of " + MAX_JSON_LENGTH + " characters");
        }
    }
}
