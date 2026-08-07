package com.newterraearth.tfe.api.climate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NTE121SolarCalculatorTest
{
    private static final float SCALE = 20_000f;
    private static final float EQUINOX_FRACTION = 81f / 365f;

    @Test
    void equatorialEquinoxPhaseTracksSunriseAndSunset()
    {
        assertEquals(0f, NTE121SolarCalculator.calculateDailyTemperaturePhase(10_000, SCALE, EQUINOX_FRACTION, 0f), 0.01f);
        assertEquals(-1f, NTE121SolarCalculator.calculateDailyTemperaturePhase(10_000, SCALE, EQUINOX_FRACTION, 0.25f), 0.01f);
        assertEquals(0f, NTE121SolarCalculator.calculateDailyTemperaturePhase(10_000, SCALE, EQUINOX_FRACTION, 0.5f), 0.01f);
        assertEquals(1f, NTE121SolarCalculator.calculateDailyTemperaturePhase(10_000, SCALE, EQUINOX_FRACTION, 0.75f), 0.01f);
    }

    @Test
    void latitudeUsesTheConfiguredHemisphereScale()
    {
        assertEquals(0f, NTE121SolarCalculator.getLatitude(10_000, SCALE), 0.0001f);
        assertEquals(Math.PI / 2d, NTE121SolarCalculator.getLatitude(-10_000, SCALE), 0.0001f);
        assertEquals(-Math.PI / 2d, NTE121SolarCalculator.getLatitude(30_000, SCALE), 0.0001f);
    }
}
