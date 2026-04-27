package com.newterraearth.tfe.common.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blockentities.CropBlockEntity;

public class NTECropBlockEntity extends CropBlockEntity
{
    public static void serverTick(Level level, BlockPos pos, BlockState state, NTECropBlockEntity crop)
    {
        CropBlockEntity.serverTick(level, pos, state, crop);
    }

    public NTECropBlockEntity(BlockPos pos, BlockState state)
    {
        super(NTEBlockEntities.CROP.get(), pos, state);
    }
}
