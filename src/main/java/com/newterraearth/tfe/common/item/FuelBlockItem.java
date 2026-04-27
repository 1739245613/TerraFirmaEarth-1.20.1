package com.newterraearth.tfe.common.item;

import javax.annotation.Nullable;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeHooks;

public class FuelBlockItem extends BlockItem
{
    private final ItemLike fuelItem;

    public FuelBlockItem(Block block, Item.Properties properties, ItemLike fuelItem)
    {
        super(block, properties);
        this.fuelItem = fuelItem;
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType)
    {
        return ForgeHooks.getBurnTime(new ItemStack(fuelItem), recipeType);
    }
}
