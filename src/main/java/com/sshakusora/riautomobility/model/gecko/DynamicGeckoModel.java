package com.sshakusora.riautomobility.model.gecko;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DynamicGeckoModel extends GeoModel<DynamicGeckoAnimatable> {
    private final ResourceLocation geoModel;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public DynamicGeckoModel(ResourceLocation geoModel, ResourceLocation texture, ResourceLocation animation) {
        this.geoModel = geoModel;
        this.texture = texture;
        this.animation = animation;
    }

    @Override
    public ResourceLocation getModelResource(DynamicGeckoAnimatable animatable) {
        return this.geoModel;
    }

    @Override
    public ResourceLocation getTextureResource(DynamicGeckoAnimatable animatable) {
        return this.texture;
    }

    @Override
    public ResourceLocation getAnimationResource(DynamicGeckoAnimatable animatable) {
        return this.animation;
    }
}
