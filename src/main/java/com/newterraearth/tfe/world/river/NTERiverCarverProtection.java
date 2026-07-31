package com.newterraearth.tfe.world.river;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Prevents caves and canyons from opening a surface pit directly through a
 * supplemental creek. Protection is deliberately wider than the water bed and
 * only covers the shallow terrain roof; deep caves remain untouched.
 */
public final class NTERiverCarverProtection
{
    private static final int BANK_BUFFER = 10;
    private static final int SEARCH_BUFFER = 16;
    private static final int MIN_ROOF_DEPTH = 5;
    private static final int MAX_ROOF_DEPTH = 11;
    private static final ThreadLocal<Protection> ACTIVE = new ThreadLocal<>();

    private record CoreColumn(int x, int z, double centerBedY, double radius) {}

    private record Protection(ChunkPos chunkPos, int[] minimumProtectedY)
    {
        boolean protects(int blockX, int y, int blockZ)
        {
            if (blockX < chunkPos.getMinBlockX() || blockX > chunkPos.getMaxBlockX()
                || blockZ < chunkPos.getMinBlockZ() || blockZ > chunkPos.getMaxBlockZ())
            {
                return false;
            }
            final int localX = blockX - chunkPos.getMinBlockX();
            final int localZ = blockZ - chunkPos.getMinBlockZ();
            final int minimumY = minimumProtectedY[localX + 16 * localZ];
            return minimumY != Integer.MAX_VALUE && y >= minimumY;
        }
    }

    public static final class Scope implements AutoCloseable
    {
        private boolean closed;

        private Scope() {}

        @Override
        public void close()
        {
            if (!closed)
            {
                ACTIVE.remove();
                closed = true;
            }
        }
    }

    private NTERiverCarverProtection() {}

    public static Scope open(
        NTERiverHydrology hydrology,
        ChunkAccess chunk,
        NTERiverHydrology.ColumnProfile[] localProfiles
    )
    {
        final ChunkPos chunkPos = chunk.getPos();
        if (!mayContainProtectedTerrain(hydrology, chunkPos, localProfiles))
        {
            final int[] empty = new int[16 * 16];
            Arrays.fill(empty, Integer.MAX_VALUE);
            ACTIVE.set(new Protection(chunkPos, empty));
            return new Scope();
        }

        final List<CoreColumn> coreColumns = new ArrayList<>();
        for (int blockZ = chunkPos.getMinBlockZ() - SEARCH_BUFFER; blockZ <= chunkPos.getMaxBlockZ() + SEARCH_BUFFER; blockZ++)
        {
            for (int blockX = chunkPos.getMinBlockX() - SEARCH_BUFFER; blockX <= chunkPos.getMaxBlockX() + SEARCH_BUFFER; blockX++)
            {
                final NTERiverHydrology.ColumnProfile profile = hydrology.findGraphProfile(blockX, blockZ);
                if (profile != null && profile.inWaterCore())
                {
                    coreColumns.add(new CoreColumn(blockX, blockZ, profile.centerBedY(), profile.channelRadius()));
                }
            }
        }

        final int[] minimumProtectedY = new int[16 * 16];
        Arrays.fill(minimumProtectedY, Integer.MAX_VALUE);
        for (int localZ = 0; localZ < 16; localZ++)
        {
            final int blockZ = chunkPos.getBlockZ(localZ);
            for (int localX = 0; localX < 16; localX++)
            {
                final int blockX = chunkPos.getBlockX(localX);
                CoreColumn nearest = null;
                double nearestDistance = Double.POSITIVE_INFINITY;
                for (CoreColumn core : coreColumns)
                {
                    final double distance = Math.hypot(blockX - core.x(), blockZ - core.z());
                    final double reach = core.radius() + BANK_BUFFER;
                    if (distance <= reach && distance < nearestDistance)
                    {
                        nearestDistance = distance;
                        nearest = core;
                    }
                }
                if (nearest != null)
                {
                    final double reach = nearest.radius() + BANK_BUFFER;
                    final double influence = 1d - Mth.clamp(nearestDistance / reach, 0d, 1d);
                    final int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ);
                    minimumProtectedY[localX + 16 * localZ] = minimumProtectedY(
                        surfaceY,
                        nearest.centerBedY(),
                        influence
                    );
                }
            }
        }

        ACTIVE.set(new Protection(chunkPos, minimumProtectedY));
        return new Scope();
    }

    private static boolean mayContainProtectedTerrain(
        NTERiverHydrology hydrology,
        ChunkPos chunkPos,
        NTERiverHydrology.ColumnProfile[] localProfiles
    )
    {
        if (localProfiles != null)
        {
            for (NTERiverHydrology.ColumnProfile profile : localProfiles)
            {
                if (profile != null)
                {
                    return true;
                }
            }
            return false;
        }

        for (int localZ = 0; localZ < 16; localZ++)
        {
            for (int localX = 0; localX < 16; localX++)
            {
                if (hydrology.findGraphProfile(chunkPos.getBlockX(localX), chunkPos.getBlockZ(localZ)) != null)
                {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean protects(int blockX, int y, int blockZ)
    {
        final Protection protection = ACTIVE.get();
        return protection != null && protection.protects(blockX, y, blockZ);
    }

    static int minimumProtectedY(int surfaceY, double centerBedY, double influence)
    {
        final double smoothInfluence = influence * influence * (3d - 2d * influence);
        final int roofDepth = Mth.floor(Mth.lerp(smoothInfluence, MIN_ROOF_DEPTH, MAX_ROOF_DEPTH));
        final int belowLocalSurface = surfaceY - roofDepth;
        final int belowCreekBed = Mth.floor(centerBedY) - roofDepth;
        return Math.min(belowLocalSurface, belowCreekBed);
    }
}
