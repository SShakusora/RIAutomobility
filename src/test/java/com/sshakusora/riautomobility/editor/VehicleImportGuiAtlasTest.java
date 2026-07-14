package com.sshakusora.riautomobility.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleImportGuiAtlasTest {
    @Test
    void spritesStayInsideAtlasWithoutOverlap() {
        VehicleImportGuiAtlas.Sprite[] sprites = VehicleImportGuiAtlas.Sprite.values();
        for (int index = 0; index < sprites.length; index++) {
            VehicleImportGuiAtlas.Sprite sprite = sprites[index];
            assertTrue(sprite.u() >= 0 && sprite.v() >= 0);
            assertTrue(sprite.u() + sprite.width() <= VehicleImportGuiAtlas.SIZE);
            assertTrue(sprite.v() + sprite.height() <= VehicleImportGuiAtlas.SIZE);
            assertTrue(sprite.border() * 2 <= sprite.width());
            assertTrue(sprite.border() * 2 <= sprite.height());
            for (int otherIndex = index + 1; otherIndex < sprites.length; otherIndex++) {
                VehicleImportGuiAtlas.Sprite other = sprites[otherIndex];
                assertFalse(overlaps(sprite, other), sprite.name() + " overlaps " + other.name());
            }
        }
    }

    private static boolean overlaps(VehicleImportGuiAtlas.Sprite first,
                                    VehicleImportGuiAtlas.Sprite second) {
        return first.u() < second.u() + second.width()
                && first.u() + first.width() > second.u()
                && first.v() < second.v() + second.height()
                && first.v() + first.height() > second.v();
    }
}
