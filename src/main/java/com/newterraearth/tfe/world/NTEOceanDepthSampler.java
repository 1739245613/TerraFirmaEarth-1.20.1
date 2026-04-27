package com.newterraearth.tfe.world;

import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.ChunkGeneratorExtension;
import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.chunkdata.RegionChunkDataGenerator;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;

/**
 * Applies a smooth region-scale depth curve to ocean biome height samplers so
 * the seafloor keeps deepening as the player moves offshore instead of quickly
 * flattening into a single deep-ocean shelf.
 */
public final class NTEOceanDepthSampler implements BiomeNoiseSampler
{
    private static final double OCEAN_STAGE_START_DEPTH = 0.5d;
    private static final double OCEAN_STAGE_FULL_DEPTH = 3d;
    private static final double REEF_STAGE_START_DEPTH = 3d;
    private static final double REEF_STAGE_FULL_DEPTH = 5d;
    private static final double DEEP_OCEAN_STAGE_START_DEPTH = 5d;
    private static final double DEEP_OCEAN_STAGE_FULL_DEPTH = 9d;
    private static final double TRENCH_STAGE_START_DEPTH = 9.2d;
    private static final double TRENCH_STAGE_FULL_DEPTH = 10.7d;
    private static final double OCEAN_TARGET_MIN_Y = 15d;
    private static final double OCEAN_REEF_TARGET_MIN_Y = 10d;
    private static final double DEEP_OCEAN_TARGET_MIN_Y = -10d;
    private static final double OCEAN_BASE_MIN_Y = 35d;
    private static final double OCEAN_REEF_BASE_MIN_Y = 39d;
    private static final double DEEP_OCEAN_BASE_MIN_Y = 33d;
    private static final double OCEAN_STAGE_MAX_DROP = OCEAN_BASE_MIN_Y - OCEAN_TARGET_MIN_Y;
    private static final double REEF_STAGE_MAX_DROP = OCEAN_REEF_BASE_MIN_Y - OCEAN_REEF_TARGET_MIN_Y - OCEAN_STAGE_MAX_DROP;
    private static final double DEEP_OCEAN_STAGE_MAX_DROP = DEEP_OCEAN_BASE_MIN_Y - DEEP_OCEAN_TARGET_MIN_Y - OCEAN_STAGE_MAX_DROP - REEF_STAGE_MAX_DROP;
    private static final double TRENCH_TARGET_MIN_Y = -30d;
    private static final double TRENCH_STEP_DROP = DEEP_OCEAN_TARGET_MIN_Y - TRENCH_TARGET_MIN_Y;

    private final BiomeNoiseSampler delegate;
    private final OceanKind kind;
    private double height;
    @Nullable private RegionGenerator regionGenerator;

    private NTEOceanDepthSampler(Noise2D heightNoise, OceanKind kind)
    {
        this.delegate = BiomeNoiseSampler.fromHeightNoise(heightNoise);
        this.kind = kind;
    }

    public static BiomeNoiseSampler ocean(long seed)
    {
        return new NTEOceanDepthSampler(BiomeNoise.ocean(seed, -28, -14), OceanKind.OCEAN);
    }

    public static BiomeNoiseSampler oceanReef(long seed)
    {
        return new NTEOceanDepthSampler(BiomeNoise.ocean(seed, -24, -12), OceanKind.OCEAN_REEF);
    }

    public static BiomeNoiseSampler deepOcean(long seed)
    {
        return new NTEOceanDepthSampler(BiomeNoise.ocean(seed, -30, -16), OceanKind.DEEP_OCEAN);
    }

    public static BiomeNoiseSampler trench(long seed)
    {
        return new NTEOceanDepthSampler(BiomeNoise.oceanRidge(seed, -30, -16), OceanKind.TRENCH);
    }

    @Override
    public void prepare(ChunkGeneratorExtension generator, @Nullable ChunkAccess chunk)
    {
        delegate.prepare(generator, chunk);

        final ChunkDataProvider provider = generator.chunkDataProvider();
        if (provider.generator() instanceof RegionChunkDataGenerator regionChunkDataGenerator)
        {
            regionGenerator = regionChunkDataGenerator.regionGenerator();
        }
        else
        {
            regionGenerator = null;
        }
    }

    @Override
    public void setColumn(int x, int z)
    {
        delegate.setColumn(x, z);
        height = delegate.height() - sampleDepthDrop(x, z);
    }

    @Override
    public double height()
    {
        return height;
    }

    @Override
    public double noise(int y)
    {
        return delegate.noise(y);
    }

    private double sampleDepthDrop(int blockX, int blockZ)
    {
        if (regionGenerator == null)
        {
            return 0d;
        }

        final double oceanDepth = sampleBaseOceanDepth(blockX, blockZ);
        final double oceanFactor = smoothstep(OCEAN_STAGE_START_DEPTH, OCEAN_STAGE_FULL_DEPTH, oceanDepth);
        final double reefFactor = smoothstep(REEF_STAGE_START_DEPTH, REEF_STAGE_FULL_DEPTH, oceanDepth);
        final double deepOceanFactor = smoothstep(DEEP_OCEAN_STAGE_START_DEPTH, DEEP_OCEAN_STAGE_FULL_DEPTH, oceanDepth);
        double drop = OCEAN_STAGE_MAX_DROP * oceanFactor;
        drop += REEF_STAGE_MAX_DROP * reefFactor;
        drop += DEEP_OCEAN_STAGE_MAX_DROP * deepOceanFactor;

        if (kind == OceanKind.TRENCH)
        {
            // Only apply the trench-specific drop to the trench core so the surrounding
            // deep ocean keeps its own decorations instead of being pulled into a huge flat basin.
            final double trenchFactor = smoothstep(TRENCH_STAGE_START_DEPTH, TRENCH_STAGE_FULL_DEPTH, oceanDepth);
            final double deepenedHeight = delegate.height() - drop;
            final double maxExtraDrop = Math.max(0d, deepenedHeight - TRENCH_TARGET_MIN_Y);
            drop += Math.min(TRENCH_STEP_DROP * trenchFactor, maxExtraDrop);
        }

        return drop;
    }

    private double sampleBaseOceanDepth(int blockX, int blockZ)
    {
        final int gridX = Units.blockToGrid(blockX);
        final int gridZ = Units.blockToGrid(blockZ);

        final Region.Point point00 = regionGenerator.getOrCreateRegionPoint(gridX, gridZ);
        final Region.Point point01 = regionGenerator.getOrCreateRegionPoint(gridX, gridZ + 1);
        final Region.Point point10 = regionGenerator.getOrCreateRegionPoint(gridX + 1, gridZ);
        final Region.Point point11 = regionGenerator.getOrCreateRegionPoint(gridX + 1, gridZ + 1);

        final double deltaX = Units.blockToGridExact(blockX) - gridX;
        final double deltaZ = Units.blockToGridExact(blockZ) - gridZ;
        final double lower = Mth.lerp(deltaX, point00.baseOceanDepth, point10.baseOceanDepth);
        final double upper = Mth.lerp(deltaX, point01.baseOceanDepth, point11.baseOceanDepth);
        return Mth.lerp(deltaZ, lower, upper);
    }

    private static double smoothstep(double edge0, double edge1, double value)
    {
        if (edge0 == edge1)
        {
            return value < edge0 ? 0d : 1d;
        }

        final double normalized = Mth.clamp((value - edge0) / (edge1 - edge0), 0d, 1d);
        return normalized * normalized * (3d - 2d * normalized);
    }

    private enum OceanKind
    {
        OCEAN,
        OCEAN_REEF,
        DEEP_OCEAN,
        TRENCH
    }
}
