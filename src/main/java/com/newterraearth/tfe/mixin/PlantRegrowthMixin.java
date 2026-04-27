package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import net.dries007.tfc.common.blocks.plant.PlantRegrowth;
import net.dries007.tfc.util.calendar.ServerCalendar;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.calendar.Month;

import com.newterraearth.tfe.world.NTESeasonalHelpers;

@Mixin(value = PlantRegrowth.class, remap = false)
public abstract class PlantRegrowthMixin
{
    @Redirect(
        method = "placeRisingRock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/calendar/ICalendar;getCalendarMonthOfYear()Lnet/dries007/tfc/util/calendar/Month;"
        ),
        require = 0
    )
    private static Month tfe$useHemispheralSpring(ICalendar calendar, ServerLevel level, BlockPos pos, RandomSource random)
    {
        return NTESeasonalHelpers.getHemispheralCalendarMonthOfYear(level, pos);
    }

    @Redirect(
        method = "placeRisingRock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/calendar/ServerCalendar;getCalendarMonthOfYear()Lnet/dries007/tfc/util/calendar/Month;"
        ),
        require = 0
    )
    private static Month tfe$useHemispheralSpring319(ServerCalendar calendar, ServerLevel level, BlockPos pos, RandomSource random)
    {
        return NTESeasonalHelpers.getHemispheralCalendarMonthOfYear(level, pos);
    }
}
