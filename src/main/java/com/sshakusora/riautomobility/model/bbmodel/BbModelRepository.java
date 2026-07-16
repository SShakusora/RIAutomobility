package com.sshakusora.riautomobility.model.bbmodel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.content.FrameSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class BbModelRepository {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, List<FrameSpec.ModelSpec>> REGISTERED = new LinkedHashMap<>();
    private static final Set<RegistrationKey> TEMPORARY = new HashSet<>();
    private static volatile Map<ResourceLocation, BbModelData.Document> MODELS = Map.of();
    private static volatile Map<String, ResourceLocation> EMBEDDED_TEXTURES = Map.of();
    private static volatile Map<ResourceLocation, PixelShape> TEXTURE_SHAPES = Map.of();
    private static final Map<ResourceLocation, Set<String>> EMBEDDED_KEYS_BY_MODEL = new HashMap<>();
    private static final Map<ResourceLocation, Set<ResourceLocation>> DYNAMIC_TEXTURE_IDS_BY_MODEL = new HashMap<>();
    private static final Map<ResourceLocation, Set<ResourceLocation>> TEXTURE_SHAPE_IDS_BY_MODEL = new HashMap<>();

    private BbModelRepository() {
    }

    public static synchronized void register(FrameSpec.ModelSpec spec) {
        if (spec.bbModel() == null) {
            throw new BbModelFormatException("BBModel model specification is missing bbmodel");
        }
        List<FrameSpec.ModelSpec> specs = REGISTERED.computeIfAbsent(spec.bbModel(), ignored -> new ArrayList<>());
        specs.removeIf(existing -> existing.modelId().equals(spec.modelId()));
        specs.add(spec);
    }

    public static synchronized void unregister(FrameSpec.ModelSpec spec) {
        if (spec.bbModel() == null) return;
        List<FrameSpec.ModelSpec> specs = REGISTERED.get(spec.bbModel());
        if (specs == null) return;
        specs.removeIf(existing -> existing.modelId().equals(spec.modelId()));
        if (specs.isEmpty()) REGISTERED.remove(spec.bbModel());
    }

    public static synchronized void registerTemporary(FrameSpec.ModelSpec spec) {
        register(spec);
        TEMPORARY.add(RegistrationKey.of(spec));
    }

    public static synchronized void unregisterTemporary(FrameSpec.ModelSpec spec) {
        TEMPORARY.remove(RegistrationKey.of(spec));
        unregister(spec);
    }

    public static synchronized void retain(Set<ResourceLocation> modelResources) {
        REGISTERED.keySet().removeIf(resource -> !modelResources.contains(resource)
                && TEMPORARY.stream().noneMatch(key -> key.resource().equals(resource)));
    }

    static synchronized boolean isRegistered(FrameSpec.ModelSpec spec) {
        List<FrameSpec.ModelSpec> specs = REGISTERED.get(spec.bbModel());
        return specs != null && specs.stream().anyMatch(existing -> existing.modelId().equals(spec.modelId()));
    }

    public static synchronized void reload(ResourceManager manager) {
        BbAnimationPlayer.clearCache();
        Map<ResourceLocation, List<FrameSpec.ModelSpec>> registered = new LinkedHashMap<>();
        REGISTERED.forEach((location, specs) -> registered.put(location, List.copyOf(specs)));

        releaseDynamicTextures(DYNAMIC_TEXTURE_IDS_BY_MODEL.keySet());
        EMBEDDED_KEYS_BY_MODEL.clear();
        DYNAMIC_TEXTURE_IDS_BY_MODEL.clear();
        TEXTURE_SHAPE_IDS_BY_MODEL.clear();

        Map<ResourceLocation, BbModelData.Document> loaded = new HashMap<>();
        Map<String, ResourceLocation> embedded = new HashMap<>();
        Map<ResourceLocation, PixelShape> shapes = new HashMap<>();
        for (ResourceLocation location : registered.keySet()) {
            BbModelData.Document document = loadDocument(manager, location);
            if (document == null) continue;
            loaded.put(location, document);
            loadModelAssets(manager, location, registered.get(location), document, embedded, shapes);
        }

        MODELS = Map.copyOf(loaded);
        EMBEDDED_TEXTURES = Map.copyOf(embedded);
        TEXTURE_SHAPES = Map.copyOf(shapes);
    }

    public static synchronized void reload(ResourceManager manager, Collection<ResourceLocation> resources) {
        if (resources.isEmpty()) return;
        BbAnimationPlayer.clearCache();

        Map<ResourceLocation, BbModelData.Document> loaded = new HashMap<>(MODELS);
        Map<String, ResourceLocation> embedded = new HashMap<>(EMBEDDED_TEXTURES);
        Map<ResourceLocation, PixelShape> shapes = new HashMap<>(TEXTURE_SHAPES);
        Set<ResourceLocation> unique = new LinkedHashSet<>(resources);
        releaseDynamicTextures(unique);

        for (ResourceLocation location : unique) {
            loaded.remove(location);
            Set<String> oldKeys = EMBEDDED_KEYS_BY_MODEL.remove(location);
            if (oldKeys != null) oldKeys.forEach(embedded::remove);
            Set<ResourceLocation> oldShapes = TEXTURE_SHAPE_IDS_BY_MODEL.remove(location);
            if (oldShapes != null) {
                oldShapes.stream().filter(shape -> !shapeUsedByAnotherModel(shape)).forEach(shapes::remove);
            }
            DYNAMIC_TEXTURE_IDS_BY_MODEL.remove(location);

            List<FrameSpec.ModelSpec> specs = REGISTERED.get(location);
            if (specs == null || specs.isEmpty()) continue;
            BbModelData.Document document = loadDocument(manager, location);
            if (document == null) continue;
            loaded.put(location, document);
            loadModelAssets(manager, location, List.copyOf(specs), document, embedded, shapes);
        }

        MODELS = Map.copyOf(loaded);
        EMBEDDED_TEXTURES = Map.copyOf(embedded);
        TEXTURE_SHAPES = Map.copyOf(shapes);
    }

    public static BbModelData.Document get(ResourceLocation location) {
        return MODELS.get(location);
    }

    public static ResolvedTexture resolveTexture(ResourceLocation modelResource, FrameSpec.ModelSpec spec, BbModelData.Document document, BbModelData.TextureReference reference) {
        BbModelData.Texture texture = findTexture(document, reference);
        if (texture == null) {
            return new ResolvedTexture(spec.texture(), document.textureWidth(), document.textureHeight(), "default");
        }

        ResourceLocation override = findOverride(spec, texture);
        if (override != null) {
            return new ResolvedTexture(override, texture.uvWidth(), texture.uvHeight(), texture.renderMode());
        }

        ResourceLocation embedded = EMBEDDED_TEXTURES.get(embeddedKey(modelResource, texture));
        if (embedded != null) {
            return new ResolvedTexture(embedded, texture.uvWidth(), texture.uvHeight(), texture.renderMode());
        }

        ResourceLocation resource = textureResource(modelResource, texture);
        return new ResolvedTexture(resource == null ? spec.texture() : resource, texture.uvWidth(), texture.uvHeight(), texture.renderMode());
    }

    public static PixelShape getTextureShape(ResourceLocation texture) {
        return TEXTURE_SHAPES.get(texture);
    }

    private static BbModelData.Texture findTexture(BbModelData.Document document, BbModelData.TextureReference reference) {
        if (reference == null || reference.index() == null && reference.key() == null) {
            return document.textures().stream().filter(BbModelData.Texture::useAsDefault).findFirst()
                    .orElse(document.textures().isEmpty() ? null : document.textures().get(0));
        }
        if (reference.index() != null) {
            int index = reference.index();
            return index >= 0 && index < document.textures().size() ? document.textures().get(index) : null;
        }
        String key = reference.key();
        return document.textures().stream()
                .filter(texture -> key.equals(texture.uuid()) || key.equals(texture.id()) || key.equals(texture.name()))
                .findFirst()
                .orElse(null);
    }

    private static ResourceLocation findOverride(FrameSpec.ModelSpec spec, BbModelData.Texture texture) {
        for (String key : List.of(texture.uuid(), texture.id(), texture.name(), Integer.toString(texture.index()))) {
            if (!key.isBlank() && spec.textureOverrides().containsKey(key)) {
                return spec.textureOverrides().get(key);
            }
        }
        return null;
    }

    private static ResourceLocation textureResource(ResourceLocation modelResource, BbModelData.Texture texture) {
        for (String path : List.of(texture.relativePath(), texture.path())) {
            if (path == null || path.isBlank() || path.startsWith("data:")) {
                continue;
            }
            ResourceLocation direct = ResourceLocation.tryParse(path.replace('\\', '/'));
            if (path.indexOf(':') >= 0 && direct != null) {
                return direct;
            }
            String normalized = path.replace('\\', '/');
            int assetsIndex = normalized.indexOf("assets/");
            if (assetsIndex >= 0) {
                String[] parts = normalized.substring(assetsIndex + "assets/".length()).split("/", 2);
                if (parts.length == 2) {
                    ResourceLocation result = ResourceLocation.tryBuild(parts[0], parts[1]);
                    if (result != null) {
                        return result;
                    }
                }
            }
            if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:/.*")) {
                continue;
            }
            String parent = modelResource.getPath();
            int slash = parent.lastIndexOf('/');
            parent = slash < 0 ? "" : parent.substring(0, slash + 1);
            String resolved = normalizeResourcePath(parent + normalized);
            if (resolved != null) {
                ResourceLocation result = ResourceLocation.tryBuild(modelResource.getNamespace(), resolved);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static String normalizeResourcePath(String path) {
        List<String> result = new ArrayList<>();
        for (String part : path.split("/")) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (result.isEmpty()) {
                    return null;
                }
                result.remove(result.size() - 1);
            } else {
                result.add(part);
            }
        }
        return String.join("/", result);
    }

    private static BbModelData.Document loadDocument(ResourceManager manager, ResourceLocation location) {
        var resource = manager.getResource(location);
        if (resource.isEmpty()) {
            LOGGER.debug("Missing Blockbench model resource {}", location);
            return null;
        }
        try (Reader reader = resource.get().openAsReader()) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            return BbModelParser.parse(json);
        } catch (Exception exception) {
            LOGGER.error("Failed to load Blockbench model {}", location, exception);
            return null;
        }
    }

    private static void loadModelAssets(ResourceManager manager, ResourceLocation modelResource,
                                        List<FrameSpec.ModelSpec> specs, BbModelData.Document document,
                                        Map<String, ResourceLocation> embedded,
                                        Map<ResourceLocation, PixelShape> shapes) {
        Minecraft minecraft = Minecraft.getInstance();
        Set<String> embeddedKeys = new HashSet<>();
        Set<ResourceLocation> dynamicTextureIds = new HashSet<>();
        Set<ResourceLocation> shapeIds = new HashSet<>();
        for (BbModelData.Texture texture : document.textures()) {
            String source = texture.source();
            int comma = source == null ? -1 : source.indexOf(',');
            if (comma < 0 || !source.startsWith("data:image/")) {
                continue;
            }
            try {
                byte[] bytes = Base64.getDecoder().decode(source.substring(comma + 1));
                NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
                ResourceLocation id = RIAutomobility.rl("bbmodel/embedded/" + sha256(modelResource + ":" + texture.uuid() + ":" + source.length()));
                shapes.put(id, PixelShape.from(image));
                minecraft.getTextureManager().register(id, new DynamicTexture(image));
                String key = embeddedKey(modelResource, texture);
                embedded.put(key, id);
                embeddedKeys.add(key);
                dynamicTextureIds.add(id);
                shapeIds.add(id);
            } catch (Exception exception) {
                LOGGER.error("Failed to decode embedded BBModel texture {} in {}", texture.name(), modelResource, exception);
            }
        }

        for (FrameSpec.ModelSpec spec : specs) {
            for (BbModelData.Texture texture : document.textures()) {
                ResourceLocation resolved = findOverride(spec, texture);
                if (resolved == null) resolved = embedded.get(embeddedKey(modelResource, texture));
                if (resolved == null) resolved = textureResource(modelResource, texture);
                if (resolved == null) resolved = spec.texture();
                if (shapes.containsKey(resolved)) {
                    shapeIds.add(resolved);
                    continue;
                }
                final ResourceLocation textureId = resolved;
                manager.getResource(textureId).ifPresent(resource -> {
                    try (var input = resource.open(); NativeImage image = NativeImage.read(input)) {
                        shapes.put(textureId, PixelShape.from(image));
                    } catch (Exception exception) {
                        LOGGER.debug("Unable to read BBModel texture pixels from {}", textureId, exception);
                    }
                });
                if (shapes.containsKey(resolved)) shapeIds.add(resolved);
            }
        }

        EMBEDDED_KEYS_BY_MODEL.put(modelResource, Set.copyOf(embeddedKeys));
        DYNAMIC_TEXTURE_IDS_BY_MODEL.put(modelResource, Set.copyOf(dynamicTextureIds));
        TEXTURE_SHAPE_IDS_BY_MODEL.put(modelResource, Set.copyOf(shapeIds));
    }

    private static void releaseDynamicTextures(Collection<ResourceLocation> modelResources) {
        Minecraft minecraft = Minecraft.getInstance();
        for (ResourceLocation modelResource : List.copyOf(modelResources)) {
            Set<ResourceLocation> textures = DYNAMIC_TEXTURE_IDS_BY_MODEL.get(modelResource);
            if (textures != null) textures.forEach(minecraft.getTextureManager()::release);
        }
    }

    private static boolean shapeUsedByAnotherModel(ResourceLocation shape) {
        return TEXTURE_SHAPE_IDS_BY_MODEL.values().stream().anyMatch(ids -> ids.contains(shape));
    }

    private static String embeddedKey(ResourceLocation model, BbModelData.Texture texture) {
        return model + "#" + texture.index();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(32);
            for (int index = 0; index < 16; index++) {
                result.append(String.format("%02x", digest[index]));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record ResolvedTexture(ResourceLocation location, int uvWidth, int uvHeight, String renderMode) {
    }

    private record RegistrationKey(ResourceLocation resource, ResourceLocation modelId) {
        static RegistrationKey of(FrameSpec.ModelSpec spec) {
            return new RegistrationKey(spec.bbModel(), spec.modelId());
        }
    }

    public record PixelShape(int width, int height, boolean[] opaque) {
        public boolean opaque(int x, int y) {
            return x >= 0 && y >= 0 && x < this.width && y < this.height && this.opaque[y * this.width + x];
        }

        private static PixelShape from(NativeImage image) {
            boolean[] opaque = new boolean[image.getWidth() * image.getHeight()];
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    opaque[y * image.getWidth() + x] = ((image.getPixelRGBA(x, y) >>> 24) & 0xFF) > 140;
                }
            }
            return new PixelShape(image.getWidth(), image.getHeight(), opaque);
        }
    }
}
