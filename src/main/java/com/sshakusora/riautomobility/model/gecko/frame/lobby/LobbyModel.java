package com.sshakusora.riautomobility.model.gecko.frame.lobby;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class LobbyModel extends GeoModel<LobbyAnimatable> {
    @Override
    public ResourceLocation getModelResource(LobbyAnimatable animatable) {
        return RIAutomobility.rl("geo/lobby.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LobbyAnimatable animatable) {
        return RIAutomobility.rl("textures/lobby.png");
    }

    @Override
    public ResourceLocation getAnimationResource(LobbyAnimatable animatable) {
        return RIAutomobility.rl("animations/lobby.animation.json");
    }
}
