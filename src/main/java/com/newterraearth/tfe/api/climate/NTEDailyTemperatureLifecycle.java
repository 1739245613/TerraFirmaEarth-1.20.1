package com.newterraearth.tfe.api.climate;

import org.jetbrains.annotations.ApiStatus;

/** Internal bridge between TFE's mod lifecycle and the public registration API. */
@ApiStatus.Internal
public final class NTEDailyTemperatureLifecycle
{
    private NTEDailyTemperatureLifecycle()
    {
    }

    public static void freezeRegistration()
    {
        NTEDailyTemperatureAPI.freezeRegistration();
    }
}
