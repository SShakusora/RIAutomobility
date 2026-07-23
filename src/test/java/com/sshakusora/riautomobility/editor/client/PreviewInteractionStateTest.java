package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.interaction.VehicleInteractionAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreviewInteractionStateTest {
    @Test
    void setInterpolatesToPersistentTarget() {
        PreviewInteractionState state = new PreviewInteractionState();

        state.apply(action(VehicleInteractionAction.MolangOperation.SET, 10), 0.0D);

        assertEquals(0.0F, state.value(0, 0.0D), 0.0001F);
        assertEquals(0.5F, state.value(0, 5.0D), 0.0001F);
        assertEquals(1.0F, state.value(0, 10.0D), 0.0001F);
        assertEquals(1.0F, state.value(0, 40.0D), 0.0001F);
        assertEquals(0.25F, state.time(0, 5.0D), 0.0001F);
    }

    @Test
    void toggleReversesFromCurrentInterpolatedValue() {
        PreviewInteractionState state = new PreviewInteractionState();
        VehicleInteractionAction.Molang toggle =
                action(VehicleInteractionAction.MolangOperation.TOGGLE, 4);

        state.apply(toggle, 0.0D);
        assertEquals(0.5F, state.value(0, 2.0D), 0.0001F);

        state.apply(toggle, 2.0D);

        assertEquals(0.25F, state.value(0, 4.0D), 0.0001F);
        assertEquals(0.0F, state.value(0, 6.0D), 0.0001F);
    }

    @Test
    void pulseReturnsAfterDurationAndResetsInteractionTime() {
        PreviewInteractionState state = new PreviewInteractionState();

        state.apply(action(VehicleInteractionAction.MolangOperation.PULSE, 4), 0.0D);

        assertEquals(0.5F, state.value(0, 2.0D), 0.0001F);
        assertEquals(1.0F, state.value(0, 10.0D), 0.0001F);
        assertEquals(0.5F, state.value(0, 12.0D), 0.0001F);
        assertEquals(0.0F, state.value(0, 14.0D), 0.0001F);
        assertEquals(0.1F, state.time(0, 12.0D), 0.0001F);
    }

    @Test
    void clearRestoresEveryChannelToZero() {
        PreviewInteractionState state = new PreviewInteractionState();
        state.apply(action(VehicleInteractionAction.MolangOperation.SET, 0), 0.0D);

        state.clear();

        assertEquals(0.0F, state.value(0, 1.0D), 0.0001F);
        assertEquals(0.0F, state.time(0, 1.0D), 0.0001F);
    }

    private static VehicleInteractionAction.Molang action(
            VehicleInteractionAction.MolangOperation operation,
            int transitionTicks) {
        return new VehicleInteractionAction.Molang(
                0, operation, 1.0F, 10, transitionTicks, false);
    }
}
