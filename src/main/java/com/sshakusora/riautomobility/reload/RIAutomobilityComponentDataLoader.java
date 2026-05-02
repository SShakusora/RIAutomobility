package com.sshakusora.riautomobility.reload;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.RIAutomobilityComponentManager;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.wheel.RIAutomobileWheel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RIAutomobilityComponentDataLoader extends SimplePreparableReloadListener<RIAutomobilityComponentDataLoader.LoadedContent> {
    private static final Gson GSON = new GsonBuilder().create();

    @Override
    protected LoadedContent prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return new LoadedContent(
                loadFrames(resourceManager),
                loadWheels(resourceManager)
        );
    }

    @Override
    protected void apply(LoadedContent content, ResourceManager resourceManager, ProfilerFiller profiler) {
        RIAutomobileFrame.reload();
        RIAutomobileWheel.reload();
        RIAutomobilityComponentManager.applyCustomComponents(content.frames(), content.wheels());
        RIAutomobilityReloadManager.refreshAllServerLevels();
    }

    private static Map<ResourceLocation, FrameSpec> loadFrames(ResourceManager resourceManager) {
        Map<ResourceLocation, FrameSpec> result = new LinkedHashMap<>();
        load(resourceManager, "riautomobility/frames", (id, json) -> result.put(id, FrameSpec.fromJson(id, json)));
        return result;
    }

    private static Map<ResourceLocation, WheelSpec> loadWheels(ResourceManager resourceManager) {
        Map<ResourceLocation, WheelSpec> result = new LinkedHashMap<>();
        load(resourceManager, "riautomobility/wheels", (id, json) -> result.put(id, WheelSpec.fromJson(id, json)));
        return result;
    }

    private static void load(ResourceManager resourceManager, String root, JsonConsumer consumer) {
        for (Map.Entry<ResourceLocation, Resource> entry : resourceManager.listResources(root, location -> location.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation file = entry.getKey();
            ResourceLocation id = toDataId(root, file);
            try (Reader reader = entry.getValue().openAsReader()) {
                consumer.accept(id, GsonHelper.fromJson(GSON, reader, JsonObject.class));
            } catch (IOException | JsonParseException exception) {
                throw new IllegalStateException("Failed to load RIAutomobility component " + id + " from " + file, exception);
            }
        }
    }

    private static ResourceLocation toDataId(String root, ResourceLocation file) {
        String path = file.getPath();
        String relativePath = path.substring(root.length() + 1, path.length() - ".json".length());
        return new ResourceLocation(file.getNamespace(), relativePath);
    }

    private interface JsonConsumer {
        void accept(ResourceLocation id, JsonObject json);
    }

    public record LoadedContent(Map<ResourceLocation, FrameSpec> frames, Map<ResourceLocation, WheelSpec> wheels) {}
}
