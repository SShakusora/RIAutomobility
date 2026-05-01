package com.sshakusora.riautomobility.model.gecko.frame.standard_formula;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class StandardFormulaModel extends GeoModel<StandardFormulaAnimatable> {
    @Override
    public ResourceLocation getModelResource(StandardFormulaAnimatable animatable) {
        return RIAutomobility.rl("geo/standard_formula.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(StandardFormulaAnimatable animatable) {
        return RIAutomobility.rl("textures/standard_formula.png");
    }

    @Override
    public ResourceLocation getAnimationResource(StandardFormulaAnimatable animatable) {
        return RIAutomobility.rl("animations/standard_formula.animation.json");
    }
}
