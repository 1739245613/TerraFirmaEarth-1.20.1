package com.newterraearth.tfe.world.noise;

import it.unimi.dsi.fastutil.HashCommon;
import net.dries007.tfc.world.noise.FastNoiseLite;

/** Exact 1.21-compatible one-dimensional cellular noise used by volcano strata. */
public final class NTECellular1D
{
    private final double jitter;
    private final int seed;
    private double frequency = 1d;

    public NTECellular1D(long seed)
    {
        this(seed, 0.43701595f);
    }

    public NTECellular1D(long seed, float jitter)
    {
        this.seed = HashCommon.long2int(seed);
        this.jitter = jitter;
    }

    public NTECellular1D spread(double scaleFactor)
    {
        frequency *= scaleFactor;
        return this;
    }

    public Cell cell(double value)
    {
        final double x = value * frequency;
        final int prime = 501125321;
        final int rounded = FastNoiseLite.FastFloor(x);
        double distance0 = Double.MAX_VALUE;
        double distance1 = Double.MAX_VALUE;
        double center = 0;
        double neighbor = 0;
        int closestHash = 0;
        int noJitterCenter = 0;
        int primed = (rounded - 1) * prime;
        for (int xi = rounded - 1; xi <= rounded + 1; xi++)
        {
            int hash = seed ^ primed;
            hash *= 0x27d4eb2d;
            final int index = hash & (255 << 1);
            final double vector = xi + FastNoiseLite.RandVecs2D[index] * jitter;
            final double distance = vector - x;
            final double oldDistance1 = distance1;
            distance1 = Math.max(Math.min(distance1, distance), distance0);
            if (distance < distance0)
            {
                distance0 = distance;
                closestHash = hash;
                neighbor = center;
                center = vector;
                noJitterCenter = xi;
            }
            else if (distance1 != oldDistance1)
            {
                neighbor = vector;
            }
            primed += prime;
        }
        return new Cell(center / frequency, noJitterCenter, neighbor / frequency, distance0, distance1, closestHash * (1 / 2147483648.0f));
    }

    public record Cell(double x, int cx, double nx, double f1, double f2, double noise)
    {
    }
}
