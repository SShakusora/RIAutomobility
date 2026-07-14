package com.sshakusora.riautomobility.editor.client;

import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import io.github.foundationgames.automobility.automobile.attachment.FrontAttachmentType;
import io.github.foundationgames.automobility.automobile.attachment.RearAttachmentType;
import io.github.foundationgames.automobility.item.AutomobilityItems;
import io.github.foundationgames.automobility.util.SimpleMapContentRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

final class VehicleComponentCatalog {
    enum Kind { FRAME, WHEEL, ENGINE, FRONT_ATTACHMENT, REAR_ATTACHMENT }

    private final List<AutomobileFrame> frames;
    private final List<AutomobileWheel> wheels;
    private final List<AutomobileEngine> engines;
    private final List<FrontAttachmentType<?>> frontAttachments;
    private final List<RearAttachmentType<?>> rearAttachments;

    VehicleComponentCatalog() {
        frames = collect(AutomobileFrame.REGISTRY, AutomobileFrame::isEmpty);
        wheels = collect(AutomobileWheel.REGISTRY, AutomobileWheel::isEmpty);
        engines = collect(AutomobileEngine.REGISTRY, AutomobileEngine::isEmpty);
        frontAttachments = collect(FrontAttachmentType.REGISTRY, FrontAttachmentType::isEmpty);
        rearAttachments = collect(RearAttachmentType.REGISTRY, RearAttachmentType::isEmpty);
        if (frames.isEmpty() || wheels.isEmpty() || engines.isEmpty()) {
            throw new IllegalStateException("Automobility component registries are empty");
        }
    }

    AutomobileFrame defaultFrame() { return frames.get(0); }
    AutomobileWheel defaultWheel() { return wheels.get(0); }
    AutomobileEngine defaultEngine() { return engines.get(0); }
    AutomobileFrame frame(int index) { return frames.get(index); }
    AutomobileWheel wheel(int index) { return wheels.get(index); }
    AutomobileEngine engine(int index) { return engines.get(index); }

    int count(Kind kind) {
        return switch (kind) {
            case FRAME -> frames.size();
            case WHEEL -> wheels.size();
            case ENGINE -> engines.size();
            case FRONT_ATTACHMENT -> frontAttachments.size();
            case REAR_ATTACHMENT -> rearAttachments.size();
        };
    }

    ItemStack stack(Kind kind, int index) {
        return switch (kind) {
            case FRAME -> AutomobilityItems.AUTOMOBILE_FRAME.require().createStack(frames.get(index));
            case WHEEL -> AutomobilityItems.AUTOMOBILE_WHEEL.require().createStack(wheels.get(index));
            case ENGINE -> AutomobilityItems.AUTOMOBILE_ENGINE.require().createStack(engines.get(index));
            case FRONT_ATTACHMENT -> AutomobilityItems.FRONT_ATTACHMENT.require()
                    .createStack(frontAttachments.get(index));
            case REAR_ATTACHMENT -> AutomobilityItems.REAR_ATTACHMENT.require()
                    .createStack(rearAttachments.get(index));
        };
    }

    ResourceLocation id(Kind kind, int index) {
        return switch (kind) {
            case FRAME -> frames.get(index).getId();
            case WHEEL -> wheels.get(index).getId();
            case ENGINE -> engines.get(index).getId();
            case FRONT_ATTACHMENT -> frontAttachments.get(index).getId();
            case REAR_ATTACHMENT -> rearAttachments.get(index).getId();
        };
    }

    List<ItemStack> attachmentStacks(Kind kind, List<ResourceLocation> ids) {
        if (kind != Kind.FRONT_ATTACHMENT && kind != Kind.REAR_ATTACHMENT) {
            throw new IllegalArgumentException("Not an attachment catalog: " + kind);
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (ResourceLocation id : ids) {
            int index = findIndex(kind, id);
            if (index >= 0) stacks.add(stack(kind, index));
        }
        return stacks;
    }

    private int findIndex(Kind kind, ResourceLocation id) {
        for (int index = 0; index < count(kind); index++) {
            if (id(kind, index).equals(id)) return index;
        }
        return -1;
    }

    private static <T extends SimpleMapContentRegistry.Identifiable> List<T> collect(
            SimpleMapContentRegistry<T> registry, Predicate<T> empty) {
        List<T> values = new ArrayList<>();
        registry.forEach(value -> {
            if (!empty.test(value) && !isImportTableComponent(value.getId()) && !values.contains(value)) {
                values.add(value);
            }
        });
        return List.copyOf(values);
    }

    private static boolean isImportTableComponent(ResourceLocation id) {
        return VehicleEditorDraft.GENERATED_NAMESPACE.equals(id.getNamespace())
                && id.getPath().matches(VehicleEditorDraft.GENERATED_COMPONENT_PREFIX + "[0-9a-f]{32}");
    }
}
