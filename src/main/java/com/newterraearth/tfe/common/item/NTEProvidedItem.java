package com.newterraearth.tfe.common.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.newterraearth.tfe.NewTerraEarthMod;

public class NTEProvidedItem extends Item
{
    public NTEProvidedItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public String getCreatorModId(ItemStack itemStack)
    {
        return NewTerraEarthMod.MOD_ID;
    }
}
