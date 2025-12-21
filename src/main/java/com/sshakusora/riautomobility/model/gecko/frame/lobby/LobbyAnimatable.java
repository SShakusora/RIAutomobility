package com.sshakusora.riautomobility.model.gecko.frame.lobby;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtils;

public class LobbyAnimatable implements GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
//        controllers.add(new AnimationController<>(
//                this,
//                "door_left",
//                0,
//                (state) -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.Jinkela_truck.left_door_open")))
//        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public double getTick(Object itemStack) {
        return RenderUtils.getCurrentTick();
    }
}
