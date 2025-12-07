package com.sshakusora.riautomobility.model.frame;

import io.github.foundationgames.automobility.Automobility;
import io.github.foundationgames.automobility.automobile.render.BaseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class DoubleMotorcarFrameModel extends BaseModel {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(Automobility.rl("automobile/frame/doublemotorcar"), "main");

    public DoubleMotorcarFrameModel(EntityRendererProvider.Context ctx) {
        super(RenderType::entityTranslucentCull, ctx, MODEL_LAYER);
    }
}
