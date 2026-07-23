package com.sshakusora.riautomobility.interaction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleInteractionActionTest {
    @Test
    void roundTripsEveryActionType() {
        List<VehicleInteractionAction> actions = List.of(
                new VehicleInteractionAction.OpenContainer(true),
                new VehicleInteractionAction.Mount(2, false),
                new VehicleInteractionAction.Molang(
                        3, VehicleInteractionAction.MolangOperation.TOGGLE,
                        0.75F, 15, 4,
                        VehicleInteractionAction.Trigger.SHIFT_LEFT_CLICK, true)
        );

        for (VehicleInteractionAction action : actions) {
            assertEquals(action, VehicleInteractionAction.fromJson(action.toJson()));
        }
    }

    @Test
    void rejectsUnknownActionsAndOutOfRangeChannels() {
        var unknown = new com.google.gson.JsonObject();
        unknown.addProperty("type", "explode");
        assertThrows(IllegalArgumentException.class, () -> VehicleInteractionAction.fromJson(unknown));
        assertThrows(IllegalArgumentException.class, () ->
                new VehicleInteractionAction.Molang(
                        32, VehicleInteractionAction.MolangOperation.SET,
                        1.0F, 0, 0, false));
    }

    @Test
    void containerActionsAlwaysRequireAccess() {
        var json = new com.google.gson.JsonObject();
        json.addProperty("type", "open_container");
        json.addProperty("requires_access", false);

        assertTrue(new VehicleInteractionAction.OpenContainer(false).requiresAccess());
        assertTrue(VehicleInteractionAction.fromJson(json).requiresAccess());
    }

    @Test
    void legacyMolangDefaultsToRightClick() {
        var json = new com.google.gson.JsonObject();
        json.addProperty("type", "molang");
        json.addProperty("channel", 1);

        VehicleInteractionAction action = VehicleInteractionAction.fromJson(json);

        assertEquals(VehicleInteractionAction.Trigger.RIGHT_CLICK, action.trigger());
    }

    @Test
    void mapsAllClickAndShiftCombinations() {
        assertEquals(VehicleInteractionAction.Trigger.LEFT_CLICK,
                VehicleInteractionAction.Trigger.fromInput(true, false));
        assertEquals(VehicleInteractionAction.Trigger.RIGHT_CLICK,
                VehicleInteractionAction.Trigger.fromInput(false, false));
        assertEquals(VehicleInteractionAction.Trigger.SHIFT_LEFT_CLICK,
                VehicleInteractionAction.Trigger.fromInput(true, true));
        assertEquals(VehicleInteractionAction.Trigger.SHIFT_RIGHT_CLICK,
                VehicleInteractionAction.Trigger.fromInput(false, true));
    }
}
