package com.newterraearth.tfe.common.block;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blocks.plant.PlantRegrowth;
import net.dries007.tfc.common.blocks.soil.ConnectedGrassBlock;

public class ConnectedDuffBlock extends ConnectedGrassBlock
{
    public ConnectedDuffBlock(Properties properties, Supplier<? extends Block> dirt, @Nullable Supplier<? extends Block> path, @Nullable Supplier<? extends Block> farmland)
    {
        super(properties, dirt, path, farmland);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (!canBeGrass(state, level, pos))
        {
            if (level.isAreaLoaded(pos, 3))
            {
                level.setBlockAndUpdate(pos, getDirt());
            }
        }
        else
        {
            PlantRegrowth.placeRisingRock(level, pos.above(), random);
        }
    }
}
