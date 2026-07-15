package com.sshakusora.riautomobility.content;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComponentSpecNetworkCodecTest {
    @Test
    void acceptsSmallComponentSets() {
        assertDoesNotThrow(() -> ComponentSpecNetworkCodec.validateComponents(
                List.of(spec("one", 100)), List.of(spec("two", 100)), List.of()));
    }

    @Test
    void rejectsAComponentThatExceedsTheStringLimit() {
        NetworkComponentSpec oversized = spec("large", ComponentSpecNetworkCodec.MAX_JSON_LENGTH);

        assertThrows(IllegalArgumentException.class,
                () -> ComponentSpecNetworkCodec.validateComponents(List.of(oversized)));
    }

    @Test
    void rejectsTooManyComponentsOfOneType() {
        NetworkComponentSpec component = spec("repeated", 1);

        assertThrows(IllegalArgumentException.class, () -> ComponentSpecNetworkCodec.validateComponents(
                Collections.nCopies(ComponentSpecNetworkCodec.MAX_COMPONENTS_PER_TYPE + 1, component)));
    }

    @Test
    void rejectsAnOversizedAggregatePayload() {
        List<NetworkComponentSpec> components = java.util.stream.IntStream.range(0, 18)
                .mapToObj(index -> spec("component_" + index, 30_000))
                .toList();

        assertThrows(IllegalArgumentException.class,
                () -> ComponentSpecNetworkCodec.validateComponents(components));
    }

    private static NetworkComponentSpec spec(String path, int valueLength) {
        JsonObject json = new JsonObject();
        json.addProperty("value", "x".repeat(valueLength));
        return new TestSpec(new ResourceLocation("test", path), json);
    }

    private record TestSpec(ResourceLocation id, JsonObject toJson) implements NetworkComponentSpec {
    }
}
