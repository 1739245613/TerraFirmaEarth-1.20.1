package com.newterraearth.tfe.common.block.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.rock.RockSpikeBlock;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.fluids.FluidProperty;
import net.dries007.tfc.common.fluids.IFluidLoggable;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;

public abstract class NTERopeAnchorBlock extends NTEAbstractRopeBlock implements IFluidLoggable
{
    public static final FluidProperty FLUID = TFCBlockStateProperties.WATER_AND_LAVA;

    protected NTERopeAnchorBlock(ExtendedProperties properties)
    {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FLUID, FLUID.keyFor(Fluids.EMPTY)));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        if (!canSurvive(defaultBlockState(), context.getLevel(), context.getClickedPos())) return null;
        final FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        final BlockState state = defaultBlockState();
        return getFluidProperty().canContain(fluidState.getType()) ? state.setValue(FLUID, FLUID.keyFor(fluidState.getType())) : state;
    }

    @Override
    public FluidState getFluidState(BlockState state)
    {
        return IFluidLoggable.super.getFluidLoggedState(state);
    }

    @Override
    public FluidProperty getFluidProperty()
    {
        return FLUID;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state)
    {
        return state.getFluidState().isRandomlyTicking();
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        state.getFluidState().randomTick(level, pos, random);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(FLUID));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos, Direction.UP, SupportType.CENTER);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        FluidHelpers.tickFluid(level, pos, state);
        if (direction == Direction.DOWN && !neighborState.isFaceSturdy(level, pos, Direction.UP, SupportType.CENTER)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult)
    {
        // 1.21 的上游实现使用 useWithoutItem；1.20 Forge 没有该入口，需显式保留空手语义，
        // 否则镐、食物或第二根绳索右键激活锚点时会被锚点先截获并回收绳索。
        if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
        removeRope(level, pos, state, player);
        return InteractionResult.SUCCESS;
    }

    public void removeRope(Level level, BlockPos pos, BlockState state, Player player)
    {
        recallRope(level, pos, state, player, state.getValue(FACING));
        level.setBlockAndUpdate(pos, getStateAfterRemoval(state));
    }

    protected abstract BlockState getStateAfterRemoval(BlockState state);

    public boolean isRopeAttached(LevelReader level, BlockPos pos, BlockState state)
    {
        final Direction facing = state.getValue(FACING);
        return isAttachedRope(level.getBlockState(pos.below().relative(facing)), facing) ||
            isAttachedRope(level.getBlockState(pos.relative(facing)), facing);
    }

    private static boolean isAttachedRope(BlockState state, Direction anchorFacing)
    {
        return (state.getBlock() instanceof NTEGroundedRopeBlock || state.getBlock() instanceof NTEHangingRopeBlock) &&
            state.getValue(FACING) == anchorFacing.getOpposite();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        final BlockPos possibleRopePos = pos.below().relative(state.getValue(FACING));
        if (level.getBlockState(possibleRopePos).getBlock() instanceof NTEAbstractRopeBlock) level.destroyBlock(possibleRopePos, true);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return RockSpikeBlock.TIP_SHAPE;
    }
}
