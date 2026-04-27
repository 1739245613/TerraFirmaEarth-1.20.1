package com.newterraearth.tfe.common.block.plant;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.plant.TFCTallGrassBlock;
import net.dries007.tfc.util.registry.RegistryPlant;

public abstract class NTETallShrubBlock extends TFCTallGrassBlock
{
    private static final VoxelShape SHRUB_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public static NTETallShrubBlock create(RegistryPlant plant, ExtendedProperties properties)
    {
        return new NTETallShrubBlock(properties)
        {
            @Override
            public RegistryPlant getPlant()
            {
                return plant;
            }
        };
    }

    protected NTETallShrubBlock(ExtendedProperties properties)
    {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHRUB_SHAPE;
    }
}
