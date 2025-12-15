package com.sshakusora.riautomobility.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class RIAutomobilityKeyBindings {
    public static final String CATEGORY = "key.categories." + RIAutomobility.MODID;

    public static final KeyMapping BOARDING_AS_PASSENGER = new KeyMapping(
            "key." + RIAutomobility.MODID + ".boarding_as_passenger",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    public static void init(RegisterKeyMappingsEvent event) {
        event.register(BOARDING_AS_PASSENGER);
    }
}
