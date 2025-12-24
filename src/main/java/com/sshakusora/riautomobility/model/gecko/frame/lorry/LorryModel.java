package com.sshakusora.riautomobility.model.gecko.frame.lorry;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class LorryModel extends GeoModel<LorryAnimatable> {
    @Override
    public ResourceLocation getModelResource(LorryAnimatable animatable) {
        return RIAutomobility.rl("geo/lorry.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LorryAnimatable animatable) {
        return RIAutomobility.rl("textures/lorry.png");
    }

    @Override
    public ResourceLocation getAnimationResource(LorryAnimatable animatable) {
        return RIAutomobility.rl("animations/lorry.animation.json");
    }
}
