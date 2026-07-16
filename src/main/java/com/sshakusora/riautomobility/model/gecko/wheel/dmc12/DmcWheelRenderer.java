package com.sshakusora.riautomobility.model.gecko.wheel.dmc12;

import com.sshakusora.riautomobility.model.gecko.AllocationFreeGeoRenderer;
import software.bernie.geckolib.model.GeoModel;

public class DmcWheelRenderer extends AllocationFreeGeoRenderer<DmcWheelAnimatable> {
    public DmcWheelRenderer(GeoModel<DmcWheelAnimatable> model, DmcWheelAnimatable animatable) {
        super(model, animatable);
    }
}
