package com.sshakusora.riautomobility.editor.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import com.sshakusora.riautomobility.carpack.CarPackManager;
import com.sshakusora.riautomobility.editor.VehicleImportMenu;
import com.sshakusora.riautomobility.model.bbmodel.BbRenderContext;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.ExportVehicleComponentItemPacket;
import com.sshakusora.riautomobility.network.packet.client.ClientCarPackUploader;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import io.github.foundationgames.automobility.automobile.render.AutomobileModels;
import io.github.foundationgames.automobility.automobile.render.AutomobileRenderer;
import io.github.foundationgames.automobility.item.AutomobilityItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class VehicleImportScreen extends AbstractContainerScreen<VehicleImportMenu> {
    private static final int TAB_WIDTH = 62;
    private static final int PARAM_WIDTH = 226;
    private static final int GUI_HEIGHT = 320;
    private static final int ITEM_PREVIEW_SIZE = 18;
    private static final int CONTROL_ROW_STEP = 24;
    private static final int NUMBER_ARROW_WIDTH = 18;
    private static final float FINE_STEP_SCALE = 0.1F;
    private static final int SELECTIONS_PER_PAGE = 12;
    private final VehicleEditorDraft draft;
    private final PreviewAutomobile preview;
    private final ClientVehiclePreviewSession previewSession = new ClientVehiclePreviewSession();
    private final List<AutomobileFrame> frames = new ArrayList<>();
    private final List<AutomobileWheel> wheels = new ArrayList<>();
    private final List<AutomobileEngine> engines = new ArrayList<>();
    private final List<ComponentEntryButton> selectionButtons = new ArrayList<>();
    private final List<FieldLabel> labels = new ArrayList<>();
    private final List<NumberControl> numberControls = new ArrayList<>();
    private PlayerModel<AbstractClientPlayer> seatPlayerModel;
    private Button exportItemButton;
    private Page page = Page.FRAME;
    private FrameTab frameTab = FrameTab.BASIC;
    private SelectionType selectionType;
    private int selectionPage;
    private int wheelPointIndex;
    private int seatIndex;
    private int hitboxIndex;
    private boolean seatFirstPerson;
    private float firstPersonYaw;
    private float firstPersonPitch;
    private float firstPersonFov = 70.0F;
    private float rotationX = 18;
    private float rotationY = 35;
    private float zoom = 38;
    private float panX;
    private float panY;
    private int dragButton = -1;
    private double lastMouseX;
    private double lastMouseY;
    private boolean exportingItem;
    private String status = "";

    public VehicleImportScreen(VehicleImportMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        AutomobileFrame.REGISTRY.forEach(v -> { if (!v.isEmpty() && !frames.contains(v)) frames.add(v); });
        AutomobileWheel.REGISTRY.forEach(v -> { if (!v.isEmpty() && !wheels.contains(v)) wheels.add(v); });
        AutomobileEngine.REGISTRY.forEach(v -> { if (!v.isEmpty() && !engines.contains(v)) engines.add(v); });
        if (frames.isEmpty() || wheels.isEmpty() || engines.isEmpty()) throw new IllegalStateException("Automobility component registries are empty");
        draft = new VehicleEditorDraft(frames.get(0), wheels.get(0), engines.get(0));
        preview = new PreviewAutomobile(draft);
        imageWidth = 620;
        imageHeight = GUI_HEIGHT;
    }

    @Override protected void init() {
        imageWidth = Math.max(420, Math.min(620, width - 16));
        imageHeight = GUI_HEIGHT;
        super.init();
        labels.clear(); selectionButtons.clear(); numberControls.clear();
        if (selectionType != null) { addSelectionControls(); return; }
        int x = leftPos + 5;
        int y = topPos + 24;
        for (Page value : Page.values()) {
            Button tab = Button.builder(Component.literal(value.label), b -> { page = value; setTargetForPage(); resetWidgets(); })
                    .bounds(x, y, TAB_WIDTH - 10, 32).build();
            tab.active = page != value;
            addRenderableWidget(tab);
            y += 36;
        }
        int panelX = leftPos + TAB_WIDTH + 8;
        int panelY = topPos + 25;
        switch (page) {
            case FRAME -> addFrameControls(panelX, panelY);
            case WHEEL -> addWheelControls(panelX, panelY);
            case ENGINE -> addEngineControls(panelX, panelY);
        }
        addExportItemControl();
        int px = previewX0() + 6;
        addRenderableWidget(Button.builder(Component.literal("重置视图"), b -> resetView()).bounds(px, topPos + 26, 72, 18).build());
    }

    private void setTargetForPage() {
        draft.target = page.target;
        if (page == Page.FRAME) showFrameTabParts();
    }

    private void showFrameTabParts() {
        draft.showPart(VehicleEditorDraft.Target.FRAME);
        if (frameTab == FrameTab.WHEELS) draft.showPart(VehicleEditorDraft.Target.WHEEL);
    }

    private int addIdentity(int x, int y) {
        labeledText(x, y, "名字：", draft.displayName, v -> draft.displayName = v); y += CONTROL_ROW_STEP;
        return y;
    }

    private void addFrameControls(int x, int y) {
        int gap = 2;
        int tabWidth = (PARAM_WIDTH - gap * (FrameTab.values().length - 1)) / FrameTab.values().length;
        int tabX = x;
        for (FrameTab value : FrameTab.values()) {
            int width = value == FrameTab.values()[FrameTab.values().length - 1] ? x + PARAM_WIDTH - tabX : tabWidth;
            Button tab = Button.builder(Component.literal(value.label), b -> {
                frameTab = value;
                draft.target = VehicleEditorDraft.Target.FRAME;
                showFrameTabParts();
                resetWidgets();
            }).bounds(tabX, y, width, 20).build();
            tab.active = frameTab != value;
            addRenderableWidget(tab);
            tabX += width + gap;
        }
        y += CONTROL_ROW_STEP;
        switch (frameTab) {
            case BASIC -> addFrameBasicControls(x, y);
            case WHEELS -> addFrameWheelControls(x, y);
            case SEATS -> addSeatControls(x, y);
            case HITBOXES -> addHitboxControls(x, y);
        }
    }

    private void addFrameBasicControls(int x, int y) {
        y = addIdentity(x, y);
        labeledNumber(x, y, "重量：", 0.05F, () -> draft.weight, v -> draft.weight = v); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "物品大小：", PARAM_WIDTH - 70 - ITEM_PREVIEW_SIZE - 8,
                1.0F, () -> draft.lengthPx, v -> draft.lengthPx = v); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "引擎后移：", 1.0F, () -> draft.enginePosBack, v -> draft.enginePosBack = v); y += CONTROL_ROW_STEP;
        addComponentButtons(x, y, SelectionType.FRAME);
    }

    private void addFrameWheelControls(int x, int y) {
        if (draft.wheelPoints.isEmpty()) {
            draft.wheelPoints.add(new VehicleEditorDraft.WheelPoint(0.0F, 0.0F, 1.0F, 0.0F, "front", "left"));
        }
        wheelPointIndex = Math.max(0, Math.min(wheelPointIndex, draft.wheelPoints.size() - 1));
        addListHeader(x, y, "轮位", wheelPointIndex, draft.wheelPoints.size(),
                () -> wheelPointIndex = (wheelPointIndex + 1) % draft.wheelPoints.size(),
                () -> {
                    VehicleEditorDraft.WheelPoint current = currentWheelPoint();
                    draft.wheelPoints.add(current.mirrored());
                    wheelPointIndex = draft.wheelPoints.size() - 1;
                },
                () -> {
                    if (draft.wheelPoints.size() > 1) draft.wheelPoints.remove(wheelPointIndex);
                    wheelPointIndex = Math.max(0, Math.min(wheelPointIndex, draft.wheelPoints.size() - 1));
                });
        y += 28;
        labeledNumber(x, y, "前后：", 1.0F, () -> currentWheelPoint().forward(), v -> replaceWheelPoint(new VehicleEditorDraft.WheelPoint(
                v, currentWheelPoint().right(), currentWheelPoint().scale(), currentWheelPoint().yaw(), currentWheelPoint().end(), currentWheelPoint().side()))); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "左右：", 1.0F, () -> currentWheelPoint().right(), v -> replaceWheelPoint(new VehicleEditorDraft.WheelPoint(
                currentWheelPoint().forward(), v, currentWheelPoint().scale(), currentWheelPoint().yaw(), currentWheelPoint().end(), currentWheelPoint().side()))); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "缩放：", 0.05F, () -> currentWheelPoint().scale(), v -> replaceWheelPoint(new VehicleEditorDraft.WheelPoint(
                currentWheelPoint().forward(), currentWheelPoint().right(), v, currentWheelPoint().yaw(), currentWheelPoint().end(), currentWheelPoint().side()))); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "偏航角：", 5.0F, () -> currentWheelPoint().yaw(), v -> replaceWheelPoint(new VehicleEditorDraft.WheelPoint(
                currentWheelPoint().forward(), currentWheelPoint().right(), currentWheelPoint().scale(), v, currentWheelPoint().end(), currentWheelPoint().side()))); y += CONTROL_ROW_STEP;
        int half = (PARAM_WIDTH - 4) / 2;
        addRenderableWidget(Button.builder(wheelEndLabel(), b -> {
            VehicleEditorDraft.WheelPoint p = currentWheelPoint();
            replaceWheelPoint(new VehicleEditorDraft.WheelPoint(p.forward(), p.right(), p.scale(), p.yaw(),
                    p.end().equals("front") ? "back" : "front", p.side()));
            b.setMessage(wheelEndLabel());
        }).bounds(x, y, half, 20).build());
        addRenderableWidget(Button.builder(wheelSideLabel(), b -> {
            VehicleEditorDraft.WheelPoint p = currentWheelPoint();
            replaceWheelPoint(new VehicleEditorDraft.WheelPoint(p.forward(), p.right(), p.scale(), p.yaw(), p.end(),
                    p.side().equals("left") ? "right" : "left"));
            b.setMessage(wheelSideLabel());
        }).bounds(x + half + 4, y, PARAM_WIDTH - half - 4, 20).build());
    }

    private void addWheelControls(int x, int y) {
        y = addIdentity(x, y);
        labeledNumber(x, y, "尺寸：", 0.05F, () -> draft.wheelSize, v -> draft.wheelSize = v); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "抓地力：", 0.05F, () -> draft.wheelGrip, v -> draft.wheelGrip = v); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "半径：", 0.25F, () -> draft.wheelRadius, v -> draft.wheelRadius = v); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "宽度：", 0.25F, () -> draft.wheelWidth, v -> draft.wheelWidth = v); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "Y轴旋转：", 5.0F, () -> draft.rotationY, v -> draft.rotationY = v); y += CONTROL_ROW_STEP;
        addComponentButtons(x, y, SelectionType.WHEEL);
    }

    private void addEngineControls(int x, int y) {
        y = addIdentity(x, y);
        labeledNumber(x, y, "扭矩：", 0.05F, () -> draft.engineTorque, v -> draft.engineTorque = v); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "速度：", 0.05F, () -> draft.engineSpeed, v -> draft.engineSpeed = v); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "Y轴旋转：", 5.0F, () -> draft.rotationY, v -> draft.rotationY = v); y += CONTROL_ROW_STEP;
        addRenderableWidget(Button.builder(Component.literal("引擎动画"), b -> preview.toggleEngine()).bounds(x, y, PARAM_WIDTH, 20).build()); y += CONTROL_ROW_STEP;
        addComponentButtons(x, y, SelectionType.ENGINE);
    }

    private void addComponentButtons(int x, int y, SelectionType type) {
        int gap = 4;
        int third = (PARAM_WIDTH - gap * 2) / 3;
        addRenderableWidget(Button.builder(Component.literal("选择预览"), b -> openSelection(type)).bounds(x, y, third, 20).build());
        addRenderableWidget(Button.builder(Component.literal("导入 BBModel"), b -> chooseModel()).bounds(x + third + gap, y, third, 20).build());
        addRenderableWidget(Button.builder(Component.literal("导出 .riauto"), b -> exportPack()).bounds(x + (third + gap) * 2, y, PARAM_WIDTH - (third + gap) * 2, 20).build());
    }

    private void addExportItemControl() {
        int x = leftPos + 238;
        int y = topPos + VehicleImportMenu.INVENTORY_Y;
        exportItemButton = Button.builder(Component.literal("导出物品"), b -> exportItem())
                .bounds(x, y, 58, 20).build();
        exportItemButton.active = !exportingItem;
        addRenderableWidget(exportItemButton);
    }

    private void addSeatControls(int x, int y) {
        seatIndex = Math.max(0, Math.min(seatIndex, draft.seats.size() - 1));
        addListHeader(x, y, "座椅", seatIndex, draft.seats.size(),
                () -> seatIndex = (seatIndex + 1) % draft.seats.size(),
                () -> { draft.seats.add(VehicleEditorDraft.defaultSeatPosition()); seatIndex = draft.seats.size() - 1; },
                () -> { if (draft.seats.size() > 1) draft.seats.remove(seatIndex); seatIndex = Math.max(0, seatIndex - 1); }); y += 28;
        Vec3 seat = draft.seats.get(seatIndex);
        vectorFields(x, y, seat, v -> draft.seats.set(seatIndex, v)); y += CONTROL_ROW_STEP * 3;
        addRenderableWidget(Button.builder(seatViewLabel(), b -> {
            seatFirstPerson = !seatFirstPerson;
            resetFirstPersonView();
            b.setMessage(seatViewLabel());
        }).bounds(x, y, PARAM_WIDTH, 20).build());
    }

    private void addHitboxControls(int x, int y) {
        hitboxIndex = Math.max(0, Math.min(hitboxIndex, draft.hitboxes.size() - 1));
        addListHeader(x, y, "碰撞箱", hitboxIndex, draft.hitboxes.size(),
                () -> hitboxIndex = (hitboxIndex + 1) % draft.hitboxes.size(),
                () -> { draft.hitboxes.add(new VehicleEditorDraft.HitboxPoint(Vec3.ZERO, 1, 1, false)); hitboxIndex = draft.hitboxes.size() - 1; },
                () -> { if (draft.hitboxes.size() > 1) draft.hitboxes.remove(hitboxIndex); hitboxIndex = Math.max(0, hitboxIndex - 1); }); y += 28;
        VehicleEditorDraft.HitboxPoint point = draft.hitboxes.get(hitboxIndex);
        vectorFields(x, y, point.origin(), v -> replaceHitbox(new VehicleEditorDraft.HitboxPoint(v, currentHitbox().width(), currentHitbox().height(), currentHitbox().hasContainer()))); y += CONTROL_ROW_STEP * 3;
        labeledNumber(x, y, "宽度：", 0.0625F, () -> currentHitbox().width(), v -> replaceHitbox(new VehicleEditorDraft.HitboxPoint(currentHitbox().origin(), v, currentHitbox().height(), currentHitbox().hasContainer()))); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "高度：", 0.0625F, () -> currentHitbox().height(), v -> replaceHitbox(new VehicleEditorDraft.HitboxPoint(currentHitbox().origin(), currentHitbox().width(), v, currentHitbox().hasContainer()))); y += CONTROL_ROW_STEP;
        addRenderableWidget(Button.builder(containerLabel(), b -> { var h = currentHitbox(); replaceHitbox(new VehicleEditorDraft.HitboxPoint(h.origin(), h.width(), h.height(), !h.hasContainer())); b.setMessage(containerLabel()); }).bounds(x, y, PARAM_WIDTH, 20).build());
    }

    private void addListHeader(int x, int y, String name, int index, int size, Runnable next, Runnable add, Runnable remove) {
        addRenderableWidget(Button.builder(Component.literal(name + " " + (index + 1) + "/" + size), b -> {
            next.run(); resetWidgets();
        }).bounds(x, y, PARAM_WIDTH - 48, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> { add.run(); resetWidgets(); }).bounds(x + PARAM_WIDTH - 44, y, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> { remove.run(); resetWidgets(); }).bounds(x + PARAM_WIDTH - 20, y, 20, 20).build());
    }

    private void vectorFields(int x, int y, Vec3 initial, Consumer<Vec3> setter) {
        final double[] values = {initial.x, initial.y, initial.z};
        String[] names = {"X：", "Y：", "Z："};
        for (int i = 0; i < 3; i++) {
            int index = i;
            labeledNumber(x, y + i * CONTROL_ROW_STEP, names[i], 0.0625F,
                    () -> (float) values[index], v -> { values[index] = v; setter.accept(new Vec3(values[0], values[1], values[2])); });
        }
    }

    private VehicleEditorDraft.HitboxPoint currentHitbox() { return draft.hitboxes.get(hitboxIndex); }
    private void replaceHitbox(VehicleEditorDraft.HitboxPoint value) { draft.hitboxes.set(hitboxIndex, value); }
    private Component containerLabel() { return Component.literal("容器碰撞箱：" + (currentHitbox().hasContainer() ? "是" : "否")); }
    private VehicleEditorDraft.WheelPoint currentWheelPoint() { return draft.wheelPoints.get(wheelPointIndex); }
    private void replaceWheelPoint(VehicleEditorDraft.WheelPoint value) { draft.wheelPoints.set(wheelPointIndex, value); }
    private Component wheelEndLabel() { return Component.literal("轴位：" + (currentWheelPoint().end().equals("front") ? "前" : "后")); }
    private Component wheelSideLabel() { return Component.literal("侧位：" + (currentWheelPoint().side().equals("left") ? "左" : "右")); }
    private Component seatViewLabel() { return Component.literal("预览视角：" + (seatFirstPerson ? "第一人称" : "外部")); }
    private boolean isSeatFirstPersonView() { return page == Page.FRAME && frameTab == FrameTab.SEATS && seatFirstPerson; }
    private void resetFirstPersonView() { firstPersonYaw = 0.0F; firstPersonPitch = 0.0F; firstPersonFov = 70.0F; }

    private void labeledText(int x, int y, String label, String value, Consumer<String> setter) {
        labels.add(new FieldLabel(label, x, y + 6));
        int fieldX = x + 70;
        EditBox field = new EditBox(font, fieldX, y, PARAM_WIDTH - 70, 20, Component.literal(label));
        field.setMaxLength(192); field.setValue(value); field.setResponder(setter); addRenderableWidget(field);
    }

    private void labeledNumber(int x, int y, String label, float step, Supplier<Float> getter, Consumer<Float> setter) {
        labeledNumber(x, y, label, PARAM_WIDTH - 70, step, getter, setter);
    }

    private void labeledNumber(int x, int y, String label, int fieldWidth, float step,
                               Supplier<Float> getter, Consumer<Float> setter) {
        labels.add(new FieldLabel(label, x, y + 6));
        int fieldX = x + 70;
        EditBox field = new EditBox(font, fieldX + NUMBER_ARROW_WIDTH, y,
                fieldWidth - NUMBER_ARROW_WIDTH * 2, 20, Component.literal(label));
        field.setValue(Float.toString(getter.get()));
        field.setResponder(value -> { try { setter.accept(Float.parseFloat(value)); status = ""; } catch (NumberFormatException e) { status = "数字格式无效"; } });
        addRenderableWidget(new NumberArrowButton(fieldX, y, NUMBER_ARROW_WIDTH, 20, Component.literal("<"),
                () -> nudgeNumber(field, getter, -effectiveNumberStep(step))));
        addRenderableWidget(field);
        addRenderableWidget(new NumberArrowButton(fieldX + fieldWidth - NUMBER_ARROW_WIDTH, y,
                NUMBER_ARROW_WIDTH, 20, Component.literal(">"),
                () -> nudgeNumber(field, getter, effectiveNumberStep(step))));
        numberControls.add(new NumberControl(field, step, getter));
    }

    private static float effectiveNumberStep(float step) {
        return Screen.hasShiftDown() ? step * FINE_STEP_SCALE : step;
    }

    private void nudgeNumber(EditBox field, Supplier<Float> getter, float delta) {
        float current;
        try {
            current = Float.parseFloat(field.getValue());
            if (!Float.isFinite(current)) current = getter.get();
        } catch (NumberFormatException ignored) {
            current = getter.get();
        }
        float next = current + delta;
        if (!Float.isFinite(next)) return;
        float rounded = (float) (Math.round((double) next * 1000000.0D) / 1000000.0D);
        field.setValue(Float.toString(rounded));
    }

    private void chooseModel() {
        String chosen = TinyFileDialogs.tinyfd_openFileDialog("选择含内嵌 PNG 纹理的 BBModel", "", null, "*.bbmodel", false);
        if (chosen == null) return;
        Path selected = Path.of(chosen);
        if (!selected.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".bbmodel")) { status = "只支持 .bbmodel 文件"; return; }
        draft.setModelFile(draft.target, selected);
        loadPreview();
    }

    private void loadPreview() {
        status = "正在载入预览...";
        try {
            previewSession.load(draft).whenComplete((unused, error) -> Minecraft.getInstance().execute(() -> status = error == null ? "预览已载入" : "预览失败：" + rootMessage(error)));
        } catch (IOException e) { status = "预览失败：" + e.getMessage(); }
    }

    private void exportPack() {
        try {
            Path directory = CarPackManager.getRootDirectory().resolve("exports");
            Path archive = VehiclePackBuilder.build(draft, directory.resolve(draft.packName() + CarPackManager.CAR_PACK_EXTENSION), false);
            status = "已导出：" + archive.getFileName();
        } catch (IOException e) { status = "导出失败：" + e.getMessage(); }
    }

    private void exportItem() {
        if (menu.hasOutputItem()) {
            status = "请先取走输出槽中的物品";
            return;
        }
        if (!menu.canPublish()) {
            status = "导出物品需要服务器管理员权限";
            return;
        }
        draft.target = page.target;
        VehicleEditorDraft.Target exportedTarget = draft.target;
        var componentId = draft.componentId();
        int containerId = menu.containerId;
        exportingItem = true;
        if (exportItemButton != null) exportItemButton.active = false;
        status = "正在生成并安装组件...";
        try {
            Path directory = CarPackManager.getRootDirectory().resolve("exports");
            Path archive = VehiclePackBuilder.build(draft,
                    directory.resolve(draft.packName() + CarPackManager.CAR_PACK_EXTENSION), false);
            ClientCarPackUploader.upload(archive, draft, result -> Minecraft.getInstance().execute(() -> {
                exportingItem = false;
                if (exportItemButton != null) exportItemButton.active = true;
                if (!result.successful()) {
                    status = "物品导出失败：" + result.detail();
                    return;
                }
                if (Minecraft.getInstance().screen != this || menu.containerId != containerId) return;
                RIAutomobilityNetwork.CHANNEL.sendToServer(new ExportVehicleComponentItemPacket(
                        containerId, exportedTarget.path, componentId));
                status = "组件已安装，物品已发送到输出槽";
            }));
        } catch (IOException exception) {
            exportingItem = false;
            if (exportItemButton != null) exportItemButton.active = true;
            status = "物品导出失败：" + exception.getMessage();
        }
    }

    private void openSelection(SelectionType type) { selectionType = type; selectionPage = 0; resetWidgets(); }
    private void closeSelection() { selectionType = null; selectionPage = 0; resetWidgets(); }

    private void addSelectionControls() {
        addRenderableWidget(Button.builder(Component.literal("返回"), b -> closeSelection()).bounds(leftPos + 8, topPos + 24, 54, 20).build());
        int count = selectionCount(); int pageCount = Math.max(1, (count + SELECTIONS_PER_PAGE - 1) / SELECTIONS_PER_PAGE);
        selectionPage = Math.max(0, Math.min(selectionPage, pageCount - 1));
        int start = selectionPage * SELECTIONS_PER_PAGE, columns = 4, gap = 4;
        int cellWidth = (imageWidth - 16 - gap * 3) / 4, cellHeight = 40;
        for (int offset = 0; offset < SELECTIONS_PER_PAGE && start + offset < count; offset++) {
            int index = start + offset;
            ComponentEntryButton entry = new ComponentEntryButton(leftPos + 8 + (offset % columns) * (cellWidth + gap), topPos + 50 + (offset / columns) * 44,
                    cellWidth, cellHeight, selectionStack(index), selectionName(index), selectionId(index), selectionIsCurrent(index), () -> selectComponent(index));
            selectionButtons.add(entry); addRenderableWidget(entry);
        }
        int navY = topPos + VehicleImportMenu.INVENTORY_Y - 42;
        Button prev = Button.builder(Component.literal("<"), b -> { selectionPage--; resetWidgets(); }).bounds(leftPos + imageWidth / 2 - 66, navY, 28, 20).build(); prev.active = selectionPage > 0; addRenderableWidget(prev);
        Button next = Button.builder(Component.literal(">"), b -> { selectionPage++; resetWidgets(); }).bounds(leftPos + imageWidth / 2 + 38, navY, 28, 20).build(); next.active = selectionPage + 1 < pageCount; addRenderableWidget(next);
    }

    private int selectionCount() { return switch (selectionType) { case FRAME -> frames.size(); case WHEEL -> wheels.size(); case ENGINE -> engines.size(); }; }
    private ItemStack selectionStack(int i) { return switch (selectionType) { case FRAME -> AutomobilityItems.AUTOMOBILE_FRAME.require().createStack(frames.get(i)); case WHEEL -> AutomobilityItems.AUTOMOBILE_WHEEL.require().createStack(wheels.get(i)); case ENGINE -> AutomobilityItems.AUTOMOBILE_ENGINE.require().createStack(engines.get(i)); }; }
    private Component selectionName(int i) { return selectionStack(i).getHoverName(); }
    private String selectionId(int i) { return switch (selectionType) { case FRAME -> frames.get(i).getId().toString(); case WHEEL -> wheels.get(i).getId().toString(); case ENGINE -> engines.get(i).getId().toString(); }; }
    private boolean selectionIsCurrent(int i) { return switch (selectionType) { case FRAME -> frames.get(i) == draft.selectedFrame; case WHEEL -> wheels.get(i) == draft.selectedWheel; case ENGINE -> engines.get(i) == draft.selectedEngine; }; }
    private void selectComponent(int i) {
        switch (selectionType) {
            case FRAME -> { draft.loadFrame(frames.get(i)); draft.setPreviewReady(VehicleEditorDraft.Target.FRAME, false); draft.showPart(VehicleEditorDraft.Target.FRAME); }
            case WHEEL -> { draft.loadWheel(wheels.get(i)); draft.setPreviewReady(VehicleEditorDraft.Target.WHEEL, false); draft.showPart(VehicleEditorDraft.Target.WHEEL); }
            case ENGINE -> { draft.loadEngine(engines.get(i)); draft.setPreviewReady(VehicleEditorDraft.Target.ENGINE, false); draft.showPart(VehicleEditorDraft.Target.ENGINE); }
        }
        closeSelection();
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF0181B20);
        if (selectionType != null) {
            g.fill(leftPos + 5, topPos + 22, leftPos + imageWidth - 5,
                    topPos + VehicleImportMenu.INVENTORY_Y - 8, 0xFF22262C);
        } else {
            g.fill(leftPos + 4, topPos + 22, leftPos + TAB_WIDTH, topPos + imageHeight - 5, 0xFF262A31);
            g.fill(leftPos + TAB_WIDTH + 4, topPos + 22, leftPos + TAB_WIDTH + PARAM_WIDTH + 12, topPos + imageHeight - 5, 0xFF20242A);
            g.fill(previewX0(), topPos + 22, leftPos + imageWidth - 5, topPos + imageHeight - 5, 0xFF0E1115);
            if (draft.hasVisibleParts()) renderVehicle(g, partialTick);
            if (page == Page.FRAME && frameTab == FrameTab.BASIC) renderFrameItemSizePreview(g);
        }
        renderInventoryBackground(g);
    }

    private void renderInventoryBackground(GuiGraphics g) {
        int panelX0 = leftPos + VehicleImportMenu.INVENTORY_X - 6;
        int panelY0 = topPos + VehicleImportMenu.INVENTORY_Y - 17;
        int panelX1 = leftPos + VehicleImportMenu.OUTPUT_SLOT_X + 23;
        int panelY1 = topPos + VehicleImportMenu.INVENTORY_Y + 77;
        g.fill(panelX0, panelY0, panelX1, panelY1, 0xFF20242A);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotBackground(g, VehicleImportMenu.INVENTORY_X + column * 18,
                        VehicleImportMenu.INVENTORY_Y + row * 18, false);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotBackground(g, VehicleImportMenu.INVENTORY_X + column * 18,
                    VehicleImportMenu.INVENTORY_Y + 58, false);
        }
        renderSlotBackground(g, VehicleImportMenu.OUTPUT_SLOT_X, VehicleImportMenu.OUTPUT_SLOT_Y, true);
    }

    private void renderSlotBackground(GuiGraphics g, int relativeX, int relativeY, boolean output) {
        int x = leftPos + relativeX;
        int y = topPos + relativeY;
        g.fill(x - 1, y - 1, x + 17, y + 17, output ? 0xFF91B568 : 0xFF5B626D);
        g.fill(x, y, x + 16, y + 16, 0xFF11151A);
    }

    private void renderFrameItemSizePreview(GuiGraphics g) {
        int panelX = leftPos + TAB_WIDTH + 8;
        int x0 = panelX + PARAM_WIDTH - ITEM_PREVIEW_SIZE;
        int y0 = topPos + 97;
        int x1 = x0 + ITEM_PREVIEW_SIZE;
        int y1 = y0 + ITEM_PREVIEW_SIZE;
        g.fill(x0, y0, x1, y1, 0xFF11151A);
        g.fill(x0, y0, x1, y0 + 1, 0xFF4A515B);
        g.fill(x0, y1 - 1, x1, y1, 0xFF4A515B);
        g.fill(x0, y0, x0 + 1, y1, 0xFF4A515B);
        g.fill(x1 - 1, y0, x1, y1, 0xFF4A515B);

        AutomobileFrame frame = draft.previewFrame();
        Model model = AutomobileModels.getModel(frame.model().modelId());
        if (model != null && Float.isFinite(draft.lengthPx) && draft.lengthPx > 0.0F) {
            g.flush();
            g.enableScissor(x0 + 1, y0 + 1, x1 - 1, y1 - 1);
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate((x0 + x1) * 0.5F, (y0 + y1) * 0.5F, 180.0F);
            // GuiGraphics.renderItem applies this screen-space Y flip before
            // scaling into the 16x16 item coordinate system.
            pose.mulPoseMatrix(new Matrix4f().scaling(1.0F, -1.0F, 1.0F));
            pose.scale(16.0F, 16.0F, 16.0F);
            // automobility:automobile_frame's GUI item transform.
            pose.translate(0.22F / 16.0F, 0.0F, 0.0F);
            pose.mulPose(new Quaternionf().rotationXYZ(
                    (float)Math.toRadians(30.0F), (float)Math.toRadians(-45.0F), 0.0F));
            pose.scale(0.44F, 0.44F, 0.44F);
            pose.translate(0.5F, 0.0F, 0.5F);
            float itemScale = VehicleEditorDraft.frameItemScale(draft.lengthPx);
            pose.scale(itemScale, -itemScale, -itemScale);
            RenderSystem.enableDepthTest();
            Lighting.setupForEntityInInventory();
            MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            BbRenderContext.begin(buffers, preview, 0.0F);
            try {
                model.renderToBuffer(pose, buffers.getBuffer(model.renderType(frame.model().texture())),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            } finally {
                BbRenderContext.end();
            }
            buffers.endBatch();
            pose.popPose();
            g.flush();
            Lighting.setupFor3DItems();
            RenderSystem.disableDepthTest();
            g.disableScissor();
        }
    }

    private void renderVehicle(GuiGraphics g, float partialTick) {
        int x0 = previewX0(), x1 = leftPos + imageWidth - 5, y0 = topPos + 22, y1 = topPos + imageHeight - 5;
        if (isSeatFirstPersonView()) {
            renderFirstPersonVehicle(g, partialTick, x0, x1, y0, y1);
        } else {
            renderOrbitVehicle(g, partialTick, x0, x1, y0, y1);
        }
    }

    private void renderOrbitVehicle(GuiGraphics g, float partialTick, int x0, int x1, int y0, int y1) {
        g.enableScissor(x0, y0, x1, y1);
        PoseStack pose = g.pose(); pose.pushPose();
        pose.translate((x0 + x1) / 2.0F + panX, (y0 + y1) / 2.0F + 24 + panY, 160);
        // Match vanilla's inventory projection: keep the screen-space mirror out
        // of PoseStack's normal matrix, then restore the previous visual basis
        // with a real rotation. A negative PoseStack.scale corrupts diffuse normals.
        pose.mulPoseMatrix(new Matrix4f().scaling(zoom, zoom, -zoom));
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));
        pose.mulPose(new Quaternionf().rotationX((float) Math.toRadians(rotationX)));
        pose.mulPose(new Quaternionf().rotationY((float) Math.toRadians(rotationY)));
        RenderSystem.enableDepthTest();
        Lighting.setupForEntityInInventory();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        AutomobileRenderer.render(pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, partialTick, preview);
        if (page == Page.FRAME && frameTab == FrameTab.HITBOXES) renderHitboxOutlines(pose, buffers);
        if (page == Page.FRAME && frameTab == FrameTab.SEATS) renderSeatPlayers(pose, buffers);
        buffers.endBatch();
        pose.popPose();
        g.flush();
        Lighting.setupFor3DItems();
        RenderSystem.disableDepthTest();
        g.disableScissor();
    }

    private void renderFirstPersonVehicle(GuiGraphics g, float partialTick, int x0, int x1, int y0, int y1) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer player = minecraft.player;
        if (player == null) {
            renderOrbitVehicle(g, partialTick, x0, x1, y0, y1);
            return;
        }

        g.flush();
        g.enableScissor(x0, y0, x1, y1);
        Window window = minecraft.getWindow();
        double guiScale = window.getGuiScale();
        int viewportX = (int)Math.round(x0 * guiScale);
        int viewportY = window.getHeight() - (int)Math.round(y1 * guiScale);
        int viewportWidth = Math.max(1, (int)Math.round((x1 - x0) * guiScale));
        int viewportHeight = Math.max(1, (int)Math.round((y1 - y0) * guiScale));
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();

        try {
            // Screen rendering inherits GameRenderer's GUI model-view translation. A perspective
            // preview must start from the same identity model-view state as normal level rendering,
            // otherwise the entire vehicle is translated beyond the camera's clipping planes.
            modelView.setIdentity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.viewport(viewportX, viewportY, viewportWidth, viewportHeight);
            Matrix4f perspective = new Matrix4f().setPerspective(
                    (float)Math.toRadians(firstPersonFov), (float)viewportWidth / viewportHeight, 0.05F, 100.0F);
            RenderSystem.setProjectionMatrix(perspective, VertexSorting.DISTANCE_TO_ORIGIN);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.clear(256, Minecraft.ON_OSX);
            Lighting.setupForEntityInInventory();

            PoseStack pose = new PoseStack();
            pose.mulPose(Axis.XP.rotationDegrees(firstPersonPitch));
            pose.mulPose(Axis.YP.rotationDegrees(firstPersonYaw + 180.0F));
            Vec3 eye = VehicleEditorDraft.firstPersonEyePosition(draft.seats.get(seatIndex), draft.wheelRadius,
                    player.getMyRidingOffset(), player.getEyeHeight(Pose.STANDING));
            pose.translate(-eye.x, -eye.y, -eye.z);

            MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
            AutomobileRenderer.render(pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, partialTick, preview);
            renderSeatPlayers(pose, buffers, seatIndex);
            buffers.endBatch();
        } finally {
            RenderSystem.setProjectionMatrix(previousProjection, VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.viewport(0, 0, window.getWidth(), window.getHeight());
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();
            RenderSystem.disableDepthTest();
            g.disableScissor();
        }

        int centerX = (x0 + x1) / 2;
        int centerY = (y0 + y1) / 2;
        g.fill(centerX - 4, centerY, centerX + 5, centerY + 1, 0xCCFFFFFF);
        g.fill(centerX, centerY - 4, centerX + 1, centerY + 5, 0xCCFFFFFF);
    }

    private void renderHitboxOutlines(PoseStack pose, MultiBufferSource.BufferSource buffers) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.mulPose(Axis.YP.rotationDegrees(180));
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (int index = 0; index < draft.hitboxes.size(); index++) {
            VehicleEditorDraft.HitboxPoint hitbox = draft.hitboxes.get(index);
            double halfWidth = hitbox.width() * 0.5D;
            Vec3 origin = hitbox.origin();
            AABB box = new AABB(origin.x - halfWidth, -origin.y - hitbox.height(), origin.z - halfWidth,
                    origin.x + halfWidth, -origin.y, origin.z + halfWidth);
            boolean selected = index == hitboxIndex;
            LevelRenderer.renderLineBox(pose, lines, box,
                    selected ? 1.0F : 0.15F, selected ? 0.72F : 0.8F, selected ? 0.12F : 1.0F, 1.0F);
        }
        pose.popPose();
    }

    private void renderSeatPlayers(PoseStack pose, MultiBufferSource.BufferSource buffers) {
        renderSeatPlayers(pose, buffers, -1);
    }

    private void renderSeatPlayers(PoseStack pose, MultiBufferSource.BufferSource buffers, int hiddenSeatIndex) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer player = minecraft.player;
        if (player == null) return;
        if (seatPlayerModel == null) {
            seatPlayerModel = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        }
        seatPlayerModel.riding = true;
        seatPlayerModel.young = false;
        seatPlayerModel.attackTime = 0;
        seatPlayerModel.prepareMobModel(player, 0, 0, 0);
        seatPlayerModel.setupAnim(player, 0, 0, 0, 0, 0);

        VertexConsumer playerBuffer = buffers.getBuffer(seatPlayerModel.renderType(player.getSkinTextureLocation()));
        for (int index = 0; index < draft.seats.size(); index++) {
            if (index == hiddenSeatIndex) continue;
            Vec3 seat = draft.seats.get(index);
            pose.pushPose();
            // EntityRenderDispatcher first places the player at the passenger's
            // world-space feet position. Keep this outside the living-model basis;
            // putting it inside AutomobileRenderer's rotations mirrors seat Z.
            Vec3 passengerPosition = VehicleEditorDraft.passengerPosition(
                    seat, draft.wheelRadius, player.getMyRidingOffset());
            pose.translate(passengerPosition.x, passengerPosition.y, passengerPosition.z);
            // LivingEntityRenderer.setupRotations for a forward-facing player,
            // followed by its model-space flip and PlayerRenderer's fixed scale.
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.scale(-1.0F, -1.0F, 1.0F);
            pose.scale(0.9375F, 0.9375F, 0.9375F);
            pose.translate(0.0F, -1.501F, 0.0F);
            seatPlayerModel.renderToBuffer(pose, playerBuffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F);
            pose.popPose();
        }
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g); super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, title, leftPos + 8, topPos + 8, 0xFFFFFF, false);
        if (selectionType != null) {
            g.drawCenteredString(font, "选择 " + selectionType.label, leftPos + imageWidth / 2, topPos + 31, 0xFFFFFF);
            int pageCount = Math.max(1, (selectionCount() + SELECTIONS_PER_PAGE - 1) / SELECTIONS_PER_PAGE);
            g.drawCenteredString(font, (selectionPage + 1) + " / " + pageCount,
                    leftPos + imageWidth / 2, topPos + VehicleImportMenu.INVENTORY_Y - 36, 0xB9C0C8);
            for (ComponentEntryButton b : selectionButtons) if (b.isHovered()) { g.renderTooltip(font, b.stack, mouseX, mouseY); break; }
        } else {
            labels.forEach(label -> g.drawString(font, label.text, label.x, label.y, 0xD8DEE8, false));
            if (!draft.hasVisibleParts()) g.drawCenteredString(font, "请选择要预览的部件，或导入对应 BBModel", (previewX0() + leftPos + imageWidth - 5) / 2, topPos + imageHeight / 2, 0x8F98A5);
            if (!status.isBlank()) g.drawString(font, font.plainSubstrByWidth(status, imageWidth - font.width(title) - 28), leftPos + font.width(title) + 18, topPos + 8, 0xC9D1D9, false);
        }
        g.drawString(font, "背包", leftPos + VehicleImportMenu.INVENTORY_X,
                topPos + VehicleImportMenu.INVENTORY_Y - 13, 0xD8DEE8, false);
        g.drawCenteredString(font, "输出", leftPos + VehicleImportMenu.OUTPUT_SLOT_X + 8,
                topPos + VehicleImportMenu.OUTPUT_SLOT_Y - 12, 0xD8DEE8);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}
    private int previewX0() { return leftPos + TAB_WIDTH + PARAM_WIDTH + 16; }
    private boolean inPreview(double x, double y) { return selectionType == null && x >= previewX0() && x < leftPos + imageWidth - 5 && y >= topPos + 22 && y < topPos + imageHeight - 5; }
    private void resetView() {
        if (isSeatFirstPersonView()) {
            resetFirstPersonView();
        } else {
            rotationX = 18; rotationY = 35; zoom = 38; panX = panY = 0;
        }
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (inPreview(x, y)) {
            // Widgets drawn over the preview (notably reset-view) keep priority,
            // but the container screen must not consume the empty preview area.
            if (getChildAt(x, y).isPresent() && super.mouseClicked(x, y, button)) return true;
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || (!isSeatFirstPersonView() && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
                dragButton = button;
                lastMouseX = x;
                lastMouseY = y;
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragButton == button) {
            if (isSeatFirstPersonView()) {
                firstPersonYaw += (float)(x - lastMouseX) * 0.35F;
                firstPersonPitch = Math.max(-80.0F, Math.min(80.0F,
                        firstPersonPitch + (float)(y - lastMouseY) * 0.35F));
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) { rotationY += (float) (x - lastMouseX); rotationX = Math.max(-80, Math.min(80, rotationX + (float) (y - lastMouseY))); }
            else { panX += (float) (x - lastMouseX); panY += (float) (y - lastMouseY); }
            lastMouseX = x; lastMouseY = y; return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) { if (dragButton == button) dragButton = -1; return super.mouseReleased(x, y, button); }
    @Override public boolean mouseScrolled(double x, double y, double amount) {
        if (amount != 0.0D) {
            for (NumberControl control : numberControls) {
                if (control.field().isMouseOver(x, y)) {
                    float direction = amount > 0.0D ? 1.0F : -1.0F;
                    nudgeNumber(control.field(), control.getter(),
                            direction * effectiveNumberStep(control.step()));
                    return true;
                }
            }
        }
        if (inPreview(x, y)) {
            if (isSeatFirstPersonView()) {
                firstPersonFov = Math.max(30.0F, Math.min(100.0F, firstPersonFov - (float)amount * 4.0F));
            } else {
                zoom = Math.max(12, Math.min(80, zoom + (float) amount * 3));
            }
            return true;
        }
        return super.mouseScrolled(x, y, amount);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && selectionType != null) { closeSelection(); return true; }
        // Printable characters are delivered through charTyped after keyPressed. EditBox therefore
        // does not consume the physical E key here, which otherwise lets AbstractContainerScreen
        // interpret it as the inventory shortcut and close the editor before the character arrives.
        if (getFocused() instanceof EditBox
                && minecraft != null
                && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public void removed() { previewSession.close(); super.removed(); }
    private void resetWidgets() { clearWidgets(); init(); }
    private static String rootMessage(Throwable error) { Throwable current = error; while (current.getCause() != null) current = current.getCause(); return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage(); }

    private enum Page {
        FRAME("车架", VehicleEditorDraft.Target.FRAME), WHEEL("车轮", VehicleEditorDraft.Target.WHEEL), ENGINE("引擎", VehicleEditorDraft.Target.ENGINE);
        final String label; final VehicleEditorDraft.Target target;
        Page(String label, VehicleEditorDraft.Target target) { this.label = label; this.target = target; }
    }
    private enum FrameTab {
        BASIC("基础"), WHEELS("轮位"), SEATS("座椅"), HITBOXES("碰撞箱");
        final String label;
        FrameTab(String label) { this.label = label; }
    }
    private enum SelectionType { FRAME("车架"), WHEEL("车轮"), ENGINE("引擎"); final String label; SelectionType(String label) { this.label = label; } }
    private record FieldLabel(String text, int x, int y) {}
    private record NumberControl(EditBox field, float step, Supplier<Float> getter) {}

    private static final class NumberArrowButton extends AbstractButton {
        private final Runnable action;

        private NumberArrowButton(int x, int y, int width, int height, Component message, Runnable action) {
            super(x, y, width, height, message);
            this.action = action;
        }

        @Override public void onPress() { action.run(); }

        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean fine = Screen.hasShiftDown();
            int background = fine
                    ? (isHoveredOrFocused() ? 0xFF657A46 : 0xFF4C5D35)
                    : (isHoveredOrFocused() ? 0xFF454B53 : 0xFF30343A);
            int border = fine ? 0xFFA7D46F : 0xFF737A84;
            g.fill(getX(), getY(), getX() + width, getY() + height, border);
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, background);
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                    getX() + width / 2, getY() + (height - 8) / 2, active ? 0xFFFFFF : 0xA0A0A0);
        }

        @Override protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage());
        }
    }

    private static final class ComponentEntryButton extends AbstractButton {
        private final ItemStack stack; private final String subtitle; private final boolean selected; private final Runnable action;
        private ComponentEntryButton(int x, int y, int width, int height, ItemStack stack, Component name, String subtitle, boolean selected, Runnable action) {
            super(x, y, width, height, name); this.stack = stack; this.subtitle = subtitle; this.selected = selected; this.action = action;
        }
        @Override public void onPress() { action.run(); }
        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int color = isHoveredOrFocused() ? 0xFF454B53 : 0xFF30343A; g.fill(getX(), getY(), getX() + width, getY() + height, color);
            if (selected) { int c = 0xFF62C778; g.fill(getX(), getY(), getX() + width, getY() + 2, c); g.fill(getX(), getY() + height - 2, getX() + width, getY() + height, c); }
            g.renderItem(stack, getX() + 7, getY() + (height - 16) / 2);
            var f = Minecraft.getInstance().font; g.drawString(f, f.plainSubstrByWidth(getMessage().getString(), width - 31), getX() + 27, getY() + 8, 0xFFFFFF, false);
            g.drawString(f, f.plainSubstrByWidth(subtitle, width - 31), getX() + 27, getY() + 22, 0xAAB2BC, false);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { output.add(NarratedElementType.TITLE, getMessage()); }
    }
}
