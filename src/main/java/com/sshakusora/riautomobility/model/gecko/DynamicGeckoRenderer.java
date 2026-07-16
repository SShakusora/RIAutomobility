package com.sshakusora.riautomobility.model.gecko;

import software.bernie.geckolib.model.GeoModel;

public class DynamicGeckoRenderer extends AllocationFreeGeoRenderer<DynamicGeckoAnimatable> {
    public DynamicGeckoRenderer(GeoModel<DynamicGeckoAnimatable> model, DynamicGeckoAnimatable animatable) {
        super(model, animatable);
    }
}
