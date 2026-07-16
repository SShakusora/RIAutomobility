package com.sshakusora.riautomobility.model.bbmodel;

import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.model.PlaceholderAutomobileModel;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DynamicBbModelTest {
    private static final ResourceLocation COMPONENT_ID = new ResourceLocation("test", "frame");
    private static final ResourceLocation MODEL_ID = new ResourceLocation("test", "frame_model");
    private static final ResourceLocation MODEL_RESOURCE = new ResourceLocation("test", "models/frame.bbmodel");
    private static final ResourceLocation CONFIGURED_TEXTURE = new ResourceLocation("test", "textures/frame.png");

    @Test
    void missingDocumentUsesPlaceholderTextureBeforeRendering() {
        ResourceLocation texture = DynamicBbModel.textureWhileModelMissing(CONFIGURED_TEXTURE);

        assertEquals(PlaceholderAutomobileModel.TEXTURE, texture);
        assertNotEquals(CONFIGURED_TEXTURE, texture);
    }

    @Test
    void missingDocumentReportsComponentOnlyOnce() {
        AtomicInteger reports = new AtomicInteger();
        DynamicBbModel model = createModel(ignored -> reports.incrementAndGet());

        model.renderToBuffer(null, null, 0, 0, 1.0F, 1.0F, 1.0F, 1.0F);
        model.renderToBuffer(null, null, 0, 0, 1.0F, 1.0F, 1.0F, 1.0F);

        assertEquals(1, reports.get());
    }

    private static DynamicBbModel createModel(Consumer<ResourceLocation> missingCallback) {
        FrameSpec.ModelSpec spec = new FrameSpec.ModelSpec(
                "bbmodel",
                CONFIGURED_TEXTURE,
                MODEL_ID,
                "entity_cutout",
                0.0F,
                MODEL_RESOURCE,
                Map.of(),
                ""
        );
        return new DynamicBbModel(COMPONENT_ID, spec, null, missingCallback);
    }
}
