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

    public static final AutomobileFrame WOODEN_DOUBLEMOTORCAR = registerDoubleMotorcar("wooden", 0.6F);
    public static final AutomobileFrame COPPER_DOUBLEMOTORCAR = registerDoubleMotorcar("copper", 0.8F);
    public static final AutomobileFrame STEEL_DOUBLEMOTORCAR = registerDoubleMotorcar("steel", 0.95F);
    public static final AutomobileFrame GOLDEN_DOUBLEMOTORCAR = registerDoubleMotorcar("golden", 1.05F);
    public static final AutomobileFrame BEJEWELED_DOUBLEMOTORCAR = registerDoubleMotorcar("bejeweled", 1.11F);

    private static AutomobileFrame registerDoubleMotorcar(String variant, float weight) {
        return register(new AutomobileFrame(
                    RIAutomobility.rl(variant + "_doublemotorcar"),
                        weight,
                    new AutomobileFrame.FrameModel(
                            RIAutomobility.rl("textures/entity/automobile/frame/" + variant + "_doublemotorcar.png"),
                            RIAutomobility.rl("frame_doublemotorcar"),
                            WheelBase.basic(32.0F, 24.0F),
                            32.0F,
                            3.0F,
                            18.0F,
                            2.0F,
                            23.0F,
                            22.0F
                    )
            ),
            List.of(
                    new Vec3(6.0/16, -1.6/16, 0),
                    new Vec3(-6.0/16, -1.6/16, 0)
            ));
    }

    private static AutomobileFrame register(AutomobileFrame frame, List<Vec3> seats){
        RIAutomobileSeatRegistry.register(frame, seats);
        FRAMES.add(frame);
        return AutomobileFrame.REGISTRY.register(frame);
    }

    public static boolean isRIAutomobileFrame(AutomobileFrame frame){
        return FRAMES.contains(frame);
    }

    public static void init() {}
}
