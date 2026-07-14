package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.editor.VehicleImportGuiAtlas;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

final class VehicleGuiTextures {
    private static final ResourceLocation FINAL_TEXTURE = new ResourceLocation(
            RIAutomobility.MODID, "textures/gui/vehicle_import.png");
    private static final ResourceLocation TEMPLATE_TEXTURE = new ResourceLocation(
            RIAutomobility.MODID, "textures/gui/vehicle_import_template.png");
    private static ResourceManager cachedManager;
    private static ResourceLocation cachedTexture = TEMPLATE_TEXTURE;
    private static long nextTextureCheck;

    private VehicleGuiTextures() {
    }

    static void blit(GuiGraphics graphics, VehicleImportGuiAtlas.Sprite sprite,
                     int x, int y, int width, int height) {
        graphics.blit(texture(), x, y, width, height, sprite.u(), sprite.v(),
                sprite.width(), sprite.height(), VehicleImportGuiAtlas.SIZE, VehicleImportGuiAtlas.SIZE);
    }

    static void blitNineSliced(GuiGraphics graphics, VehicleImportGuiAtlas.Sprite sprite,
                               int x, int y, int width, int height) {
        graphics.blitNineSliced(texture(), x, y, width, height,
                sprite.border(), sprite.border(), sprite.width(), sprite.height(), sprite.u(), sprite.v());
    }

    private static ResourceLocation texture() {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        long now = Util.getMillis();
        if (manager != cachedManager || now >= nextTextureCheck) {
            cachedManager = manager;
            cachedTexture = manager.getResource(FINAL_TEXTURE).isPresent() ? FINAL_TEXTURE : TEMPLATE_TEXTURE;
            nextTextureCheck = now + 1000L;
        }
        return cachedTexture;
    }
}
