package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import net.dries007.tfc.common.blocks.plant.fruit.GrowingFruitTreeBranchBlock;

import com.newterraearth.tfe.world.NTESeasonalHelpers;

@Mixin(value = GrowingFruitTreeBranchBlock.class, remap = false)
public abstract class GrowingFruitTreeBranchBlockMixin
{
    @Redirect(
        method = "randomTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blocks/plant/fruit/FruitTreeLeavesBlock;getHydration(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I"
        ),
        remap = false,
        require = 0
    )
    private int tfe$useRootHydrationForClimateCheck(Level level, BlockPos pos)
    {
        final BlockPos stemPos = NTESeasonalHelpers.getFruitTreeStemPos(level, pos);
        return NTESeasonalHelpers.getFruitBushHydrationFromRootPos(level, stemPos.below());
    }

    @Redirect(
        method = "randomTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/climate/Climate;getAverageTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)F"
        ),
        remap = false,
        require = 0
    )
    private float tfe$useStemTemperatureForClimateCheck(Level level, BlockPos pos)
    {
        return NTESeasonalHelpers.getPlantTemperature(level, NTESeasonalHelpers.getFruitTreeStemPos(level, pos));
    }
}
