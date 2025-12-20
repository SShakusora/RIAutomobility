package com.sshakusora.riautomobility.model;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.model.frame.DoubleMotorcarFrameModel;
import com.sshakusora.riautomobility.model.frame.QuadMotorcarFrameModel;
import com.sshakusora.riautomobility.model.gecko.GeckoFrameModel;
import com.sshakusora.riautomobility.model.gecko.frame.TestGeckoFrameAnimatable;
import com.sshakusora.riautomobility.model.gecko.frame.TestGeckoFrameModel;
import com.sshakusora.riautomobility.model.gecko.frame.TestGeckoFrameRenderer;
import io.github.foundationgames.automobility.automobile.render.AutomobileModels;
import io.github.foundationgames.automobility.forge.vendored.jsonem.JsonEM;

public class RIAutomobileModels {
    public static void init(){
        AutomobileModels.register(RIAutomobility.rl("frame_doublemotorcar"), DoubleMotorcarFrameModel::new);
        JsonEM.registerModelLayer(DoubleMotorcarFrameModel.MODEL_LAYER);

        AutomobileModels.register(RIAutomobility.rl("frame_quadmotorcar"), QuadMotorcarFrameModel::new);
        JsonEM.registerModelLayer(QuadMotorcarFrameModel.MODEL_LAYER);

        AutomobileModels.register(RIAutomobility.rl("test_geckoframe"), context -> {
            TestGeckoFrameAnimatable anim = new TestGeckoFrameAnimatable();
            TestGeckoFrameModel model = new TestGeckoFrameModel();
            TestGeckoFrameRenderer renderer = new TestGeckoFrameRenderer(model, anim);

            return new GeckoFrameModel<>(model, renderer, anim);
        });
    }
}
