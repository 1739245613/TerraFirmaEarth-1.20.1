package com.newterraearth.tfe.common.block.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.util.Helpers;

public class NTEGroundedRopeBlock extends NTEAbstractRopeBlock
{
    public static final BooleanProperty ASCENDING = BooleanProperty.create("ascending");
    public static final VoxelShape[] SHAPES_ASCENDING = Helpers.computeHorizontalShapes(dir -> Shapes.or(
        Helpers.rotateShape(dir, 7, 0, 0, 9, 2, 16),
        Helpers.rotateShape(dir, 7, 2, 0, 9, 4, 14),
        Helpers.rotateShape(dir, 7, 4, 0, 9, 6, 12),
        Helpers.rotateShape(dir, 7, 6, 0, 9, 8, 10),
        Helpers.rotateShape(dir, 7, 8, 0, 9, 10, 8),
        Helpers.rotateShape(dir, 7, 10, 0, 9, 12, 6),
        Helpers.rotateShape(dir, 7, 12, 0, 9, 14, 4),
        Helpers.rotateShape(dir, 7, 14, 0, 9, 16, 2)
    ));

    public NTEGroundedRopeBlock(ExtendedProperties properties)
    {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(ASCENDING, false));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        return isAttached(level, pos, state);
    }

    /** Forge 1.20 does not infer climbability from the rope model or shape. */
    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity)
    {
        return true;
    }

    public boolean isAttached(LevelReader level, BlockPos pos, BlockState state)
    {
        final Direction facing = state.getValue(FACING);
        BlockPos offsetPos = pos.relative(facing);
        if (state.getValue(ASCENDING))
        {
            final BlockState aboveState = level.getBlockState(pos.above());
            if (aboveState.getBlock() instanceof NTEHangingRopeBlock && aboveState.getValue(FACING) == facing)
                return isBlockBelowSturdy(level, pos);
            offsetPos = offsetPos.above();
        }
        final BlockState offsetState = level.getBlockState(offsetPos);
        return isBlockBelowSturdy(level, pos) && (
            (offsetState.getBlock() instanceof NTEGroundedRopeBlock && offsetState.getValue(FACING) == facing) ||
            (offsetState.getBlock() instanceof NTERopeAnchorBlock && offsetState.getValue(FACING) == facing.getOpposite())
        );
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        final BlockPos possibleRopePos = pos.below().relative(state.getValue(FACING).getOpposite());
        if (level.getBlockState(possibleRopePos).getBlock() instanceof NTEAbstractRopeBlock)
            level.destroyBlock(possibleRopePos, true);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return state.getValue(ASCENDING) ? SHAPES_ASCENDING[state.getValue(FACING).get2DDataValue()] :
            state.getValue(FACING).getAxis() == Direction.Axis.X ? SHAPE_Z : SHAPE_X;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return Shapes.empty();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        final BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        if (isAttached(context.getLevel(), context.getClickedPos(), state)) return state;
        if (isAttached(context.getLevel(), context.getClickedPos(), state.setValue(ASCENDING, true))) return state.setValue(ASCENDING, true);
        return null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        return isAttached(level, pos, state) ? super.updateShape(state, direction, neighborState, level, pos, neighborPos) : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(ASCENDING));
    }
}
