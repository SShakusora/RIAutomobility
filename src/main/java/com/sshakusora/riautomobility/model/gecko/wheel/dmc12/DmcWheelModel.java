package com.sshakusora.riautomobility.model.gecko.wheel.dmc12;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DmcWheelModel extends GeoModel<DmcWheelAnimatable> {
    @Override
    public ResourceLocation getModelResource(DmcWheelAnimatable animatable) {
        return RIAutomobility.rl("geo/wheel/dmc12.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DmcWheelAnimatable animatable) {
        return RIAutomobility.rl("textures/entity/automobile/wheel/dmc12.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DmcWheelAnimatable animatable) {
        return RIAutomobility.rl("animations/empty.animation.json");
    }
}
