package com.newterraearth.tfe.common.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blockentities.FarmlandBlockEntity;

public class NTEFarmlandBlockEntity extends FarmlandBlockEntity
{
    public NTEFarmlandBlockEntity(BlockPos pos, BlockState state)
    {
        super(NTEBlockEntities.FARMLAND.get(), pos, state);
    }
}
