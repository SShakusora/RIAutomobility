package com.sshakusora.riautomobility.model;

import io.github.foundationgames.automobility.block.model.GeometryBuilder;
import io.github.foundationgames.automobility.block.model.SlopeUnbakedModel;
import io.github.foundationgames.automobility.forge.block.render.ForgeSlopeBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class RIAForgeSlopeBakedModel extends ForgeSlopeBakedModel {
    public RIAForgeSlopeBakedModel(TextureAtlasSprite frame,
                                  Map<BlockState, TextureAtlasSprite> frameTexOverrides,
                                  @Nullable TextureAtlasSprite plateInner,
                                  @Nullable TextureAtlasSprite plateOuter,
                                  ModelState settings,
                                  SlopeUnbakedModel.Type type) {
        super(frame, frameTexOverrides, plateInner, plateOuter, settings, type);
    }

    @Override
    public void buildSlopeGeometry(@Nullable TextureAtlasSprite sprite, GeometryBuilder geometry,
                                   int frameColor, boolean borderedLeft, boolean borderedRight) {
        TextureAtlasSprite resolvedSprite = sprite != null ? sprite : getFrameSprite(null, null);
        if (resolvedSprite == null) {
            return;
        }

        super.buildSlopeGeometry(resolvedSprite, geometry, frameColor, borderedLeft, borderedRight);
    }
}
