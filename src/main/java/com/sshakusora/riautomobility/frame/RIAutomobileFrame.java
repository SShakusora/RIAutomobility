package com.sshakusora.riautomobility.frame;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.definition.RIAutomobileDefinition;
import com.sshakusora.riautomobility.definition.RIAutomobileRegistry;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.WheelBase;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.List;

public final class RIAutomobileFrame {
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
    public static final AutomobileFrame LORRY = register(
            new AutomobileFrame(
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
                    )
            ),
            definition -> definition
                    .dimensions(EntityDimensions.scalable(21.7727F / 8, 53.3636F / 16))
                    .seats(
                            new RIAutomobileDefinition.SeatPos(9.7727 / 16, 40.3091 / 16),
                            new RIAutomobileDefinition.SeatPos(-9.7727 / 16, 40.3091 / 16)
                    )
                    .cameraPositions(
                            new Vec3(-7.0, 3.0, 0.0),
                            new Vec3(-7.0, 3.0, 0.0)
                    )
                    .hitboxes(
                            new RIAutomobileDefinition.Hitbox(new Vec3(0, 0.2, 35.9091 / 16), 21.7727F / 8, 53.3636F / 16, false),
                            new RIAutomobileDefinition.Hitbox(new Vec3(0, 0.2, 14.1364 / 16), 21.7727F / 8, 53.3636F / 16, false),
                            new RIAutomobileDefinition.Hitbox(new Vec3(0, 0.2, -28.5455 / 16), 21.7727F / 8, 53.3636F / 16, false),
                            new RIAutomobileDefinition.Hitbox(new Vec3(0, 0.2, -50.3182 / 16), 21.7727F / 8, 53.3636F / 16, true)
                    )
    );

    private RIAutomobileFrame() {}

    private static AutomobileFrame registerDoubleMotorcar(String variant, float weight) {
        return register(
                new AutomobileFrame(
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
                definition -> definition
                        .dimensions(EntityDimensions.scalable(1.75F, 0.66F))
                        .seats(
                                new RIAutomobileDefinition.SeatPos(6.0 / 16, 0.0),
                                new RIAutomobileDefinition.SeatPos(-6.0 / 16, 0.0)
                        )
        );
    }

    private static AutomobileFrame registerQuadMotorcar(String variant, float weight) {
        return register(
                new AutomobileFrame(
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
                definition -> definition
                        .dimensions(EntityDimensions.scalable(1.75F, 0.66F))
                        .seats(
                                new RIAutomobileDefinition.SeatPos(6.0 / 16, 8.0 / 16),
                                new RIAutomobileDefinition.SeatPos(-6.0 / 16, 8.0 / 16),
                                new RIAutomobileDefinition.SeatPos(6.0 / 16, -9.0 / 16),
                                new RIAutomobileDefinition.SeatPos(-6.0 / 16, -9.0 / 16)
                        )
        );
    }

    private static AutomobileFrame register(AutomobileFrame frame, Consumer<RIAutomobileDefinition.Builder> spec) {
        RIAutomobileDefinition.Builder builder = RIAutomobileDefinition.builder();
        spec.accept(builder);
        return RIAutomobileRegistry.register(frame, builder.build());
    }

    public static boolean isRIAutomobileFrame(AutomobileFrame frame) {
        return RIAutomobileRegistry.isRegistered(frame);
    }

    public static void init() {}
}
