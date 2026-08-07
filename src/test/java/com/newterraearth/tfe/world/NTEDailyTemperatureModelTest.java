package com.newterraearth.tfe.world;

import org.junit.jupiter.api.Test;

import net.dries007.tfc.util.calendar.ICalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NTEDailyTemperatureModelTest
{
    private static final float SCALE = 20_000f;
    private static final float EPSILON = 0.001f;

    @Test
    void latitudeAmplitudeUsesEquatorSubtropicalAndPolarAnchors()
    {
        assertEquals(7f, NTEDailyTemperatureModel.latitudeAmplitudeFromSeasonalAmplitude(0f), EPSILON);
        assertEquals(9f, NTEDailyTemperatureModel.latitudeAmplitudeFromSeasonalAmplitude(6f), EPSILON);
        assertEquals(6.75f, NTEDailyTemperatureModel.latitudeAmplitudeFromSeasonalAmplitude(9f), EPSILON);
        assertEquals(0f, NTEDailyTemperatureModel.latitudeAmplitudeFromSeasonalAmplitude(18f), EPSILON);
    }

    @Test
    void rainfallAdjustmentIsCenteredAtTwoHundredFifty()
    {
        assertEquals(13.25f, NTEDailyTemperatureModel.rainfallAdjustedAmplitude(10_000, SCALE, 0f), EPSILON);
        assertEquals(7f, NTEDailyTemperatureModel.rainfallAdjustedAmplitude(10_000, SCALE, 250f), EPSILON);
        assertEquals(0.75f, NTEDailyTemperatureModel.rainfallAdjustedAmplitude(10_000, SCALE, 500f), EPSILON);
        assertEquals(0.75f, NTEDailyTemperatureModel.rainfallAdjustedAmplitude(10_000, SCALE, 1_000f), EPSILON);
        assertEquals(7f, NTEDailyTemperatureModel.rainfallAdjustedAmplitude(10_000, SCALE, Float.NaN), EPSILON);
        assertEquals(6.25f, NTEDailyTemperatureModel.rainfallAdjustedAmplitude(-10_000, SCALE, 0f), EPSILON);
        assertEquals(0f, NTEDailyTemperatureModel.rainfallAdjustedAmplitude(-10_000, SCALE, 250f), EPSILON);
        assertEquals(0f, NTEDailyTemperatureModel.rainfallAdjustedAmplitude(-10_000, SCALE, 500f), EPSILON);
    }

    @Test
    void mirroredLatitudesHaveTheSameDailyAmplitude()
    {
        assertEquals(
            NTEDailyTemperatureModel.latitudeAmplitude(5_000, SCALE),
            NTEDailyTemperatureModel.latitudeAmplitude(15_000, SCALE),
            EPSILON
        );
    }

    @Test
    void defaultClockPhaseAppliesTheNewEquatorialRainfallAmplitudes()
    {
        final long noon = 12L * ICalendar.TICKS_IN_HOUR;
        final long midnight = 0L;

        assertEquals(13.25f, NTEDailyTemperatureModel.calculateDailyTemperature(noon, 8, 10_000, SCALE, 0f), EPSILON);
        assertEquals(-13.25f, NTEDailyTemperatureModel.calculateDailyTemperature(midnight, 8, 10_000, SCALE, 0f), EPSILON);
        assertEquals(7f, NTEDailyTemperatureModel.calculateDailyTemperature(noon, 8, 10_000, SCALE, 250f), EPSILON);
        assertEquals(-7f, NTEDailyTemperatureModel.calculateDailyTemperature(midnight, 8, 10_000, SCALE, 250f), EPSILON);
        assertEquals(0.75f, NTEDailyTemperatureModel.calculateDailyTemperature(noon, 8, 10_000, SCALE, 500f), EPSILON);
        assertEquals(-0.75f, NTEDailyTemperatureModel.calculateDailyTemperature(midnight, 8, 10_000, SCALE, 500f), EPSILON);
    }
}
