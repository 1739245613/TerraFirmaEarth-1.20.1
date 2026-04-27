package com.newterraearth.tfe.common.block;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;

import com.newterraearth.tfe.common.blockentities.NTEBlockEntities;

public class NTEFarmlandBlock extends FarmlandBlock
{
    public NTEFarmlandBlock(ExtendedProperties properties, Supplier<? extends Block> dirt)
    {
        super(properties, dirt);
    }

    @Override
    public void addHoeOverlayInfo(Level level, BlockPos pos, BlockState state, List<Component> text, boolean isDebug)
    {
        level.getBlockEntity(pos, NTEBlockEntities.FARMLAND.get()).ifPresent(farmland -> farmland.addHoeOverlayInfo(level, pos, text, true, true));
    }
}
