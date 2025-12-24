package com.sshakusora.riautomobility.model;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.model.frame.DoubleMotorcarFrameModel;
import com.sshakusora.riautomobility.model.frame.QuadMotorcarFrameModel;
import com.sshakusora.riautomobility.model.gecko.GeckoFrameModel;
import com.sshakusora.riautomobility.model.gecko.frame.lorry.LorryAnimatable;
import com.sshakusora.riautomobility.model.gecko.frame.lorry.LorryModel;
import com.sshakusora.riautomobility.model.gecko.frame.lorry.LorryRenderer;
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
    }
}
