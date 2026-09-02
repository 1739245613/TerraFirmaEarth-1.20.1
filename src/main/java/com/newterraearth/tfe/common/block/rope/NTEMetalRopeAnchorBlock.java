package com.newterraearth.tfe.common.block.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

import net.dries007.tfc.common.blocks.ExtendedProperties;

public class NTEMetalRopeAnchorBlock extends NTERopeAnchorBlock
{
    public static final BooleanProperty HAS_ROPE = BooleanProperty.create("has_rope");

    public NTEMetalRopeAnchorBlock(ExtendedProperties properties)
    {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FLUID, FLUID.keyFor(Fluids.EMPTY)).setValue(HAS_ROPE, false));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult)
    {
        return state.getValue(HAS_ROPE) ? super.use(state, level, pos, player, hand, hitResult) : InteractionResult.PASS;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        return super.updateShape(state.setValue(HAS_ROPE, isRopeAttached(level, pos, state)), direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (state.getValue(HAS_ROPE) && !isRopeAttached(level, pos, state)) level.setBlockAndUpdate(pos, state.setValue(HAS_ROPE, false));
    }

    @Override
    protected BlockState getStateAfterRemoval(BlockState state)
    {
        return state.setValue(HAS_ROPE, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(HAS_ROPE));
    }
}
