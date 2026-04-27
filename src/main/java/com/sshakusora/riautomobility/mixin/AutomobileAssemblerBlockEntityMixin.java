package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import io.github.foundationgames.automobility.block.entity.AutomobileAssemblerBlockEntity;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AutomobileAssemblerBlockEntity.class)
public class AutomobileAssemblerBlockEntityMixin {
    @Unique private final TagKey<Item> FORGE_WRENCH = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/wrench"));

    @Shadow protected AutomobileFrame frame;
    @Shadow protected AutomobileEngine engine;
    @Shadow protected AutomobileWheel wheel;
    @Shadow protected int wheelCount;

    @Shadow protected Vec3 centerPos() { return Vec3.ZERO; }
    @Shadow public float getAutomobileYaw(float tickDelta) { return 0; }
    @Shadow public boolean isComplete() { return false; }
    @Shadow public void clear() {}

    @Redirect(method = "handleItemInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z", ordinal = 0))
    private boolean allowForgeWrench(ItemStack stack, Item item) {
        return stack.is(item) || stack.is(FORGE_WRENCH);
    }

    @Inject(method = "tryConstructAutomobile", at = @At("HEAD"), cancellable = true, remap = false)
    private void createRIAutomobile(CallbackInfo ci) {
        if (!this.isComplete()) {
            return;
        }

        AutomobileAssemblerBlockEntity self = (AutomobileAssemblerBlockEntity) (Object) this;
        if (self.getLevel() == null) {
            return;
        }

        Vec3 pos = this.centerPos();
        RIAutomobileEntity auto = new RIAutomobileEntity(self.getLevel());
        auto.moveTo(pos.x, pos.y, pos.z, this.getAutomobileYaw(0), 0);
        auto.setComponents(this.frame, this.wheel, this.engine);
        self.getLevel().addFreshEntity(auto);

        self.getLevel().players().forEach(player -> {
            if (player instanceof ServerPlayer serverPlayer && player.blockPosition().distSqr(self.getBlockPos()) < 80000) {
                serverPlayer.connection.send(new ClientboundLevelParticlesPacket(ParticleTypes.EXPLOSION, false, pos.x, pos.y + 0.47, pos.z, 0, 0, 0, 0, 1));
            }
        });
        self.getLevel().playSound(null, self.getBlockPos(), SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.23f, 0.5f);

        this.clear();
        ci.cancel();
    }
}
