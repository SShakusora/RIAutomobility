package com.sshakusora.riautomobility.editor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.editor.VehicleImportGuiAtlas;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;

final class VehicleGuiTextures {
    private static final ResourceLocation TABLE_BACKGROUND = new ResourceLocation(
            RIAutomobility.MODID, "textures/gui/vehicle_import_table.png");
    private static final ResourceLocation SELECTION_BACKGROUND = new ResourceLocation(
            RIAutomobility.MODID, "textures/gui/vehicle_import_selection.png");
    private static final int TABLE_BACKGROUND_WIDTH = 512;
    private static final int TABLE_BACKGROUND_HEIGHT = 300;
    private static final ResourceLocation FINAL_TEXTURE = new ResourceLocation(
            RIAutomobility.MODID, "textures/gui/vehicle_import.png");
    private static final ResourceLocation TEMPLATE_TEXTURE = new ResourceLocation(
            RIAutomobility.MODID, "textures/gui/vehicle_import_template.png");
    private static ResourceManager cachedBackgroundManager;
    private static boolean tableBackgroundAvailable;
    private static boolean selectionBackgroundAvailable;
    private static ResourceManager cachedManager;
    private static ResourceLocation cachedTexture = TEMPLATE_TEXTURE;
    private static long nextTextureCheck;

    private VehicleGuiTextures() {
    }

    static boolean blitTableBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        refreshBackgroundAvailability();
        if (!tableBackgroundAvailable) return false;
        graphics.blit(TABLE_BACKGROUND, x, y, width, height, 0, 0,
                TABLE_BACKGROUND_WIDTH, TABLE_BACKGROUND_HEIGHT,
                TABLE_BACKGROUND_WIDTH, TABLE_BACKGROUND_HEIGHT);
        return true;
    }

    static boolean blitSelectionBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        refreshBackgroundAvailability();
        if (!selectionBackgroundAvailable) return false;
        graphics.blit(SELECTION_BACKGROUND, x, y, width, height, 0, 0,
                TABLE_BACKGROUND_WIDTH, TABLE_BACKGROUND_HEIGHT,
                TABLE_BACKGROUND_WIDTH, TABLE_BACKGROUND_HEIGHT);
        return true;
    }

    private static void refreshBackgroundAvailability() {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (manager == cachedBackgroundManager) return;
        cachedBackgroundManager = manager;
        tableBackgroundAvailable = manager.getResource(TABLE_BACKGROUND).isPresent();
        selectionBackgroundAvailable = manager.getResource(SELECTION_BACKGROUND).isPresent();
    }

    static void blit(GuiGraphics graphics, VehicleImportGuiAtlas.Sprite sprite,
                     int x, int y, int width, int height) {
        graphics.blit(texture(), x, y, width, height, sprite.u(), sprite.v(),
                sprite.width(), sprite.height(), VehicleImportGuiAtlas.SIZE, VehicleImportGuiAtlas.SIZE);
    }

    static void blitNineSliced(GuiGraphics graphics, VehicleImportGuiAtlas.Sprite sprite,
                               int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;

        int left = Math.min(sprite.border(), width / 2);
        int right = left;
        int top = Math.min(sprite.border(), height / 2);
        int bottom = top;

        int x0 = x;
        int x1 = x + left;
        int x2 = x + width - right;
        int x3 = x + width;
        int y0 = y;
        int y1 = y + top;
        int y2 = y + height - bottom;
        int y3 = y + height;

        float inverseAtlasSize = 1.0F / VehicleImportGuiAtlas.SIZE;
        float u0 = sprite.u() * inverseAtlasSize;
        float u1 = (sprite.u() + left) * inverseAtlasSize;
        float u2 = (sprite.u() + sprite.width() - right) * inverseAtlasSize;
        float u3 = (sprite.u() + sprite.width()) * inverseAtlasSize;
        float v0 = sprite.v() * inverseAtlasSize;
        float v1 = (sprite.v() + top) * inverseAtlasSize;
        float v2 = (sprite.v() + sprite.height() - bottom) * inverseAtlasSize;
        float v3 = (sprite.v() + sprite.height()) * inverseAtlasSize;

        RenderSystem.setShaderTexture(0, texture());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        quad(buffer, matrix, x0, y0, x1, y1, u0, v0, u1, v1);
        quad(buffer, matrix, x1, y0, x2, y1, u1, v0, u2, v1);
        quad(buffer, matrix, x2, y0, x3, y1, u2, v0, u3, v1);
        quad(buffer, matrix, x0, y1, x1, y2, u0, v1, u1, v2);
        quad(buffer, matrix, x1, y1, x2, y2, u1, v1, u2, v2);
        quad(buffer, matrix, x2, y1, x3, y2, u2, v1, u3, v2);
        quad(buffer, matrix, x0, y2, x1, y3, u0, v2, u1, v3);
        quad(buffer, matrix, x1, y2, x2, y3, u1, v2, u2, v3);
        quad(buffer, matrix, x2, y2, x3, y3, u2, v2, u3, v3);

        BufferUploader.drawWithShader(buffer.end());
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix,
                             int x0, int y0, int x1, int y1,
                             float u0, float v0, float u1, float v1) {
        if (x1 <= x0 || y1 <= y0) return;
        buffer.vertex(matrix, x0, y0, 0).uv(u0, v0).endVertex();
        buffer.vertex(matrix, x0, y1, 0).uv(u0, v1).endVertex();
        buffer.vertex(matrix, x1, y1, 0).uv(u1, v1).endVertex();
        buffer.vertex(matrix, x1, y0, 0).uv(u1, v0).endVertex();
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
