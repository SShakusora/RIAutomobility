package com.sshakusora.riautomobility.interaction;

/**
 * Supplies the runtime values exposed to BBModel vehicle-interaction Molang queries.
 */
public interface VehicleInteractionStateProvider {
    float getInteractionValue(int channel, float partialTick);

    float getInteractionTime(int channel, float partialTick);
}
