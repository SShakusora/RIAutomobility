package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.carpack.CarPackManager;
import com.sshakusora.riautomobility.editor.VehicleImportGuiAtlas;
import com.sshakusora.riautomobility.editor.VehicleImportMenu;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.ExportVehicleComponentItemPacket;
import com.sshakusora.riautomobility.network.packet.client.ClientCarPackSynchronizer;
import com.sshakusora.riautomobility.network.packet.client.ClientCarPackUploader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class VehicleImportScreen extends AbstractContainerScreen<VehicleImportMenu> {
    private static final int TAB_WIDTH = 62;
    private static final int PARAM_WIDTH = 226;
    private static final int GUI_HEIGHT = 300;
    private static final int CONTROL_ROW_STEP = 24;
    private static final int DENSE_CONTROL_ROW_STEP = 20;
    private static final float COMPACT_TOGGLE_TEXT_SCALE = 0.8F;
    private static final int FIELD_LABEL_WIDTH = 66;
    private static final int NUMBER_ARROW_WIDTH = 18;
    private static final int EXPORT_ITEM_BUTTON_WIDTH = 58;
    private static final float FINE_STEP_SCALE = 0.1F;
    private static final int SELECTION_COLUMNS = 12;
    private static final int SELECTION_CELL_SIZE = 24;
    private static final int SELECTION_GAP = 4;
    private final VehicleEditorDraft draft;
    private final PreviewAutomobile preview;
    private final VehiclePreviewRenderer previewRenderer;
    private final ClientVehiclePreviewSession previewSession = new ClientVehiclePreviewSession();
    private final VehicleComponentCatalog components = new VehicleComponentCatalog();
    private final List<VehicleComponentIconButton> selectionButtons = new ArrayList<>();
    private final List<VehicleAttachmentIconList> attachmentIconLists = new ArrayList<>();
    private final List<FieldLabel> labels = new ArrayList<>();
    private final List<VehicleImportTooltips.Area> parameterTooltips = new ArrayList<>();
    private final List<NumberControl> numberControls = new ArrayList<>();
    private final List<AbstractWidget> availableWithoutPreview = new ArrayList<>();
    private final EnumMap<VehicleEditorDraft.Target, Path> extractedImportFiles =
            new EnumMap<>(VehicleEditorDraft.Target.class);
    private Button exportItemButton;
    private Page page = Page.FRAME;
    private FrameTab frameTab = FrameTab.BASIC;
    private WheelTab wheelTab = WheelTab.BASIC;
    private SelectionType selectionType;
    private int selectionPage;
    private int wheelPointIndex;
    private int seatIndex;
    private int hitboxIndex = -1;
    private VehiclePositionDropdown positionDropdown;
    private int wheelPositionDropdownScroll;
    private int seatPositionDropdownScroll;
    private int collisionDropdownScroll;
    private boolean seatFirstPerson;
    private boolean exportingItem;
    private double frontAttachmentListScroll;
    private double rearAttachmentListScroll;
    private String status = "";

    public VehicleImportScreen(VehicleImportMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        draft = new VehicleEditorDraft(components.defaultFrame(), components.defaultWheel(), components.defaultEngine());
        preview = new PreviewAutomobile(draft);
        previewRenderer = new VehiclePreviewRenderer(draft, preview);
        imageWidth = 620;
        imageHeight = GUI_HEIGHT;
    }

    @Override protected void init() {
        positionDropdown = null;
        imageWidth = Math.max(420, Math.min(620, width - 16));
        imageHeight = GUI_HEIGHT;
        super.init();
        menu.setSlotsActive(selectionType == null);
        labels.clear(); parameterTooltips.clear(); selectionButtons.clear(); attachmentIconLists.clear();
        numberControls.clear(); availableWithoutPreview.clear();
        if (selectionType != null) { addSelectionControls(); return; }
        int x = leftPos + 7;
        int y = topPos + 24;
        for (Page value : Page.values()) {
            Button tab = texturedButton(VehicleImportText.component(value.label),
                    b -> { page = value; setTargetForPage(); resetWidgets(); },
                    x, y, TAB_WIDTH - 10, 32);
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
        VehicleTexturedButton resetViewButton = texturedButton(VehicleImportText.component("button.reset_view"),
                b -> resetView(), px, topPos + 26, 72, 18);
        resetViewButton.active = hasCurrentPartPreview();
        addRenderableWidget(resetViewButton);
        disableUnavailablePartControls(panelX);
    }

    private void setTargetForPage() {
        draft.target = page.target;
        preview.setAttachmentGuidesVisible(page == Page.FRAME && frameTab == FrameTab.ATTACHMENTS);
        if (page == Page.FRAME) showFrameTabParts();
    }

    private boolean hasCurrentPartPreview() {
        return draft.isPartVisible(page.target);
    }

    private void disableUnavailablePartControls(int panelX) {
        if (hasCurrentPartPreview()) return;
        for (var child : children()) {
            if (child instanceof AbstractWidget widget
                    && widget.getX() >= panelX
                    && widget.getX() < panelX + PARAM_WIDTH
                    && !availableWithoutPreview.contains(widget)) {
                if (widget instanceof EditBox field) {
                    field.setResponder(null);
                    field.setValue("");
                }
                widget.active = false;
            }
        }
    }

    private void showFrameTabParts() {
        boolean frameReady = draft.isPartVisible(VehicleEditorDraft.Target.FRAME);
        preview.setAttachmentGuidesVisible(frameReady && frameTab == FrameTab.ATTACHMENTS);
    }

    private int addIdentity(int x, int y) {
        labeledText(x, y, "label.name", draft.displayName(), draft::setDisplayName); y += CONTROL_ROW_STEP;
        return y;
    }

    private void addFrameControls(int x, int y) {
        int gap = 2;
        int tabWidth = (PARAM_WIDTH - gap * (FrameTab.values().length - 1)) / FrameTab.values().length;
        int tabX = x;
        for (FrameTab value : FrameTab.values()) {
            int width = value == FrameTab.values()[FrameTab.values().length - 1] ? x + PARAM_WIDTH - tabX : tabWidth;
            Button tab = texturedButton(VehicleImportText.component(value.label), b -> {
                frameTab = value;
                draft.target = VehicleEditorDraft.Target.FRAME;
                showFrameTabParts();
                resetWidgets();
            }, tabX, y, width, 20);
            tab.active = frameTab != value
                    && value.isAvailable(draft);
            addRenderableWidget(tab);
            availableWithoutPreview.add(tab);
            tabX += width + gap;
        }
        y += CONTROL_ROW_STEP;
        switch (frameTab) {
            case BASIC -> addFrameBasicControls(x, y);
            case WHEELS -> addFrameWheelControls(x, y);
            case SEATS -> addSeatControls(x, y);
            case HITBOXES -> addHitboxControls(x, y);
            case ATTACHMENTS -> addAttachmentControls(x, y);
        }
    }

    private void addFrameBasicControls(int x, int y) {
        y = addIdentity(x, y);
        labeledNumber(x, y, "label.weight", 0.05F, () -> draft.weight, v -> draft.weight = v); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "label.engine_back", 1.0F, () -> draft.enginePosBack,
                v -> draft.enginePosBack = v, !draft.hideEngine); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "label.engine_height", 1.0F, () -> draft.enginePosUp,
                v -> draft.enginePosUp = v, !draft.hideEngine); y += CONTROL_ROW_STEP;
        binaryToggle(x, y, "label.hide_engine", "off", "on",
                () -> draft.hideEngine, () -> {
                    draft.hideEngine = !draft.hideEngine;
                    resetWidgets();
                });
        y += CONTROL_ROW_STEP;
        addComponentButtons(x, y, SelectionType.FRAME);
    }

    private void addFrameWheelControls(int x, int y) {
        if (draft.wheelPoints.isEmpty()) {
            draft.wheelPoints.add(new VehicleEditorDraft.WheelPoint(0.0F, 0.0F, 1.0F, 0.0F, "front", "left"));
        }
        wheelPointIndex = Math.max(0, Math.min(wheelPointIndex, draft.wheelPoints.size() - 1));
        addListHeader(x, y, "header.wheel", wheelPointIndex, draft.wheelPoints.size(),
                VehiclePositionDropdown.Type.WHEEL, this::wheelPositionDropdownLabel,
                index -> wheelPointIndex = index,
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
        labeledNumber(x, y, "label.forward", 1.0F, () -> currentWheelPoint().forward(), v -> replaceWheelPoint(new VehicleEditorDraft.WheelPoint(
                v, currentWheelPoint().right(), currentWheelPoint().scale(), currentWheelPoint().yaw(), currentWheelPoint().end(), currentWheelPoint().side()))); y += DENSE_CONTROL_ROW_STEP;
        labeledNumber(x, y, "label.right", 1.0F, () -> currentWheelPoint().right(), v -> replaceWheelPoint(new VehicleEditorDraft.WheelPoint(
                currentWheelPoint().forward(), v, currentWheelPoint().scale(), currentWheelPoint().yaw(), currentWheelPoint().end(), currentWheelPoint().side()))); y += DENSE_CONTROL_ROW_STEP;
        labeledNumber(x, y, "label.scale", 0.05F, () -> currentWheelPoint().scale(), v -> replaceWheelPoint(new VehicleEditorDraft.WheelPoint(
                currentWheelPoint().forward(), currentWheelPoint().right(), v, currentWheelPoint().yaw(), currentWheelPoint().end(), currentWheelPoint().side()))); y += DENSE_CONTROL_ROW_STEP;
        labeledNumber(x, y, "label.yaw", 5.0F, () -> currentWheelPoint().yaw(), v -> replaceWheelPoint(new VehicleEditorDraft.WheelPoint(
                currentWheelPoint().forward(), currentWheelPoint().right(), currentWheelPoint().scale(), v, currentWheelPoint().end(), currentWheelPoint().side()))); y += DENSE_CONTROL_ROW_STEP;
        binaryToggle(x, y, "label.axle", "rear", "front", () -> currentWheelPoint().end().equals("front"), () -> {
            VehicleEditorDraft.WheelPoint p = currentWheelPoint();
            replaceWheelPoint(new VehicleEditorDraft.WheelPoint(p.forward(), p.right(), p.scale(), p.yaw(),
                    p.end().equals("front") ? "back" : "front", p.side()));
        });
        y += DENSE_CONTROL_ROW_STEP;
        binaryToggle(x, y, "label.side", "right", "left", () -> currentWheelPoint().side().equals("left"), () -> {
            VehicleEditorDraft.WheelPoint p = currentWheelPoint();
            replaceWheelPoint(new VehicleEditorDraft.WheelPoint(p.forward(), p.right(), p.scale(), p.yaw(), p.end(),
                    p.side().equals("left") ? "right" : "left"));
        });
    }

    private void addWheelControls(int x, int y) {
        if (!wheelTab.isAvailable(draft)) wheelTab = WheelTab.BASIC;
        int gap = 2;
        int tabWidth = (PARAM_WIDTH - gap) / WheelTab.values().length;
        int tabX = x;
        for (WheelTab value : WheelTab.values()) {
            int width = value == WheelTab.values()[WheelTab.values().length - 1]
                    ? x + PARAM_WIDTH - tabX : tabWidth;
            Button tab = texturedButton(VehicleImportText.component(value.label), b -> {
                wheelTab = value;
                draft.target = VehicleEditorDraft.Target.WHEEL;
                resetWidgets();
            }, tabX, y, width, 20);
            tab.active = wheelTab != value && value.isAvailable(draft);
            addRenderableWidget(tab);
            availableWithoutPreview.add(tab);
            tabX += width + gap;
        }
        y += CONTROL_ROW_STEP;

        if (wheelTab == WheelTab.BASIC) {
            y = addIdentity(x, y);
            labeledNumber(x, y, "label.size", 0.05F, () -> draft.wheelSize, v -> draft.wheelSize = v); y += CONTROL_ROW_STEP;
            labeledNumber(x, y, "label.grip", 0.05F, () -> draft.wheelGrip, v -> draft.wheelGrip = v); y += CONTROL_ROW_STEP;
            addComponentButtons(x, y, SelectionType.WHEEL);
        } else {
            labeledNumber(x, y, "label.radius", 0.25F, () -> draft.wheelRadius, draft::setManualWheelRadius); y += CONTROL_ROW_STEP;
            labeledNumber(x, y, "label.width", 0.25F, () -> draft.wheelWidth, draft::setManualWheelWidth); y += CONTROL_ROW_STEP;
            labeledNumber(x, y, "label.rotation_y", 5.0F, () -> draft.wheelRotationY, draft::setManualWheelRotationY);
        }
    }

    private void addEngineControls(int x, int y) {
        y = addIdentity(x, y);
        labeledNumber(x, y, "label.torque", 0.05F, () -> draft.engineTorque, v -> draft.engineTorque = v); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "label.speed", 0.05F, () -> draft.engineSpeed, v -> draft.engineSpeed = v); y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "label.rotation_y", 5.0F, () -> draft.engineRotationY, v -> draft.engineRotationY = v); y += CONTROL_ROW_STEP;
        binaryToggle(x, y, "label.engine_animation", "stopped", "running",
                preview::engineRunning, preview::toggleEngine);
        y += CONTROL_ROW_STEP;
        addComponentButtons(x, y, SelectionType.ENGINE);
    }

    private void addComponentButtons(int x, int y, SelectionType type) {
        int gap = 4;
        int third = (PARAM_WIDTH - gap * 2) / 3;
        Button select = texturedButton(VehicleImportText.component("button.select_preview"),
                b -> openSelection(type), x, y, third, 20);
        Button importModel = texturedButton(VehicleImportText.component("button.import_model"),
                b -> chooseModel(), x + third + gap, y, third, 20);
        addRenderableWidget(select);
        addRenderableWidget(importModel);
        availableWithoutPreview.add(select);
        availableWithoutPreview.add(importModel);
        addRenderableWidget(texturedButton(VehicleImportText.component("button.export_pack"),
                b -> exportPack(), x + (third + gap) * 2, y,
                PARAM_WIDTH - (third + gap) * 2, 20));
    }

    private void addExportItemControl() {
        int x = leftPos + VehicleImportMenu.OUTPUT_SLOT_X + 8 - EXPORT_ITEM_BUTTON_WIDTH / 2;
        int y = topPos + VehicleImportMenu.INVENTORY_Y;
        exportItemButton = texturedButton(VehicleImportText.component("button.export_item"),
                b -> exportItem(), x, y, EXPORT_ITEM_BUTTON_WIDTH, 20);
        exportItemButton.active = !exportingItem;
        addRenderableWidget(exportItemButton);
    }

    private void addSeatControls(int x, int y) {
        seatIndex = Math.max(0, Math.min(seatIndex, draft.seats.size() - 1));
        addListHeader(x, y, "header.seat", seatIndex, draft.seats.size(),
                VehiclePositionDropdown.Type.SEAT, this::seatPositionDropdownLabel,
                index -> seatIndex = index,
                () -> { draft.seats.add(VehicleEditorDraft.defaultSeatPosition()); seatIndex = draft.seats.size() - 1; },
                () -> { if (draft.seats.size() > 1) draft.seats.remove(seatIndex); seatIndex = Math.max(0, seatIndex - 1); }); y += 28;
        Vec3 seat = draft.seats.get(seatIndex);
        vectorFields(x, y, seat, v -> draft.seats.set(seatIndex, v)); y += CONTROL_ROW_STEP * 3;
        labels.add(new FieldLabel("label.preview_view", x, y + 6, FIELD_LABEL_WIDTH));
        addParameterTooltip(x, y, PARAM_WIDTH, VehicleImportText.component("tooltip.preview_view"));
        addRenderableWidget(texturedButton(VehicleImportText.component(seatFirstPerson ? "option.first_person" : "option.external"), button -> {
            seatFirstPerson = !seatFirstPerson;
            previewRenderer.resetView(true);
            button.setMessage(VehicleImportText.component(seatFirstPerson ? "option.first_person" : "option.external"));
        }, x + 70, y, PARAM_WIDTH - 70, 20));
    }

    private void addAttachmentControls(int x, int y) {
        labeledNumber(x, y, "label.front_attachment_position", 1.0F,
                () -> draft.frontAttachmentPos, value -> draft.frontAttachmentPos = value,
                draft.frontAttachmentEnabled);
        y += CONTROL_ROW_STEP;
        attachmentToggleRow(x, y,
                "label.front_attachment", "disabled", "enabled",
                () -> draft.frontAttachmentEnabled,
                () -> {
                    draft.frontAttachmentEnabled = !draft.frontAttachmentEnabled;
                    resetWidgets();
                },
                "label.front_list", "blacklist", "whitelist",
                () -> draft.frontAttachmentWhitelistMode,
                () -> draft.frontAttachmentWhitelistMode = !draft.frontAttachmentWhitelistMode,
                draft.frontAttachmentEnabled);
        y += CONTROL_ROW_STEP;
        attachmentListField(x, y, "label.front_list", draft.frontAttachmentListText,
                SelectionType.FRONT_ATTACHMENTS, draft.frontAttachmentEnabled);
        y += CONTROL_ROW_STEP;
        labeledNumber(x, y, "label.rear_attachment_position", 1.0F,
                () -> draft.rearAttachmentPos, value -> draft.rearAttachmentPos = value,
                draft.rearAttachmentEnabled);
        y += CONTROL_ROW_STEP;
        attachmentToggleRow(x, y,
                "label.rear_attachment", "disabled", "enabled",
                () -> draft.rearAttachmentEnabled,
                () -> {
                    draft.rearAttachmentEnabled = !draft.rearAttachmentEnabled;
                    resetWidgets();
                },
                "label.rear_list", "blacklist", "whitelist",
                () -> draft.rearAttachmentWhitelistMode,
                () -> draft.rearAttachmentWhitelistMode = !draft.rearAttachmentWhitelistMode,
                draft.rearAttachmentEnabled);
        y += CONTROL_ROW_STEP;
        attachmentListField(x, y, "label.rear_list", draft.rearAttachmentListText,
                SelectionType.REAR_ATTACHMENTS, draft.rearAttachmentEnabled);
    }

    private void addHitboxControls(int x, int y) {
        hitboxIndex = Math.max(-1, Math.min(hitboxIndex, draft.hitboxes.size() - 1));
        addCollisionHeader(x, y);
        y += 28;
        if (hitboxIndex < 0) {
            labeledNumber(x, y, "label.entity_width", 0.0625F,
                    () -> draft.widthBlocks, value -> draft.widthBlocks = value);
            y += CONTROL_ROW_STEP;
            labeledNumber(x, y, "label.entity_height", 0.0625F,
                    () -> draft.heightBlocks, value -> draft.heightBlocks = value);
            return;
        }

        VehicleEditorDraft.HitboxPoint point = draft.hitboxes.get(hitboxIndex);
        vectorFields(x, y, point.origin(), v -> replaceHitbox(new VehicleEditorDraft.HitboxPoint(v, currentHitbox().width(), currentHitbox().height(), currentHitbox().hasContainer()))); y += DENSE_CONTROL_ROW_STEP * 3;
        labeledNumber(x, y, "label.width", 0.0625F, () -> currentHitbox().width(), v -> replaceHitbox(new VehicleEditorDraft.HitboxPoint(currentHitbox().origin(), v, currentHitbox().height(), currentHitbox().hasContainer()))); y += DENSE_CONTROL_ROW_STEP;
        labeledNumber(x, y, "label.height", 0.0625F, () -> currentHitbox().height(), v -> replaceHitbox(new VehicleEditorDraft.HitboxPoint(currentHitbox().origin(), currentHitbox().width(), v, currentHitbox().hasContainer()))); y += DENSE_CONTROL_ROW_STEP;
        binaryToggle(x, y, "label.container_hitbox", "off", "on", () -> currentHitbox().hasContainer(), () -> {
            var hitbox = currentHitbox();
            replaceHitbox(new VehicleEditorDraft.HitboxPoint(
                    hitbox.origin(), hitbox.width(), hitbox.height(), !hitbox.hasContainer()));
        });
    }

    private void addCollisionHeader(int x, int y) {
        Component name = hitboxIndex < 0
                ? VehicleImportText.component("header.entity_hitbox")
                : VehicleImportText.component("header.additional_hitbox", hitboxIndex + 1, draft.hitboxes.size());
        int headerWidth = PARAM_WIDTH - 48;
        addParameterTooltip(x, y, PARAM_WIDTH, VehicleImportText.component("tooltip.collision"));
        addRenderableWidget(texturedButton(name, button -> togglePositionDropdown(
                VehiclePositionDropdown.Type.COLLISION, x, y, headerWidth, draft.hitboxes.size() + 1,
                hitboxIndex + 1, this::collisionDropdownLabel, selected -> {
                    closePositionDropdown(() -> {
                        hitboxIndex = selected - 1;
                        resetWidgets();
                    });
                }), x, y, headerWidth, 20));
        addRenderableWidget(texturedButton(Component.literal("+"), button -> {
            positionDropdown = null;
            draft.hitboxes.add(new VehicleEditorDraft.HitboxPoint(
                    Vec3.ZERO, draft.widthBlocks, draft.heightBlocks, false));
            hitboxIndex = draft.hitboxes.size() - 1;
            resetWidgets();
        }, x + PARAM_WIDTH - 44, y, 20, 20));
        Button remove = texturedButton(Component.literal("-"), button -> {
            positionDropdown = null;
            draft.hitboxes.remove(hitboxIndex);
            hitboxIndex = draft.hitboxes.isEmpty() ? -1 : Math.min(hitboxIndex, draft.hitboxes.size() - 1);
            resetWidgets();
        }, x + PARAM_WIDTH - 20, y, 20, 20);
        remove.active = hitboxIndex >= 0;
        addRenderableWidget(remove);
    }

    private void addListHeader(int x, int y, String name, int index, int size,
                               VehiclePositionDropdown.Type type, IntFunction<Component> optionLabel,
                               Consumer<Integer> select, Runnable add, Runnable remove) {
        int headerWidth = PARAM_WIDTH - 48;
        addParameterTooltip(x, y, PARAM_WIDTH, VehicleImportText.component("tooltip." + type.name().toLowerCase(Locale.ROOT)));
        addRenderableWidget(texturedButton(VehicleImportText.component(name, index + 1, size), b ->
                togglePositionDropdown(type, x, y, headerWidth, size, index, optionLabel, selected -> {
                    closePositionDropdown(() -> {
                        select.accept(selected);
                        resetWidgets();
                    });
                }), x, y, headerWidth, 20));
        addRenderableWidget(texturedButton(Component.literal("+"), b -> {
            positionDropdown = null;
            add.run();
            resetWidgets();
        }, x + PARAM_WIDTH - 44, y, 20, 20));
        addRenderableWidget(texturedButton(Component.literal("-"), b -> {
            positionDropdown = null;
            remove.run();
            resetWidgets();
        }, x + PARAM_WIDTH - 20, y, 20, 20));
    }

    private void togglePositionDropdown(VehiclePositionDropdown.Type type, int x, int y, int width,
                                        int size, int selectedIndex, IntFunction<Component> optionLabel,
                                        Consumer<Integer> select) {
        if (positionDropdown != null && positionDropdown.type() == type) {
            closePositionDropdown();
            return;
        }
        int initialScroll = switch (type) {
            case WHEEL -> wheelPositionDropdownScroll;
            case SEAT -> seatPositionDropdownScroll;
            case COLLISION -> collisionDropdownScroll;
        };
        positionDropdown = new VehiclePositionDropdown(type, x, y, width, size, selectedIndex,
                optionLabel, select, initialScroll, scroll -> {
                    switch (type) {
                        case WHEEL -> wheelPositionDropdownScroll = scroll;
                        case SEAT -> seatPositionDropdownScroll = scroll;
                        case COLLISION -> collisionDropdownScroll = scroll;
                    }
                });
    }

    private void closePositionDropdown() {
        closePositionDropdown(null);
    }

    private void closePositionDropdown(Runnable afterClose) {
        if (positionDropdown == null) {
            if (afterClose != null) afterClose.run();
            return;
        }
        positionDropdown.close(afterClose);
    }

    private Component wheelPositionDropdownLabel(int index) {
        VehicleEditorDraft.WheelPoint point = draft.wheelPoints.get(index);
        return VehicleImportText.component("position.wheel", index + 1, point.forward(), point.right());
    }

    private Component seatPositionDropdownLabel(int index) {
        Vec3 seat = draft.seats.get(index);
        return VehicleImportText.component("position.seat", index + 1, seat.x, seat.y, seat.z);
    }

    private Component collisionDropdownLabel(int index) {
        if (index == 0) return VehicleImportText.component("header.entity_hitbox");
        VehicleEditorDraft.HitboxPoint hitbox = draft.hitboxes.get(index - 1);
        Vec3 origin = hitbox.origin();
        return VehicleImportText.component("position.additional_hitbox", index, origin.x, origin.y, origin.z);
    }

    private void vectorFields(int x, int y, Vec3 initial, Consumer<Vec3> setter) {
        final double[] values = {initial.x, initial.y, initial.z};
        String[] names = {"label.x", "label.y", "label.z"};
        for (int i = 0; i < 3; i++) {
            int index = i;
            labeledNumber(x, y + i * CONTROL_ROW_STEP, names[i], 0.0625F,
                    () -> (float) values[index], v -> { values[index] = v; setter.accept(new Vec3(values[0], values[1], values[2])); });
        }
    }

    private VehicleEditorDraft.HitboxPoint currentHitbox() { return draft.hitboxes.get(hitboxIndex); }
    private void replaceHitbox(VehicleEditorDraft.HitboxPoint value) { draft.hitboxes.set(hitboxIndex, value); }
    private VehicleEditorDraft.WheelPoint currentWheelPoint() { return draft.wheelPoints.get(wheelPointIndex); }
    private void replaceWheelPoint(VehicleEditorDraft.WheelPoint value) { draft.wheelPoints.set(wheelPointIndex, value); }
    private boolean isSeatFirstPersonView() { return page == Page.FRAME && frameTab == FrameTab.SEATS && seatFirstPerson; }
    private boolean isPerspectiveView() { return isSeatFirstPersonView(); }

    private void addParameterTooltip(int x, int y, int width, Component description) {
        parameterTooltips.add(new VehicleImportTooltips.Area(x, y, width, 20, description));
    }

    private void binaryToggle(int x, int y, String label, String falseLabel, String trueLabel,
                              BooleanSupplier getter, Runnable toggle) {
        labels.add(new FieldLabel(label, x, y + 6, FIELD_LABEL_WIDTH));
        addParameterTooltip(x, y, PARAM_WIDTH, VehicleImportTooltips.toggle(label));
        addRenderableWidget(new VehicleToggleSliderButton(x + 70, y, PARAM_WIDTH - 70, 20,
                label, falseLabel, trueLabel, getter, toggle));
    }

    private void attachmentToggleRow(int x, int y,
                                     String firstLabel, String firstFalseLabel, String firstTrueLabel,
                                     BooleanSupplier firstGetter, Runnable firstToggle,
                                     String secondLabel, String secondFalseLabel, String secondTrueLabel,
                                     BooleanSupplier secondGetter, Runnable secondToggle,
                                     boolean secondActive) {
        int gap = 4;
        int groupWidth = (PARAM_WIDTH - gap) / 2;
        compactBinaryToggle(x, y, groupWidth, firstLabel, firstFalseLabel, firstTrueLabel,
                firstGetter, firstToggle, true);
        compactBinaryToggle(x + groupWidth + gap, y, PARAM_WIDTH - groupWidth - gap,
                secondLabel, secondFalseLabel, secondTrueLabel, secondGetter, secondToggle, secondActive);
    }

    private void compactBinaryToggle(int x, int y, int width, String label,
                                     String falseLabel, String trueLabel,
                                     BooleanSupplier getter, Runnable toggle, boolean active) {
        int labelWidth = 44;
        labels.add(new FieldLabel(label, x, y + 6, labelWidth - 4));
        addParameterTooltip(x, y, width, VehicleImportTooltips.toggle(label));
        VehicleToggleSliderButton button = new VehicleToggleSliderButton(x + labelWidth, y,
                width - labelWidth, 20, label, falseLabel, trueLabel, getter, toggle,
                COMPACT_TOGGLE_TEXT_SCALE);
        button.active = active;
        addRenderableWidget(button);
    }

    private void labeledText(int x, int y, String label, String value, Consumer<String> setter) {
        labels.add(new FieldLabel(label, x, y + 6, FIELD_LABEL_WIDTH));
        addParameterTooltip(x, y, PARAM_WIDTH, VehicleImportTooltips.text(label.substring("label.".length())));
        int fieldX = x + 70;
        EditBox field = new VehicleEditBox(font, fieldX, y, PARAM_WIDTH - 70, 20,
                VehicleImportText.component(label));
        field.setMaxLength(1024); field.setValue(value); field.setResponder(setter); addRenderableWidget(field);
    }

    private void attachmentListField(int x, int y, String label, String value, SelectionType type,
                                     boolean active) {
        labels.add(new FieldLabel(label, x, y + 6, FIELD_LABEL_WIDTH));
        addParameterTooltip(x, y, PARAM_WIDTH, VehicleImportText.component(type == SelectionType.FRONT_ATTACHMENTS
                ? "tooltip.front_list" : "tooltip.rear_list"));
        int fieldX = x + 70;
        int buttonWidth = 58;
        int gap = 4;
        VehicleAttachmentIconList iconList = new VehicleAttachmentIconList(
                fieldX, y, PARAM_WIDTH - 70 - buttonWidth - gap, 20,
                VehicleImportText.component(label), attachmentListStacks(type, value),
                type == SelectionType.FRONT_ATTACHMENTS
                        ? frontAttachmentListScroll : rearAttachmentListScroll,
                scroll -> {
                    if (type == SelectionType.FRONT_ATTACHMENTS) frontAttachmentListScroll = scroll;
                    else rearAttachmentListScroll = scroll;
                });
        attachmentIconLists.add(iconList);
        addRenderableWidget(iconList);
        VehicleTexturedButton selectButton = texturedButton(VehicleImportText.component("button.select_list"),
                b -> openAttachmentSelection(type),
                x + PARAM_WIDTH - buttonWidth, y, buttonWidth, 20);
        selectButton.active = active;
        addRenderableWidget(selectButton);
    }

    private List<ItemStack> attachmentListStacks(SelectionType type, String value) {
        List<ResourceLocation> selected;
        try {
            selected = VehicleEditorDraft.parseResourceLocations(value);
        } catch (IllegalArgumentException exception) {
            return List.of();
        }

        return components.attachmentStacks(type.catalogKind, selected);
    }

    private void labeledNumber(int x, int y, String label, float step, Supplier<Float> getter, Consumer<Float> setter) {
        labeledNumber(x, y, label, PARAM_WIDTH - 70, step, getter, setter, true);
    }

    private void labeledNumber(int x, int y, String label, float step, Supplier<Float> getter,
                               Consumer<Float> setter, boolean active) {
        labeledNumber(x, y, label, PARAM_WIDTH - 70, step, getter, setter, active);
    }

    private void labeledNumber(int x, int y, String label, int fieldWidth, float step,
                               Supplier<Float> getter, Consumer<Float> setter) {
        labeledNumber(x, y, label, fieldWidth, step, getter, setter, true);
    }

    private void labeledNumber(int x, int y, String label, int fieldWidth, float step,
                               Supplier<Float> getter, Consumer<Float> setter, boolean active) {
        labels.add(new FieldLabel(label, x, y + 6, FIELD_LABEL_WIDTH));
        addParameterTooltip(x, y, Math.min(PARAM_WIDTH, 70 + fieldWidth),
                VehicleImportTooltips.number(label.substring("label.".length()), page == Page.WHEEL, frameTab == FrameTab.SEATS));
        int fieldX = x + 70;
        EditBox field = new VehicleEditBox(font, fieldX + NUMBER_ARROW_WIDTH, y,
                fieldWidth - NUMBER_ARROW_WIDTH * 2, 20, VehicleImportText.component(label));
        field.setValue(Float.toString(getter.get()));
        field.setResponder(value -> { try { setter.accept(Float.parseFloat(value)); status = ""; } catch (NumberFormatException e) { status = VehicleImportText.string("status.invalid_number"); } });
        VehicleNumberArrowButton decrease = new VehicleNumberArrowButton(fieldX, y, NUMBER_ARROW_WIDTH, 20,
                Component.literal("<"), () -> nudgeNumber(field, getter, -effectiveNumberStep(step)));
        VehicleNumberArrowButton increase = new VehicleNumberArrowButton(fieldX + fieldWidth - NUMBER_ARROW_WIDTH, y,
                NUMBER_ARROW_WIDTH, 20, Component.literal(">"), () -> nudgeNumber(field, getter, effectiveNumberStep(step)));
        field.active = active;
        decrease.active = active;
        increase.active = active;
        addRenderableWidget(decrease);
        addRenderableWidget(increase);
        addRenderableWidget(field);
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
        String chosen;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(2);
            filters.put(stack.UTF8("*.bbmodel"));
            filters.put(stack.UTF8("*.riauto"));
            filters.flip();
            chosen = TinyFileDialogs.tinyfd_openFileDialog(
                    VehicleImportText.string("dialog.open_file"), "",
                    filters, VehicleImportText.string("dialog.import_filter"), false);
        }
        if (chosen == null) return;
        Path selected = Path.of(chosen);
        String fileName = selected.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".bbmodel")) {
            replaceExtractedImportFile(draft.target, null);
            draft.setModelFile(draft.target, selected);
            loadPreview();
            return;
        }
        if (!fileName.endsWith(CarPackManager.CAR_PACK_EXTENSION)) {
            status = VehicleImportText.string("status.file_only");
            return;
        }
        try {
            Path extractionDirectory = CarPackManager.getRootDirectory().resolve("cache").resolve("editor").resolve("imports");
            VehiclePackImporter.ImportedComponent imported =
                    VehiclePackImporter.importComponent(selected, extractionDirectory);
            imported.applyTo(draft);
            replaceExtractedImportFile(imported.target(), imported.modelFile());
            page = Page.forTarget(imported.target());
            setTargetForPage();
            resetWidgets();
            loadPreview();
        } catch (IOException exception) {
            status = VehicleImportText.string("status.import_failed", exception.getMessage());
        }
    }

    private void replaceExtractedImportFile(VehicleEditorDraft.Target target, Path replacement) {
        Path previous = replacement == null ? extractedImportFiles.remove(target)
                : extractedImportFiles.put(target, replacement);
        if (previous == null || previous.equals(replacement)) return;
        try {
            Files.deleteIfExists(previous);
        } catch (IOException ignored) {
        }
    }

    private void loadPreview() {
        status = VehicleImportText.string("status.loading_preview");
        try {
            previewSession.load(draft).whenComplete((unused, error) -> Minecraft.getInstance().execute(() -> {
                status = error == null ? VehicleImportText.string("status.preview_loaded") : VehicleImportText.string("status.preview_failed", rootMessage(error));
                if (error == null && Minecraft.getInstance().screen == this) resetWidgets();
            }));
        } catch (IOException e) { status = VehicleImportText.string("status.preview_failed", e.getMessage()); }
    }

    private void exportPack() {
        try {
            Path defaultDirectory = CarPackManager.getRootDirectory().resolve("exports").toAbsolutePath();
            Files.createDirectories(defaultDirectory);
            String fileStem = exportFileStem(draft.displayName());
            Path defaultPath = nextAvailableExportPath(defaultDirectory, fileStem);
            String selected = TinyFileDialogs.tinyfd_saveFileDialog(
                    VehicleImportText.string("dialog.export_pack"), defaultPath.toString(), null, VehicleImportText.string("dialog.export_filter"));
            if (selected == null) return;

            Path selectedPath = Path.of(selected).toAbsolutePath().normalize();
            Path directory = selectedPath.getParent() == null ? defaultDirectory : selectedPath.getParent();
            Path archive = VehiclePackBuilder.build(draft, nextAvailableExportPath(directory, fileStem), false);
            status = VehicleImportText.string("status.exported", archive.getFileName());
        } catch (IOException e) { status = VehicleImportText.string("status.export_failed", e.getMessage()); }
    }

    static String exportFileStem(String displayName) {
        String source = displayName == null ? "" : displayName.strip();
        StringBuilder sanitized = new StringBuilder(source.length());
        String invalid = "<>:\"/\\|?*";
        for (int index = 0; index < source.length(); index++) {
            char value = source.charAt(index);
            sanitized.append(Character.isISOControl(value) || invalid.indexOf(value) >= 0 ? '_' : value);
        }
        while (!sanitized.isEmpty()) {
            char last = sanitized.charAt(sanitized.length() - 1);
            if (last != '.' && last != ' ') break;
            sanitized.setLength(sanitized.length() - 1);
        }
        String result = sanitized.isEmpty() ? "vehicle" : sanitized.toString();
        if (result.matches("(?i)^(con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\\..*)?$")) {
            result = "_" + result;
        }
        return result;
    }

    static Path nextAvailableExportPath(Path directory, String fileStem) {
        String extension = CarPackManager.CAR_PACK_EXTENSION;
        Path candidate = directory.resolve(fileStem + extension);
        for (int suffix = 1; Files.exists(candidate); suffix++) {
            candidate = directory.resolve(fileStem + " (" + suffix + ")" + extension);
        }
        return candidate;
    }

    private void exportItem() {
        if (menu.hasOutputItem()) {
            status = VehicleImportText.string("status.output_not_empty");
            return;
        }
        if (!menu.canPublish()) {
            status = VehicleImportText.string("status.export_permission_required");
            return;
        }
        draft.target = page.target;
        VehicleEditorDraft.Target exportedTarget = draft.target;
        var componentId = draft.componentId();
        String exportedDisplayName = draft.displayName();
        int containerId = menu.containerId;
        exportingItem = true;
        if (exportItemButton != null) exportItemButton.active = false;
        status = VehicleImportText.string("status.installing_component");
        try {
            Path directory = CarPackManager.getRootDirectory().resolve("exports");
            Path archive = VehiclePackBuilder.build(draft,
                    directory.resolve(draft.packName() + CarPackManager.CAR_PACK_EXTENSION), false);
            ClientCarPackUploader.upload(archive, draft, result -> Minecraft.getInstance().execute(() -> {
                if (!result.successful()) {
                    exportingItem = false;
                    if (exportItemButton != null) exportItemButton.active = true;
                    status = VehicleImportText.string("status.item_export_failed", result.detail());
                    return;
                }
                status = VehicleImportText.string("status.component_installed_syncing");
                ClientCarPackSynchronizer.runWhenReady(() -> {
                    exportingItem = false;
                    if (exportItemButton != null) exportItemButton.active = true;
                    if (Minecraft.getInstance().screen != this || menu.containerId != containerId) return;
                    RIAutomobilityNetwork.CHANNEL.sendToServer(new ExportVehicleComponentItemPacket(
                            containerId, exportedTarget.path, componentId, exportedDisplayName));
                    status = VehicleImportText.string("status.item_exported");
                });
            }));
        } catch (IOException exception) {
            exportingItem = false;
            if (exportItemButton != null) exportItemButton.active = true;
            status = VehicleImportText.string("status.item_export_failed", exception.getMessage());
        }
    }

    private void openSelection(SelectionType type) {
        selectionType = type;
        selectionPage = 0;
        menu.setSlotsActive(false);
        resetWidgets();
    }
    private void openAttachmentSelection(SelectionType type) {
        try {
            currentAttachmentSelection(type);
            status = "";
            openSelection(type);
        } catch (IllegalArgumentException exception) {
            status = VehicleImportText.string("status.invalid_resource_id", exception.getMessage());
        }
    }
    private void closeSelection() {
        selectionType = null;
        selectionPage = 0;
        menu.setSlotsActive(true);
        resetWidgets();
    }

    private void addSelectionControls() {
        addRenderableWidget(texturedButton(VehicleImportText.component("button.back"),
                b -> closeSelection(), leftPos + 8, topPos + 24, 54, 20));
        int selectionsPerPage = selectionsPerPage();
        int count = selectionCount(); int pageCount = Math.max(1, (count + selectionsPerPage - 1) / selectionsPerPage);
        selectionPage = Math.max(0, Math.min(selectionPage, pageCount - 1));
        int start = selectionPage * selectionsPerPage;
        int gridWidth = SELECTION_COLUMNS * SELECTION_CELL_SIZE + (SELECTION_COLUMNS - 1) * SELECTION_GAP;
        int gridX = leftPos + (imageWidth - gridWidth) / 2;
        int gridY = topPos + 50;
        for (int offset = 0; offset < selectionsPerPage && start + offset < count; offset++) {
            int index = start + offset;
            VehicleComponentIconButton entry = new VehicleComponentIconButton(
                    gridX + (offset % SELECTION_COLUMNS) * (SELECTION_CELL_SIZE + SELECTION_GAP),
                    gridY + (offset / SELECTION_COLUMNS) * (SELECTION_CELL_SIZE + SELECTION_GAP),
                    selectionStack(index), selectionIsCurrent(index), () -> selectComponent(index));
            selectionButtons.add(entry); addRenderableWidget(entry);
        }
        int navY = selectionNavY();
        Button prev = texturedButton(Component.literal("<"), b -> { selectionPage--; resetWidgets(); },
                leftPos + imageWidth / 2 - 66, navY, 28, 20); prev.active = selectionPage > 0; addRenderableWidget(prev);
        Button next = texturedButton(Component.literal(">"), b -> { selectionPage++; resetWidgets(); },
                leftPos + imageWidth / 2 + 38, navY, 28, 20); next.active = selectionPage + 1 < pageCount; addRenderableWidget(next);
        if (selectionType.isAttachmentList()) {
            addRenderableWidget(texturedButton(VehicleImportText.component("button.select_all"),
                    b -> selectAllAttachments(), leftPos + 8, navY, 54, 20));
            addRenderableWidget(texturedButton(VehicleImportText.component("button.clear"),
                    b -> clearAttachmentSelection(), leftPos + 66, navY, 54, 20));
        }
    }

    private int selectionsPerPage() {
        int gridY = topPos + 50;
        int availableHeight = selectionNavY() - gridY - SELECTION_GAP;
        int rows = Math.max(1, availableHeight / (SELECTION_CELL_SIZE + SELECTION_GAP));
        return SELECTION_COLUMNS * rows;
    }

    private int selectionNavY() {
        return topPos + imageHeight - 29;
    }

    private int selectionCount() { return components.count(selectionType.catalogKind); }
    private ItemStack selectionStack(int i) { return components.stack(selectionType.catalogKind, i); }
    private boolean selectionIsCurrent(int i) { return switch (selectionType) {
        case FRAME -> components.frame(i) == draft.selectedFrame;
        case WHEEL -> components.wheel(i) == draft.selectedWheel;
        case ENGINE -> components.engine(i) == draft.selectedEngine;
        case FRONT_ATTACHMENTS, REAR_ATTACHMENTS -> currentAttachmentSelection(selectionType)
                .contains(attachmentId(selectionType, i));
    }; }
    private void selectComponent(int i) {
        switch (selectionType) {
            case FRAME -> {
                draft.loadFrame(components.frame(i));
                draft.setPreviewReady(VehicleEditorDraft.Target.FRAME, false);
                draft.showPart(VehicleEditorDraft.Target.FRAME);
            }
            case WHEEL -> { draft.loadWheel(components.wheel(i)); draft.setPreviewReady(VehicleEditorDraft.Target.WHEEL, false); draft.showPart(VehicleEditorDraft.Target.WHEEL); }
            case ENGINE -> { draft.loadEngine(components.engine(i)); draft.setPreviewReady(VehicleEditorDraft.Target.ENGINE, false); draft.showPart(VehicleEditorDraft.Target.ENGINE); }
            case FRONT_ATTACHMENTS, REAR_ATTACHMENTS -> {
                List<ResourceLocation> selected = currentAttachmentSelection(selectionType);
                ResourceLocation id = attachmentId(selectionType, i);
                if (!selected.remove(id)) selected.add(id);
                setAttachmentSelection(selectionType, selected);
                resetWidgets();
                return;
            }
        }
        closeSelection();
    }

    private List<ResourceLocation> currentAttachmentSelection(SelectionType type) {
        String value = type == SelectionType.FRONT_ATTACHMENTS
                ? draft.frontAttachmentListText : draft.rearAttachmentListText;
        return new ArrayList<>(VehicleEditorDraft.parseResourceLocations(value));
    }

    private ResourceLocation attachmentId(SelectionType type, int index) {
        return components.id(type.catalogKind, index);
    }

    private void setAttachmentSelection(SelectionType type, List<ResourceLocation> selected) {
        String value = String.join(", ", selected.stream().map(ResourceLocation::toString).toList());
        if (type == SelectionType.FRONT_ATTACHMENTS) draft.frontAttachmentListText = value;
        else draft.rearAttachmentListText = value;
    }

    private void selectAllAttachments() {
        List<ResourceLocation> selected = new ArrayList<>();
        for (int index = 0; index < selectionCount(); index++) selected.add(attachmentId(selectionType, index));
        setAttachmentSelection(selectionType, selected);
        resetWidgets();
    }

    private void clearAttachmentSelection() {
        setAttachmentSelection(selectionType, List.of());
        resetWidgets();
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        VehicleGuiTextures.blitNineSliced(g, VehicleImportGuiAtlas.Sprite.SCREEN,
                leftPos, topPos, imageWidth, imageHeight);
        if (selectionType != null) {
            VehicleGuiTextures.blitNineSliced(g, VehicleImportGuiAtlas.Sprite.SELECTION,
                    leftPos + 5, topPos + 22, imageWidth - 10, imageHeight - 27);
        } else {
            VehicleGuiTextures.blitNineSliced(g, VehicleImportGuiAtlas.Sprite.SIDEBAR,
                    leftPos + 4, topPos + 22, TAB_WIDTH - 4, imageHeight - 27);
            VehicleGuiTextures.blitNineSliced(g, VehicleImportGuiAtlas.Sprite.CONTROLS,
                    leftPos + TAB_WIDTH + 4, topPos + 22, PARAM_WIDTH + 8, imageHeight - 27);
            VehicleGuiTextures.blitNineSliced(g, VehicleImportGuiAtlas.Sprite.PREVIEW,
                    previewX0(), topPos + 22, leftPos + imageWidth - 5 - previewX0(), imageHeight - 27);
            if (hasCurrentPartPreview()) renderVehicle(g, partialTick);
        }
        if (selectionType == null) renderInventoryBackground(g);
    }

    private void renderInventoryBackground(GuiGraphics g) {
        int panelX0 = leftPos + VehicleImportMenu.INVENTORY_X - 4;
        int panelY0 = topPos + VehicleImportMenu.INVENTORY_Y - 17;
        int panelX1 = leftPos + VehicleImportMenu.OUTPUT_SLOT_X + 8
                + EXPORT_ITEM_BUTTON_WIDTH / 2 + 4;
        int panelY1 = topPos + VehicleImportMenu.INVENTORY_Y + 77;
        VehicleGuiTextures.blitNineSliced(g, VehicleImportGuiAtlas.Sprite.INVENTORY,
                panelX0, panelY0, panelX1 - panelX0, panelY1 - panelY0);
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
        VehicleGuiTextures.blit(g, output ? VehicleImportGuiAtlas.Sprite.SLOT_OUTPUT
                : VehicleImportGuiAtlas.Sprite.SLOT_NORMAL, x - 1, y - 1, 18, 18);
    }

    private void renderVehicle(GuiGraphics graphics, float partialTick) {
        previewRenderer.render(graphics, partialTick,
                previewX0(), leftPos + imageWidth - 5, topPos + 22, topPos + imageHeight - 5,
                previewView(), wheelPointIndex, seatIndex, hitboxIndex);
    }

    private VehiclePreviewRenderer.View previewView() {
        if (isSeatFirstPersonView()) return VehiclePreviewRenderer.View.SEAT_FIRST_PERSON;
        return switch (page) {
            case WHEEL -> VehiclePreviewRenderer.View.WHEEL;
            case ENGINE -> VehiclePreviewRenderer.View.ENGINE;
            case FRAME -> switch (frameTab) {
                case WHEELS -> VehiclePreviewRenderer.View.FRAME_WHEELS;
                case SEATS -> VehiclePreviewRenderer.View.FRAME_SEATS;
                case HITBOXES -> VehiclePreviewRenderer.View.FRAME_HITBOXES;
                case BASIC -> VehiclePreviewRenderer.View.FRAME;
                case ATTACHMENTS -> VehiclePreviewRenderer.View.FRAME_ATTACHMENTS;
            };
        };
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g); super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, title, leftPos + 8, topPos + 8, 0xFFFFFF, false);
        if (selectionType != null) {
            g.drawCenteredString(font, VehicleImportText.component("selection.title", VehicleImportText.component(selectionType.label)), leftPos + imageWidth / 2, topPos + 31, 0xFFFFFF);
            int selectionsPerPage = selectionsPerPage();
            int pageCount = Math.max(1, (selectionCount() + selectionsPerPage - 1) / selectionsPerPage);
            g.drawCenteredString(font, (selectionPage + 1) + " / " + pageCount,
                    leftPos + imageWidth / 2, selectionNavY() + 6, 0xB9C0C8);
            for (VehicleComponentIconButton b : selectionButtons) if (b.isHovered()) { g.renderTooltip(font, b.stack(), mouseX, mouseY); break; }
        } else {
            int labelColor = hasCurrentPartPreview() ? 0xD8DEE8 : 0x777D86;
            labels.forEach(label -> VehicleScrollingText.renderLeftAligned(g, font,
                    VehicleImportText.component(label.text), label.x, label.y, label.width, labelColor));
            if (!hasCurrentPartPreview()) g.drawCenteredString(font, VehicleImportText.component("message.no_preview"), (previewX0() + leftPos + imageWidth - 5) / 2, topPos + imageHeight / 2, 0x8F98A5);
            if (!status.isBlank()) g.drawString(font, font.plainSubstrByWidth(status, imageWidth - font.width(title) - 28), leftPos + font.width(title) + 18, topPos + 8, 0xC9D1D9, false);
        }
        if (selectionType == null) {
            g.drawString(font, VehicleImportText.component("label.inventory"), leftPos + VehicleImportMenu.INVENTORY_X,
                    topPos + VehicleImportMenu.INVENTORY_Y - 13, 0xD8DEE8, false);
            g.drawCenteredString(font, VehicleImportText.component("label.output"), leftPos + VehicleImportMenu.OUTPUT_SLOT_X + 8,
                    topPos + VehicleImportMenu.OUTPUT_SLOT_Y - 12, 0xD8DEE8);
        }
        if (positionDropdown != null) {
            VehiclePositionDropdown renderedDropdown = positionDropdown;
            renderedDropdown.render(g, mouseX, mouseY);
            if (renderedDropdown.isClosed()) {
                positionDropdown = null;
                renderedDropdown.finishClose();
            }
        }
        renderTooltip(g, mouseX, mouseY);
        boolean renderedAttachmentTooltip = false;
        for (VehicleAttachmentIconList iconList : attachmentIconLists) {
            ItemStack hovered = iconList.hoveredStack(mouseX, mouseY);
            if (!hovered.isEmpty()) {
                g.renderTooltip(font, hovered, mouseX, mouseY);
                renderedAttachmentTooltip = true;
                break;
            }
        }
        if (!renderedAttachmentTooltip && positionDropdown == null) {
            for (VehicleImportTooltips.Area tooltip : parameterTooltips) {
                if (tooltip.contains(mouseX, mouseY)) {
                    g.renderTooltip(font, font.split(tooltip.description(), 240), mouseX, mouseY);
                    break;
                }
            }
        }
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}
    private int previewX0() { return leftPos + TAB_WIDTH + PARAM_WIDTH + 16; }
    private boolean inPreview(double x, double y) { return selectionType == null && x >= previewX0() && x < leftPos + imageWidth - 5 && y >= topPos + 22 && y < topPos + imageHeight - 5; }
    private void resetView() { previewRenderer.resetView(isSeatFirstPersonView()); }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (positionDropdown != null) {
            if (positionDropdown.isClosing()) return true;
            if (positionDropdown.isOverHeader(x, y)) {
                closePositionDropdown();
                return true;
            }
            if (positionDropdown.mouseClicked(x, y, button)) return true;
            closePositionDropdown();
            return true;
        }
        if (inPreview(x, y)) {
            // Widgets drawn over the preview (notably reset-view) keep priority,
            // but the container screen must not consume the empty preview area.
            if (getChildAt(x, y).isPresent() && super.mouseClicked(x, y, button)) return true;
            if (previewRenderer.beginDrag(x, y, button, isPerspectiveView())) return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (positionDropdown != null && positionDropdown.isClosing()) return true;
        if (positionDropdown != null && positionDropdown.mouseDragged(x, y, button)) return true;
        if (previewRenderer.mouseDragged(x, y, button, isSeatFirstPersonView())) return true;
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (positionDropdown != null && positionDropdown.mouseReleased(button)) return true;
        previewRenderer.mouseReleased(button);
        return super.mouseReleased(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double amount) {
        if (positionDropdown != null && positionDropdown.isClosing()) return true;
        if (positionDropdown != null && positionDropdown.mouseScrolled(x, y, amount)) return true;
        if (amount != 0.0D) {
            for (NumberControl control : numberControls) {
                if (control.field().active && control.field().isMouseOver(x, y)) {
                    float direction = amount > 0.0D ? 1.0F : -1.0F;
                    nudgeNumber(control.field(), control.getter(),
                            direction * effectiveNumberStep(control.step()));
                    return true;
                }
            }
        }
        if (inPreview(x, y)) {
            previewRenderer.mouseScrolled(amount, isSeatFirstPersonView());
            return true;
        }
        return super.mouseScrolled(x, y, amount);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && positionDropdown != null) {
            closePositionDropdown();
            return true;
        }
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
    @Override public void removed() {
        previewSession.close();
        for (Path extracted : extractedImportFiles.values()) {
            try {
                Files.deleteIfExists(extracted);
            } catch (IOException ignored) {
            }
        }
        extractedImportFiles.clear();
        super.removed();
    }
    private static VehicleTexturedButton texturedButton(Component message, Button.OnPress onPress,
                                                        int x, int y, int width, int height) {
        return new VehicleTexturedButton(x, y, width, height, message, onPress);
    }
    private void resetWidgets() { clearWidgets(); init(); }
    private static String rootMessage(Throwable error) { Throwable current = error; while (current.getCause() != null) current = current.getCause(); return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage(); }

    private enum Page {
        FRAME("page.frame", VehicleEditorDraft.Target.FRAME), WHEEL("page.wheel", VehicleEditorDraft.Target.WHEEL), ENGINE("page.engine", VehicleEditorDraft.Target.ENGINE);
        final String label; final VehicleEditorDraft.Target target;
        Page(String label, VehicleEditorDraft.Target target) { this.label = label; this.target = target; }

        static Page forTarget(VehicleEditorDraft.Target target) {
            return switch (target) {
                case FRAME -> FRAME;
                case WHEEL -> WHEEL;
                case ENGINE -> ENGINE;
            };
        }
    }
    private enum FrameTab {
        BASIC("tab.basic"), WHEELS("tab.wheels"), SEATS("tab.seats"), HITBOXES("tab.hitboxes"), ATTACHMENTS("tab.attachments");
        final String label;
        FrameTab(String label) { this.label = label; }
        boolean isAvailable(VehicleEditorDraft draft) {
            if (this == BASIC) return true;
            if (!draft.isPartVisible(VehicleEditorDraft.Target.FRAME)) return false;
            return this != WHEELS || draft.isPartVisible(VehicleEditorDraft.Target.WHEEL);
        }
    }
    private enum WheelTab {
        BASIC("tab.basic"), ADVANCED("tab.advanced");
        final String label;
        WheelTab(String label) { this.label = label; }
        boolean isAvailable(VehicleEditorDraft draft) {
            return this == BASIC || draft.isPartVisible(VehicleEditorDraft.Target.WHEEL);
        }
    }
    private enum SelectionType {
        FRAME("page.frame", VehicleComponentCatalog.Kind.FRAME),
        WHEEL("page.wheel", VehicleComponentCatalog.Kind.WHEEL),
        ENGINE("page.engine", VehicleComponentCatalog.Kind.ENGINE),
        FRONT_ATTACHMENTS("selection.front_attachments", VehicleComponentCatalog.Kind.FRONT_ATTACHMENT),
        REAR_ATTACHMENTS("selection.rear_attachments", VehicleComponentCatalog.Kind.REAR_ATTACHMENT);
        final String label;
        final VehicleComponentCatalog.Kind catalogKind;
        SelectionType(String label, VehicleComponentCatalog.Kind catalogKind) {
            this.label = label;
            this.catalogKind = catalogKind;
        }
        boolean isAttachmentList() {
            return this == FRONT_ATTACHMENTS || this == REAR_ATTACHMENTS;
        }
    }
    private record FieldLabel(String text, int x, int y, int width) {}
    private record NumberControl(EditBox field, float step, Supplier<Float> getter) {}
}
