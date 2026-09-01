package com.newterraearth.tfe.mixin;

import java.util.BitSet;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.dries007.tfc.world.region.FloodFillSmallOceans;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;

import com.newterraearth.tfe.world.region.NTEPointAccess;

@Mixin(value = FloodFillSmallOceans.class, remap = false)
public abstract class FloodFillSmallOceansMixin
{
    private static final int SMALL_OCEAN_FILL_THRESHOLD = 180;

    /**
     * @author Codex
     * @reason Backport the 4.2.9 shelf-depression fill in addition to the old
     * small-ocean fill, using the compatibility ocean-depth field.
     */
    @Overwrite(remap = false)
    public void apply(RegionGenerator.Context context)
    {
        final Region region = context.region;
        final Region.Point[] points = region.data();
        final BitSet explored = new BitSet(region.sizeX() * region.sizeZ());

        for (int index = 0; index < points.length; index++)
        {
            final Region.Point point = points[index];
            if (point != null && !point.land() && !explored.get(index))
            {
                tfe$floodSmallOcean(explored, index, region);
            }
        }

        final BitSet exploredForShelf = new BitSet(region.sizeX() * region.sizeZ());
        for (int index = 0; index < points.length; index++)
        {
            final Region.Point point = points[index];
            if (point != null && !point.land()
                && ((NTEPointAccess) point).nte$getOceanDepth() != 2
                && !exploredForShelf.get(index))
            {
                tfe$floodShelfDepression(exploredForShelf, index, region);
            }
        }
    }

    private void tfe$floodSmallOcean(BitSet explored, int index, Region region)
    {
        final IntSet values = new IntOpenHashSet();
        final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        queue.enqueue(index);
        explored.set(index);
        values.add(index);
        boolean unbounded = false;

        while (!queue.isEmpty())
        {
            final int last = queue.dequeueInt();
            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dz = -1; dz <= 1; dz++)
                {
                    final int next = region.offset(last, dx, dz);
                    if (next < 0)
                    {
                        unbounded = true;
                        continue;
                    }
                    final Region.Point point = region.data()[next];
                    if (point == null)
                    {
                        unbounded = true;
                        continue;
                    }
                    if (point.land() || explored.get(next))
                    {
                        continue;
                    }
                    explored.set(next);
                    queue.enqueue(next);
                    values.add(next);
                }
            }
        }

        if (values.size() < SMALL_OCEAN_FILL_THRESHOLD && !unbounded)
        {
            values.forEach(i -> region.data()[i].setLand());
        }
    }

    private void tfe$floodShelfDepression(BitSet explored, int index, Region region)
    {
        final IntSet values = new IntOpenHashSet();
        final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        queue.enqueue(index);
        explored.set(index);
        values.add(index);
        boolean unbounded = false;

        while (!queue.isEmpty())
        {
            final int last = queue.dequeueInt();
            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dz = -1; dz <= 1; dz++)
                {
                    final int next = region.offset(last, dx, dz);
                    if (next < 0)
                    {
                        unbounded = true;
                        continue;
                    }
                    final Region.Point point = region.data()[next];
                    if (point == null)
                    {
                        unbounded = true;
                        continue;
                    }
                    if (((NTEPointAccess) point).nte$getOceanDepth() == 2 || explored.get(next))
                    {
                        continue;
                    }
                    explored.set(next);
                    queue.enqueue(next);
                    values.add(next);
                }
            }
        }

        if (values.size() < SMALL_OCEAN_FILL_THRESHOLD && !unbounded)
        {
            values.forEach(i -> region.data()[i].setLand());
        }
    }
}
