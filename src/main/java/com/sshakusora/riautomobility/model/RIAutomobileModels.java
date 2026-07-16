package com.sshakusora.riautomobility.model;

import com.mojang.logging.LogUtils;
import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.content.EngineSpec;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.mixin.accessor.AutomobileModelsAccessor;
import com.sshakusora.riautomobility.model.bbmodel.BbInstancedRenderer;
import com.sshakusora.riautomobility.model.bbmodel.BbModelRepository;
import com.sshakusora.riautomobility.model.bbmodel.DynamicBbModel;
import com.sshakusora.riautomobility.model.frame.DoubleMotorcarFrameModel;
import com.sshakusora.riautomobility.model.frame.QuadMotorcarFrameModel;
import com.sshakusora.riautomobility.network.packet.client.ClientCarPackSynchronizer;
import io.github.foundationgames.automobility.automobile.render.AutomobileModels;
import io.github.foundationgames.automobility.util.EntityRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RIAutomobileModels {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ModelLayerLocation PLACEHOLDER_LAYER = new ModelLayerLocation(RIAutomobility.rl("automobile/missing_component"), "main");
    private static final Set<ResourceLocation> MISSING_COMPONENTS = new HashSet<>();
    private static EntityRendererProvider.Context renderContext;
    private static final Map<ResourceLocation, FrameSpec.ModelSpec> CUSTOM_MODEL_SPECS = new LinkedHashMap<>();
    private static final List<FrameSpec.ModelSpec> BUILTIN_BB_MODELS = new ArrayList<>();

    public static void init() {
        EntityRenderHelper.registerContextListener(ctx -> {
            renderContext = ctx;
            rebuildDynamicModels();
        });

        AutomobileModels.register(RIAutomobility.rl("frame_doublemotorcar"), DoubleMotorcarFrameModel::new);

        AutomobileModels.register(RIAutomobility.rl("frame_quadmotorcar"), QuadMotorcarFrameModel::new);

        registerBuiltinBbModel("frame_lorry", "lorry_frame.bbmodel",
                "textures/entity/automobile/frame/lorry.png");
        registerBuiltinBbModel("frame_dmc12", "dmc12_frame.bbmodel",
                "textures/entity/automobile/frame/dmc12.png");
        registerBuiltinBbModel("frame_standard_formula", "standard_formula_frame.bbmodel",
                "textures/entity/automobile/frame/standard_formula.png");
        registerBuiltinBbModel("wheel_dmc12", "dmc12_wheel.bbmodel",
                "textures/entity/automobile/wheel/dmc12.png");
        registerBuiltinBbModel("wheel_standard_formula", "standard_formula_wheel.bbmodel",
                "textures/entity/automobile/wheel/standard_formula.png");

    }

    private static void registerBuiltinBbModel(String modelId, String modelFile, String texturePath) {
        ResourceLocation id = RIAutomobility.rl(modelId);
        FrameSpec.ModelSpec spec = new FrameSpec.ModelSpec(
                "bbmodel",
                RIAutomobility.rl(texturePath),
                id,
                "entity_cutout",
                0.0F,
                RIAutomobility.rl("models/entity/automobile/builtin/" + modelFile),
                Map.of(),
                ""
        );
        BUILTIN_BB_MODELS.add(spec);
        BbModelRepository.register(spec);
        AutomobileModels.register(id, context -> new DynamicBbModel(
                id, spec, createPlaceholderModel(context), ignored -> {
        }));
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        registerJsonLayer(event, DoubleMotorcarFrameModel.MODEL_LAYER);
        registerJsonLayer(event, QuadMotorcarFrameModel.MODEL_LAYER);
        registerJsonLayer(event, PLACEHOLDER_LAYER);
    }

    public static void applyDynamicModels(Collection<FrameSpec> frames, Collection<WheelSpec> wheels, Collection<EngineSpec> engines) {
        registerDynamicModels(frames, wheels, engines);
        rebuildDynamicModels();
    }

    public static void registerDynamicModels(Collection<FrameSpec> frames, Collection<WheelSpec> wheels, Collection<EngineSpec> engines) {
        CUSTOM_MODEL_SPECS.forEach(RIAutomobileModels::unregisterDynamicModel);
        CUSTOM_MODEL_SPECS.clear();
        clearMissingFlags(frames, wheels, engines);
        prepareBbModels(frames, wheels, engines);
        for (FrameSpec spec : frames) {
            registerDynamicModel(spec.id(), spec.model());
            CUSTOM_MODEL_SPECS.put(spec.id(), spec.model());
        }
        for (WheelSpec spec : wheels) {
            registerDynamicModel(spec.id(), spec.model());
            CUSTOM_MODEL_SPECS.put(spec.id(), spec.model());
        }
        for (EngineSpec spec : engines) {
            registerDynamicModel(spec.id(), spec.model());
            CUSTOM_MODEL_SPECS.put(spec.id(), spec.model());
        }
    }

    public static void registerTemporaryDynamicModel(ResourceLocation componentId, FrameSpec.ModelSpec modelSpec) {
        clearMissingComponent(componentId);
        try {
            BbModelRepository.registerTemporary(modelSpec);
            AutomobileModels.register(modelSpec.modelId(), ctx -> new DynamicBbModel(
                    componentId,
                    modelSpec,
                    createPlaceholderModel(ctx),
                    RIAutomobileModels::markMissingComponent
            ));
        } catch (RuntimeException exception) {
            markMissingComponent(componentId);
            LOGGER.error("Failed to register temporary Blockbench automobile model {} using {}",
                    modelSpec.modelId(), modelSpec.bbModel(), exception);
            AutomobileModels.register(modelSpec.modelId(), RIAutomobileModels::createPlaceholderModel);
        }
    }

    public static void unregisterTemporaryDynamicModel(ResourceLocation componentId, FrameSpec.ModelSpec modelSpec) {
        AutomobileModelsAccessor.riautomobility$getModelProviders().remove(modelSpec.modelId());
        AutomobileModelsAccessor.riautomobility$getModels().remove(modelSpec.modelId());
        BbModelRepository.unregisterTemporary(modelSpec);
        clearMissingComponent(componentId);
    }

    private static void unregisterDynamicModel(ResourceLocation componentId, FrameSpec.ModelSpec modelSpec) {
        AutomobileModelsAccessor.riautomobility$getModelProviders().remove(modelSpec.modelId());
        AutomobileModelsAccessor.riautomobility$getModels().remove(modelSpec.modelId());
        BbModelRepository.unregister(modelSpec);
        clearMissingComponent(componentId);
    }

    private static void registerDynamicModel(ResourceLocation componentId, FrameSpec.ModelSpec modelSpec) {
        registerDynamicBbModel(componentId, modelSpec);
    }

    private static void prepareBbModels(Collection<FrameSpec> frames, Collection<WheelSpec> wheels, Collection<EngineSpec> engines) {
        Set<ResourceLocation> resources = Stream.concat(BUILTIN_BB_MODELS.stream(), Stream.concat(Stream.concat(
                                frames.stream().map(FrameSpec::model), wheels.stream().map(WheelSpec::model)),
                        engines.stream().map(EngineSpec::model)))
                .filter(FrameSpec.ModelSpec::isBbModel)
                .map(FrameSpec.ModelSpec::bbModel)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        BbModelRepository.retain(resources);
    }

    private static void registerDynamicBbModel(ResourceLocation componentId, FrameSpec.ModelSpec modelSpec) {
        try {
            BbModelRepository.register(modelSpec);
            AutomobileModels.register(modelSpec.modelId(), ctx -> new DynamicBbModel(
                    componentId,
                    modelSpec,
                    createPlaceholderModel(ctx),
                    RIAutomobileModels::markMissingComponent
            ));
        } catch (RuntimeException exception) {
            markMissingComponent(componentId);
            LOGGER.error("Failed to register dynamic Blockbench automobile model {} using {}", modelSpec.modelId(), modelSpec.bbModel(), exception);
            AutomobileModels.register(modelSpec.modelId(), RIAutomobileModels::createPlaceholderModel);
        }
    }

    private static Model createPlaceholderModel(EntityRendererProvider.Context ctx) {
        try {
            return new PlaceholderAutomobileModel(ctx, PLACEHOLDER_LAYER);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to bake placeholder automobile model", exception);
            return AutomobileModels.getEmpty();
        }
    }

    private static void registerJsonLayer(EntityRenderersEvent.RegisterLayerDefinitions event, ModelLayerLocation layer) {
        event.registerLayerDefinition(layer, () -> BuiltinJsonModelLoader.loadRequiredModel(
                Minecraft.getInstance().getResourceManager(), layer));
    }

    public static void rebuildDynamicModelsNow() {
        rebuildDynamicModels();
    }

    public static void rebuildDynamicModelsNow(Collection<ResourceLocation> componentIds) {
        if (renderContext == null || componentIds.isEmpty()) return;
        Map<ResourceLocation, Function<EntityRendererProvider.Context, Model>> providers =
                AutomobileModelsAccessor.riautomobility$getModelProviders();
        Map<ResourceLocation, Model> models = AutomobileModelsAccessor.riautomobility$getModels();
        for (ResourceLocation componentId : componentIds) {
            FrameSpec.ModelSpec spec = CUSTOM_MODEL_SPECS.get(componentId);
            if (spec == null) continue;
            Model previous = models.get(spec.modelId());
            if (previous instanceof DynamicBbModel bbModel) BbInstancedRenderer.clearModel(bbModel);
            Function<EntityRendererProvider.Context, Model> provider = providers.get(spec.modelId());
            if (provider != null) models.put(spec.modelId(), provider.apply(renderContext));
        }
    }

    public static void markMissingComponent(ResourceLocation componentId) {
        if (componentId != null) {
            MISSING_COMPONENTS.add(componentId);
            ClientCarPackSynchronizer.requestComponent(componentId);
        }
    }

    public static boolean isMissingComponent(ResourceLocation componentId) {
        return componentId != null && MISSING_COMPONENTS.contains(componentId);
    }

    public static void clearMissingComponent(ResourceLocation componentId) {
        if (componentId != null) {
            MISSING_COMPONENTS.remove(componentId);
        }
    }

    private static void clearMissingFlags(Collection<FrameSpec> frames, Collection<WheelSpec> wheels, Collection<EngineSpec> engines) {
        frames.forEach(spec -> MISSING_COMPONENTS.remove(spec.id()));
        wheels.forEach(spec -> MISSING_COMPONENTS.remove(spec.id()));
        engines.forEach(spec -> MISSING_COMPONENTS.remove(spec.id()));
    }

    private static void rebuildDynamicModels() {
        if (renderContext == null) {
            return;
        }

        BbInstancedRenderer.clearGpuResources();

        Map<ResourceLocation, Function<EntityRendererProvider.Context, Model>> providers = AutomobileModelsAccessor.riautomobility$getModelProviders();
        Map<ResourceLocation, Model> models = AutomobileModelsAccessor.riautomobility$getModels();
        for (Map.Entry<ResourceLocation, Function<EntityRendererProvider.Context, Model>> entry : providers.entrySet()) {
            models.put(entry.getKey(), entry.getValue().apply(renderContext));
        }
    }
}
