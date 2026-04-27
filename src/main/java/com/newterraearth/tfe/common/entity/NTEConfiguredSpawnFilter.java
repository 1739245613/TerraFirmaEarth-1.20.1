package com.newterraearth.tfe.common.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.registries.ForgeRegistries;

import com.newterraearth.tfe.config.NTECommonConfig;

public final class NTEConfiguredSpawnFilter
{
    private static final Map<WeightedRandomList<MobSpawnSettings.SpawnerData>, CacheEntry> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private NTEConfiguredSpawnFilter()
    {
    }

    public static WeightedRandomList<MobSpawnSettings.SpawnerData> filter(WeightedRandomList<MobSpawnSettings.SpawnerData> original)
    {
        if (original == null || !NTECommonConfig.hasDisabledConfiguredEntitySpawns())
        {
            return original;
        }

        final int mask = NTECommonConfig.getConfiguredEntitySpawnMask();
        final CacheEntry cached = CACHE.get(original);
        if (cached != null && cached.mask() == mask)
        {
            return cached.filtered();
        }

        final List<MobSpawnSettings.SpawnerData> filtered = new ArrayList<>();
        boolean changed = false;
        for (MobSpawnSettings.SpawnerData spawner : original.unwrap())
        {
            final ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(spawner.type);
            if (!NTECommonConfig.isEntitySpawnEnabled(entityId))
            {
                changed = true;
                continue;
            }
            filtered.add(spawner);
        }

        final WeightedRandomList<MobSpawnSettings.SpawnerData> result = changed ? WeightedRandomList.create(filtered) : original;
        CACHE.put(original, new CacheEntry(mask, result));
        return result;
    }

    private record CacheEntry(int mask, WeightedRandomList<MobSpawnSettings.SpawnerData> filtered) {}
}
