package com.newterraearth.tfe.world;

public final class NTEClimateSeasonModel
{
    private NTEClimateSeasonModel()
    {
    }

    public static float seasonalTemperatureAmplitude(int z, float temperatureScale)
    {
        return Math.abs(signedSeasonalTemperatureAmplitude(z, temperatureScale));
    }

    public static float seasonalTemperature(int z, float temperatureScale, float monthTemperatureModifier)
    {
        return monthTemperatureModifier * signedSeasonalTemperatureAmplitude(z, temperatureScale);
    }

    public static boolean isNorthernHemisphere(int z, float hemisphereScale)
    {
        if (hemisphereScale == 0f)
        {
            return true;
        }
        final int adjustedZ = z - (int) (hemisphereScale / 2f);
        final int poleToPoleDistance = (int) (hemisphereScale * 2f);
        final int normalizedZ = Math.floorMod(adjustedZ, poleToPoleDistance * 2);
        return normalizedZ > poleToPoleDistance;
    }

    private static float signedSeasonalTemperatureAmplitude(int z, float temperatureScale)
    {
        return temperatureScale == 0f
            ? 0f
            : triangle(-18f, 0f, 1f / (4f * temperatureScale), z - temperatureScale / 2f);
    }

    private static float triangle(float amplitude, float midpoint, float frequency, float value)
    {
        return midpoint + amplitude * (Math.abs(4f * frequency * value + 1f - 4f * (float) Math.floor(frequency * value + 0.75f)) - 1f);
    }
}
