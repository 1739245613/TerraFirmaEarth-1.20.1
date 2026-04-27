package com.newterraearth.tfe.common.block.plant;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.IPlantable;

public class NTEBambooSaplingBlock extends BambooSaplingBlock implements IPlantable
{
    private final Supplier<? extends Block> stalk;

    public NTEBambooSaplingBlock(Properties properties, Supplier<? extends Block> stalk)
    {
        super(properties);
        this.stalk = stalk;
    }

    @Override
    protected void growBamboo(Level level, BlockPos pos)
    {
        level.setBlock(pos.above(), stalk.get().defaultBlockState().setValue(BambooStalkBlock.LEAVES, BambooLeaves.SMALL), 3);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos)
    {
        if (!state.canSurvive(level, currentPos))
        {
            return Blocks.AIR.defaultBlockState();
        }
        if (facing == Direction.UP && facingState.getBlock() == stalk.get())
        {
            level.setBlock(currentPos, stalk.get().defaultBlockState(), 2);
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state)
    {
        return new ItemStack(stalk.get());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        final BlockPos belowPos = pos.below();
        final BlockState belowState = level.getBlockState(belowPos);
        return belowState.canSustainPlant(level, belowPos, Direction.UP, this) || belowState.is(BlockTags.BAMBOO_PLANTABLE_ON);
    }

    @Override
    public BlockState getPlant(BlockGetter level, BlockPos pos)
    {
        final BlockState state = level.getBlockState(pos);
        return state.getBlock() == this ? state : defaultBlockState();
    }
}
