package com.sshakusora.riautomobility.model.gecko.frame.standard_formula;

import com.sshakusora.riautomobility.model.gecko.AllocationFreeGeoRenderer;
import software.bernie.geckolib.model.GeoModel;

public class StandardFormulaRenderer extends AllocationFreeGeoRenderer<StandardFormulaAnimatable> {
    public StandardFormulaRenderer(GeoModel<StandardFormulaAnimatable> model, StandardFormulaAnimatable animatable) {
        super(model, animatable);
    }
}
