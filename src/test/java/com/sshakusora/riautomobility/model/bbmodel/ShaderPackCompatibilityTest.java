package com.sshakusora.riautomobility.model.bbmodel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShaderPackCompatibilityTest {
    @Test
    void customInstancingIsDisabledWhileShaderPackIsActive() {
        assertFalse(ShaderPackCompatibility.allowsCustomInstancing(true));
    }

    @Test
    void customInstancingRemainsAvailableWithoutShaderPack() {
        assertTrue(ShaderPackCompatibility.allowsCustomInstancing(false));
    }
}
