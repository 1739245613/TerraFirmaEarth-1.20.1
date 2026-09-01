package com.newterraearth.tfe.mixin;

import java.util.BitSet;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.util.RandomSource;

import net.dries007.tfc.world.region.AddMountains;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;

import com.newterraearth.tfe.world.region.NTEPointAccess;

@Mixin(value = AddMountains.class, remap = false)
public abstract class AddMountainsMixin
{
    /**
     * @author Codex
     * @reason Backport TFC 4.2.9 collisional ranges, volcanic arcs, and barrier-island chains to the 1.20 region task.
     */
    @Overwrite(remap = false)
    public void apply(RegionGenerator.Context context)
    {
        final Region region = context.region;
        final Region.Point[] points = region.data();
        final RandomSource random = context.random;
        final int width = region.sizeX();

        for (int index = 0; index < points.length; index++)
        {
            final Region.Point point = points[index];
            if (point == null || !point.land())
            {
                continue;
            }
            final int x = region.minX() + index % width;
            final int z = region.minZ() + index / width;
            final NTEPointAccess access = (NTEPointAccess) point;
            if (access.nte$getDivergence() < 0d
                && point.distanceToEdge < 1.5d * context.generator().continentNoise.noise(x, z) - 5.2d)
            {
                point.setMountain();
            }
        }

        for (int attempt = 0, mountainsPlaced = 0, islandsPlaced = 0;
             attempt < 40 && !(mountainsPlaced >= 3 && islandsPlaced >= 6);
             attempt++)
        {
            final int x = region.minX() + random.nextInt(region.sizeX());
            final int z = region.minZ() + random.nextInt(region.sizeZ());
            final Region.Point origin = region.maybeAt(x, z);
            if (origin == null)
            {
                continue;
            }

            final int originIndex = region.index(x, z);
            final NTEPointAccess originAccess = (NTEPointAccess) origin;
            if (mountainsPlaced < 3
                && (origin.land() && origin.baseLandHeight <= 1
                    || origin.baseLandHeight >= 4 && origin.baseLandHeight <= 11))
            {
                final IntSet range = tfe$placeRange(region, random, originIndex);
                if (range.size() > 45)
                {
                    range.forEach(index -> {
                        final Region.Point point = points[index];
                        final NTEPointAccess access = (NTEPointAccess) point;
                        point.setMountain();
                        if (originAccess.nte$getDivergence() < 0d && origin.baseLandHeight < 8)
                        {
                            access.nte$setVolcanic(true);
                        }
                        if (origin.baseLandHeight <= 2)
                        {
                            point.setCoastalMountain();
                        }
                    });
                    mountainsPlaced++;
                }
            }
            else if (islandsPlaced < 6
                && !origin.land()
                && originAccess.nte$getOceanDepth() == 2
                && originAccess.nte$getDivergence() < 0d
                && originAccess.nte$getDistanceToDeepOcean() <= 3
                && originAccess.nte$getDistanceToLand() > 2)
            {
                final IntSet arc = tfe$placeVolcanicArc(region, random, originIndex);
                if (arc.size() > 45)
                {
                    final byte originContour = originAccess.nte$getDistanceToDeepOcean();
                    arc.forEach(index -> {
                        final NTEPointAccess access = (NTEPointAccess) points[index];
                        if (access.nte$getDistanceToDeepOcean() >= originContour
                            && access.nte$getDistanceToDeepOcean() <= originContour + 1)
                        {
                            access.nte$setBarrierIsland(true);
                        }
                        access.nte$setVolcanic(true);
                        access.nte$setOceanDepth((byte) 1);
                    });
                    islandsPlaced += 2;
                }
            }
            else if (islandsPlaced < 6
                && !origin.land()
                && originAccess.nte$getOceanDepth() == 2
                && originAccess.nte$getDivergence() > 0d
                && originAccess.nte$getDistanceToLand() > 2
                && originAccess.nte$getDistanceToLand() < 6)
            {
                final IntSet barrier = tfe$placeBarrier(region, random, originIndex);
                if (barrier.size() > 45)
                {
                    final byte startContour = (byte) Math.max(1, originAccess.nte$getDistanceToLand());
                    barrier.forEach(index -> {
                        final NTEPointAccess access = (NTEPointAccess) points[index];
                        if (access.nte$getDistanceToLand() == startContour)
                        {
                            access.nte$setBarrierIsland(true);
                        }
                        access.nte$setOceanDepth((byte) 1);
                    });
                    islandsPlaced++;
                }
            }
        }

    }

    @Unique
    private IntSet tfe$placeRange(Region region, RandomSource random, int originIndex)
    {
        final BitSet explored = new BitSet(region.sizeX() * region.sizeZ());
        final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        final IntSet range = new IntOpenHashSet();
        final Region.Point[] points = region.data();

        queue.enqueue(originIndex);
        explored.set(originIndex);
        range.add(originIndex);

        final int originBaseLandHeight = Math.max(1, points[originIndex].baseLandHeight);
        final int maxSize = 70 + random.nextInt(40);

        while (!queue.isEmpty())
        {
            final int last = queue.dequeueInt();
            final Region.Point lastPoint = points[last];
            if (range.size() > maxSize)
            {
                break;
            }

            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dz = -1; dz <= 1; dz++)
                {
                    final int index = region.offset(last, dx, dz);
                    if (index < 0 || explored.get(index))
                    {
                        continue;
                    }
                    explored.set(index);
                    final Region.Point point = points[index];
                    if (point != null
                        && point.land()
                        && point.baseLandHeight >= originBaseLandHeight - 1
                        && point.baseLandHeight <= originBaseLandHeight + 1
                        && (point.baseLandHeight > 2 || point.distanceToOcean < 3))
                    {
                        if (lastPoint.baseLandHeight != point.baseLandHeight)
                        {
                            queue.enqueue(index);
                        }
                        else
                        {
                            queue.enqueueFirst(index);
                        }
                        range.add(index);
                    }
                }
            }
        }
        return range;
    }

    @Unique
    private IntSet tfe$placeVolcanicArc(Region region, RandomSource random, int originIndex)
    {
        final BitSet explored = new BitSet(region.sizeX() * region.sizeZ());
        final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        final IntSet arc = new IntOpenHashSet();
        final Region.Point[] points = region.data();

        queue.enqueue(originIndex);
        explored.set(originIndex);
        arc.add(originIndex);

        final int originDistance = Math.max(1, ((NTEPointAccess) points[originIndex]).nte$getDistanceToDeepOcean());
        final int maxSize = 90 + random.nextInt(50);

        while (!queue.isEmpty())
        {
            final int last = queue.dequeueInt();
            final NTEPointAccess lastAccess = (NTEPointAccess) points[last];
            if (arc.size() > maxSize)
            {
                break;
            }

            for (int dx = -2; dx <= 2; dx++)
            {
                for (int dz = -2; dz <= 2; dz++)
                {
                    final int index = region.offset(last, dx, dz);
                    if (index < 0 || explored.get(index))
                    {
                        continue;
                    }
                    explored.set(index);
                    final Region.Point point = points[index];
                    if (point == null)
                    {
                        continue;
                    }
                    final NTEPointAccess access = (NTEPointAccess) point;
                    final int distance = access.nte$getDistanceToDeepOcean();
                    if (access.nte$getOceanDepth() == 2
                        && access.nte$getDivergence() < 0d
                        && distance >= originDistance - 1
                        && distance <= originDistance + 2)
                    {
                        if (lastAccess.nte$getDistanceToDeepOcean() != distance)
                        {
                            queue.enqueue(index);
                        }
                        else
                        {
                            queue.enqueueFirst(index);
                        }
                        arc.add(index);
                    }
                }
            }
        }
        return arc;
    }

    @Unique
    private IntSet tfe$placeBarrier(Region region, RandomSource random, int originIndex)
    {
        final BitSet explored = new BitSet(region.sizeX() * region.sizeZ());
        final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        final IntSet barrier = new IntOpenHashSet();
        final Region.Point[] points = region.data();

        queue.enqueue(originIndex);
        explored.set(originIndex);
        barrier.add(originIndex);

        final int originDistance = Math.max(1, ((NTEPointAccess) points[originIndex]).nte$getDistanceToLand());
        final int maxSize = 70 + random.nextInt(40);

        while (!queue.isEmpty())
        {
            final int last = queue.dequeueInt();
            final NTEPointAccess lastAccess = (NTEPointAccess) points[last];
            if (barrier.size() > maxSize)
            {
                break;
            }

            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dz = -1; dz <= 1; dz++)
                {
                    final int index = region.offset(last, dx, dz);
                    if (index < 0 || explored.get(index))
                    {
                        continue;
                    }
                    explored.set(index);
                    final Region.Point point = points[index];
                    if (point == null)
                    {
                        continue;
                    }
                    final NTEPointAccess access = (NTEPointAccess) point;
                    final int distance = access.nte$getDistanceToLand();
                    if (access.nte$getOceanDepth() == 2
                        && distance >= originDistance - 1
                        && distance <= originDistance + 1)
                    {
                        if (lastAccess.nte$getDistanceToLand() != distance)
                        {
                            queue.enqueue(index);
                        }
                        else
                        {
                            queue.enqueueFirst(index);
                        }
                        barrier.add(index);
                    }
                }
            }
        }
        return barrier;
    }
}
