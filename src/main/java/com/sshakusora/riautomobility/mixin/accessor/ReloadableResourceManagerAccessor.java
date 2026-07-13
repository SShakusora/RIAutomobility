package com.sshakusora.riautomobility.mixin.accessor;

import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ReloadableResourceManager.class)
public interface ReloadableResourceManagerAccessor {
    @Accessor("resources")
    CloseableResourceManager riautomobility$getResources();

    @Accessor("resources")
    void riautomobility$setResources(CloseableResourceManager resources);
}
