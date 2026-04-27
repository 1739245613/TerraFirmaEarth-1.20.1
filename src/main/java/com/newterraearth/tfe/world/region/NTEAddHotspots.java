package com.newterraearth.tfe.world.region;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.RegionTask;

public enum NTEAddHotspots implements RegionTask
{
    INSTANCE;

    private static final double HOTSPOT_THRESHOLD = 0.65;
    private static final double EXPANSION_THRESHOLD = 0.15;

    @Override
    public void apply(RegionGenerator.Context context)
    {
        final Region region = context.region;
        final Region.Point[] points = region.data();
        final int sizeX = region.sizeX();
        final NTERegionGeneratorAccess generator = (NTERegionGeneratorAccess) context.generator();
        final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();

        for (int index = 0; index < points.length; index++)
        {
            final Region.Point point = points[index];
            if (point == null)
            {
                continue;
            }

            final int x = region.minX() + index % sizeX;
            final int z = region.minZ() + index / sizeX;
            if (generator.nte$continentFactor(x, z) <= 0.5f)
            {
                continue;
            }

            final Cellular2D.Cell cell = generator.nte$getPlateRegionNoise().cell(x, z);
            final double edgeDistance = Math.abs(cell.f1() - cell.f2());
            final double intensity = generator.nte$getHotSpotIntensityNoise().noise(shift(x), shift(z));
            if (intensity > HOTSPOT_THRESHOLD && edgeDistance > 0.05)
            {
                final byte age = (byte) (int) generator.nte$getHotSpotAgeNoise().noise(shift(x), shift(z));
                final NTEPointAccess access = (NTEPointAccess) point;
                access.nte$setHotSpotAge(age);
                if (age != 4)
                {
                    point.setLand();
                }
                queue.enqueue(index);
            }
        }

        while (!queue.isEmpty())
        {
            final int index = queue.dequeueInt();
            final Region.Point point = points[index];
            if (point == null)
            {
                continue;
            }

            final byte age = ((NTEPointAccess) point).nte$getHotSpotAge();
            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dz = -1; dz <= 1; dz++)
                {
                    final int offset = region.offset(index, dx, dz);
                    if (offset == -1)
                    {
                        continue;
                    }

                    final Region.Point next = points[offset];
                    if (next == null)
                    {
                        continue;
                    }

                    final NTEPointAccess nextAccess = (NTEPointAccess) next;
                    if (nextAccess.nte$getHotSpotAge() != 0)
                    {
                        continue;
                    }

                    final int nextX = region.minX() + offset % sizeX;
                    final int nextZ = region.minZ() + offset / sizeX;
                    final double intensity = generator.nte$getHotSpotIntensityNoise().noise(shift(nextX), shift(nextZ));
                    if (intensity > EXPANSION_THRESHOLD)
                    {
                        queue.enqueue(offset);
                        nextAccess.nte$setHotSpotAge(age);
                        if (age != 4)
                        {
                            next.setLand();
                        }
                    }
                    else
                    {
                        final double buffer = generator.nte$getHotSpotIntensityNoise().noise(shift(nextX) - dx, shift(nextZ) - dz);
                        if (!next.land() && buffer > EXPANSION_THRESHOLD)
                        {
                            nextAccess.nte$setHotSpotAge(age);
                            if (age != 4)
                            {
                                next.setLand();
                            }
                        }
                    }
                }
            }
        }
    }

    private static double shift(int point)
    {
        return point + 0.5;
    }
}
