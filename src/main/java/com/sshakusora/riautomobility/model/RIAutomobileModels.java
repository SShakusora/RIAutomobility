package com.sshakusora.riautomobility.model;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.model.frame.DoubleMotorcarFrameModel;
import io.github.foundationgames.automobility.automobile.render.AutomobileModels;
import io.github.foundationgames.automobility.forge.vendored.jsonem.JsonEM;

public class RIAutomobileModels {
    public static void init(){
        AutomobileModels.register(RIAutomobility.rl("frame_doublemotorcar"), DoubleMotorcarFrameModel::new);
        JsonEM.registerModelLayer(DoubleMotorcarFrameModel.MODEL_LAYER);
    }
}
