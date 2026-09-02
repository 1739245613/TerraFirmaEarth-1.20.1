package com.newterraearth.tfe.common.block.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.util.Helpers;

public class NTEHangingRopeBlock extends NTEAbstractRopeBlock
{
    public static final VoxelShape[] SHAPES = Helpers.computeHorizontalShapes(dir -> Helpers.rotateShape(dir, 7, 0, 0, 9, 16, 2));

    public NTEHangingRopeBlock(ExtendedProperties properties)
    {
        super(properties);
    }

    /** Forge 1.20 does not infer climbability from the rope model or shape. */
    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity)
    {
        return true;
    }

    public boolean isAttached(LevelReader level, BlockPos pos, BlockState state)
    {
        if (level.getBlockState(pos.above()).getBlock() == this) return true;
        final BlockState relativeState = level.getBlockState(pos.above().relative(state.getValue(FACING)));
        return (relativeState.getBlock() instanceof NTEGroundedRopeBlock && state.getValue(FACING) == relativeState.getValue(FACING)) ||
            (relativeState.getBlock() instanceof NTERopeAnchorBlock && state.getValue(FACING) == relativeState.getValue(FACING).getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        return isAttached(level, pos, state) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        return isAttached(level, pos, state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        for (Direction dir : Direction.Plane.HORIZONTAL)
        {
            final BlockState state = defaultBlockState().setValue(FACING, dir);
            if (isAttached(context.getLevel(), context.getClickedPos(), state)) return state;
        }
        return null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }
}
