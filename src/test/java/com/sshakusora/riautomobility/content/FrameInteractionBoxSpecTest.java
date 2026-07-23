package com.sshakusora.riautomobility.content;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sshakusora.riautomobility.interaction.VehicleInteractionAction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrameInteractionBoxSpecTest {
    @Test
    void parsesAndWritesComposableInteractionBoxes() {
        JsonObject json = JsonParser.parseString("""
                {
                  "weight": 1,
                  "model": {
                    "type": "bbmodel",
                    "texture": "minecraft:textures/item/barrier.png",
                    "model_id": "automobility:empty",
                    "bbmodel": "test:models/frame.bbmodel"
                  },
                  "wheel_base": {"forward_separation": 16, "side_separation": 10},
                  "length_px": 24,
                  "engine_pos_back": 8,
                  "engine_pos_up": 2,
                  "rear_attachment_pos": 12,
                  "front_attachment_pos": 12,
                  "dimensions": {"width": 1.5, "height": 1},
                  "seats": [],
                  "camera_positions": [],
                   "hitboxes": [],
                   "disable_hitbox_interactions": true,
                   "interaction_boxes": [{
                    "id": "trunk",
                    "center": {"x": 0, "y": 0.75, "z": 1},
                    "size": {"x": 1.2, "y": 0.5, "z": 0.2},
                    "rotation": {"y": 15},
                    "actions": [
                       {"type": "open_container"},
                       {"type": "molang", "channel": 2, "operation": "toggle",
                        "trigger": "shift_right_click", "transition_ticks": 5}
                    ]
                  }]
                }
                """).getAsJsonObject();

        FrameSpec spec = FrameSpec.fromJson(new ResourceLocation("test", "frame"), json);

        assertEquals(1, spec.interactionBoxes().size());
        FrameSpec.InteractionBoxSpec box = spec.interactionBoxes().get(0);
        assertEquals("trunk", box.id());
        assertEquals(2, box.actions().size());
        assertInstanceOf(VehicleInteractionAction.OpenContainer.class, box.actions().get(0));
        assertInstanceOf(VehicleInteractionAction.Molang.class, box.actions().get(1));
        assertEquals(VehicleInteractionAction.Trigger.SHIFT_RIGHT_CLICK,
                box.actions().get(1).trigger());
        assertTrue(spec.disableHitboxInteractions());
        assertTrue(spec.toDefinition().disableHitboxInteractions());
        FrameSpec restored = FrameSpec.fromJson(new ResourceLocation("test", "frame"), spec.toJson());
        assertEquals(spec.interactionBoxes(), restored.interactionBoxes());
        assertTrue(restored.disableHitboxInteractions());
    }

    @Test
    void rejectsDuplicateInteractionIds() {
        JsonObject json = JsonParser.parseString("""
                {
                  "weight": 1,
                  "model": {"type": "bbmodel", "texture": "minecraft:textures/item/barrier.png",
                            "model_id": "automobility:empty",
                            "bbmodel": "test:models/frame.bbmodel"},
                  "wheel_base": {"forward_separation": 16, "side_separation": 10},
                  "length_px": 24, "engine_pos_back": 8, "engine_pos_up": 2,
                  "rear_attachment_pos": 12, "front_attachment_pos": 12,
                  "dimensions": {"width": 1.5, "height": 1},
                  "seats": [], "camera_positions": [], "hitboxes": [],
                  "interaction_boxes": [
                    {"id": "door", "size": {"x": 1, "y": 1, "z": 1},
                     "actions": [{"type": "mount"}]},
                    {"id": "door", "size": {"x": 1, "y": 1, "z": 1},
                     "actions": [{"type": "mount"}]}
                  ]
                }
                """).getAsJsonObject();

        assertThrows(IllegalArgumentException.class, () ->
                FrameSpec.fromJson(new ResourceLocation("test", "frame"), json));
    }

    @Test
    void keepsHitboxInteractionsEnabledForLegacyFrames() {
        JsonObject json = JsonParser.parseString("""
                {
                  "weight": 1,
                  "model": {"type": "bbmodel", "texture": "minecraft:textures/item/barrier.png",
                            "model_id": "automobility:empty",
                            "bbmodel": "test:models/frame.bbmodel"},
                  "wheel_base": {"forward_separation": 16, "side_separation": 10},
                  "length_px": 24, "engine_pos_back": 8, "engine_pos_up": 2,
                  "rear_attachment_pos": 12, "front_attachment_pos": 12,
                  "dimensions": {"width": 1.5, "height": 1},
                  "seats": [], "camera_positions": [], "hitboxes": []
                }
                """).getAsJsonObject();

        FrameSpec spec = FrameSpec.fromJson(new ResourceLocation("test", "legacy_frame"), json);

        assertFalse(spec.disableHitboxInteractions());
        assertFalse(spec.toDefinition().disableHitboxInteractions());
    }
}
