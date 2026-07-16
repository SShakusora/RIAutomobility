package com.sshakusora.riautomobility.editor.client;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleVerticalScrollBarTest {
    @Test
    void scrollsOverflowingHitboxControlsByOneAlignedRow() {
        AtomicInteger appliedScroll = new AtomicInteger();
        VehicleVerticalScrollBar scrollBar = new VehicleVerticalScrollBar(
                0, 0, 7, 122, 140, 24, 0, appliedScroll::set, ignored -> {
                });

        assertTrue(scrollBar.visible);
        assertEquals(0, scrollBar.scroll());
        assertTrue(scrollBar.scrollBy(-1.0D));
        assertEquals(24, scrollBar.scroll());
        assertEquals(24, appliedScroll.get());
        assertTrue(scrollBar.scrollBy(1.0D));
        assertEquals(0, scrollBar.scroll());
    }

    @Test
    void hidesWhenAllControlsFitInsideTheViewport() {
        VehicleVerticalScrollBar scrollBar = new VehicleVerticalScrollBar(
                0, 0, 7, 140, 140, 24, 0, ignored -> {
                }, ignored -> {
                });

        assertFalse(scrollBar.visible);
        assertFalse(scrollBar.scrollBy(-1.0D));
    }

    @Test
    void usesTheSameSmoothStepCurveAsPositionDropdowns() {
        assertEquals(0.0D, VehicleVerticalScrollBar.interpolate(0.0D, 24.0D, 0.0D));
        assertEquals(12.0D, VehicleVerticalScrollBar.interpolate(0.0D, 24.0D, 0.5D));
        assertEquals(24.0D, VehicleVerticalScrollBar.interpolate(0.0D, 24.0D, 1.0D));
    }
}
