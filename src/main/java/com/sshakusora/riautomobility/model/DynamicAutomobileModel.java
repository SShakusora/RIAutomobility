package com.sshakusora.riautomobility.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.foundationgames.automobility.automobile.render.BaseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class DynamicAutomobileModel extends BaseModel {
    private final float rotationY;

    public DynamicAutomobileModel(EntityRendererProvider.Context ctx, ModelLayerLocation layer, String renderType, float rotationY) {
        super(renderType(renderType), ctx, layer);
        this.rotationY = rotationY;
    }

    @Override
    protected void prepare(PoseStack matrices) {
        if (this.rotationY != 0.0F) {
            matrices.mulPose(Axis.YP.rotationDegrees(this.rotationY));
        }
    }

    private static Function<ResourceLocation, RenderType> renderType(String name) {
        return switch (name) {
            case "entity_cutout_no_cull" -> RenderType::entityCutoutNoCull;
            case "entity_translucent" -> RenderType::entityTranslucent;
            case "entity_translucent_cull" -> RenderType::entityTranslucentCull;
            case "entity_solid" -> RenderType::entitySolid;
            default -> RenderType::entityCutout;
        };
    }
}
