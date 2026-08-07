package com.newterraearth.tfe.api.climate;

import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import net.dries007.tfc.util.calendar.ICalendar;

import com.newterraearth.tfe.NewTerraEarthMod;

/**
 * Registration and dispatch API for the one active daily-temperature phase.
 * Addons must register during mod initialization or common setup, before the
 * load-complete lifecycle event freezes this registry.
 */
public final class NTEDailyTemperatureAPI
{
    public static final ResourceLocation DEFAULT_CLOCK_ID = new ResourceLocation(NewTerraEarthMod.MOD_ID, "clock_120");
    public static final NTEDailyTemperaturePhaseProvider CLOCK_120 = NTEDailyTemperatureAPI::calculateClockPhase;
    public static final NTEDailyTemperaturePhaseProvider SOLAR_121 = NTEDailyTemperatureAPI::calculateSolarPhase;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean WARNED_INVALID_PHASE = new AtomicBoolean();
    private static final NTEDailyTemperaturePhaseRegistry REGISTRY = new NTEDailyTemperaturePhaseRegistry(DEFAULT_CLOCK_ID, CLOCK_120);

    private NTEDailyTemperatureAPI()
    {
    }

    /**
     * Registers the only custom phase provider for this game instance.
     * Call during mod construction or common setup on both logical sides.
     *
     * @throws IllegalStateException if another custom provider is active or registration is already frozen
     * @throws IllegalArgumentException if {@code id} is the reserved default id
     */
    public static void registerPhaseProvider(ResourceLocation id, NTEDailyTemperaturePhaseProvider provider)
    {
        REGISTRY.register(id, provider);
        LOGGER.info("Registered TFE daily temperature phase provider {}", id);
    }

    /** Convenience registration for addons that want the built-in TFC 1.21 solar calculation. */
    public static void register121SolarPhaseProvider(ResourceLocation id)
    {
        registerPhaseProvider(id, SOLAR_121);
    }

    public static ResourceLocation activePhaseProviderId()
    {
        return REGISTRY.activeId();
    }

    /** Returns the normalized phase currently consumed by TFE's real temperature calculation. */
    public static float calculateActivePhase(long calendarTime, int daysInMonth, int z, float hemisphereScale)
    {
        final float phase = REGISTRY.activeProvider().calculatePhase(calendarTime, daysInMonth, z, hemisphereScale);
        if (Float.isFinite(phase))
        {
            return Mth.clamp(phase, -1f, 1f);
        }
        if (WARNED_INVALID_PHASE.compareAndSet(false, true))
        {
            LOGGER.error("Daily temperature phase provider {} returned a non-finite value; using the default clock phase", REGISTRY.activeId());
        }
        return Mth.clamp(REGISTRY.defaultProvider().calculatePhase(calendarTime, daysInMonth, z, hemisphereScale), -1f, 1f);
    }

    static void freezeRegistration()
    {
        REGISTRY.freeze();
        LOGGER.info("Using TFE daily temperature phase provider {}", REGISTRY.activeId());
    }

    private static float calculateClockPhase(long calendarTime, int daysInMonth, int z, float hemisphereScale)
    {
        int hourOfDay = ICalendar.getHourOfDay(calendarTime);
        if (hourOfDay > 12)
        {
            hourOfDay = 24 - hourOfDay;
        }
        return hourOfDay / 6f - 1f;
    }

    private static float calculateSolarPhase(long calendarTime, int daysInMonth, int z, float hemisphereScale)
    {
        final float fractionOfYear = ICalendar.getFractionOfYear(calendarTime, daysInMonth);
        final float fractionOfDay = (float) Math.floorMod(calendarTime, ICalendar.TICKS_IN_DAY) / ICalendar.TICKS_IN_DAY;
        return NTE121SolarCalculator.calculateDailyTemperaturePhase(
            z,
            hemisphereScale,
            fractionOfYear,
            fractionOfDay
        );
    }
}
