package com.newterraearth.tfe.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blocks.plant.fruit.SpreadingBushBlock;

import com.newterraearth.tfe.world.NTESeasonalHelpers;

@Mixin(value = SpreadingBushBlock.class, remap = false)
public abstract class SpreadingBushBlockMixin
{
    /**
     * @author Codex
     * @reason Spreading bush overlays should use the same current-temperature and instant-hydration bridge as other seasonal bushes.
     */
    @Overwrite(remap = false)
    public void addHoeOverlayInfo(Level level, BlockPos pos, BlockState state, List<Component> text, boolean isDebug)
    {
        final var range = ((SeasonalPlantBlockAccessor) this).tfe$getClimateRange().get();
        NTESeasonalHelpers.addPlantClimateTooltips(text, level, pos, pos.below(), range, NTESeasonalHelpers.getFruitBushHydrationFromRootPos(level, pos.below()));
    }
}
