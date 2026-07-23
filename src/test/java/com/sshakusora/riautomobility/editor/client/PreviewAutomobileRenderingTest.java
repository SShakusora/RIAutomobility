package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.model.bbmodel.BbInstancedRenderer;
import com.sshakusora.riautomobility.interaction.VehicleInteractionStateProvider;
import io.github.foundationgames.automobility.automobile.render.item.ItemRenderableAutomobile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewAutomobileRenderingTest {
    @Test
    void onlyEditorPreviewOptsIntoNonEntityInstancing() {
        assertTrue(BbInstancedRenderer.ImmediateTarget.class.isAssignableFrom(PreviewAutomobile.class));
        assertTrue(VehicleInteractionStateProvider.class.isAssignableFrom(PreviewAutomobile.class));
        assertFalse(BbInstancedRenderer.ImmediateTarget.class.isAssignableFrom(ItemRenderableAutomobile.class));
    }
}
