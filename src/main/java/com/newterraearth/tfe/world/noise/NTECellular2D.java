package com.newterraearth.tfe.world.noise;

import java.util.function.ToDoubleFunction;

import it.unimi.dsi.fastutil.HashCommon;
import net.dries007.tfc.world.noise.FastNoiseLite;
import net.dries007.tfc.world.noise.Noise2D;

/**
 * Exact 1.21-style cellular noise needed by centered volcanic features.
 */
public class NTECellular2D implements Noise2D
{
    private final double jitter;
    private final int seed;
    private final int sample;
    private double frequency;

    public NTECellular2D(long seed)
    {
        this(seed, 0.43701595f, 1);
    }

    public NTECellular2D(long seed, int sample)
    {
        this(seed, 0.43701595f, sample);
    }

    public NTECellular2D(long seed, float jitter, int sample)
    {
        this.seed = HashCommon.long2int(seed);
        this.frequency = 1;
        this.jitter = jitter;
        this.sample = sample;
    }

    private NTECellular2D(int hashedSeed, float jitter, int sample)
    {
        this.seed = hashedSeed;
        this.frequency = 1;
        this.jitter = jitter;
        this.sample = sample;
    }

    /**
     * Reuses the hashed seed stored by the 1.20 TFC Cellular2D implementation.
     * This keeps the local 4.2.9-compatible cell math on the same worldgen field.
     */
    public static NTECellular2D fromHashedSeed(int hashedSeed)
    {
        return new NTECellular2D(hashedSeed, 0.43701595f, 1);
    }

    @Override
    public double noise(double x, double y)
    {
        return cell(x, y).noise();
    }

    @Override
    public NTECellular2D spread(double scaleFactor)
    {
        frequency *= scaleFactor;
        return this;
    }

    public Noise2D then(ToDoubleFunction<Cell> f)
    {
        return (x, y) -> f.applyAsDouble(cell(x, y));
    }

    public Cell cell(double x, double y)
    {
        x *= frequency;
        y *= frequency;

        final int primeX = 501125321;
        final int primeY = 1136930381;

        final int xr = FastNoiseLite.FastFloor(x);
        final int yr = FastNoiseLite.FastFloor(y);

        double distance0 = Double.MAX_VALUE;
        double distance1 = Double.MAX_VALUE;
        double angle0 = -1;
        double closestCenterX = 0;
        double closestCenterY = 0;
        double neighborCenterX = 0;
        double neighborCenterY = 0;
        int closestHash = 0;
        int closestCellX = 0;
        int closestCellY = 0;

        int xPrimed = (xr - sample) * primeX;
        final int yPrimedBase = (yr - sample) * primeY;

        for (int xi = xr - sample; xi <= xr + sample; xi++)
        {
            int yPrimed = yPrimedBase;

            for (int yi = yr - sample; yi <= yr + sample; yi++)
            {
                final int hash = FastNoiseLite.Hash(seed, xPrimed, yPrimed);
                final int idx = hash & (255 << 1);

                final double vecX = xi + FastNoiseLite.RandVecs2D[idx] * jitter;
                final double vecY = yi + FastNoiseLite.RandVecs2D[idx | 1] * jitter;

                final double newDistanceX = vecX - x;
                final double newDistanceY = vecY - y;
                final double newAngle = diamondAngle(newDistanceX, newDistanceY);
                final double newDistance = newDistanceX * newDistanceX + newDistanceY * newDistanceY;

                final double oldDist1 = distance1;
                distance1 = FastNoiseLite.FastMax(FastNoiseLite.FastMin(distance1, newDistance), distance0);
                if (newDistance < distance0)
                {
                    distance0 = newDistance;
                    angle0 = newAngle;
                    closestHash = hash;
                    neighborCenterX = closestCenterX;
                    neighborCenterY = closestCenterY;
                    closestCenterX = vecX;
                    closestCenterY = vecY;
                    closestCellX = xi;
                    closestCellY = yi;
                }
                else if (distance1 != oldDist1)
                {
                    neighborCenterX = vecX;
                    neighborCenterY = vecY;
                }
                yPrimed += primeY;
            }
            xPrimed += primeX;
        }

        return new Cell(
            closestCenterX / frequency,
            closestCenterY / frequency,
            closestCellX,
            closestCellY,
            neighborCenterX / frequency,
            neighborCenterY / frequency,
            distance0,
            distance1,
            closestHash * (1 / 2147483648.0f),
            angle0
        );
    }

    private static double diamondAngle(double x, double y)
    {
        if (y >= 0)
        {
            return x >= 0 ? y / (x + y) : 1 - x / (-x + y);
        }
        return x < 0 ? 2 - y / (-x - y) : 3 + x / (x - y);
    }

    public record Cell(double x, double y, int cx, int cy, double nx, double ny, double f1, double f2, double noise, double angle) {}
}
