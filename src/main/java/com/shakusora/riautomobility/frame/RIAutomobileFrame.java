package com.shakusora.riautomobility.frame;

import com.shakusora.riautomobility.RIAutomobility;
import io.github.foundationgames.automobility.Automobility;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.WheelBase;

import java.util.HashSet;
import java.util.Set;

public class RIAutomobileFrame {
    private static Set<AutomobileFrame> FRAMES = new HashSet<>();

    public static final AutomobileFrame TEST_FRAME = rigister(new AutomobileFrame(
            RIAutomobility.rl("test_frame"),
            0.6F,
            new AutomobileFrame.FrameModel(
                    Automobility.rl("textures/entity/automobile/frame/c_arr.png"),
                    Automobility.rl("frame_c_arr"),
                    WheelBase.basic(44.5F, 16.0F),
                    44.0F,
                    6.0F,
                    19.5F,
                    10.5F,
                    23.0F,
                    23.0F
            )
    ));

    private static AutomobileFrame rigister(AutomobileFrame frame){
        FRAMES.add(frame);
        return AutomobileFrame.REGISTRY.register(frame);
    }

    public boolean isRIAutomobileFrame(AutomobileFrame frame){
        return FRAMES.contains(frame);
    }

    public static void init() {
    }
}
