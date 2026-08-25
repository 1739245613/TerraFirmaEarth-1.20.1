package com.newterraearth.tfe.world.crop;

public final class NTECropTemperatureModel
{
    public static final float DEATH_MARGIN = 10f;
    public static final int STRESS_LIMIT = 20;

    public static final int HEALTH_STEP_PERCENT = 100 / STRESS_LIMIT;

    public static int updateStress(int stress, boolean conditionsValid)
    {
        if (hasFatalStress(stress))
        {
            return STRESS_LIMIT;
        }
        return conditionsValid ? Math.max(0, stress - 1) : Math.min(STRESS_LIMIT, stress + 1);
    }

    public static boolean hasFatalStress(int stress)
    {
        return stress >= STRESS_LIMIT;
    }

    public static int healthPercent(int stress)
    {
        final int clampedStress = Math.max(0, Math.min(STRESS_LIMIT, stress));
        return 100 - clampedStress * HEALTH_STEP_PERCENT;
    }

    public static boolean withinDeathRange(float minTemperature, float maxTemperature, float temperature)
    {
        return temperature >= minTemperature - DEATH_MARGIN && temperature <= maxTemperature + DEATH_MARGIN;
    }

    private NTECropTemperatureModel()
    {
    }
}
