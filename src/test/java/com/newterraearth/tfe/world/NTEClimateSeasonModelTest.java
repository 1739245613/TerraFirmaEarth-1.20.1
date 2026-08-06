package com.newterraearth.tfe.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NTEClimateSeasonModelTest
{
    private static final float SCALE = 20_000f;
    private static final float EPSILON = 0.001f;

    @Test
    void seasonalAmplitudeRunsLinearlyFromZeroAtEquatorToEighteenAtPoles()
    {
        assertEquals(0f, NTEClimateSeasonModel.seasonalTemperatureAmplitude(-30_000, SCALE), EPSILON);
        assertEquals(18f, NTEClimateSeasonModel.seasonalTemperatureAmplitude(-10_000, SCALE), EPSILON);
        assertEquals(9f, NTEClimateSeasonModel.seasonalTemperatureAmplitude(0, SCALE), EPSILON);
        assertEquals(4.5f, NTEClimateSeasonModel.seasonalTemperatureAmplitude(5_000, SCALE), EPSILON);
        assertEquals(0f, NTEClimateSeasonModel.seasonalTemperatureAmplitude(10_000, SCALE), EPSILON);
        assertEquals(9f, NTEClimateSeasonModel.seasonalTemperatureAmplitude(20_000, SCALE), EPSILON);
        assertEquals(18f, NTEClimateSeasonModel.seasonalTemperatureAmplitude(30_000, SCALE), EPSILON);
        assertEquals(0f, NTEClimateSeasonModel.seasonalTemperatureAmplitude(50_000, SCALE), EPSILON);
    }

    @Test
    void mirroredHemispheresUseOppositeTemperatureSeasons()
    {
        assertEquals(-9f, NTEClimateSeasonModel.seasonalTemperature(0, SCALE, -1f), EPSILON);
        assertEquals(9f, NTEClimateSeasonModel.seasonalTemperature(20_000, SCALE, -1f), EPSILON);
        assertEquals(9f, NTEClimateSeasonModel.seasonalTemperature(0, SCALE, 1f), EPSILON);
        assertEquals(-9f, NTEClimateSeasonModel.seasonalTemperature(20_000, SCALE, 1f), EPSILON);
    }

    @Test
    void seasonalTemperatureCrossesTheEquatorContinuously()
    {
        final float north = NTEClimateSeasonModel.seasonalTemperature(9_999, SCALE, 1f);
        final float equator = NTEClimateSeasonModel.seasonalTemperature(10_000, SCALE, 1f);
        final float south = NTEClimateSeasonModel.seasonalTemperature(10_001, SCALE, 1f);

        assertEquals(0.0009f, north, 0.00001f);
        assertEquals(0f, equator, 0.00001f);
        assertEquals(-0.0009f, south, 0.00001f);
        assertTrue(Math.abs(north - south) < 0.002f);
    }

    @Test
    void zeroScaleDisablesSeasonalTemperature()
    {
        assertEquals(0f, NTEClimateSeasonModel.seasonalTemperatureAmplitude(10_000, 0f), EPSILON);
        assertEquals(0f, NTEClimateSeasonModel.seasonalTemperature(10_000, 0f, 1f), EPSILON);
    }
}
