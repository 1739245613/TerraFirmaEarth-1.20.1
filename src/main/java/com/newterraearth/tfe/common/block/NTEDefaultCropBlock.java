package com.newterraearth.tfe.common.block;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.crop.DefaultCropBlock;

import com.newterraearth.tfe.world.crop.NTECrop;

public final class NTEDefaultCropBlock
{
    private NTEDefaultCropBlock()
    {
    }

    public static DefaultCropBlock create(ExtendedProperties properties, int stages, Supplier<? extends Block> dead, Supplier<? extends Item> seeds, NTECrop crop)
    {
        final IntegerProperty property = TFCBlockStateProperties.getAgeProperty(stages - 1);
        return new DefaultCropBlock(properties, stages - 1, dead, seeds, crop.primaryNutrient(), crop.climateRange())
        {
            @Override
            public IntegerProperty getAgeProperty()
            {
                return property;
            }
        };
    }
}
