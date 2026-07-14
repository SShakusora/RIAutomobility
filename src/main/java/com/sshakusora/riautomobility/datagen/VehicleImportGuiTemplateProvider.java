package com.sshakusora.riautomobility.datagen;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.editor.VehicleImportGuiAtlas;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class VehicleImportGuiTemplateProvider implements DataProvider {
    public static final String ART_OUTPUT_PROPERTY = "riautomobility.guiArtOutput";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String TEXTURE_META = "{\n  \"texture\": {\n    \"blur\": false,\n    \"clamp\": true\n  }\n}\n";
    private final Path runtimeOutput;
    private final Path artOutput;

    public VehicleImportGuiTemplateProvider(PackOutput output) {
        runtimeOutput = output.getOutputFolder().resolve(
                "assets/" + RIAutomobility.MODID + "/textures/gui");
        String configuredArtOutput = System.getProperty(ART_OUTPUT_PROPERTY);
        artOutput = configuredArtOutput == null || configuredArtOutput.isBlank()
                ? output.getOutputFolder().resolve("../../art/vehicle_import_gui").normalize()
                : Path.of(configuredArtOutput);
    }

    @Override public CompletableFuture<?> run(CachedOutput cachedOutput) {
        return CompletableFuture.runAsync(() -> {
            try {
                BufferedImage template = createTemplate();
                byte[] templatePng = png(template);
                write(cachedOutput, runtimeOutput.resolve("vehicle_import_template.png"), templatePng);
                write(cachedOutput, runtimeOutput.resolve("vehicle_import_template.png.mcmeta"),
                        TEXTURE_META.getBytes(StandardCharsets.UTF_8));

                write(cachedOutput, artOutput.resolve("vehicle_import_template.png"), templatePng);
                write(cachedOutput, artOutput.resolve("vehicle_import_guide.png"), png(createGuide(template)));
                write(cachedOutput, artOutput.resolve("vehicle_import_atlas.json"),
                        (GSON.toJson(createManifest()) + "\n").getBytes(StandardCharsets.UTF_8));
                write(cachedOutput, artOutput.resolve("vehicle_import.png.mcmeta"),
                        TEXTURE_META.getBytes(StandardCharsets.UTF_8));
                write(cachedOutput, artOutput.resolve("README.md"),
                        readme().getBytes(StandardCharsets.UTF_8));
            } catch (IOException exception) {
                throw new CompletionException(exception);
            }
        }, Util.backgroundExecutor());
    }

    @Override public String getName() {
        return "Vehicle Import GUI artist template";
    }

    private static BufferedImage createTemplate() {
        BufferedImage image = new BufferedImage(
                VehicleImportGuiAtlas.SIZE, VehicleImportGuiAtlas.SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setComposite(java.awt.AlphaComposite.Src);

        panel(graphics, VehicleImportGuiAtlas.Sprite.SCREEN, 0xF0181B20);
        panel(graphics, VehicleImportGuiAtlas.Sprite.SIDEBAR, 0xFF262A31);
        panel(graphics, VehicleImportGuiAtlas.Sprite.CONTROLS, 0xFF20242A);
        panel(graphics, VehicleImportGuiAtlas.Sprite.PREVIEW, 0xFF0E1115);
        panel(graphics, VehicleImportGuiAtlas.Sprite.SELECTION, 0xFF22262C);
        panel(graphics, VehicleImportGuiAtlas.Sprite.INVENTORY, 0xFF20242A);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.ATTACHMENT_LIST, 0xFF5B626D, 0xFF11151A, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.DROPDOWN, 0xFF69717C, 0xFF15191E, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.ROW_NORMAL, 0xFF454C55, 0xFF20252B, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.ROW_HOVERED, 0xFF69717C, 0xFF353B43, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.ROW_SELECTED, 0xFF62C778, 0xFF34754D, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.ROW_SELECTED_HOVERED, 0xFF8AE09B, 0xFF4B9C68, 1);

        bordered(graphics, VehicleImportGuiAtlas.Sprite.BUTTON_NORMAL, 0xFF737A84, 0xFF30343A, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.BUTTON_HOVERED, 0xFFAEB7C2, 0xFF454B53, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.BUTTON_DISABLED, 0xFF5A6068, 0xFF25282D, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.BUTTON_FINE, 0xFFA7D46F, 0xFF4C5D35, 1);

        bordered(graphics, VehicleImportGuiAtlas.Sprite.INPUT_NORMAL, 0xFFA0A0A0, 0xFF000000, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.INPUT_FOCUSED, 0xFFFFFFFF, 0xFF000000, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.INPUT_DISABLED, 0xFF55585E, 0xFF17191D, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.SLOT_NORMAL, 0xFF5B626D, 0xFF11151A, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.SLOT_OUTPUT, 0xFF91B568, 0xFF11151A, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.ICON_NORMAL, 0xFF5A6068, 0xFF30343A, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.ICON_HOVERED, 0xFFAEB7C2, 0xFF454B53, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.ICON_SELECTED, 0xFF62C778, 0xFF30343A, 1);
        bordered(graphics, VehicleImportGuiAtlas.Sprite.ICON_DISABLED, 0xFF454A51, 0xFF25282D, 1);

        toggle(graphics, VehicleImportGuiAtlas.Sprite.TOGGLE_OFF, false, false);
        toggle(graphics, VehicleImportGuiAtlas.Sprite.TOGGLE_ON, true, false);
        toggle(graphics, VehicleImportGuiAtlas.Sprite.TOGGLE_DISABLED, false, true);
        panel(graphics, VehicleImportGuiAtlas.Sprite.SCROLL_TRACK_VERTICAL, 0xFF30353B);
        panel(graphics, VehicleImportGuiAtlas.Sprite.SCROLL_THUMB_VERTICAL, 0xFFAEB7C2);
        panel(graphics, VehicleImportGuiAtlas.Sprite.SCROLL_TRACK_HORIZONTAL, 0xFF30353B);
        panel(graphics, VehicleImportGuiAtlas.Sprite.SCROLL_THUMB_HORIZONTAL, 0xFFAEB7C2);
        graphics.dispose();
        return image;
    }

    private static BufferedImage createGuide(BufferedImage template) {
        BufferedImage guide = new BufferedImage(1280, 840, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = guide.createGraphics();
        graphics.setColor(new Color(0xFF181B20, true));
        graphics.fillRect(0, 0, guide.getWidth(), guide.getHeight());
        graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        graphics.setColor(Color.WHITE);
        graphics.drawString("Vehicle Import GUI Atlas - 256x256 logical pixels", 20, 24);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(template, 20, 40, 768, 768, null);

        graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        int line = 52;
        for (VehicleImportGuiAtlas.Sprite sprite : VehicleImportGuiAtlas.Sprite.values()) {
            graphics.setColor(new Color(0xE6E9ED));
            graphics.drawString(String.format("%-24s u=%3d v=%3d %2dx%-2d border=%d",
                    sprite.name(), sprite.u(), sprite.v(), sprite.width(), sprite.height(), sprite.border()),
                    805, line);
            line += 17;
        }
        graphics.dispose();
        return guide;
    }

    private static JsonObject createManifest() {
        JsonObject root = new JsonObject();
        root.addProperty("texture_width", VehicleImportGuiAtlas.SIZE);
        root.addProperty("texture_height", VehicleImportGuiAtlas.SIZE);
        root.addProperty("runtime_path", "assets/riautomobility/textures/gui/vehicle_import.png");
        JsonArray sprites = new JsonArray();
        for (VehicleImportGuiAtlas.Sprite sprite : VehicleImportGuiAtlas.Sprite.values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", sprite.name());
            entry.addProperty("u", sprite.u());
            entry.addProperty("v", sprite.v());
            entry.addProperty("width", sprite.width());
            entry.addProperty("height", sprite.height());
            entry.addProperty("border", sprite.border());
            sprites.add(entry);
        }
        root.add("sprites", sprites);
        return root;
    }

    private static String readme() {
        return """
                # 车辆导入台 GUI 美术交付包

                请以 1:1 逻辑像素绘制。图集固定为 256x256，不要缩放画布、移动区域或改变任何 Sprite 的尺寸。
                可以使用透明像素；未使用区域建议保留至少 1 像素安全间距。文字、物品图标、滚动位置与 3D 车辆预览均为动态内容，不应画入图集。

                ## 文件

                - `vehicle_import_template.png`：按照当前界面颜色生成的可编辑模板。
                - `vehicle_import_guide.png`：放大后的区域、名称、UV、尺寸及九宫格边距参考图。
                - `vehicle_import_atlas.json`：与代码一致的机器可读坐标表。
                - `vehicle_import.png.mcmeta`：关闭模糊采样并启用边缘约束。

                ## Sprite 分组

                - `SCREEN/SIDEBAR/CONTROLS/PREVIEW/SELECTION/INVENTORY`：页面与各功能区背景。
                - `BUTTON_*`：按钮普通、悬浮、禁用及 Shift 精细调整状态。
                - `INPUT_*`：输入框普通、聚焦及禁用状态。
                - `SLOT_*`：普通背包槽与输出槽。
                - `ICON_*`：部件选择图标的普通、悬浮、选中及禁用状态。
                - `TOGGLE_*`：开关关闭、开启及禁用状态。
                - `DROPDOWN/ROW_*`：下拉目录背景与目录项状态。
                - `SCROLL_*`：横向、纵向滚动条轨道和滑块。

                `border` 大于 0 的 Sprite 会采用九宫格拉伸。四角和边框应在指定边距内完成，中间区域允许平铺；`border` 为 0 的 Sprite 会整体缩放到控件尺寸。

                ## 交付与应用

                1. 将完成的图集保存为 `vehicle_import.png`。
                2. 放入 `src/main/resources/assets/riautomobility/textures/gui/vehicle_import.png`。
                3. 将 `vehicle_import.png.mcmeta` 放在同一目录。
                4. 在开发客户端按 `F3+T` 重新加载资源。

                最终稿不存在时，界面会自动使用 `runData` 生成的 `vehicle_import_template.png`；最终稿出现后会自动优先使用最终稿。
                """;
    }

    private static void panel(Graphics2D graphics, VehicleImportGuiAtlas.Sprite sprite, int color) {
        graphics.setColor(new Color(color, true));
        graphics.fillRect(sprite.u(), sprite.v(), sprite.width(), sprite.height());
    }

    private static void bordered(Graphics2D graphics, VehicleImportGuiAtlas.Sprite sprite,
                                 int borderColor, int backgroundColor, int border) {
        graphics.setColor(new Color(borderColor, true));
        graphics.fillRect(sprite.u(), sprite.v(), sprite.width(), sprite.height());
        graphics.setColor(new Color(backgroundColor, true));
        graphics.fillRect(sprite.u() + border, sprite.v() + border,
                sprite.width() - border * 2, sprite.height() - border * 2);
    }

    private static void toggle(Graphics2D graphics, VehicleImportGuiAtlas.Sprite sprite,
                               boolean enabled, boolean disabled) {
        bordered(graphics, sprite, disabled ? 0xFF555A61 : 0xFF737A84,
                enabled ? 0xFF3E8E5A : 0xFF30343A, 1);
        int knobSize = sprite.height() - 4;
        int knobX = enabled ? sprite.u() + sprite.width() - knobSize - 2 : sprite.u() + 2;
        graphics.setColor(new Color(disabled ? 0xFF777B80 : 0xFFF1F3F5, true));
        graphics.fillRect(knobX, sprite.v() + 2, knobSize, knobSize);
    }

    private static byte[] png(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "PNG", output)) throw new IOException("PNG writer is unavailable");
        return output.toByteArray();
    }

    private static void write(CachedOutput output, Path path, byte[] bytes) throws IOException {
        output.writeIfNeeded(path, bytes, Hashing.sha1().hashBytes(bytes));
    }
}
