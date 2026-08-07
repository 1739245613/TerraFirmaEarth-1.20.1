package com.newterraearth.tfe.api.climate;

/**
 * Common-side extension point for selecting the phase of TFE's daily
 * temperature curve. Implementations must be deterministic on both logical
 * sides and should return a value in {@code [-1, 1]}. TFE owns the latitude
 * and rainfall amplitude; providers select only its current phase.
 */
@FunctionalInterface
public interface NTEDailyTemperaturePhaseProvider
{
    /**
     * @param calendarTime TFC calendar ticks
     * @param daysInMonth the active TFC calendar's month length
     * @param z block Z, used as latitude by TFC climate
     * @param hemisphereScale TFC temperature scale from the active generator
     */
    float calculatePhase(long calendarTime, int daysInMonth, int z, float hemisphereScale);
}
