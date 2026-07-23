package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.interaction.VehicleInteractionAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MolangActionEditorTest {
    @Test
    void sequentialEditsPreservePreviouslySelectedChannel() {
        VehicleInteractionAction.Molang action = new VehicleInteractionAction.Molang(
                0, VehicleInteractionAction.MolangOperation.PULSE, 1.0F, 10, 0,
                VehicleInteractionAction.Trigger.RIGHT_CLICK, false);

        action = MolangActionEditor.withChannel(action, 1.0F);
        action = MolangActionEditor.withValue(action, 0.35F);
        action = MolangActionEditor.withDurationTicks(action, 40.0F);
        action = MolangActionEditor.withTransitionTicks(action, 6.0F);
        action = MolangActionEditor.withOperation(
                action, VehicleInteractionAction.MolangOperation.TOGGLE);
        action = MolangActionEditor.withTrigger(
                action, VehicleInteractionAction.Trigger.SHIFT_LEFT_CLICK);

        assertEquals(1, action.channel());
        assertEquals(0.35F, action.value(), 0.0001F);
        assertEquals(40, action.durationTicks());
        assertEquals(6, action.transitionTicks());
        assertEquals(VehicleInteractionAction.MolangOperation.TOGGLE, action.operation());
        assertEquals(VehicleInteractionAction.Trigger.SHIFT_LEFT_CLICK, action.trigger());
    }

    @Test
    void numericEditsRemainWithinSupportedBounds() {
        VehicleInteractionAction.Molang action = new VehicleInteractionAction.Molang(
                1, VehicleInteractionAction.MolangOperation.SET, 1.0F, 10, 0, false);

        assertEquals(0, MolangActionEditor.withChannel(action, -10.0F).channel());
        assertEquals(VehicleInteractionAction.MAX_CHANNEL,
                MolangActionEditor.withChannel(action, 100.0F).channel());
        assertEquals(0, MolangActionEditor.withDurationTicks(action, -10.0F).durationTicks());
        assertEquals(72_000,
                MolangActionEditor.withTransitionTicks(action, 100_000.0F).transitionTicks());
    }
}
