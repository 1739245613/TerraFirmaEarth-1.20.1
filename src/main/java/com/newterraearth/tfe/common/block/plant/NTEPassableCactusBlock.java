package com.newterraearth.tfe.common.block.plant;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.plant.PlantBlock;
import net.dries007.tfc.common.blocks.plant.TFCTallGrassBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.registry.RegistryPlant;

public abstract class NTEPassableCactusBlock extends TFCTallGrassBlock
{
    public static NTEPassableCactusBlock create(RegistryPlant plant, ExtendedProperties properties)
    {
        return new NTEPassableCactusBlock(properties)
        {
            @Override
            public RegistryPlant getPlant()
            {
                return plant;
            }
        };
    }

    protected NTEPassableCactusBlock(ExtendedProperties properties)
    {
        super(properties);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        final BlockState belowState = level.getBlockState(pos.below());
        if (state.getValue(PART) == Part.LOWER)
        {
            return Helpers.isBlock(belowState, TFCTags.Blocks.BUSH_PLANTABLE_ON) || PlantBlock.isDryBlockPlantable(belowState);
        }
        if (state.getBlock() != this)
        {
            return Helpers.isBlock(belowState, TFCTags.Blocks.BUSH_PLANTABLE_ON) || PlantBlock.isDryBlockPlantable(belowState);
        }
        return belowState.getBlock() == this && belowState.getValue(PART) == Part.LOWER;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity)
    {
        entity.hurt(level.damageSources().cactus(), 1f);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType pathComputationType)
    {
        return false;
    }
}
