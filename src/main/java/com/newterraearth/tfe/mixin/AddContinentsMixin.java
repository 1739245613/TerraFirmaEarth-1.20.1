package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.BitSet;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

import net.dries007.tfc.world.region.AddContinents;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;

import com.newterraearth.tfe.world.region.NTERegionGeneratorAccess;
import com.newterraearth.tfe.world.region.NTEPointAccess;

@Mixin(value = AddContinents.class, remap = false)
public abstract class AddContinentsMixin
{
    /**
     * @author Codex
     * @reason Run AddContinents on the 1.21-style initialized region points instead of the old 1.20 scan-and-init pass.
     */
    @Overwrite(remap = false)
    public void apply(RegionGenerator.Context context)
    {
        final NTERegionGeneratorAccess generator = (NTERegionGeneratorAccess) context.generator();
        final Region region = context.region;
        final Region.Point[] points = region.data();
        final int sizeX = region.sizeX();
        final int minX = region.minX();
        final int minZ = region.minZ();

        // 1.21 annotates the cell edge before assigning ocean depth. The 1.20
        // task order is older, so calculate the same field locally first.
        tfe$annotateDistanceToCellEdge(region);

        for (int index = 0; index < points.length; index++)
        {
            final Region.Point point = points[index];
            if (point == null)
            {
                continue;
            }

            final int gridX = minX + index % sizeX;
            final int gridZ = minZ + index / sizeX;
            final NTEPointAccess access = (NTEPointAccess) point;
            final double divergence = generator.nte$getDivergence(gridX, gridZ);
            access.nte$setDivergence(divergence);
            final double tectonicFeatures = point.distanceToEdge <= 5 && divergence > 0d
                ? (5d - point.distanceToEdge) * -0.12d
                : 0d;
            final double continent = (context.generator().continentNoise.noise(gridX, gridZ) + tectonicFeatures)
                * generator.nte$continentFactor(gridX, gridZ);
            if (continent > 4.4)
            {
                point.setLand();
                access.nte$setOceanDepth((byte) 0);
            }
            else if (divergence > 0d && point.distanceToEdge < 2)
            {
                access.nte$setOceanDepth((byte) 3);
            }
            else if (continent > 3.3)
            {
                access.nte$setOceanDepth((byte) 2);
            }
            else if (continent > 3d && divergence < 0d)
            {
                access.nte$setOceanDepth((byte) 5);
            }
            else if (point.distanceToEdge < 2 && !(divergence < 0d && continent > 2d))
            {
                access.nte$setOceanDepth((byte) 3);
            }
            else
            {
                access.nte$setOceanDepth((byte) 4);
            }
        }
    }

    @Unique
    private void tfe$annotateDistanceToCellEdge(Region region)
    {
        final BitSet explored = new BitSet(region.sizeX() * region.sizeZ());
        final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        final Region.Point[] points = region.data();

        for (int dx = 0; dx < region.sizeX(); dx++)
        {
            for (int dz = 0; dz < region.sizeZ(); dz++)
            {
                final int index = dx + region.sizeX() * dz;
                final Region.Point point = points[index];
                if (point == null || dx == 0 || dz == 0 || dx == region.sizeX() - 1 || dz == region.sizeZ() - 1)
                {
                    explored.set(index);
                    queue.enqueue(index);
                    if (point != null)
                    {
                        point.distanceToEdge = -1;
                    }
                }
            }
        }

        while (!queue.isEmpty())
        {
            final int last = queue.dequeueInt();
            final Region.Point lastPoint = points[last];
            final int nextDistance = lastPoint == null ? 0 : lastPoint.distanceToEdge + 1;
            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dz = -1; dz <= 1; dz++)
                {
                    final int next = region.offset(last, dx, dz);
                    if (next < 0 || explored.get(next))
                    {
                        continue;
                    }
                    final Region.Point point = points[next];
                    if (point != null && point.distanceToEdge == 0)
                    {
                        point.distanceToEdge = (byte) nextDistance;
                        queue.enqueue(next);
                    }
                    explored.set(next);
                }
            }
        }
    }
}
