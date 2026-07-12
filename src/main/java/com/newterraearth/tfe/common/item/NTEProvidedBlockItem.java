package com.newterraearth.tfe.common.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import com.newterraearth.tfe.NewTerraEarthMod;

public class NTEProvidedBlockItem extends BlockItem
{
    public NTEProvidedBlockItem(Block block, Properties properties)
    {
        super(block, properties);
    }

    @Override
    public String getCreatorModId(ItemStack itemStack)
    {
        return NewTerraEarthMod.MOD_ID;
    }
}
