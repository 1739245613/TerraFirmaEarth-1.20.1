package com.newterraearth.tfe.world.region;

import java.util.ArrayDeque;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;

/**
 * Reconstructs the small set of 4.2.9 region annotations which does not exist
 * in the 1.20 Region.Point class. The values are deliberately kept as addon
 * metadata so the vanilla/TFC region pipeline remains binary compatible.
 */
public enum NTERegionFeatureAnnotations
{
    INSTANCE;

    private static final byte UNREACHABLE = Byte.MAX_VALUE;

    public void prepareForMountainPlacement(RegionGenerator.Context context)
    {
        final Region region = context.region;
        final NTERegionGeneratorAccess generator = (NTERegionGeneratorAccess) context.generator();
        final Region.Point[] points = region.data();
        final int width = region.sizeX();

        for (int index = 0; index < points.length; index++)
        {
            final Region.Point point = points[index];
            if (point == null)
            {
                continue;
            }
            final int x = region.minX() + index % width;
            final int z = region.minZ() + index / width;
            final NTEPointAccess access = (NTEPointAccess) point;
            access.nte$setDivergence(generator.nte$getDivergence(x, z));
            // AddContinentsMixin already assigned the 4.2.9 ocean depth. A
            // later flood fill may have turned a cell into land, in which
            // case its depth must no longer act as a deep-ocean source.
            if (point.land() || point.island())
            {
                access.nte$setOceanDepth((byte) 0);
            }
            access.nte$setDistanceToLand(UNREACHABLE);
            access.nte$setDistanceToDeepOcean(UNREACHABLE);
        }

        floodDistance(region, points, width, true);
        floodDistance(region, points, width, false);
    }

    public void apply(RegionGenerator.Context context)
    {
        final Region.Point[] points = context.region.data();
        // Ocean-ridge depth and barrier-island eligibility are assigned by
        // the 4.2.9 continent and mountain tasks. Do not reclassify them from
        // approximate local heuristics here.
        classifyVolcanicFeatures(points);
    }

    private void floodDistance(Region region, Region.Point[] points, int width, boolean fromLand)
    {
        final ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int index = 0; index < points.length; index++)
        {
            final Region.Point point = points[index];
            if (point == null)
            {
                continue;
            }
            final boolean source = fromLand ? point.land() || point.island() : ((NTEPointAccess) point).nte$getOceanDepth() >= 3;
            if (source)
            {
                queue.add(index);
                if (fromLand)
                {
                    ((NTEPointAccess) point).nte$setDistanceToLand((byte) 0);
                }
                else
                {
                    ((NTEPointAccess) point).nte$setDistanceToDeepOcean((byte) 0);
                }
            }
        }

        while (!queue.isEmpty())
        {
            final int index = queue.removeFirst();
            final Region.Point point = points[index];
            final NTEPointAccess access = (NTEPointAccess) point;
            final int distance = fromLand ? access.nte$getDistanceToLand() : access.nte$getDistanceToDeepOcean();
            final int localX = index % width;
            final int localZ = index / width;
            // 4.2.9's distance annotations expand over the full 3x3
            // neighborhood. The 4-neighbor version understated diagonal
            // proximity and changed the ocean-atoll / volcanic-arc branches.
            for (int dz = -1; dz <= 1; dz++)
            {
                for (int dx = -1; dx <= 1; dx++)
                {
                    if (dx == 0 && dz == 0)
                    {
                        continue;
                    }
                    final int neighborX = localX + dx;
                    final int neighborZ = localZ + dz;
                    if (neighborX < 0 || neighborX >= width || neighborZ < 0 || neighborZ >= region.sizeZ())
                    {
                        continue;
                    }
                    final int neighborIndex = neighborX + width * neighborZ;
                    final Region.Point neighbor = points[neighborIndex];
                    if (neighbor == null)
                    {
                        continue;
                    }
                    final NTEPointAccess neighborAccess = (NTEPointAccess) neighbor;
                    final int oldDistance = fromLand ? neighborAccess.nte$getDistanceToLand() : neighborAccess.nte$getDistanceToDeepOcean();
                    if (oldDistance <= distance + 1)
                    {
                        continue;
                    }
                    final byte newDistance = (byte) Math.min(UNREACHABLE, distance + 1);
                    if (fromLand)
                    {
                        neighborAccess.nte$setDistanceToLand(newDistance);
                    }
                    else
                    {
                        neighborAccess.nte$setDistanceToDeepOcean(newDistance);
                    }
                    queue.addLast(neighborIndex);
                }
            }
        }
    }

    private void classifyVolcanicFeatures(Region.Point[] points)
    {
        for (Region.Point point : points)
        {
            if (point == null || point.island())
            {
                continue;
            }
            final NTEPointAccess access = (NTEPointAccess) point;
            if (point.mountain())
            {
                if (point.baseLandHeight < 8 && access.nte$getDivergence() < 0d)
                {
                    access.nte$setVolcanic(true);
                }
                continue;
            }
            if (point.land())
            {
                continue;
            }
            final int distanceToLand = access.nte$getDistanceToLand();
            final int distanceToDeepOcean = access.nte$getDistanceToDeepOcean();
            if (access.nte$getOceanDepth() == 1
                && access.nte$getDivergence() < -0.04d
                && distanceToLand > 2
                && distanceToDeepOcean <= 5)
            {
                access.nte$setVolcanic(true);
            }
        }
    }

}
