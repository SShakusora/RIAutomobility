package com.sshakusora.riautomobility.model;

import com.mojang.logging.LogUtils;
import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.mixin.accessor.AutomobileModelsAccessor;
import com.sshakusora.riautomobility.model.frame.DoubleMotorcarFrameModel;
import com.sshakusora.riautomobility.model.frame.QuadMotorcarFrameModel;
import com.sshakusora.riautomobility.model.gecko.DynamicGeckoAnimatable;
import com.sshakusora.riautomobility.model.gecko.DynamicGeckoModel;
import com.sshakusora.riautomobility.model.gecko.DynamicGeckoRenderer;
import com.sshakusora.riautomobility.model.gecko.GeckoFrameModel;
import com.sshakusora.riautomobility.model.gecko.frame.dmc12.DmcAnimatable;
import com.sshakusora.riautomobility.model.gecko.frame.dmc12.DmcModel;
import com.sshakusora.riautomobility.model.gecko.frame.dmc12.DmcRenderer;
import com.sshakusora.riautomobility.model.gecko.frame.lorry.LorryAnimatable;
import com.sshakusora.riautomobility.model.gecko.frame.lorry.LorryModel;
import com.sshakusora.riautomobility.model.gecko.frame.lorry.LorryRenderer;
import com.sshakusora.riautomobility.model.gecko.frame.standard_formula.StandardFormulaAnimatable;
import com.sshakusora.riautomobility.model.gecko.frame.standard_formula.StandardFormulaModel;
import com.sshakusora.riautomobility.model.gecko.frame.standard_formula.StandardFormulaRenderer;
import com.sshakusora.riautomobility.model.gecko.wheel.dmc12.DmcWheelAnimatable;
import com.sshakusora.riautomobility.model.gecko.wheel.dmc12.DmcWheelModel;
import com.sshakusora.riautomobility.model.gecko.wheel.dmc12.DmcWheelRenderer;
import com.sshakusora.riautomobility.model.gecko.wheel.standard_formula.StandardFormulaWheelAnimatable;
import com.sshakusora.riautomobility.model.gecko.wheel.standard_formula.StandardFormulaWheelModel;
import com.sshakusora.riautomobility.model.gecko.wheel.standard_formula.StandardFormulaWheelRenderer;
import io.github.foundationgames.automobility.automobile.render.AutomobileModels;
import io.github.foundationgames.automobility.forge.vendored.jsonem.JsonEM;
import io.github.foundationgames.automobility.util.EntityRenderHelper;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class RIAutomobileModels {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ModelLayerLocation PLACEHOLDER_LAYER = new ModelLayerLocation(RIAutomobility.rl("automobile/missing_component"), "main");
    private static final Set<ResourceLocation> MISSING_COMPONENTS = new HashSet<>();
    private static EntityRendererProvider.Context renderContext;

    public static void init(){
        EntityRenderHelper.registerContextListener(ctx -> {
            renderContext = ctx;
            rebuildDynamicModels();
        });

        AutomobileModels.register(RIAutomobility.rl("frame_doublemotorcar"), DoubleMotorcarFrameModel::new);
        JsonEM.registerModelLayer(DoubleMotorcarFrameModel.MODEL_LAYER);

        AutomobileModels.register(RIAutomobility.rl("frame_quadmotorcar"), QuadMotorcarFrameModel::new);
        JsonEM.registerModelLayer(QuadMotorcarFrameModel.MODEL_LAYER);

        AutomobileModels.register(RIAutomobility.rl("frame_lorry"), context -> {
            LorryAnimatable anim = new LorryAnimatable();
            LorryModel model = new LorryModel();
            LorryRenderer renderer = new LorryRenderer(model, anim);

            return new GeckoFrameModel<>(model, renderer, anim);
        });

        AutomobileModels.register(RIAutomobility.rl("frame_dmc12"), context -> {
            DmcAnimatable anim = new DmcAnimatable();
            DmcModel model = new DmcModel();
            DmcRenderer renderer = new DmcRenderer(model, anim);

            return new GeckoFrameModel<>(model, renderer, anim);
        });

        AutomobileModels.register(RIAutomobility.rl("frame_standard_formula"), context -> {
            StandardFormulaAnimatable anim = new StandardFormulaAnimatable();
            StandardFormulaModel model = new StandardFormulaModel();
            StandardFormulaRenderer renderer = new StandardFormulaRenderer(model, anim);

            return new GeckoFrameModel<>(model, renderer, anim);
        });

        AutomobileModels.register(RIAutomobility.rl("wheel_dmc12"), context -> {
            DmcWheelAnimatable anim = new DmcWheelAnimatable();
            DmcWheelModel model = new DmcWheelModel();
            DmcWheelRenderer renderer = new DmcWheelRenderer(model, anim);

            return new GeckoFrameModel<>(model, renderer, anim);
        });

        AutomobileModels.register(RIAutomobility.rl("wheel_standard_formula"), context -> {
            StandardFormulaWheelAnimatable anim = new StandardFormulaWheelAnimatable();
            StandardFormulaWheelModel model = new StandardFormulaWheelModel();
            StandardFormulaWheelRenderer renderer = new StandardFormulaWheelRenderer(model, anim);

            return new GeckoFrameModel<>(model, renderer, anim);
        });

        JsonEM.registerModelLayer(PLACEHOLDER_LAYER);
        DynamicJsonModelLoader.register(PLACEHOLDER_LAYER);
    }

    public static void applyDynamicModels(Collection<FrameSpec> frames, Collection<WheelSpec> wheels) {
        clearMissingFlags(frames, wheels);
        for (FrameSpec spec : frames) {
            registerDynamicModel(spec.id(), spec.model());
        }
        for (WheelSpec spec : wheels) {
            registerDynamicModel(spec.id(), spec.model());
        }
        rebuildDynamicModels();
    }

    public static void registerDynamicModels(Collection<FrameSpec> frames, Collection<WheelSpec> wheels) {
        clearMissingFlags(frames, wheels);
        for (FrameSpec spec : frames) {
            registerDynamicModel(spec.id(), spec.model());
        }
        for (WheelSpec spec : wheels) {
            registerDynamicModel(spec.id(), spec.model());
        }
    }

    private static void registerDynamicModel(ResourceLocation componentId, FrameSpec.ModelSpec modelSpec) {
        if (modelSpec.isGeckoLib()) {
            registerDynamicGeckoModel(componentId, modelSpec);
            return;
        }

        ModelLayerLocation layer = new ModelLayerLocation(modelSpec.layerLocation(), "main");
        JsonEM.registerModelLayer(layer);
        DynamicJsonModelLoader.register(layer);
        AutomobileModels.register(modelSpec.modelId(), ctx -> {
            try {
                return new DynamicAutomobileModel(ctx, layer, modelSpec.renderType(), modelSpec.rotationY());
            } catch (RuntimeException exception) {
                markMissingComponent(componentId);
                LOGGER.error("Failed to bake dynamic automobile model {} from layer {}", modelSpec.modelId(), modelSpec.layerLocation(), exception);
                return createPlaceholderModel(ctx);
            }
        });
    }

    private static void registerDynamicGeckoModel(ResourceLocation componentId, FrameSpec.ModelSpec modelSpec) {
        AutomobileModels.register(modelSpec.modelId(), ctx -> {
            try {
                DynamicGeckoAnimatable animatable = new DynamicGeckoAnimatable();
                DynamicGeckoModel model = new DynamicGeckoModel(modelSpec.geoModel(), modelSpec.texture(), modelSpec.animation());
                DynamicGeckoRenderer renderer = new DynamicGeckoRenderer(model, animatable);
                return new GeckoFrameModel<>(model, renderer, animatable, createPlaceholderModel(ctx), componentId, RIAutomobileModels::markMissingComponent);
            } catch (RuntimeException exception) {
                markMissingComponent(componentId);
                LOGGER.error("Failed to bake dynamic GeckoLib automobile model {} using geo {}", modelSpec.modelId(), modelSpec.geoModel(), exception);
                return createPlaceholderModel(ctx);
            }
        });
    }

    private static Model createPlaceholderModel(EntityRendererProvider.Context ctx) {
        try {
            return new PlaceholderAutomobileModel(ctx, PLACEHOLDER_LAYER);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to bake placeholder automobile model", exception);
            return AutomobileModels.getEmpty();
        }
    }

    public static void rebuildDynamicModelsNow() {
        rebuildDynamicModels();
    }

    public static void markMissingComponent(ResourceLocation componentId) {
        if (componentId != null) {
            MISSING_COMPONENTS.add(componentId);
        }
    }

    public static boolean isMissingComponent(ResourceLocation componentId) {
        return componentId != null && MISSING_COMPONENTS.contains(componentId);
    }

    private static void clearMissingFlags(Collection<FrameSpec> frames, Collection<WheelSpec> wheels) {
        frames.forEach(spec -> MISSING_COMPONENTS.remove(spec.id()));
        wheels.forEach(spec -> MISSING_COMPONENTS.remove(spec.id()));
    }

    private static void rebuildDynamicModels() {
        if (renderContext == null) {
            return;
        }

        Map<ResourceLocation, Function<EntityRendererProvider.Context, Model>> providers = AutomobileModelsAccessor.riautomobility$getModelProviders();
        Map<ResourceLocation, Model> models = AutomobileModelsAccessor.riautomobility$getModels();
        for (Map.Entry<ResourceLocation, Function<EntityRendererProvider.Context, Model>> entry : providers.entrySet()) {
            models.put(entry.getKey(), entry.getValue().apply(renderContext));
        }
    }
}
