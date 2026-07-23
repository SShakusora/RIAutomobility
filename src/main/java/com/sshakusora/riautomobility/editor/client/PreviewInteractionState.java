package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.interaction.VehicleInteractionAction;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

final class PreviewInteractionState {
    private static final int TICKS_PER_SECOND = 20;
    private final ChannelState[] channels =
            new ChannelState[VehicleInteractionAction.MAX_CHANNEL + 1];

    void apply(VehicleInteractionAction.Molang action, double nowTick) {
        int channel = action.channel();
        ChannelState state = channels[channel];
        if (state == null) {
            state = new ChannelState();
            channels[channel] = state;
        }
        advancePulse(state, nowTick);
        float current = valueAt(state, nowTick);
        float target = switch (action.operation()) {
            case SET, PULSE -> action.value();
            case TOGGLE -> state.target >= 0.5F ? 0.0F : action.value();
        };
        state.from = current;
        state.target = target;
        state.startTick = nowTick;
        state.transitionTicks = action.transitionTicks();
        if (action.operation() == VehicleInteractionAction.MolangOperation.PULSE) {
            state.pulseEndTick = nowTick + action.durationTicks();
            state.returnTransitionTicks = action.transitionTicks();
        } else {
            state.pulseEndTick = Double.NaN;
            state.returnTransitionTicks = 0;
        }
    }

    float value(int channel, double nowTick) {
        ChannelState state = channel(channel);
        if (state == null) {
            return 0.0F;
        }
        advancePulse(state, nowTick);
        return valueAt(state, nowTick);
    }

    float time(int channel, double nowTick) {
        ChannelState state = channel(channel);
        if (state == null) {
            return 0.0F;
        }
        advancePulse(state, nowTick);
        return (float) Math.max(0.0D, (nowTick - state.startTick) / TICKS_PER_SECOND);
    }

    void clear() {
        Arrays.fill(channels, null);
    }

    private @Nullable ChannelState channel(int channel) {
        if (channel < 0 || channel >= channels.length) {
            return null;
        }
        return channels[channel];
    }

    private static void advancePulse(ChannelState state, double nowTick) {
        if (Double.isNaN(state.pulseEndTick) || nowTick < state.pulseEndTick) {
            return;
        }
        double returnStart = state.pulseEndTick;
        state.from = valueAt(state, returnStart);
        state.target = 0.0F;
        state.startTick = returnStart;
        state.transitionTicks = state.returnTransitionTicks;
        state.pulseEndTick = Double.NaN;
        state.returnTransitionTicks = 0;
    }

    private static float valueAt(ChannelState state, double nowTick) {
        if (state.transitionTicks <= 0) {
            return state.target;
        }
        float progress = Mth.clamp(
                (float) ((nowTick - state.startTick) / state.transitionTicks),
                0.0F, 1.0F);
        return Mth.lerp(progress, state.from, state.target);
    }

    private static final class ChannelState {
        private float from;
        private float target;
        private double startTick;
        private int transitionTicks;
        private double pulseEndTick = Double.NaN;
        private int returnTransitionTicks;
    }
}
