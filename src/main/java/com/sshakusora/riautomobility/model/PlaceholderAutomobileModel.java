package com.sshakusora.riautomobility.model;

import io.github.foundationgames.automobility.automobile.render.BaseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class PlaceholderAutomobileModel extends BaseModel {
    public static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/item/barrier.png");

    public PlaceholderAutomobileModel(EntityRendererProvider.Context ctx, ModelLayerLocation layer) {
        super(texture -> RenderType.entityCutout(TEXTURE), ctx, layer);
    }
}
