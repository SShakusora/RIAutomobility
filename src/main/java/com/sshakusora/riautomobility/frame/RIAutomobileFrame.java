package com.sshakusora.riautomobility.frame;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.util.RIAutomobileCameraRegistry;
import com.sshakusora.riautomobility.util.RIAutomobileEntityDimensionsRegistry;
import com.sshakusora.riautomobility.util.RIAutomobileHitboxRegistry;
import com.sshakusora.riautomobility.util.RIAutomobileSeatRegistry;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.WheelBase;
import net.minecraft.world.entity.EntityDimensions;
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
    public static final AutomobileFrame WOODEN_QUADMOTORCAR = registerQuadMotorcar("wooden", 1.2F);
    public static final AutomobileFrame COPPER_QUADMOTORCAR = registerQuadMotorcar("copper", 1.6F);
    public static final AutomobileFrame STEEL_QUADMOTORCAR = registerQuadMotorcar("steel", 1.9F);
    public static final AutomobileFrame GOLDEN_QUADMOTORCAR = registerQuadMotorcar("golden", 2.1F);
    public static final AutomobileFrame BEJEWELED_QUADMOTORCAR = registerQuadMotorcar("bejeweled", 2.22F);
    public static final AutomobileFrame LORRY = register(new AutomobileFrame(
                    RIAutomobility.rl("lorry"),
                    1.5F,
                    new AutomobileFrame.FrameModel(
                            RIAutomobility.rl("textures/entity/automobile/frame/lorry.png"),
                            RIAutomobility.rl("frame_lorry"),
                            WheelBase.basic(72.0F, 32.0F),
                            76.0F,
                            11.3636F,
                            18.0F,
                            16.0F,
                            23.0F,
                            22.0F
                    )),
            List.of(
                    new RIAutomobileSeatRegistry.SeatPos(9.7727/16,40.3091/16),
                    new RIAutomobileSeatRegistry.SeatPos(-9.7727/16, 40.3091/16)
            ),
            EntityDimensions.scalable(21.7727F/8, 53.3636F/16),
            List.of(
                    /*
                    x:forward and back
                    y:up and down
                    z:left and right
                    */
                    new Vec3(-7.0, 3.0, 0.0),
                    new Vec3(-7.0, 3.0, 0.0)
            ),
            List.of(
                    new RIAutomobileFrame.Hitbox(new Vec3(0, 0.2, 35.9091/16), 21.7727F/8, 53.3636F/16, false),
                    new RIAutomobileFrame.Hitbox(new Vec3(0, 0.2, 14.1364/16), 21.7727F/8, 53.3636F/16, false),
                    new RIAutomobileFrame.Hitbox(new Vec3(0, 0.2, -28.5455/16), 21.7727F/8, 53.3636F/16, false),
                    new RIAutomobileFrame.Hitbox(new Vec3(0, 0.2, -50.3182/16), 21.7727F/8, 53.3636F/16, true)
            )
    );
    public static final AutomobileFrame DMC12 = register(new AutomobileFrame(
                    RIAutomobility.rl("dmc12"),
                    0.98F,
                    new AutomobileFrame.FrameModel(
                            RIAutomobility.rl("textures/entity/automobile/frame/dmc12.png"),
                            RIAutomobility.rl("frame_dmc12"),
                            WheelBase.basic(64.0F, 36.0F),
                            76.0F,
                            0.97F,
                            28.0F,
                            5.0F,
                            23.0F,
                            22.0F
                    )),
            List.of(
                    new RIAutomobileSeatRegistry.SeatPos(11.601/16,-8.322/16),
                    new RIAutomobileSeatRegistry.SeatPos(-11.601/16, -8.322/16)
            ),
            EntityDimensions.scalable(24.2214F/8, 25.97F/16),
            List.of(
                    new Vec3(-3.0, 0.0, 0.0),
                    new Vec3(-3.0, 0.0, 0.0)
            ),
            List.of(
                    new RIAutomobileFrame.Hitbox(new Vec3(0, 0, 24.2214F/16), 24.2214F/8, 25.97F/16, false),
                    new RIAutomobileFrame.Hitbox(new Vec3(0, 0, -24.2214F/16), 24.2214F/8, 25.97F/16, true)
            )
    );

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
                        new RIAutomobileSeatRegistry.SeatPos(6.0/16,0.0),
                        new RIAutomobileSeatRegistry.SeatPos(-6.0/16, 0.0)
                ),
                EntityDimensions.scalable(1.75F, 0.66F));
    }

    private static AutomobileFrame registerQuadMotorcar(String variant, float weight) {
        return register(new AutomobileFrame(
                        RIAutomobility.rl(variant + "_quadmotorcar"),
                        weight,
                        new AutomobileFrame.FrameModel(
                                RIAutomobility.rl("textures/entity/automobile/frame/" + variant + "_doublemotorcar.png"),
                                RIAutomobility.rl("frame_quadmotorcar"),
                                WheelBase.basic(49.0F, 24.0F),
                                36.0F,
                                3.0F,
                                26.0F,
                                2.0F,
                                23.0F,
                                22.0F
                        )
                ),
                List.of(
                        new RIAutomobileSeatRegistry.SeatPos(6.0/16,8.0/16),
                        new RIAutomobileSeatRegistry.SeatPos(-6.0/16,8.0/16),
                        new RIAutomobileSeatRegistry.SeatPos(6.0/16,-9.0/16),
                        new RIAutomobileSeatRegistry.SeatPos(-6.0/16,-9.0/16)
                ),
                EntityDimensions.scalable(1.75F, 0.66F));
    }

    private static AutomobileFrame register(AutomobileFrame frame, List<RIAutomobileSeatRegistry.SeatPos> seats, EntityDimensions dimensions) {
        RIAutomobileSeatRegistry.register(frame, seats);
        RIAutomobileEntityDimensionsRegistry.register(frame, dimensions);
        FRAMES.add(frame);
        return AutomobileFrame.REGISTRY.register(frame);
    }

    private static AutomobileFrame register(AutomobileFrame frame, List<RIAutomobileSeatRegistry.SeatPos> seats, EntityDimensions dimensions, List<Vec3> cameraPoses, List<RIAutomobileFrame.Hitbox> hitboxes) {
        RIAutomobileSeatRegistry.register(frame, seats);
        RIAutomobileEntityDimensionsRegistry.register(frame, dimensions);
        RIAutomobileCameraRegistry.register(frame, cameraPoses);
        RIAutomobileHitboxRegistry.register(frame, hitboxes);
        FRAMES.add(frame);
        return AutomobileFrame.REGISTRY.register(frame);
    }

    public static boolean isRIAutomobileFrame(AutomobileFrame frame){
        return FRAMES.contains(frame);
    }

    public static void init() {}

    public record Hitbox(Vec3 origin, float width, float height, boolean hasContainer) {
        public static final Hitbox DEFAULT = new Hitbox(Vec3.ZERO, 1.0f, 0.66f, false);
    }
}
