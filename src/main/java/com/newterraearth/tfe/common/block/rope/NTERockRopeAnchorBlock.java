package com.newterraearth.tfe.common.block.rope;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.rock.RockSpikeBlock;

public class NTERockRopeAnchorBlock extends NTERopeAnchorBlock
{
    private final Supplier<? extends Block> spike;

    public NTERockRopeAnchorBlock(ExtendedProperties properties, Supplier<? extends Block> spike)
    {
        super(properties);
        this.spike = spike;
    }

    public Supplier<? extends Block> getSpike()
    {
        return spike;
    }

    @Override
    protected BlockState getStateAfterRemoval(BlockState state)
    {
        return spike.get().defaultBlockState().setValue(RockSpikeBlock.PART, RockSpikeBlock.Part.TIP);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player)
    {
        return spike.get().getCloneItemStack(state, target, level, pos, player);
    }

}
