package com.sshakusora.riautomobility.editor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class VehicleImportTableBlock extends BaseEntityBlock {
    public VehicleImportTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof VehicleImportTableBlockEntity blockEntity) {
            NetworkHooks.openScreen(serverPlayer, blockEntity, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeBoolean(serverPlayer.hasPermissions(2));
                buffer.writeNbt(blockEntity.getEditorState());
            });
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VehicleImportTableBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof VehicleImportTableBlockEntity blockEntity) {
            Containers.dropContents(level, pos, blockEntity);
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
