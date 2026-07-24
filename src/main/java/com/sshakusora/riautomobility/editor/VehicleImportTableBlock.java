package com.sshakusora.riautomobility.editor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public final class VehicleImportTableBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            box(0, 0, 2, 1, 11, 16),
            box(15, 0, 2, 16, 11, 16),
            box(1, 0, 2, 15, 1, 15),
            box(1, 1, 0.5, 15, 3, 9.5),
            box(1, 4, 1.5, 15, 6, 10.5),
            box(1, 7, 1, 15, 9, 10),
            box(1, 9, 3, 15, 10, 16),
            box(2, 10.5, 9, 14, 12, 11.5),
            box(2, 11.5, 10, 14, 13, 12.5),
            box(2, 12.5, 11, 14, 14, 13.5),
            box(2, 13.5, 12, 14, 15.75, 14.5)
    ).optimize();
    private static final Map<Direction, VoxelShape> SHAPES = createShapes();

    public VehicleImportTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
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

    private static Map<Direction, VoxelShape> createShapes() {
        EnumMap<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.NORTH, NORTH_SHAPE);
        shapes.put(Direction.EAST, rotateClockwise(NORTH_SHAPE));
        shapes.put(Direction.SOUTH, rotateClockwise(shapes.get(Direction.EAST)));
        shapes.put(Direction.WEST, rotateClockwise(shapes.get(Direction.SOUTH)));
        return shapes;
    }

    private static VoxelShape rotateClockwise(VoxelShape shape) {
        VoxelShape[] rotated = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                rotated[0] = Shapes.or(rotated[0],
                        Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
        return rotated[0].optimize();
    }
}
