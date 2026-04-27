package com.newterraearth.tfe.common.block.plant;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.plant.PlantBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.registry.RegistryPlant;

public abstract class NTEPlantBlock extends PlantBlock
{
    public static NTEPlantBlock createShortShrub(RegistryPlant plant, ExtendedProperties properties)
    {
        return new NTEPlantBlock(properties)
        {
            private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

            @Override
            public RegistryPlant getPlant()
            {
                return plant;
            }

            @Override
            public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
            {
                return SHAPE;
            }
        };
    }

    public static NTEPlantBlock createShrub(RegistryPlant plant, ExtendedProperties properties)
    {
        return new NTEPlantBlock(properties)
        {
            private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

            @Override
            public RegistryPlant getPlant()
            {
                return plant;
            }

            @Override
            public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
            {
                return SHAPE;
            }
        };
    }

    public static NTEPlantBlock createPerchedEpiphyte(RegistryPlant plant, ExtendedProperties properties)
    {
        return new NTEPlantBlock(properties)
        {
            @Override
            public RegistryPlant getPlant()
            {
                return plant;
            }

            @Override
            public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
            {
                return Helpers.isBlock(level.getBlockState(pos.below()), TFCTags.Blocks.BUSH_PLANTABLE_ON);
            }
        };
    }

    public static NTEPlantBlock createFlowerbed(RegistryPlant plant, ExtendedProperties properties)
    {
        return new NTEPlantBlock(properties)
        {
            private static final VoxelShape SHAPE = box(0, 0, 0, 16, 3, 16);

            @Override
            public RegistryPlant getPlant()
            {
                return plant;
            }

            @Override
            public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
            {
                return SHAPE;
            }

            @Override
            public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext)
            {
                final IntegerProperty ageProp = AGE;
                if (!useContext.isSecondaryUseActive() && useContext.getItemInHand().is(this.asItem()) && state.getValue(ageProp) < ageProp.getPossibleValues().stream().max(Integer::compareTo).orElse(0))
                {
                    return true;
                }
                return super.canBeReplaced(state, useContext);
            }

            @Nullable
            @Override
            public BlockState getStateForPlacement(BlockPlaceContext context)
            {
                final BlockState state = context.getLevel().getBlockState(context.getClickedPos());
                final IntegerProperty ageProp = AGE;
                if (Helpers.isBlock(state, this))
                {
                    return state.setValue(ageProp, Math.min(ageProp.getPossibleValues().stream().max(Integer::compareTo).orElse(0), state.getValue(ageProp) + 1));
                }
                return super.getStateForPlacement(context);
            }
        };
    }

    protected NTEPlantBlock(ExtendedProperties properties)
    {
        super(properties);
    }
}
