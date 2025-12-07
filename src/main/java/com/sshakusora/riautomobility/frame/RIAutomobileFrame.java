package com.sshakusora.riautomobility.frame;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.util.RIAutomobileSeatRegistry;
import io.github.foundationgames.automobility.Automobility;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.WheelBase;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RIAutomobileFrame {
    private static Set<AutomobileFrame> FRAMES = new HashSet<>();

    public static final AutomobileFrame TEST_FRAME = rigister(new AutomobileFrame(
            RIAutomobility.rl("test_frame"),
            0.6F,
            new AutomobileFrame.FrameModel(
                    Automobility.rl("textures/entity/automobile/frame/golden_doublemotorcar.png"),
                    RIAutomobility.rl("frame_doublemotorcar"),
                    WheelBase.basic(32.0F, 24.0F),
                    24.0F,
                    3.0F,
                    18.0F,
                    2.0F,
                    23.0F,
                    22.0F
            )
    ),
            List.of(
                    new Vec3(6.0/16, -1.0/16, 0),
                    new Vec3(-6.0/16, -1.0/16, 0)
            ));

    private static AutomobileFrame rigister(AutomobileFrame frame, List<Vec3> seats){
        RIAutomobileSeatRegistry.register(frame, seats);
        FRAMES.add(frame);
        return AutomobileFrame.REGISTRY.register(frame);
    }

    public static boolean isRIAutomobileFrame(AutomobileFrame frame){
        return FRAMES.contains(frame);
    }

    public static void init() {}
}
