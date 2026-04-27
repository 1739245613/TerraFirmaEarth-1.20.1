package com.newterraearth.tfe.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blocks.plant.fruit.SpreadingBushBlock;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;

import com.newterraearth.tfe.config.NTECommonConfig;
import com.newterraearth.tfe.world.NTESeasonalHelpers;

@Mixin(value = SpreadingBushBlock.class, remap = false)
public abstract class SpreadingBushBlockMixin
{
    /**
     * @author Codex
     * @reason Spreading bush overlays should use the same average-temperature and instant-hydration bridge as other seasonal bushes.
     */
    @Overwrite(remap = false)
    public void addHoeOverlayInfo(Level level, BlockPos pos, BlockState state, List<Component> text, boolean isDebug)
    {
        final var range = ((SeasonalPlantBlockAccessor) this).tfe$getClimateRange().get();
        text.add(FarmlandBlock.getHydrationTooltip(level, pos, range, false, NTESeasonalHelpers.getFruitBushHydrationFromRootPos(level, pos.below())));
        NTESeasonalHelpers.addAverageHydrationTooltipIfNeeded(text, level, pos.below(), range, false, NTECommonConfig.useCurrentRainfallForFruit());
        text.add(FarmlandBlock.getAverageTemperatureTooltip(level, pos, range, false));
    }
}
