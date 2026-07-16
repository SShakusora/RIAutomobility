package com.sshakusora.riautomobility.carpack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Exposes flat RIAuto v1 files through their logical Minecraft resource paths. */
final class FlatRiautoPackResources extends AbstractPackResources {
    private final ZipFile zip;
    private final Map<String, String> logicalToFile;

    FlatRiautoPackResources(String id, File file) {
        super(id, false);
        try {
            Map<String, String> mappings = CarPackArchiveStore.readFileMappings(file.toPath());
            this.logicalToFile = mappings.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                    Map.Entry::getValue, Map.Entry::getKey));
            this.zip = new ZipFile(file);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to open RIAuto v1 pack " + file, exception);
        }
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length != 1 || !CarPackArchiveStore.RIAUTO_METADATA_FILE.equals(elements[0])) return null;
        return entry(CarPackArchiveStore.RIAUTO_METADATA_FILE);
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        return entry(logicalToFile.get(type.getDirectory() + "/" + location.getNamespace() + "/" + location.getPath()));
    }

    @Override
    public void listResources(PackType type, String namespace, String path, PackResources.ResourceOutput output) {
        String prefix = type.getDirectory() + "/" + namespace + "/";
        String resourcePrefix = path.isEmpty() ? "" : path + "/";
        logicalToFile.forEach((logicalPath, file) -> {
            if (!logicalPath.startsWith(prefix)) return;
            String resourcePath = logicalPath.substring(prefix.length());
            if (!resourcePath.startsWith(resourcePrefix)) return;
            output.accept(new ResourceLocation(namespace, resourcePath), entry(file));
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        String prefix = type.getDirectory() + "/";
        Set<String> namespaces = new HashSet<>();
        for (String logicalPath : logicalToFile.keySet()) {
            if (!logicalPath.startsWith(prefix)) continue;
            int separator = logicalPath.indexOf('/', prefix.length());
            if (separator > prefix.length()) namespaces.add(logicalPath.substring(prefix.length(), separator));
        }
        return Set.copyOf(namespaces);
    }

    private IoSupplier<InputStream> entry(String file) {
        if (file == null) return null;
        ZipEntry zipEntry = zip.getEntry(file);
        return zipEntry == null || zipEntry.isDirectory() ? null : IoSupplier.create(zip, zipEntry);
    }

    @Override
    public void close() {
        try {
            zip.close();
        } catch (IOException ignored) {
        }
    }
}
