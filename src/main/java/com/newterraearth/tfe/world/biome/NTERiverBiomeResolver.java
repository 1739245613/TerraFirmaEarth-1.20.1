package com.newterraearth.tfe.world.biome;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.dries007.tfc.world.biome.BiomeSourceExtension;
import com.newterraearth.tfe.world.river.NTERiverHydrology;

/**
 * Bridges biome-source queries to the hydrology owned by the active TFC chunk generator.
 * Weak keys avoid retaining biome-source copies after a world is unloaded.
 */
public final class NTERiverBiomeResolver
{
    private static final Map<BiomeSourceExtension, NTERiverHydrology> HYDROLOGY = new WeakHashMap<>();
    private static final ThreadLocal<Set<BiomeSourceExtension>> ACTIVE_QUERIES = ThreadLocal.withInitial(
        () -> Collections.newSetFromMap(new IdentityHashMap<>())
    );
    private static final ThreadLocal<Integer> SUPPRESSION_DEPTH = ThreadLocal.withInitial(() -> 0);

    public interface Scope extends AutoCloseable
    {
        @Override
        void close();
    }

    private NTERiverBiomeResolver()
    {
    }

    public static void register(BiomeSourceExtension biomeSource, NTERiverHydrology hydrology)
    {
        synchronized (HYDROLOGY)
        {
            HYDROLOGY.put(biomeSource, hydrology);
        }
    }

    @org.jetbrains.annotations.Nullable
    public static NTERiverHydrology hydrology(BiomeSourceExtension biomeSource)
    {
        synchronized (HYDROLOGY)
        {
            return HYDROLOGY.get(biomeSource);
        }
    }

    public static boolean isVisibleRiver(BiomeSourceExtension biomeSource, int blockX, int blockZ)
    {
        if (SUPPRESSION_DEPTH.get() > 0)
        {
            return false;
        }
        final NTERiverHydrology hydrology;
        synchronized (HYDROLOGY)
        {
            hydrology = HYDROLOGY.get(biomeSource);
        }
        if (hydrology == null)
        {
            return false;
        }

        // River visibility samples the ambient terrain. Some ambient samplers (for example
        // centered volcanic features) ask the biome source again while that sample is in
        // progress. Treat only that internal re-entrant query as non-river terrain so the
        // outer query can finish with the full fill-safety check.
        final Set<BiomeSourceExtension> active = ACTIVE_QUERIES.get();
        if (!active.add(biomeSource))
        {
            return false;
        }
        try
        {
            return hydrology.isVisibleRiver(blockX, blockZ);
        }
        finally
        {
            active.remove(biomeSource);
            if (active.isEmpty())
            {
                ACTIVE_QUERIES.remove();
            }
        }
    }

    /**
     * Ambient terrain sampling must stay river-free through biome lookups too.
     * The height filler already suppresses carving; this scope prevents its
     * nested biome queries from re-entering hydrology while that terrain is
     * being used to constrain the same drainage graph.
     */
    public static Scope suppressRivers()
    {
        SUPPRESSION_DEPTH.set(SUPPRESSION_DEPTH.get() + 1);
        return () -> {
            final int depth = SUPPRESSION_DEPTH.get() - 1;
            if (depth <= 0)
            {
                SUPPRESSION_DEPTH.remove();
            }
            else
            {
                SUPPRESSION_DEPTH.set(depth);
            }
        };
    }
}
