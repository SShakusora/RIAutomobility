package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.interaction.VehicleInteractionAction;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VehicleInteractionEditorDraftTest {
    @Test
    void persistsInteractionGeometryAndAllActions() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        VehicleEditorDraft original = draft();
        original.disableHitboxInteractions = true;
        original.interactionBoxes.add(new VehicleEditorDraft.InteractionBoxPoint(
                "left_door",
                new Vec3(0.8D, 0.7D, 0.0D),
                new Vec3(0.2D, 1.2D, 1.0D),
                new Vec3(0.0D, 12.0D, 0.0D),
                List.of(
                        new VehicleInteractionAction.Mount(0, true),
                        new VehicleInteractionAction.Molang(
                                0, VehicleInteractionAction.MolangOperation.PULSE,
                                1.0F, 12, 4,
                                VehicleInteractionAction.Trigger.SHIFT_LEFT_CLICK, false)
                )
        ));

        VehicleEditorDraft restored = draft();
        restored.load(original.save());

        assertEquals(original.interactionBoxes, restored.interactionBoxes);
        assertEquals(original.disableHitboxInteractions, restored.disableHitboxInteractions);
        assertEquals(original.disableHitboxInteractions,
                restored.frameSpec(false).disableHitboxInteractions());
        assertEquals(original.interactionBoxes.get(0).toSpec(),
                restored.interactionBoxes.get(0).toSpec());
    }

    private static VehicleEditorDraft draft() {
        return new VehicleEditorDraft(
                AutomobileFrame.EMPTY, AutomobileWheel.EMPTY, AutomobileEngine.EMPTY);
    }
}
