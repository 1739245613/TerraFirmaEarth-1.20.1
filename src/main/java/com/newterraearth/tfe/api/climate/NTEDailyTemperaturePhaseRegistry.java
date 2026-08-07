package com.newterraearth.tfe.api.climate;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

final class NTEDailyTemperaturePhaseRegistry
{
    private final ResourceLocation defaultId;
    private final NTEDailyTemperaturePhaseProvider defaultProvider;
    private volatile ResourceLocation activeId;
    private volatile NTEDailyTemperaturePhaseProvider activeProvider;
    private boolean customProviderRegistered;
    private boolean frozen;

    NTEDailyTemperaturePhaseRegistry(ResourceLocation defaultId, NTEDailyTemperaturePhaseProvider defaultProvider)
    {
        this.defaultId = Objects.requireNonNull(defaultId);
        this.defaultProvider = Objects.requireNonNull(defaultProvider);
        this.activeId = defaultId;
        this.activeProvider = defaultProvider;
    }

    synchronized void register(ResourceLocation id, NTEDailyTemperaturePhaseProvider provider)
    {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        if (frozen)
        {
            throw new IllegalStateException("Daily temperature phase registration is already frozen");
        }
        if (customProviderRegistered)
        {
            throw new IllegalStateException("Daily temperature phase provider already registered: " + activeId);
        }
        if (defaultId.equals(id))
        {
            throw new IllegalArgumentException("The default daily temperature phase id is reserved: " + id);
        }
        activeId = id;
        activeProvider = provider;
        customProviderRegistered = true;
    }

    synchronized void freeze()
    {
        frozen = true;
    }

    ResourceLocation activeId()
    {
        return activeId;
    }

    NTEDailyTemperaturePhaseProvider activeProvider()
    {
        return activeProvider;
    }

    NTEDailyTemperaturePhaseProvider defaultProvider()
    {
        return defaultProvider;
    }
}
