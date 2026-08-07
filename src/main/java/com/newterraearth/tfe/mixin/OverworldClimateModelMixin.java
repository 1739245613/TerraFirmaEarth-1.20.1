package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.calendar.Month;
import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.dries007.tfc.world.chunkdata.ChunkData;

import com.newterraearth.tfe.world.NTEClimateSeasonModel;
import com.newterraearth.tfe.world.NTEDailyTemperatureModel;
import com.newterraearth.tfe.world.NTESeasonalHelpers;

@Mixin(value = OverworldClimateModel.class, remap = false)
public abstract class OverworldClimateModelMixin
{
    @Shadow private float temperatureScale;
    @Shadow protected abstract float adjustTemperatureByElevation(int y, float averageTemperature, float monthTemperature, float dailyTemperature);

    @Inject(method = "calculateMonthlyTemperature", at = @At("HEAD"), cancellable = true, remap = false)
    private void tfe$useSeasonalAmplitude(int z, float monthTemperatureModifier, CallbackInfoReturnable<Float> cir)
    {
        cir.setReturnValue(monthTemperatureModifier * NTEClimateSeasonModel.seasonalTemperatureAmplitude(z, temperatureScale));
    }

    @Inject(
        method = "getTemperature(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/dries007/tfc/world/chunkdata/ChunkData;JI)F",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void tfe$useLatitudeRainfallDailyTemperature(
        @Nullable LevelReader level,
        BlockPos pos,
        ChunkData data,
        long calendarTicks,
        int daysInMonth,
        CallbackInfoReturnable<Float> cir
    )
    {
        final Month currentMonth = ICalendar.getMonthOfYear(calendarTicks, daysInMonth);
        final float monthDelta = ICalendar.getFractionOfMonth(calendarTicks, daysInMonth);
        final float monthFactor = Mth.lerp(monthDelta, currentMonth.getTemperatureModifier(), currentMonth.next().getTemperatureModifier());
        final float monthTemperature = NTEClimateSeasonModel.seasonalTemperature(pos.getZ(), temperatureScale, monthFactor);

        float currentRainfall = data.getRainfall(pos);
        if (level instanceof Level runtimeLevel)
        {
            currentRainfall = NTESeasonalHelpers.getInstantRainfall(runtimeLevel, pos, calendarTicks, daysInMonth, currentRainfall);
        }
        final float dailyTemperature = NTEDailyTemperatureModel.calculateDailyTemperature(
            calendarTicks,
            daysInMonth,
            pos.getZ(),
            temperatureScale,
            currentRainfall
        );
        cir.setReturnValue(adjustTemperatureByElevation(
            pos.getY(),
            data.getAverageTemp(pos),
            monthTemperature,
            dailyTemperature
        ));
    }
}
