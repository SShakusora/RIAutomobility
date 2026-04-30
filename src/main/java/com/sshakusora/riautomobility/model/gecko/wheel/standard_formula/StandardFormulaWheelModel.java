package com.sshakusora.riautomobility.model.gecko.wheel.standard_formula;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class StandardFormulaWheelModel extends GeoModel<StandardFormulaWheelAnimatable> {
    @Override
    public ResourceLocation getModelResource(StandardFormulaWheelAnimatable animatable) {
        return RIAutomobility.rl("geo/wheel/standard_formula.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(StandardFormulaWheelAnimatable animatable) {
        return RIAutomobility.rl("textures/entity/automobile/wheel/standard_formula.png");
    }

    @Override
    public ResourceLocation getAnimationResource(StandardFormulaWheelAnimatable animatable) {
        return RIAutomobility.rl("animations/empty.animation.json");
    }
}
