package com.newterraearth.tfe.world;

import net.minecraft.util.Mth;

import com.newterraearth.tfe.api.climate.NTEDailyTemperatureAPI;

/** Shared latitude/rainfall amplitude used with exactly one registered phase. */
public final class NTEDailyTemperatureModel
{
    private static final float EQUATOR_AMPLITUDE = 7f;
    private static final float SUBTROPICAL_AMPLITUDE = 9f;
    private static final float POLE_AMPLITUDE = 0f;
    private static final float SEASONAL_POLE_AMPLITUDE = 18f;
    private static final float SUBTROPICAL_POLAR_FRACTION = 1f / 3f;
    private static final float NEUTRAL_RAINFALL = 250f;
    private static final float MAX_RAINFALL_INPUT = 500f;
    private static final float RAINFALL_PER_DEGREE = 40f;

    private NTEDailyTemperatureModel()
    {
    }

    public static float calculateDailyTemperature(
        long calendarTime,
        int daysInMonth,
        int z,
        float temperatureScale,
        float currentRainfall
    )
    {
        final float amplitude = rainfallAdjustedAmplitude(z, temperatureScale, currentRainfall);
        final float phase = NTEDailyTemperatureAPI.calculateActivePhase(calendarTime, daysInMonth, z, temperatureScale);
        return amplitude * phase;
    }

    public static float rainfallAdjustedAmplitude(int z, float temperatureScale, float currentRainfall)
    {
        final float finiteRainfall = Float.isFinite(currentRainfall) ? currentRainfall : NEUTRAL_RAINFALL;
        final float clampedRainfall = Mth.clamp(finiteRainfall, 0f, MAX_RAINFALL_INPUT);
        final float rainfallAdjustment = (NEUTRAL_RAINFALL - clampedRainfall) / RAINFALL_PER_DEGREE;
        return Math.max(0f, latitudeAmplitude(z, temperatureScale) + rainfallAdjustment);
    }

    public static float latitudeAmplitude(int z, float temperatureScale)
    {
        final float seasonalAmplitude = NTEClimateSeasonModel.seasonalTemperatureAmplitude(z, temperatureScale);
        return latitudeAmplitudeFromSeasonalAmplitude(seasonalAmplitude);
    }

    static float latitudeAmplitudeFromSeasonalAmplitude(float seasonalAmplitude)
    {
        final float polarFraction = Mth.clamp(Math.abs(seasonalAmplitude) / SEASONAL_POLE_AMPLITUDE, 0f, 1f);
        if (polarFraction <= SUBTROPICAL_POLAR_FRACTION)
        {
            return Mth.lerp(polarFraction / SUBTROPICAL_POLAR_FRACTION, EQUATOR_AMPLITUDE, SUBTROPICAL_AMPLITUDE);
        }
        return Mth.lerp(
            (polarFraction - SUBTROPICAL_POLAR_FRACTION) / (1f - SUBTROPICAL_POLAR_FRACTION),
            SUBTROPICAL_AMPLITUDE,
            POLE_AMPLITUDE
        );
    }
}
