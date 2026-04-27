package com.newterraearth.tfe.common.block.plant;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.material.FluidState;

public class NTEBambooStalkBlock extends BambooStalkBlock
{
    private final Supplier<? extends Block> sapling;

    public NTEBambooStalkBlock(Properties properties, Supplier<? extends Block> sapling)
    {
        super(properties);
        this.sapling = sapling;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        final FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        if (!fluidState.isEmpty())
        {
            return null;
        }

        final BlockPos belowPos = context.getClickedPos().below();
        final BlockState belowState = context.getLevel().getBlockState(context.getClickedPos().below());
        if (belowState.getBlock() != sapling.get() && belowState.getBlock() != this && !canPlantOn(context.getLevel(), belowPos, belowState))
        {
            return null;
        }

        if (belowState.getBlock() == sapling.get())
        {
            return defaultBlockState().setValue(AGE, 0);
        }
        if (belowState.getBlock() == this)
        {
            return defaultBlockState().setValue(AGE, belowState.getValue(AGE) > 0 ? 1 : 0);
        }

        final BlockState aboveState = context.getLevel().getBlockState(context.getClickedPos().above());
        return aboveState.getBlock() == this ? defaultBlockState().setValue(AGE, aboveState.getValue(AGE)) : sapling.get().defaultBlockState();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        final BlockPos belowPos = pos.below();
        final BlockState belowState = level.getBlockState(belowPos);
        return belowState.getBlock() == this || belowState.getBlock() == sapling.get() || canPlantOn(level, belowPos, belowState);
    }

    @Override
    public BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        if (!state.canSurvive(level, pos))
        {
            level.scheduleTick(pos, this, 1);
        }
        if (direction == net.minecraft.core.Direction.UP && neighborState.getBlock() == this && neighborState.getValue(AGE) > state.getValue(AGE))
        {
            level.setBlock(pos, state.cycle(AGE), 2);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void growBamboo(BlockState state, Level level, BlockPos pos, RandomSource random, int age)
    {
        final BlockState belowState = level.getBlockState(pos.below());
        final BlockPos belowPos2 = pos.below(2);
        final BlockState belowState2 = level.getBlockState(belowPos2);
        BambooLeaves leafState = BambooLeaves.NONE;
        if (age >= 1)
        {
            if (belowState2.getBlock() == this && belowState.getValue(LEAVES) != BambooLeaves.NONE)
            {
                if (belowState.getBlock() == this && belowState.getValue(LEAVES) != BambooLeaves.NONE)
                {
                    leafState = BambooLeaves.LARGE;
                    if (belowState2.getBlock() == this)
                    {
                        level.setBlock(pos.below(), belowState.setValue(LEAVES, BambooLeaves.SMALL), 3);
                        level.setBlock(belowPos2, belowState2.setValue(LEAVES, BambooLeaves.NONE), 3);
                    }
                }
            }
            else
            {
                leafState = BambooLeaves.SMALL;
            }
        }

        final int newAge = state.getValue(AGE) != 1 && belowState2.getBlock() != this ? 0 : 1;
        final int newStage = (age < 11 || random.nextFloat() >= 0.25F) && age != 15 ? 0 : 1;
        level.setBlock(pos.above(), defaultBlockState().setValue(AGE, newAge).setValue(LEAVES, leafState).setValue(STAGE, newStage), 3);
    }

    @Override
    protected int getHeightAboveUpToMax(BlockGetter level, BlockPos pos)
    {
        int i;
        for (i = 0; i < 16 && level.getBlockState(pos.above(i + 1)).getBlock() == this; ++i) { }
        return i;
    }

    @Override
    protected int getHeightBelowUpToMax(BlockGetter level, BlockPos pos)
    {
        int i;
        for (i = 0; i < 16 && level.getBlockState(pos.below(i + 1)).getBlock() == this; ++i) { }
        return i;
    }

    private boolean canPlantOn(LevelReader level, BlockPos pos, BlockState state)
    {
        return state.canSustainPlant(level, pos, Direction.UP, this) || state.is(BlockTags.BAMBOO_PLANTABLE_ON);
    }
}
