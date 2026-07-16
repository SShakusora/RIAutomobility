package com.sshakusora.riautomobility.model.gecko.frame.dmc12;

import com.sshakusora.riautomobility.model.gecko.AllocationFreeGeoRenderer;
import software.bernie.geckolib.model.GeoModel;

public class DmcRenderer extends AllocationFreeGeoRenderer<DmcAnimatable> {
    public DmcRenderer(GeoModel<DmcAnimatable> model, DmcAnimatable animatable) {
        super(model, animatable);
    }
}
