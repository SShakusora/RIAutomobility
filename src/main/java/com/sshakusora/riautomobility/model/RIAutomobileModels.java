package com.sshakusora.riautomobility.model;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.model.frame.DoubleMotorcarFrameModel;
import com.sshakusora.riautomobility.model.frame.QuadMotorcarFrameModel;
import com.sshakusora.riautomobility.model.gecko.GeckoFrameModel;
import com.sshakusora.riautomobility.model.gecko.frame.dmc12.DmcAnimatable;
import com.sshakusora.riautomobility.model.gecko.frame.dmc12.DmcModel;
import com.sshakusora.riautomobility.model.gecko.frame.dmc12.DmcRenderer;
import com.sshakusora.riautomobility.model.gecko.frame.lorry.LorryAnimatable;
import com.sshakusora.riautomobility.model.gecko.frame.lorry.LorryModel;
import com.sshakusora.riautomobility.model.gecko.frame.lorry.LorryRenderer;
import com.sshakusora.riautomobility.model.gecko.frame.standard_formula.StandardFormulaAnimatable;
import com.sshakusora.riautomobility.model.gecko.frame.standard_formula.StandardFormulaModel;
import com.sshakusora.riautomobility.model.gecko.frame.standard_formula.StandardFormulaRenderer;
import io.github.foundationgames.automobility.automobile.render.AutomobileModels;
import io.github.foundationgames.automobility.forge.vendored.jsonem.JsonEM;

public class RIAutomobileModels {
    public static void init(){
        AutomobileModels.register(RIAutomobility.rl("frame_doublemotorcar"), DoubleMotorcarFrameModel::new);
        JsonEM.registerModelLayer(DoubleMotorcarFrameModel.MODEL_LAYER);

        AutomobileModels.register(RIAutomobility.rl("frame_quadmotorcar"), QuadMotorcarFrameModel::new);
        JsonEM.registerModelLayer(QuadMotorcarFrameModel.MODEL_LAYER);

        AutomobileModels.register(RIAutomobility.rl("frame_lorry"), context -> {
            LorryAnimatable anim = new LorryAnimatable();
            LorryModel model = new LorryModel();
            LorryRenderer renderer = new LorryRenderer(model, anim);

            return new GeckoFrameModel<>(model, renderer, anim);
        });

        AutomobileModels.register(RIAutomobility.rl("frame_dmc12"), context -> {
            DmcAnimatable anim = new DmcAnimatable();
            DmcModel model = new DmcModel();
            DmcRenderer renderer = new DmcRenderer(model, anim);

            return new GeckoFrameModel<>(model, renderer, anim);
        });

        AutomobileModels.register(RIAutomobility.rl("frame_standard_formula"), context -> {
            StandardFormulaAnimatable anim = new StandardFormulaAnimatable();
            StandardFormulaModel model = new StandardFormulaModel();
            StandardFormulaRenderer renderer = new StandardFormulaRenderer(model, anim);

            return new GeckoFrameModel<>(model, renderer, anim);
        });
    }
}
