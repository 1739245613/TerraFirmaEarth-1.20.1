/*
 * Licensed under the EUPL, Version 1.2.
 * Derived from TerraFirmaCraft 1.21 SolarCalculator and SkyPos.
 */
package com.newterraearth.tfe.api.climate;

import net.minecraft.util.Mth;

/**
 * Common-side port of TFC 1.21's solar position calculations. This class has
 * no renderer dependency, so common-side addons may reuse the same values for
 * gameplay while client integrations pass them to rendering or shader APIs.
 */
public final class NTE121SolarCalculator
{
    private NTE121SolarCalculator()
    {
    }

    public static int getSunBasedDayTime(int z, float hemisphereScale, float fractionOfYear, float fractionOfDay)
    {
        final float zenith = getSunPosition(z, hemisphereScale, fractionOfYear, fractionOfDay).zenith();
        if (fractionOfDay < 0.5f)
        {
            if (zenith > Mth.HALF_PI)
            {
                final float minZenith = getSunPosition(z, hemisphereScale, fractionOfYear, 0f).zenith();
                return (int) Mth.clampedMap(zenith, minZenith, Mth.HALF_PI, 18_000, 24_000);
            }
            final float maxZenith = getSunPosition(z, hemisphereScale, fractionOfYear, 0.5f).zenith();
            return (int) Mth.clampedMap(zenith, Mth.HALF_PI, maxZenith, 0, 6_000);
        }

        if (zenith < Mth.HALF_PI)
        {
            final float maxZenith = getSunPosition(z, hemisphereScale, fractionOfYear, 0.5f).zenith();
            return (int) Mth.clampedMap(zenith, maxZenith, Mth.HALF_PI, 6_000, 12_000);
        }
        final float minZenith = getSunPosition(z, hemisphereScale, fractionOfYear, 1f).zenith();
        return (int) Mth.clampedMap(zenith, Mth.HALF_PI, minZenith, 12_000, 18_000);
    }

    public static float calculateDailyTemperaturePhase(int z, float hemisphereScale, float fractionOfYear, float fractionOfDay)
    {
        final float sunBasedFractionOfDay = getSunBasedDayTime(z, hemisphereScale, fractionOfYear, fractionOfDay) / 24_000f;
        return sunBasedFractionOfDay < 0.5f
            ? Mth.map(sunBasedFractionOfDay, 0f, 0.5f, -1f, 1f)
            : Mth.map(sunBasedFractionOfDay, 0.5f, 1f, 1f, -1f);
    }

    public static SkyPosition getSunPosition(int z, float hemisphereScale, float fractionOfYear, float fractionOfDay)
    {
        final double latitude = getLatitude(z, hemisphereScale);
        final double declination = 23.44f * Mth.DEG_TO_RAD * Mth.sin(Mth.TWO_PI * (284f / 365f + fractionOfYear));
        final double hourAngle = Mth.TWO_PI * (0.5f - fractionOfDay);

        final double sinLatitude = Math.sin(latitude);
        final double cosLatitude = org.joml.Math.cosFromSin(sinLatitude, latitude);
        final double sinDeclination = Math.sin(declination);
        final double cosDeclination = org.joml.Math.cosFromSin(sinDeclination, declination);
        final double solarZenithAngle = Math.acos(Mth.clamp(
            sinLatitude * sinDeclination + cosLatitude * cosDeclination * Math.cos(hourAngle),
            -1d,
            1d
        ));

        final double sinZenith = Math.sin(solarZenithAngle);
        final double cosZenith = org.joml.Math.cosFromSin(sinZenith, solarZenithAngle);
        final double absSolarAzimuthAngle = Math.acos(Mth.clamp(
            (sinDeclination - cosZenith * sinLatitude) / (sinZenith * cosLatitude),
            -1d,
            1d
        ));
        final double solarAzimuthAngle = hourAngle < 0d
            ? absSolarAzimuthAngle
            : Mth.TWO_PI - absSolarAzimuthAngle;
        return SkyPosition.of(solarZenithAngle, solarAzimuthAngle);
    }

    public static float getLatitude(int z, float hemisphereScale)
    {
        if (hemisphereScale == 0f)
        {
            return 0f;
        }
        return triangle(-Mth.HALF_PI, 0f, 1f / (4f * hemisphereScale), z - 0.5f * hemisphereScale);
    }

    private static float triangle(float amplitude, float midpoint, float frequency, float value)
    {
        return midpoint + amplitude * (Math.abs(4f * frequency * value + 1f - 4f * (float) Math.floor(frequency * value + 0.75f)) - 1f);
    }

    public record SkyPosition(float zenith, float azimuth)
    {
        public static SkyPosition of(double zenith, double azimuth)
        {
            return new SkyPosition((float) zenith, (float) azimuth);
        }
    }
}
