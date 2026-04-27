package com.newterraearth.tfe.common.block;

import javax.annotation.Nullable;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.wood.ExtendedRotatedPillarBlock;
import net.dries007.tfc.util.Helpers;

public class GoldenBambooBlock extends ExtendedRotatedPillarBlock
{
    public GoldenBambooBlock(ExtendedProperties properties)
    {
        super(properties);
    }

    @Nullable
    @Override
    public BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction action, boolean simulate)
    {
        if (action == ToolActions.AXE_STRIP && context.getItemInHand().canPerformAction(action))
        {
            return Helpers.copyProperties(Blocks.STRIPPED_BAMBOO_BLOCK.defaultBlockState(), state);
        }
        return null;
    }
}
