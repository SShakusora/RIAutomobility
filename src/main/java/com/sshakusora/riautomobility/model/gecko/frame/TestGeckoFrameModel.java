package com.sshakusora.riautomobility.model.gecko.frame;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TestGeckoFrameModel extends GeoModel<TestGeckoFrameAnimatable> {
    @Override
    public ResourceLocation getModelResource(TestGeckoFrameAnimatable animatable) {
        return RIAutomobility.rl("geo/test_geckoframe.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TestGeckoFrameAnimatable animatable) {
        return RIAutomobility.rl("textures/test_geckoframe.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TestGeckoFrameAnimatable animatable) {
        return RIAutomobility.rl("animations/test_geckoframe.animation.json");
    }
}
