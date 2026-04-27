package com.newterraearth.tfe.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import net.dries007.tfc.world.biome.BiomeExtension;

public final class NTEBiomeCache
{
    private static final Map<String, BiomeExtension> CACHE = new ConcurrentHashMap<>();

    private NTEBiomeCache()
    {
    }

    public static BiomeExtension get(String name, Supplier<BiomeExtension> factory)
    {
        return CACHE.computeIfAbsent(name, key -> factory.get());
    }
}
