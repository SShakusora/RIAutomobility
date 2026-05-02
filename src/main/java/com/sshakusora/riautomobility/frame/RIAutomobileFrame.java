package com.sshakusora.riautomobility.frame;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.definition.RIAutomobileDefinition;
import com.sshakusora.riautomobility.definition.RIAutomobileRegistry;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.WheelBase;
import io.github.foundationgames.automobility.automobile.attachment.RearAttachmentType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public final class RIAutomobileFrame {
    public static AutomobileFrame WOODEN_DOUBLEMOTORCAR;
    public static AutomobileFrame COPPER_DOUBLEMOTORCAR;
    public static AutomobileFrame STEEL_DOUBLEMOTORCAR;
    public static AutomobileFrame GOLDEN_DOUBLEMOTORCAR;
    public static AutomobileFrame BEJEWELED_DOUBLEMOTORCAR;
    public static AutomobileFrame WOODEN_QUADMOTORCAR;
    public static AutomobileFrame COPPER_QUADMOTORCAR;
    public static AutomobileFrame STEEL_QUADMOTORCAR;
    public static AutomobileFrame GOLDEN_QUADMOTORCAR;
    public static AutomobileFrame BEJEWELED_QUADMOTORCAR;
    public static AutomobileFrame LORRY;
    public static AutomobileFrame DMC12;
    public static AutomobileFrame STANDARD_FORMULA;

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
                        .rearAttachmentBlacklist(RearAttachmentType.PASSENGER_SEAT)
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
                                31.0F,
                                30.5F
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
                        .rearAttachmentBlacklist(RearAttachmentType.PASSENGER_SEAT)
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

    public static void init() {
        reload();
    }

    public static void reload() {
        WOODEN_DOUBLEMOTORCAR = registerDoubleMotorcar("wooden", 0.6F);
        COPPER_DOUBLEMOTORCAR = registerDoubleMotorcar("copper", 0.8F);
        STEEL_DOUBLEMOTORCAR = registerDoubleMotorcar("steel", 0.95F);
        GOLDEN_DOUBLEMOTORCAR = registerDoubleMotorcar("golden", 1.05F);
        BEJEWELED_DOUBLEMOTORCAR = registerDoubleMotorcar("bejeweled", 1.11F);
        WOODEN_QUADMOTORCAR = registerQuadMotorcar("wooden", 1.2F);
        COPPER_QUADMOTORCAR = registerQuadMotorcar("copper", 1.6F);
        STEEL_QUADMOTORCAR = registerQuadMotorcar("steel", 1.9F);
        GOLDEN_QUADMOTORCAR = registerQuadMotorcar("golden", 2.1F);
        BEJEWELED_QUADMOTORCAR = registerQuadMotorcar("bejeweled", 2.22F);
        LORRY = createLorry();
        DMC12 = createDmc12();
        STANDARD_FORMULA = createStandardFormula();
    }

    private static AutomobileFrame createLorry() {
        return register(
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
                        .hideEngine(true)
                        .frontAttachmentEnabled(false)
                        .rearAttachmentEnabled(false)
        );
    }

    private static AutomobileFrame createDmc12() {
        return register(
                new AutomobileFrame(
                        RIAutomobility.rl("dmc12"),
                        0.98F,
                        new AutomobileFrame.FrameModel(
                                RIAutomobility.rl("textures/entity/automobile/frame/dmc12.png"),
                                RIAutomobility.rl("frame_dmc12"),
                                new WheelBase(
                                        new WheelBase.WheelPos(31.2F, 18, 1, 180, WheelBase.WheelEnd.FRONT, WheelBase.WheelSide.RIGHT),
                                        new WheelBase.WheelPos(31.2F, -18, 1, 0, WheelBase.WheelEnd.FRONT, WheelBase.WheelSide.LEFT),
                                        new WheelBase.WheelPos(-33F, 18, 1, 180, WheelBase.WheelEnd.BACK, WheelBase.WheelSide.RIGHT),
                                        new WheelBase.WheelPos(-33F, -18, 1, 0, WheelBase.WheelEnd.BACK, WheelBase.WheelSide.LEFT)
                                ),
                                74.0F,
                                2.97F,
                                32.50F,
                                1.10F,
                                23.00F,
                                22.00F
                        )
                ),
                definition -> definition
                        .dimensions(EntityDimensions.scalable(24.2214F / 8, 25.97F / 16F))
                        .seats(
                                new RIAutomobileDefinition.SeatPos(11.601 / 16, -8.322 / 16),
                                new RIAutomobileDefinition.SeatPos(-11.601 / 16, -8.322 / 16)
                        )
                        .cameraPositions(
                                new Vec3(-3.0, 0.0, 0.0),
                                new Vec3(-3.0, 0.0, 0.0)
                        )
                        .hitboxes(
                                new RIAutomobileDefinition.Hitbox(new Vec3(0, 0, 34.2214F / 16), 24.2214F / 8, 22.97F / 16, false),
                                new RIAutomobileDefinition.Hitbox(new Vec3(0, 0, -34.2214F / 16), 24.2214F / 8, 28.97F / 16, true)
                        )
                        .hideEngine(true)
                        .frontAttachmentEnabled(false)
                        .rearAttachmentEnabled(false)
        );
    }

    private static AutomobileFrame createStandardFormula() {
        return register(
                new AutomobileFrame(
                        RIAutomobility.rl("standard_formula"),
                        0.98F,
                        new AutomobileFrame.FrameModel(
                                RIAutomobility.rl("textures/entity/automobile/frame/standard_formula.png"),
                                RIAutomobility.rl("frame_standard_formula"),
                                WheelBase.basic(84F, 31.5F),
                                76F,
                                -1.36F,
                                32.5F,
                                3.5F,
                                23F,
                                22F
                        )
                ),
                definition -> definition
                        .dimensions(EntityDimensions.scalable(2.8F, 1.61F))
                        .seats(
                                new RIAutomobileDefinition.SeatPos(0, 1.6 / 16)
                        )
                        .cameraPositions(
                                new Vec3(-3, 0, 0)
                        )
                        .hitboxes(
                                new RIAutomobileDefinition.Hitbox(new Vec3(0, 0, -44.8 / 16), 2.5F, 1.5F, false),
                                new RIAutomobileDefinition.Hitbox(new Vec3(0, 0, -22.4 / 16), 2.8F, 1.61F, false),
                                new RIAutomobileDefinition.Hitbox(new Vec3(0, 0, 22.4 / 16), 2.8F, 1.4F, false),
                                new RIAutomobileDefinition.Hitbox(new Vec3(0, 0, 44.8 / 16), 2.8F, 1F, false)
                        )
                        .hideEngine(true)
                        .frontAttachmentEnabled(false)
                        .rearAttachmentEnabled(false)
        );
    }
}
