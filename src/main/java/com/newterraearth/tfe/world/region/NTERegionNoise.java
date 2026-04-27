package com.newterraearth.tfe.world.region;

import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.region.Units;

public final class NTERegionNoise
{
    private NTERegionNoise()
    {
    }

    public static Noise2D activeHotSpots(long seed)
    {
        final double horizontalScale = 0.003;
        final double cutoff = 0.75;
        final double rescale = 7.2;

        return new OpenSimplex2D(seed)
            .map(y -> {
                final double clipped = y > cutoff ? y - cutoff : 0;
                return clipped * rescale;
            })
            .octaves(3)
            .spread(horizontalScale);
    }

    public static Noise2D dormantHotSpots(long seed)
    {
        return hotSpotWarp(activeHotSpots(seed), plateRegionNoise(seed), 1024, 0).map(y -> Math.max(y - 0.1, 0) * 1.111);
    }

    public static Noise2D extinctHotSpots(long seed)
    {
        return hotSpotWarp(activeHotSpots(seed), plateRegionNoise(seed), 2048, 0.25).map(y -> Math.max(y - 0.2, 0) * 1.25);
    }

    public static Noise2D ancientHotSpots(long seed)
    {
        return hotSpotWarp(activeHotSpots(seed), plateRegionNoise(seed), 3072, 0.5).map(y -> Math.max(y - 0.3, 0) * 1.4286);
    }

    public static Noise2D hotSpotIntensity(long seed)
    {
        final Noise2D active = activeHotSpots(seed);
        final Noise2D dormant = dormantHotSpots(seed);
        final Noise2D extinct = extinctHotSpots(seed);
        final Noise2D ancient = ancientHotSpots(seed);
        return (x, z) -> Math.max(
            Math.max(active.noise(x, z), dormant.noise(x, z)),
            Math.max(extinct.noise(x, z), ancient.noise(x, z))
        );
    }

    public static Noise2D hotSpotAge(long seed)
    {
        return mapAges(
            activeHotSpots(seed),
            dormantHotSpots(seed),
            extinctHotSpots(seed),
            ancientHotSpots(seed)
        );
    }

    public static Noise2D hotSpotWarp(Noise2D noiseToWarp, Noise2D warp, int velocityScale, double accelScale)
    {
        return (x, z) -> {
            final double ux = warp.noise(x, z);
            final double uz = (Math.abs(ux * 16) % 1 > 0.5 ? 1 : -1) * (ux * 256) % 1;

            final int sx = ux > 0 ? 1 : -1;
            final int sz = uz > 0 ? 1 : -1;
            final double vx = (ux + sx) * velocityScale;
            final double vz = (uz + sz) * velocityScale;

            final double ax = -vz * accelScale;
            final double az = vx * accelScale;

            return noiseToWarp.noise(x + vx + ax, z + vz + az);
        };
    }

    public static Noise2D mapAges(Noise2D activeNoise, Noise2D youngNoise, Noise2D oldNoise, Noise2D oldestNoise)
    {
        return (x, z) -> {
            final double active = activeNoise.noise(x, z);
            final double young = youngNoise.noise(x, z);
            final double old = oldNoise.noise(x, z);
            final double oldest = oldestNoise.noise(x, z);

            if (Math.max(Math.max(active, young), Math.max(old, oldest)) <= -0.8)
            {
                return 0;
            }
            if (active > young && active > old && active > oldest)
            {
                return 1;
            }
            if (young > active && young > old && young > oldest)
            {
                return 2;
            }
            if (old > young && old > oldest && old > active)
            {
                return 3;
            }
            if (oldest > young && oldest > old && oldest > active)
            {
                return 4;
            }
            return 0;
        };
    }

    public static Cellular2D plateRegions(long seed)
    {
        return new Cellular2D(seed).spread(0.00590625f / Units.CELL_WIDTH_IN_GRID);
    }

    private static Noise2D plateRegionNoise(long seed)
    {
        return plateRegions(seed)::noise;
    }
}
