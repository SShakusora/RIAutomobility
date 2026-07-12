package com.sshakusora.riautomobility.carpack;

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
import java.util.List;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class CarPackComponentDataLoader extends SimplePreparableReloadListener<CarPackComponentDataLoader.LoadedContent> {
    private static final Gson GSON = new GsonBuilder().create();

    @Override
    protected LoadedContent prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return new LoadedContent(loadFrames(resourceManager), loadWheels(resourceManager));
    }

    @Override
    protected void apply(LoadedContent content, ResourceManager resourceManager, ProfilerFiller profiler) {
        RIAutomobileFrame.reload();
        RIAutomobileWheel.reload();
        RIAutomobilityComponentManager.applyCustomComponents(content.frames(), content.wheels());
        CarPackManager.refreshAllServerLevels();
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
        Map<ResourceLocation, List<Resource>> stacks = resourceManager.listResourceStacks(
                root,
                location -> location.getPath().endsWith(".json")
        );
        for (Map.Entry<ResourceLocation, List<Resource>> entry : stacks.entrySet()) {
            Resource resource = highestPriorityCarPackResource(entry.getValue());
            if (resource == null) {
                continue;
            }

            ResourceLocation file = entry.getKey();
            ResourceLocation id = toDataId(root, file);
            try (Reader reader = resource.openAsReader()) {
                consumer.accept(id, GsonHelper.fromJson(GSON, reader, JsonObject.class));
            } catch (IOException | JsonParseException exception) {
                throw new IllegalStateException("Failed to load RIAutomobility car pack component " + id + " from " + file, exception);
            }
        }
    }

    private static Resource highestPriorityCarPackResource(List<Resource> resources) {
        for (int index = resources.size() - 1; index >= 0; index--) {
            Resource resource = resources.get(index);
            if (resource.sourcePackId().startsWith(CarPackManager.PACK_ID_PREFIX)) {
                return resource;
            }
        }
        return null;
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
