package com.sshakusora.riautomobility.model.gecko.frame.lorry;

import com.sshakusora.riautomobility.model.gecko.AllocationFreeGeoRenderer;
import software.bernie.geckolib.model.GeoModel;

public class LorryRenderer extends AllocationFreeGeoRenderer<LorryAnimatable> {
    public LorryRenderer(GeoModel<LorryAnimatable> model, LorryAnimatable animatable) {
        super(model, animatable);
    }
}
