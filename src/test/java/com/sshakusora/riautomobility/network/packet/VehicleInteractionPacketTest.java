package com.sshakusora.riautomobility.network.packet;

import io.netty.buffer.Unpooled;
import com.sshakusora.riautomobility.interaction.VehicleInteractionAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VehicleInteractionPacketTest {
    @Test
    void roundTripsRequest() {
        VehicleInteractionPacket original =
                new VehicleInteractionPacket(
                        42, "driver_door", InteractionHand.MAIN_HAND,
                        VehicleInteractionAction.Trigger.SHIFT_LEFT_CLICK);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        VehicleInteractionPacket.encode(original, buffer);

        assertEquals(original, VehicleInteractionPacket.decode(buffer));
    }
}
