package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.interaction.VehicleInteractionAction;

final class MolangActionEditor {
    private static final int MAX_TICKS = 72_000;

    private MolangActionEditor() {
    }

    static VehicleInteractionAction.Molang withChannel(
            VehicleInteractionAction.Molang current, float value) {
        int channel = Math.max(0, Math.min(
                VehicleInteractionAction.MAX_CHANNEL, Math.round(value)));
        return new VehicleInteractionAction.Molang(
                channel, current.operation(), current.value(), current.durationTicks(),
                current.transitionTicks(), current.trigger(), current.requiresAccess());
    }

    static VehicleInteractionAction.Molang withOperation(
            VehicleInteractionAction.Molang current,
            VehicleInteractionAction.MolangOperation operation) {
        return new VehicleInteractionAction.Molang(
                current.channel(), operation, current.value(), current.durationTicks(),
                current.transitionTicks(), current.trigger(), current.requiresAccess());
    }

    static VehicleInteractionAction.Molang withValue(
            VehicleInteractionAction.Molang current, float value) {
        return new VehicleInteractionAction.Molang(
                current.channel(), current.operation(), value, current.durationTicks(),
                current.transitionTicks(), current.trigger(), current.requiresAccess());
    }

    static VehicleInteractionAction.Molang withDurationTicks(
            VehicleInteractionAction.Molang current, float value) {
        return new VehicleInteractionAction.Molang(
                current.channel(), current.operation(), current.value(), clampedTicks(value),
                current.transitionTicks(), current.trigger(), current.requiresAccess());
    }

    static VehicleInteractionAction.Molang withTransitionTicks(
            VehicleInteractionAction.Molang current, float value) {
        return new VehicleInteractionAction.Molang(
                current.channel(), current.operation(), current.value(), current.durationTicks(),
                clampedTicks(value), current.trigger(), current.requiresAccess());
    }

    static VehicleInteractionAction.Molang withTrigger(
            VehicleInteractionAction.Molang current,
            VehicleInteractionAction.Trigger trigger) {
        return new VehicleInteractionAction.Molang(
                current.channel(), current.operation(), current.value(), current.durationTicks(),
                current.transitionTicks(), trigger, current.requiresAccess());
    }

    private static int clampedTicks(float value) {
        return Math.max(0, Math.min(MAX_TICKS, Math.round(value)));
    }
}
