package com.newterraearth.tfe.common.block.plant;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.plant.PlantBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.registry.RegistryPlant;

public abstract class NTECactusBedBlock extends NTEPlantBlock
{
    public static final IntegerProperty AGE = TFCBlockStateProperties.AGE_3;

    public static NTECactusBedBlock createBarrel(RegistryPlant plant, ExtendedProperties properties)
    {
        return new NTECactusBedBlock(properties)
        {
            private static final VoxelShape SHAPE = box(4, 0, 4, 12, 8, 12);

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

    protected NTECactusBedBlock(ExtendedProperties properties)
    {
        super(properties);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        return PlantBlock.isDryBlockPlantable(level.getBlockState(pos.below()));
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

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext)
    {
        return !useContext.isSecondaryUseActive() && useContext.getItemInHand().is(this.asItem()) && state.getValue(AGE) < 3 || super.canBeReplaced(state, useContext);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        final BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (Helpers.isBlock(state, this))
        {
            return state.setValue(AGE, Math.min(3, state.getValue(AGE) + 1));
        }
        return super.getStateForPlacement(context);
    }
}
