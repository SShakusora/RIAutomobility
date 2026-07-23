package com.sshakusora.riautomobility.interaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleInteractionPriorityTest {
    @Test
    void interactionBoxWinsAgainstHitboxFromSameVehicle() {
        assertTrue(VehicleInteractionHandler.interactionBoxTakesPriority(
                true, 1.0D, 3.0D));
    }

    @Test
    void nearerUnrelatedTargetStillBlocksInteractionBox() {
        assertFalse(VehicleInteractionHandler.interactionBoxTakesPriority(
                false, 1.0D, 3.0D));
    }

    @Test
    void nearerInteractionBoxWinsAgainstUnrelatedTarget() {
        assertTrue(VehicleInteractionHandler.interactionBoxTakesPriority(
                false, 3.0D, 1.0D));
    }
}
