package com.sshakusora.riautomobility.carpack;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record CarPackManifestEntry(
        String id,
        String displayName,
        String contentDigest,
        String archiveDigest,
        long archiveSize,
        ResourceLocation component
) {
    public static final int MAX_PACKS = 4096;
    public static final int MAX_ID_LENGTH = 256;
    public static final int MAX_DISPLAY_NAME_LENGTH = 256;
    public static final long MAX_ARCHIVE_SIZE = 256L * 1024L * 1024L;

    public CarPackManifestEntry {
        if (id == null || !id.startsWith(CarPackManager.PACK_ID_PREFIX) || id.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException("Invalid car pack id: " + id);
        }
        if (displayName == null || displayName.isBlank() || displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("Invalid car pack display name");
        }
        validateDigest(contentDigest, "content");
        validateDigest(archiveDigest, "archive");
        if (archiveSize < 0 || archiveSize > MAX_ARCHIVE_SIZE) {
            throw new IllegalArgumentException("Invalid car pack archive size: " + archiveSize);
        }
        Objects.requireNonNull(component, "component");
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.id, MAX_ID_LENGTH);
        buffer.writeUtf(this.displayName, MAX_DISPLAY_NAME_LENGTH);
        buffer.writeUtf(this.contentDigest, 64);
        buffer.writeUtf(this.archiveDigest, 64);
        buffer.writeLong(this.archiveSize);
        buffer.writeResourceLocation(this.component);
    }

    public static CarPackManifestEntry read(FriendlyByteBuf buffer) {
        return new CarPackManifestEntry(
                buffer.readUtf(MAX_ID_LENGTH),
                buffer.readUtf(MAX_DISPLAY_NAME_LENGTH),
                buffer.readUtf(64),
                buffer.readUtf(64),
                buffer.readLong(),
                buffer.readResourceLocation()
        );
    }

    public static void validateDigest(String digest, String name) {
        if (digest == null || !digest.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid car pack " + name + " digest");
        }
    }
}
