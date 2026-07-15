package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.model.DynamicJsonModelLoader;
import com.sshakusora.riautomobility.model.bbmodel.BbModelRepository;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(EntityModelSet.class)
public class EntityModelSetMixin {
    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void riautomobility$loadDynamicJsonEntityModels(ResourceManager manager, CallbackInfo ci) {
        EntityModelSet entityModels = (EntityModelSet) (Object) this;
        entityModels.roots = new HashMap<>(entityModels.roots);
        DynamicJsonModelLoader.loadModels(manager, entityModels.roots);
        BbModelRepository.reload(manager);
    }
}
