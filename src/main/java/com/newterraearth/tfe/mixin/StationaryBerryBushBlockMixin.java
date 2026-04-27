package com.newterraearth.tfe.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.blocks.plant.fruit.SeasonalPlantBlock;
import net.dries007.tfc.common.blocks.plant.fruit.StationaryBerryBushBlock;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.Climate;

import com.newterraearth.tfe.config.NTECommonConfig;
import com.newterraearth.tfe.world.NTESeasonalHelpers;

@Mixin(value = StationaryBerryBushBlock.class, remap = false)
public abstract class StationaryBerryBushBlockMixin
{
    @Shadow protected abstract boolean mayDie(Level level, BlockPos pos, BlockState state, int monthsSpentDying);

    @Shadow protected abstract BlockState getDeadState(BlockState state);

    @Shadow protected abstract BlockState growAndPropagate(Level level, BlockPos pos, net.minecraft.util.RandomSource random, BlockState state);

    /**
     * @author Codex
     * @reason Berry bushes should initialize with hemisphere-aware lifecycle months.
     */
    @Overwrite(remap = true)
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        final SeasonalPlantBlockAccessor accessor = (SeasonalPlantBlockAccessor) this;
        final Lifecycle lifecycle = accessor.tfe$invokeGetLifecycleForMonth(NTESeasonalHelpers.getHemispheralCalendarMonthOfYear(context.getLevel(), context.getClickedPos()));
        return ((StationaryBerryBushBlock) (Object) this).defaultBlockState().setValue(StationaryBerryBushBlock.LIFECYCLE, lifecycle.active() ? Lifecycle.HEALTHY : Lifecycle.DORMANT);
    }

    /**
     * @author Codex
     * @reason Berry bush lifecycle replay needs hemisphere-aware month lookup while preserving the 1.20 growth chain.
     */
    @Overwrite(remap = false)
    public void onUpdate(Level level, BlockPos pos, BlockState state)
    {
        if (level.getBlockEntity(pos) instanceof BerryBushBlockEntity bush)
        {
            final SeasonalPlantBlockAccessor accessor = (SeasonalPlantBlockAccessor) this;
            Lifecycle currentLifecycle = state.getValue(StationaryBerryBushBlock.LIFECYCLE);
            Lifecycle expectedLifecycle = accessor.tfe$invokeGetLifecycleForMonth(NTESeasonalHelpers.getHemispheralCalendarMonthOfYear(level, pos));
            if (!SeasonalPlantBlock.checkAndSetDormant(level, pos, state, currentLifecycle, expectedLifecycle))
            {
                long deltaTicks = Math.min(bush.getTicksSinceBushUpdate(), Calendars.SERVER.getCalendarTicksInYear());
                long currentCalendarTick = Calendars.SERVER.getCalendarTicks();
                long nextCalendarTick = currentCalendarTick - deltaTicks;

                final var range = accessor.tfe$getClimateRange().get();
                final int hydration = NTESeasonalHelpers.getFruitBushHydrationFromRootPos(level, pos.below());

                int monthsSpentDying = 0;
                do
                {
                    nextCalendarTick = Math.min(nextCalendarTick + Calendars.SERVER.getCalendarTicksInMonth(), currentCalendarTick);

                    final float temperatureAtNextTick = Climate.getTemperature(level, pos, nextCalendarTick, Calendars.SERVER.getCalendarDaysInMonth());
                    final Lifecycle lifecycleAtNextTick = accessor.tfe$invokeGetLifecycleForMonth(NTESeasonalHelpers.getHemispheralCalendarMonthOfYear(level, pos, nextCalendarTick));
                    if (range.checkBoth(hydration, temperatureAtNextTick, false))
                    {
                        currentLifecycle = currentLifecycle.advanceTowards(lifecycleAtNextTick);
                    }
                    else
                    {
                        currentLifecycle = Lifecycle.DORMANT;
                    }

                    if (lifecycleAtNextTick != Lifecycle.DORMANT && currentLifecycle == Lifecycle.DORMANT)
                    {
                        monthsSpentDying++;
                    }
                    else
                    {
                        monthsSpentDying = 0;
                    }
                }
                while (nextCalendarTick < currentCalendarTick);

                final BlockState newState = mayDie(level, pos, state, monthsSpentDying)
                    ? getDeadState(state)
                    : growAndPropagate(level, pos, level.getRandom(), state.setValue(StationaryBerryBushBlock.LIFECYCLE, currentLifecycle));

                if (state != newState)
                {
                    level.setBlock(pos, newState, 3);
                }
            }
        }
    }

    /**
     * @author Codex
     * @reason Hoe overlay should reflect the ported root hydration and average temperature semantics.
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
