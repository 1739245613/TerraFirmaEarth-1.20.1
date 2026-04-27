package com.newterraearth.tfe.common.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blockentities.TickCounterBlockEntity;

public class NTETickCounterBlockEntity extends TickCounterBlockEntity
{
    public NTETickCounterBlockEntity(BlockPos pos, BlockState state)
    {
        super(NTEBlockEntities.TICK_COUNTER.get(), pos, state);
    }
}
