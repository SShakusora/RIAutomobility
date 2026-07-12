package com.sshakusora.riautomobility.editor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public final class VehicleImportTableBlock extends Block {
    private static final Component TITLE = Component.translatable("container.riautomobility.vehicle_import");

    public VehicleImportTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new VehicleImportMenu(
                            containerId,
                            inventory,
                            ContainerLevelAccess.create(level, pos),
                            serverPlayer.hasPermissions(2)
                    ),
                    TITLE
            );
            NetworkHooks.openScreen(serverPlayer, provider, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeBoolean(serverPlayer.hasPermissions(2));
            });
        }
        return InteractionResult.CONSUME;
    }
}
