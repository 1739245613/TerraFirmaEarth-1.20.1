package com.newterraearth.tfe.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.common.blocks.plant.fruit.FruitTreeLeavesBlock;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.blocks.plant.fruit.SeasonalPlantBlock;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.util.climate.Climate;

import com.newterraearth.tfe.config.NTECommonConfig;
import com.newterraearth.tfe.world.NTESeasonalHelpers;

@Mixin(value = FruitTreeLeavesBlock.class, remap = false)
public abstract class FruitTreeLeavesBlockMixin
{
    /**
     * @author Codex
     * @reason Fruit tree leaves should use hemisphere-aware lifecycle months and evaluate climate from the tree base.
     */
    @Overwrite(remap = false)
    public void onUpdate(Level level, BlockPos pos, BlockState state)
    {
        if (state.getValue(FruitTreeLeavesBlock.PERSISTENT))
        {
            return;
        }
        if (level.getBlockEntity(pos) instanceof BerryBushBlockEntity)
        {
            final SeasonalPlantBlockAccessor accessor = (SeasonalPlantBlockAccessor) this;
            Lifecycle currentLifecycle = state.getValue(FruitTreeLeavesBlock.LIFECYCLE);
            Lifecycle expectedLifecycle = accessor.tfe$invokeGetLifecycleForMonth(NTESeasonalHelpers.getHemispheralCalendarMonthOfYear(level, pos));
            if (!SeasonalPlantBlock.checkAndSetDormant(level, pos, state, currentLifecycle, expectedLifecycle))
            {
                final var range = accessor.tfe$getClimateRange().get();
                final BlockPos stemPos = NTESeasonalHelpers.getFruitTreeStemPos(level, pos);
                final int hydration = NTESeasonalHelpers.getFruitBushHydrationFromRootPos(level, stemPos.below());

                if (range.checkBoth(hydration, Climate.getAverageTemperature(level, stemPos), false))
                {
                    currentLifecycle = currentLifecycle.advanceTowards(expectedLifecycle);
                }
                else
                {
                    currentLifecycle = Lifecycle.DORMANT;
                }

                final BlockState newState = state.setValue(FruitTreeLeavesBlock.LIFECYCLE, currentLifecycle);
                if (state != newState)
                {
                    level.setBlock(pos, newState, 3);
                }
            }
        }
    }

    /**
     * @author Codex
     * @reason Fruit tree leaf overlays should use the tree stem/root climate, not canopy height only.
     */
    @Overwrite(remap = false)
    public void addHoeOverlayInfo(Level level, BlockPos pos, BlockState state, List<Component> text, boolean isDebug)
    {
        final var range = ((SeasonalPlantBlockAccessor) this).tfe$getClimateRange().get();
        final BlockPos stemPos = NTESeasonalHelpers.getFruitTreeStemPos(level, pos);
        text.add(FarmlandBlock.getHydrationTooltip(level, stemPos, range, false, NTESeasonalHelpers.getFruitBushHydrationFromRootPos(level, stemPos.below())));
        NTESeasonalHelpers.addAverageHydrationTooltipIfNeeded(text, level, stemPos.below(), range, false, NTECommonConfig.useCurrentRainfallForFruit());
        text.add(FarmlandBlock.getAverageTemperatureTooltip(level, stemPos, range, false));
    }
}
