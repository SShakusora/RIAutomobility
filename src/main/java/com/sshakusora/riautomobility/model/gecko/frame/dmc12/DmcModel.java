package com.sshakusora.riautomobility.model.gecko.frame.dmc12;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DmcModel extends GeoModel<DmcAnimatable> {
    @Override
    public ResourceLocation getModelResource(DmcAnimatable animatable) {
        return RIAutomobility.rl("geo/dmc12.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DmcAnimatable animatable) {
        return RIAutomobility.rl("textures/dmc12.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DmcAnimatable animatable) {
        return RIAutomobility.rl("animations/dmc12.animation.json");
    }
}
