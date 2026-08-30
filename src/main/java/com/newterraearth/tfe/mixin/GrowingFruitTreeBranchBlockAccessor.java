package com.newterraearth.tfe.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.dries007.tfc.common.blocks.plant.fruit.GrowingFruitTreeBranchBlock;
import net.dries007.tfc.util.climate.ClimateRange;

@Mixin(value = GrowingFruitTreeBranchBlock.class, remap = false)
public interface GrowingFruitTreeBranchBlockAccessor
{
    @Accessor("climateRange")
    Supplier<ClimateRange> tfe$getClimateRange();
}
