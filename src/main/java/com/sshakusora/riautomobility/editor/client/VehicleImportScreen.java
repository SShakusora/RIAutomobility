package com.sshakusora.riautomobility.editor.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.sshakusora.riautomobility.editor.VehicleImportMenu;
import com.sshakusora.riautomobility.carpack.CarPackManager;
import com.sshakusora.riautomobility.network.packet.client.ClientCarPackUploader;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import io.github.foundationgames.automobility.automobile.render.AutomobileRenderer;
import io.github.foundationgames.automobility.item.AutomobilityItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class VehicleImportScreen extends AbstractContainerScreen<VehicleImportMenu> {
    private final VehicleEditorDraft draft;
    private final PreviewAutomobile preview;
    private final List<AutomobileFrame> frames = new ArrayList<>();
    private final List<AutomobileWheel> wheels = new ArrayList<>();
    private final List<AutomobileEngine> engines = new ArrayList<>();
    private final List<ComponentEntryButton> selectionButtons = new ArrayList<>();
    private int frameIndex;
    private int wheelIndex;
    private int engineIndex;
    private int wheelPointIndex;
    private Page page = Page.MODEL;
    private SelectionType selectionType;
    private int selectionPage;
    private static final int SELECTIONS_PER_PAGE = 12;
    private float rotationX = 18.0F;
    private float rotationY = 35.0F;
    private float zoom = 38.0F;
    private boolean draggingPreview;
    private double lastMouseX;
    private double lastMouseY;
    private String status = "";
    private final ClientVehiclePreviewSession previewSession = new ClientVehiclePreviewSession();

    public VehicleImportScreen(VehicleImportMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        AutomobileFrame.REGISTRY.forEach(frame -> { if (!frame.isEmpty()) this.frames.add(frame); });
        AutomobileWheel.REGISTRY.forEach(wheel -> { if (!wheel.isEmpty()) this.wheels.add(wheel); });
        AutomobileEngine.REGISTRY.forEach(engine -> { if (!engine.isEmpty()) this.engines.add(engine); });
        if (this.frames.isEmpty() || this.wheels.isEmpty() || this.engines.isEmpty()) {
            throw new IllegalStateException("Automobility component registries are empty");
        }
        this.draft = new VehicleEditorDraft(this.frames.get(0), this.wheels.get(0), this.engines.get(0));
        this.preview = new PreviewAutomobile(this.draft);
        this.imageWidth = 430;
        this.imageHeight = 238;
    }

    @Override
    protected void init() {
        this.imageWidth = Math.min(430, this.width - 16);
        this.imageHeight = Math.min(238, this.height - 16);
        super.init();
        this.selectionButtons.clear();
        if (this.selectionType != null) {
            addSelectionControls();
            return;
        }
        int panelX = this.leftPos + Math.max(184, this.imageWidth / 2);
        int panelWidth = Math.max(100, this.leftPos + this.imageWidth - panelX - 8);
        int y = this.topPos + 25;

        int tabWidth = (panelWidth - 8) / 3;
        addPageButton(panelX, y, tabWidth, Page.MODEL, "Model");
        addPageButton(panelX + tabWidth + 4, y, tabWidth, Page.ASSEMBLY, "Assembly");
        addPageButton(panelX + (tabWidth + 4) * 2, y, panelWidth - (tabWidth + 4) * 2, Page.PUBLISH, "Publish");
        y += 24;

        switch (this.page) {
            case MODEL -> addModelControls(panelX, panelWidth, y);
            case ASSEMBLY -> addAssemblyControls(panelX, panelWidth, y);
            case PUBLISH -> addPublishControls(panelX, panelWidth, y);
        }
    }

    private void addPageButton(int x, int y, int width, Page target, String label) {
        Button button = Button.builder(Component.literal(label), ignored -> {
            this.page = target;
            resetEditorWidgets();
        }).bounds(x, y, width, 20).build();
        button.active = this.page != target;
        addRenderableWidget(button);
    }

    private void addModelControls(int panelX, int panelWidth, int y) {
        int half = (panelWidth - 4) / 2;

        addRenderableWidget(Button.builder(targetLabel(), button -> {
            this.draft.target = this.draft.target == VehicleEditorDraft.Target.FRAME
                    ? VehicleEditorDraft.Target.WHEEL : VehicleEditorDraft.Target.FRAME;
            button.setMessage(targetLabel());
            resetEditorWidgets();
        }).bounds(panelX, y, half, 20).build());
        addRenderableWidget(Button.builder(formatLabel(), button -> {
            VehicleEditorDraft.ModelFormat[] values = VehicleEditorDraft.ModelFormat.values();
            this.draft.modelFormat = values[(this.draft.modelFormat.ordinal() + 1) % values.length];
            button.setMessage(formatLabel());
        }).bounds(panelX + half + 4, y, half, 20).build());
        y += 24;

        int third = (panelWidth - 8) / 3;
        addRenderableWidget(Button.builder(Component.literal("Model..."), button -> chooseModel())
                .bounds(panelX, y, third, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Texture..."), button -> chooseTexture())
                .bounds(panelX + third + 4, y, third, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Anim..."), button -> chooseAnimation())
                .bounds(panelX + (third + 4) * 2, y, panelWidth - (third + 4) * 2, 20).build());
        y += 24;

        addRenderableWidget(Button.builder(frameLabel(), button -> openSelection(SelectionType.FRAME))
                .bounds(panelX, y, panelWidth, 20).build());
        y += 24;
        addRenderableWidget(Button.builder(wheelLabel(), button -> openSelection(SelectionType.WHEEL))
                .bounds(panelX, y, panelWidth, 20).build());
        y += 24;
        addRenderableWidget(Button.builder(engineLabel(), button -> openSelection(SelectionType.ENGINE))
                .bounds(panelX, y, panelWidth, 20).build());
        y += 24;
        addRenderableWidget(Button.builder(Component.literal("Load imported preview"), button -> loadPreview())
                .bounds(panelX, y, panelWidth, 20).build());
    }

    private void openSelection(SelectionType type) {
        this.selectionType = type;
        int selectedIndex = switch (type) {
            case FRAME -> this.frames.indexOf(this.draft.selectedFrame);
            case WHEEL -> this.wheels.indexOf(this.draft.selectedWheel);
            case ENGINE -> this.engines.indexOf(this.draft.selectedEngine);
        };
        this.selectionPage = Math.max(0, selectedIndex) / SELECTIONS_PER_PAGE;
        resetEditorWidgets();
    }

    private void closeSelection() {
        this.selectionType = null;
        this.selectionPage = 0;
        resetEditorWidgets();
    }

    private void addSelectionControls() {
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> closeSelection())
                .bounds(this.leftPos + 8, this.topPos + 24, 54, 20).build());

        int count = selectionCount();
        int pageCount = Math.max(1, (count + SELECTIONS_PER_PAGE - 1) / SELECTIONS_PER_PAGE);
        this.selectionPage = Math.max(0, Math.min(this.selectionPage, pageCount - 1));
        int start = this.selectionPage * SELECTIONS_PER_PAGE;
        int columns = 4;
        int gap = 4;
        int gridWidth = this.imageWidth - 16;
        int cellWidth = (gridWidth - gap * (columns - 1)) / columns;
        int cellHeight = 40;
        int gridX = this.leftPos + 8;
        int gridY = this.topPos + 49;
        for (int offset = 0; offset < SELECTIONS_PER_PAGE && start + offset < count; offset++) {
            int index = start + offset;
            int column = offset % columns;
            int row = offset / columns;
            ComponentEntryButton entry = new ComponentEntryButton(
                    gridX + column * (cellWidth + gap), gridY + row * (cellHeight + gap), cellWidth, cellHeight,
                    selectionStack(index), selectionName(index), selectionId(index), selectionIsCurrent(index),
                    () -> selectComponent(index));
            this.selectionButtons.add(entry);
            addRenderableWidget(entry);
        }

        int navigationY = this.topPos + this.imageHeight - 27;
        Button previous = Button.builder(Component.literal("<"), button -> {
            this.selectionPage--;
            resetEditorWidgets();
        }).bounds(this.leftPos + this.imageWidth / 2 - 66, navigationY, 28, 20).build();
        previous.active = this.selectionPage > 0;
        addRenderableWidget(previous);
        Button next = Button.builder(Component.literal(">"), button -> {
            this.selectionPage++;
            resetEditorWidgets();
        }).bounds(this.leftPos + this.imageWidth / 2 + 38, navigationY, 28, 20).build();
        next.active = this.selectionPage + 1 < pageCount;
        addRenderableWidget(next);
    }

    private int selectionCount() {
        return switch (this.selectionType) {
            case FRAME -> this.frames.size();
            case WHEEL -> this.wheels.size();
            case ENGINE -> this.engines.size();
        };
    }

    private ItemStack selectionStack(int index) {
        return switch (this.selectionType) {
            case FRAME -> AutomobilityItems.AUTOMOBILE_FRAME.require().createStack(this.frames.get(index));
            case WHEEL -> AutomobilityItems.AUTOMOBILE_WHEEL.require().createStack(this.wheels.get(index));
            case ENGINE -> AutomobilityItems.AUTOMOBILE_ENGINE.require().createStack(this.engines.get(index));
        };
    }

    private Component selectionName(int index) {
        ItemStack stack = selectionStack(index);
        return stack.getHoverName();
    }

    private String selectionId(int index) {
        return switch (this.selectionType) {
            case FRAME -> this.frames.get(index).getId().toString();
            case WHEEL -> this.wheels.get(index).getId().toString();
            case ENGINE -> this.engines.get(index).getId().toString();
        };
    }

    private boolean selectionIsCurrent(int index) {
        return switch (this.selectionType) {
            case FRAME -> this.frames.get(index) == this.draft.selectedFrame;
            case WHEEL -> this.wheels.get(index) == this.draft.selectedWheel;
            case ENGINE -> this.engines.get(index) == this.draft.selectedEngine;
        };
    }

    private void selectComponent(int index) {
        switch (this.selectionType) {
            case FRAME -> {
                this.frameIndex = index;
                this.draft.loadFrame(this.frames.get(index));
            }
            case WHEEL -> {
                this.wheelIndex = index;
                this.draft.loadWheel(this.wheels.get(index));
            }
            case ENGINE -> {
                this.engineIndex = index;
                this.draft.selectedEngine = this.engines.get(index);
            }
        }
        closeSelection();
    }

    private void addAssemblyControls(int panelX, int panelWidth, int y) {
        int half = (panelWidth - 4) / 2;
        if (this.draft.target == VehicleEditorDraft.Target.FRAME) {
            numericField(panelX, y, half, "Weight", () -> this.draft.weight, v -> this.draft.weight = v);
            numericField(panelX + half + 4, y, half, "Length", () -> this.draft.lengthPx, v -> this.draft.lengthPx = v);
        } else {
            numericField(panelX, y, half, "Size", () -> this.draft.wheelSize, v -> this.draft.wheelSize = v);
            numericField(panelX + half + 4, y, half, "Grip", () -> this.draft.wheelGrip, v -> this.draft.wheelGrip = v);
        }
        y += 24;
        if (this.draft.target == VehicleEditorDraft.Target.FRAME) {
            numericField(panelX, y, half, "Engine back", () -> this.draft.enginePosBack, v -> this.draft.enginePosBack = v);
            numericField(panelX + half + 4, y, half, "Engine up", () -> this.draft.enginePosUp, v -> this.draft.enginePosUp = v);
        } else {
            numericField(panelX, y, half, "Radius", () -> this.draft.wheelRadius, v -> this.draft.wheelRadius = v);
            numericField(panelX + half + 4, y, half, "Width", () -> this.draft.wheelWidth, v -> this.draft.wheelWidth = v);
        }
        y += 24;
        if (this.draft.target == VehicleEditorDraft.Target.FRAME) {
            numericField(panelX, y, half, "Vehicle width", () -> this.draft.widthBlocks, v -> this.draft.widthBlocks = v);
            numericField(panelX + half + 4, y, half, "Vehicle height", () -> this.draft.heightBlocks, v -> this.draft.heightBlocks = v);
        } else {
            numericField(panelX, y, panelWidth, "Model rotation Y", () -> this.draft.rotationY, v -> this.draft.rotationY = v);
        }
        y += 24;
        addRenderableWidget(Button.builder(Component.literal("Engine animation"), button -> this.preview.toggleEngine())
                .bounds(panelX, y, panelWidth, 20).build());

        if (this.draft.target == VehicleEditorDraft.Target.FRAME) addWheelPointControls();
    }

    private void addPublishControls(int panelX, int panelWidth, int y) {
        int half = (panelWidth - 4) / 2;
        textField(panelX, y, half, "Namespace", this.draft.namespace, value -> this.draft.namespace = value);
        textField(panelX + half + 4, y, half, "Component path", this.draft.componentPath, value -> this.draft.componentPath = value);
        y += 24;
        textField(panelX, y, panelWidth, "Display name", this.draft.displayName, value -> this.draft.displayName = value);
        y += 24;
        addRenderableWidget(Button.builder(Component.literal("Export ZIP"), button -> exportPack())
                .bounds(panelX, y, panelWidth, 20).build());
        y += 24;
        addRenderableWidget(Button.builder(overwriteLabel(), button -> {
            this.draft.overwrite = !this.draft.overwrite;
            button.setMessage(overwriteLabel());
        }).bounds(panelX, y, half, 20).build());
        Button publish = Button.builder(Component.literal("Publish to server"), button -> publishPack())
                .bounds(panelX + half + 4, y, half, 20).build();
        publish.active = this.menu.canPublish();
        addRenderableWidget(publish);
    }

    private void addWheelPointControls() {
        this.wheelPointIndex = Math.max(0, Math.min(this.wheelPointIndex, this.draft.wheelPoints.size() - 1));
        int x = this.leftPos + 10;
        int areaWidth = Math.max(160, Math.max(178, this.imageWidth / 2 - 6) - this.leftPos - 14);
        int y = this.topPos + this.imageHeight - 72;
        int selectorWidth = areaWidth - 48;
        addRenderableWidget(Button.builder(wheelPointLabel(), button -> {
            if (!this.draft.wheelPoints.isEmpty()) this.wheelPointIndex = (this.wheelPointIndex + 1) % this.draft.wheelPoints.size();
            resetEditorWidgets();
        }).bounds(x, y, selectorWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            this.draft.wheelPoints.add(new VehicleEditorDraft.WheelPoint(16, 12, 1, 0, "front", "left"));
            this.wheelPointIndex = this.draft.wheelPoints.size() - 1;
            resetEditorWidgets();
        }).bounds(x + selectorWidth + 4, y, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), button -> {
            if (this.draft.wheelPoints.size() > 1) this.draft.wheelPoints.remove(this.wheelPointIndex);
            this.wheelPointIndex = Math.max(0, this.wheelPointIndex - 1);
            resetEditorWidgets();
        }).bounds(x + selectorWidth + 28, y, 20, 20).build());
        if (this.draft.wheelPoints.isEmpty()) return;
        VehicleEditorDraft.WheelPoint point = this.draft.wheelPoints.get(this.wheelPointIndex);
        int half = (areaWidth - 4) / 2;
        y += 22;
        wheelPointField(x, y, half, "Forward", point.forward(), 0);
        wheelPointField(x + half + 4, y, half, "Right", point.right(), 1);
        y += 22;
        wheelPointField(x, y, half, "Scale", point.scale(), 2);
        wheelPointField(x + half + 4, y, half, "Yaw", point.yaw(), 3);
    }

    private void wheelPointField(int x, int y, int width, String hint, float value, int property) {
        EditBox field = new EditBox(this.font, x, y, width, 20, Component.literal(hint));
        field.setHint(Component.literal(hint));
        field.setValue(Float.toString(value));
        field.setResponder(text -> {
            try {
                float parsed = Float.parseFloat(text);
                VehicleEditorDraft.WheelPoint old = this.draft.wheelPoints.get(this.wheelPointIndex);
                this.draft.wheelPoints.set(this.wheelPointIndex, switch (property) {
                    case 0 -> new VehicleEditorDraft.WheelPoint(parsed, old.right(), old.scale(), old.yaw(), old.end(), old.side());
                    case 1 -> new VehicleEditorDraft.WheelPoint(old.forward(), parsed, old.scale(), old.yaw(), old.end(), old.side());
                    case 2 -> new VehicleEditorDraft.WheelPoint(old.forward(), old.right(), parsed, old.yaw(), old.end(), old.side());
                    default -> new VehicleEditorDraft.WheelPoint(old.forward(), old.right(), old.scale(), parsed, old.end(), old.side());
                });
            } catch (NumberFormatException ignored) {
                this.status = "Invalid wheel position number";
            }
        });
        addRenderableWidget(field);
    }

    private void resetEditorWidgets() {
        clearWidgets();
        init();
    }

    private void numericField(int x, int y, int width, String hint, Supplier<Float> getter, Consumer<Float> setter) {
        EditBox field = new EditBox(this.font, x, y, width, 20, Component.literal(hint));
        field.setHint(Component.literal(hint));
        field.setValue(Float.toString(getter.get()));
        field.setResponder(value -> {
            try {
                setter.accept(Float.parseFloat(value));
                this.status = "";
            } catch (NumberFormatException ignored) {
                this.status = "Invalid number: " + value;
            }
        });
        addRenderableWidget(field);
    }

    private void textField(int x, int y, int width, String hint, String value, Consumer<String> setter) {
        EditBox field = new EditBox(this.font, x, y, width, 20, Component.literal(hint));
        field.setHint(Component.literal(hint));
        field.setMaxLength(192);
        field.setValue(value);
        field.setResponder(setter);
        addRenderableWidget(field);
    }

    private void chooseModel() {
        String chosen = TinyFileDialogs.tinyfd_openFileDialog(
                "Select vehicle model", "", null, this.draft.modelFormat.label, false);
        if (chosen != null) {
            this.draft.modelFile = Path.of(chosen);
            this.status = this.draft.modelFile.getFileName().toString();
        }
    }

    private void chooseTexture() {
        String chosen = TinyFileDialogs.tinyfd_openFileDialog(
                "Select vehicle texture", "", null, "PNG texture", false);
        if (chosen != null) {
            this.draft.textureFile = Path.of(chosen);
            this.status = this.draft.textureFile.getFileName().toString();
        }
    }

    private void chooseAnimation() {
        String chosen = TinyFileDialogs.tinyfd_openFileDialog(
                "Select GeckoLib animation", "", null, "*.animation.json", false);
        if (chosen != null) {
            this.draft.animationFile = Path.of(chosen);
            this.status = this.draft.animationFile.getFileName().toString();
        }
    }

    private void loadPreview() {
        this.status = "Loading preview...";
        try {
            this.previewSession.load(this.draft).whenComplete((unused, error) -> Minecraft.getInstance().execute(() ->
                    this.status = error == null ? "Preview loaded" : "Preview failed: " + rootMessage(error)));
        } catch (IOException exception) {
            this.status = "Preview failed: " + exception.getMessage();
        }
    }

    private Path buildExport() throws IOException {
        Path directory = CarPackManager.getRootDirectory().resolve("exports");
        return VehiclePackBuilder.build(this.draft, directory.resolve(this.draft.packName() + ".zip"), false);
    }

    private void exportPack() {
        try {
            Path archive = buildExport();
            this.status = "Exported: " + archive.getFileName();
        } catch (IOException exception) {
            this.status = "Export failed: " + exception.getMessage();
        }
    }

    private void publishPack() {
        if (!this.menu.canPublish()) {
            this.status = "Server operator permission is required";
            return;
        }
        try {
            Path archive = buildExport();
            this.status = "Uploading " + archive.getFileName() + "...";
            ClientCarPackUploader.upload(archive, this.draft, result -> Minecraft.getInstance().execute(() ->
                    this.status = (result.successful() ? "Published: " : "Publish failed: ") + result.detail()));
        } catch (IOException exception) {
            this.status = "Publish failed: " + exception.getMessage();
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x0 = this.leftPos;
        int y0 = this.topPos;
        graphics.fill(x0, y0, x0 + this.imageWidth, y0 + this.imageHeight, 0xE61B1D20);
        if (this.selectionType != null) {
            graphics.fill(x0 + 6, y0 + 22, x0 + this.imageWidth - 6, y0 + this.imageHeight - 6, 0xFF202328);
            return;
        }
        graphics.fill(x0 + 6, y0 + 22, x0 + Math.max(178, this.imageWidth / 2 - 6), y0 + this.imageHeight - 8, 0xFF0F1114);
        graphics.fill(x0 + Math.max(184, this.imageWidth / 2), y0 + 22, x0 + this.imageWidth - 6,
                y0 + this.imageHeight - 8, 0xFF26292E);
        renderVehicle(graphics, partialTick);
    }

    private void renderVehicle(GuiGraphics graphics, float partialTick) {
        int previewX0 = this.leftPos + 6;
        int previewX1 = this.leftPos + Math.max(178, this.imageWidth / 2 - 6);
        int previewY0 = this.topPos + 22;
        int previewY1 = this.topPos + this.imageHeight - (showWheelPointEditor() ? 80 : 8);
        int centerX = (previewX0 + previewX1) / 2;
        int centerY = (previewY0 + previewY1) / 2 + 24;

        graphics.enableScissor(previewX0, previewY0, previewX1, previewY1);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 160);
        pose.scale(this.zoom, -this.zoom, this.zoom);
        pose.mulPose(new Quaternionf().rotationX((float) Math.toRadians(this.rotationX)));
        pose.mulPose(new Quaternionf().rotationY((float) Math.toRadians(this.rotationY)));
        Lighting.setupForEntityInInventory();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        AutomobileRenderer.render(pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, partialTick, this.preview);
        buffers.endBatch();
        pose.popPose();
        Lighting.setupFor3DItems();
        RenderSystem.enableDepthTest();
        graphics.disableScissor();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 8, 0xFFFFFF, false);
        if (this.selectionType != null) {
            Component selectionTitle = Component.literal("Select " + this.selectionType.label);
            graphics.drawCenteredString(this.font, selectionTitle, this.leftPos + this.imageWidth / 2, this.topPos + 30, 0xFFFFFF);
            int pageCount = Math.max(1, (selectionCount() + SELECTIONS_PER_PAGE - 1) / SELECTIONS_PER_PAGE);
            graphics.drawCenteredString(this.font, (this.selectionPage + 1) + " / " + pageCount,
                    this.leftPos + this.imageWidth / 2, this.topPos + this.imageHeight - 21, 0xB9C0C8);
            for (ComponentEntryButton button : this.selectionButtons) {
                if (button.isHovered()) {
                    graphics.renderTooltip(this.font, button.stack, mouseX, mouseY);
                    break;
                }
            }
        } else if (!this.status.isBlank()) {
            int statusX = this.leftPos + this.font.width(this.title) + 18;
            String visibleStatus = this.font.plainSubstrByWidth(this.status,
                    Math.max(0, this.leftPos + this.imageWidth - statusX - 8));
            graphics.drawString(this.font, visibleStatus, statusX, this.topPos + 8, 0xC9D1D9, false);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.selectionType == null && button == 0 && inPreview(mouseX, mouseY)) {
            this.draggingPreview = true;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingPreview && button == 0) {
            this.rotationY += (float) (mouseX - this.lastMouseX);
            this.rotationX = Math.max(-80, Math.min(80, this.rotationX + (float) (mouseY - this.lastMouseY)));
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingPreview = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.selectionType != null) {
            int pageCount = Math.max(1, (selectionCount() + SELECTIONS_PER_PAGE - 1) / SELECTIONS_PER_PAGE);
            int nextPage = Math.max(0, Math.min(pageCount - 1, this.selectionPage + (amount < 0 ? 1 : -1)));
            if (nextPage != this.selectionPage) {
                this.selectionPage = nextPage;
                resetEditorWidgets();
            }
            return true;
        }
        if (inPreview(mouseX, mouseY)) {
            this.zoom = Math.max(12, Math.min(80, this.zoom + (float) amount * 3));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private boolean inPreview(double x, double y) {
        return this.selectionType == null
                && x >= this.leftPos + 6 && x < this.leftPos + Math.max(178, this.imageWidth / 2 - 6)
                && y >= this.topPos + 22
                && y < this.topPos + this.imageHeight - (showWheelPointEditor() ? 80 : 8);
    }

    private Component targetLabel() { return Component.literal("Target: " + this.draft.target.path); }
    private Component formatLabel() { return Component.literal(this.draft.modelFormat.label); }
    private Component frameLabel() { return Component.literal("Frame: " + shortId(this.draft.selectedFrame.getId().toString())); }
    private Component wheelLabel() { return Component.literal("Wheel: " + shortId(this.draft.selectedWheel.getId().toString())); }
    private Component engineLabel() { return Component.literal("Engine: " + shortId(this.draft.selectedEngine.getId().toString())); }
    private Component overwriteLabel() { return Component.literal("Overwrite: " + (this.draft.overwrite ? "on" : "off")); }
    private Component wheelPointLabel() { return Component.literal("Wheel position " + (this.wheelPointIndex + 1) + "/" + this.draft.wheelPoints.size()); }
    private boolean showWheelPointEditor() { return this.page == Page.ASSEMBLY && this.draft.target == VehicleEditorDraft.Target.FRAME; }
    private static String shortId(String id) { return id.length() <= 24 ? id : "..." + id.substring(id.length() - 21); }

    private enum Page { MODEL, ASSEMBLY, PUBLISH }

    private enum SelectionType {
        FRAME("Frame"), WHEEL("Wheel"), ENGINE("Engine");

        final String label;

        SelectionType(String label) {
            this.label = label;
        }
    }

    private static final class ComponentEntryButton extends AbstractButton {
        private final ItemStack stack;
        private final String subtitle;
        private final boolean selected;
        private final Runnable action;

        private ComponentEntryButton(int x, int y, int width, int height, ItemStack stack,
                                     Component name, String subtitle, boolean selected, Runnable action) {
            super(x, y, width, height, name);
            this.stack = stack;
            this.subtitle = subtitle;
            this.selected = selected;
            this.action = action;
        }

        @Override
        public void onPress() {
            this.action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int color = this.isHoveredOrFocused() ? 0xFF454B53 : 0xFF30343A;
            graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, color);
            if (this.selected) {
                int border = 0xFF62C778;
                graphics.fill(getX(), getY(), getX() + this.width, getY() + 2, border);
                graphics.fill(getX(), getY() + this.height - 2, getX() + this.width, getY() + this.height, border);
                graphics.fill(getX(), getY(), getX() + 2, getY() + this.height, border);
                graphics.fill(getX() + this.width - 2, getY(), getX() + this.width, getY() + this.height, border);
            }
            graphics.renderItem(this.stack, getX() + 7, getY() + (this.height - 16) / 2);
            var font = Minecraft.getInstance().font;
            String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(0, this.width - 31));
            String id = font.plainSubstrByWidth(this.subtitle, Math.max(0, this.width - 31));
            graphics.drawString(font, label, getX() + 27, getY() + 8, 0xFFFFFF, false);
            graphics.drawString(font, id, getX() + 27, getY() + 22, 0xAAB2BC, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage());
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.selectionType != null) {
            closeSelection();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        this.previewSession.close();
        super.removed();
    }
}
