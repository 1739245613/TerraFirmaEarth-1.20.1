package com.newterraearth.tfe.api.climate;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NTEDailyTemperaturePhaseRegistryTest
{
    private static final ResourceLocation DEFAULT_ID = new ResourceLocation("tfe", "default_test");
    private static final ResourceLocation SOLAR_ID = new ResourceLocation("test", "solar");
    private static final NTEDailyTemperaturePhaseProvider DEFAULT = (time, days, z, scale) -> 0f;
    private static final NTEDailyTemperaturePhaseProvider SOLAR = (time, days, z, scale) -> 1f;

    @Test
    void oneCustomProviderReplacesTheDefault()
    {
        final NTEDailyTemperaturePhaseRegistry registry = new NTEDailyTemperaturePhaseRegistry(DEFAULT_ID, DEFAULT);
        registry.register(SOLAR_ID, SOLAR);

        assertEquals(SOLAR_ID, registry.activeId());
        assertSame(SOLAR, registry.activeProvider());
    }

    @Test
    void duplicateAndLateRegistrationsAreRejected()
    {
        final NTEDailyTemperaturePhaseRegistry duplicateRegistry = new NTEDailyTemperaturePhaseRegistry(DEFAULT_ID, DEFAULT);
        duplicateRegistry.register(SOLAR_ID, SOLAR);
        assertThrows(IllegalStateException.class, () -> duplicateRegistry.register(new ResourceLocation("test", "other"), DEFAULT));

        final NTEDailyTemperaturePhaseRegistry frozenRegistry = new NTEDailyTemperaturePhaseRegistry(DEFAULT_ID, DEFAULT);
        frozenRegistry.freeze();
        assertThrows(IllegalStateException.class, () -> frozenRegistry.register(SOLAR_ID, SOLAR));
    }
}
