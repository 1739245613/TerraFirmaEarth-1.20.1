package com.newterraearth.tfe.world;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;

import net.dries007.tfc.world.ChunkGeneratorExtension;
import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.chunkdata.ChunkDataGenerator;
import net.dries007.tfc.world.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.chunkdata.LerpFloatLayer;
import net.dries007.tfc.world.chunkdata.RegionChunkDataGenerator;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.RiverEdge;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.river.MidpointFractal;

import com.newterraearth.tfe.world.biome.NTERiverBiomeResolver;
import com.newterraearth.tfe.world.river.NTERiverHydrology;

public final class NTE121ClimateHelpers
{
    private static final int MIN_RIVER_WIDTH = 12;
    private static final float RIVER_INFLUENCE = (float) Units.blockToGridExact(40);
    private static final float RIVER_INFLUENCE_SQ = RIVER_INFLUENCE * RIVER_INFLUENCE;
    private static final float RIVER_INFLUENCE_BLOCKS_SQ = 40f * 40f;
    private static final Map<Long, Noise2D> RAINFALL_VARIANCE_NOISE = new ConcurrentHashMap<>();
    private static final Map<Region, WestCoastCache> WEST_COAST_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private NTE121ClimateHelpers()
    {
    }

    public static float getAverageGroundwater(ChunkGenerator generator, BlockPos pos)
    {
        return sampleGroundwater(generator, pos, true);
    }

    public static float getBaseGroundwater(ChunkGenerator generator, BlockPos pos)
    {
        return sampleGroundwater(generator, pos, false);
    }

    @Nullable
    public static LerpFloatLayer getAverageRainfallLayer(ChunkDataGenerator generator, ChunkPos chunkPos)
    {
        if (!(generator instanceof RegionChunkDataGenerator regionGenerator))
        {
            return null;
        }

        final RegionGenerator region = regionGenerator.regionGenerator();
        final int blockX = chunkPos.getMinBlockX();
        final int blockZ = chunkPos.getMinBlockZ();
        final int gridX = Units.blockToGrid(blockX);
        final int gridZ = Units.blockToGrid(blockZ);

        final Region.Point point00 = region.getOrCreateRegionPoint(gridX, gridZ);
        final Region.Point point01 = region.getOrCreateRegionPoint(gridX, gridZ + 1);
        final Region.Point point10 = region.getOrCreateRegionPoint(gridX + 1, gridZ);
        final Region.Point point11 = region.getOrCreateRegionPoint(gridX + 1, gridZ + 1);

        final double exactGridX = Units.blockToGridExact(blockX);
        final double exactGridZ = Units.blockToGridExact(blockZ);
        final double deltaX = exactGridX - gridX;
        final double deltaZ = exactGridZ - gridZ;
        final double dG = Units.blockToGridExact(16);

        return new LerpFloatLayer(point00.rainfall, point01.rainfall, point10.rainfall, point11.rainfall)
            .scaled(deltaX, deltaZ, dG)
            .apply(value -> Mth.clamp(value, 0f, 500f));
    }

    public static float getAverageRainfall(ChunkDataGenerator generator, ChunkPos chunkPos, int x, int z)
    {
        final LerpFloatLayer rainfallLayer = getAverageRainfallLayer(generator, chunkPos);
        return rainfallLayer != null ? rainfallLayer.getValue((x & 15) / 16f, (z & 15) / 16f) : Float.NEGATIVE_INFINITY;
    }

    private static float sampleGroundwater(ChunkGenerator generator, BlockPos pos, boolean includeRainfall)
    {
        if (!(generator instanceof ChunkGeneratorExtension extension))
        {
            return Float.NEGATIVE_INFINITY;
        }

        final ChunkDataProvider provider = extension.chunkDataProvider();
        if (!(provider.generator() instanceof RegionChunkDataGenerator regionGenerator))
        {
            return Float.NEGATIVE_INFINITY;
        }

        final RegionGenerator region = regionGenerator.regionGenerator();
        final ChunkPos chunkPos = new ChunkPos(pos);
        final int blockX = chunkPos.getMinBlockX();
        final int blockZ = chunkPos.getMinBlockZ();
        final int gridX = Units.blockToGrid(blockX);
        final int gridZ = Units.blockToGrid(blockZ);

        final Region.Point point00 = region.getOrCreateRegionPoint(gridX, gridZ);
        final Region.Point point01 = region.getOrCreateRegionPoint(gridX, gridZ + 1);
        final Region.Point point10 = region.getOrCreateRegionPoint(gridX + 1, gridZ);
        final Region.Point point11 = region.getOrCreateRegionPoint(gridX + 1, gridZ + 1);

        final LerpFloatLayer rainfallGridLayer = new LerpFloatLayer(point00.rainfall, point01.rainfall, point10.rainfall, point11.rainfall);
        final double exactGridX = Units.blockToGridExact(blockX);
        final double exactGridZ = Units.blockToGridExact(blockZ);
        final double deltaX = exactGridX - gridX;
        final double deltaZ = exactGridZ - gridZ;
        final double dG = Units.blockToGridExact(16);
        final LerpFloatLayer rainfallLayer = rainfallGridLayer.scaled(deltaX, deltaZ, dG);

        float groundwater00 = 0f;
        float groundwater01 = 0f;
        float groundwater10 = 0f;
        float groundwater11 = 0f;
        final NTERiverHydrology hydrology = generator.getBiomeSource() instanceof BiomeSourceExtension biomeSource
            ? NTERiverBiomeResolver.hydrology(biomeSource)
            : null;

        for (RiverEdge edge : region.getOrCreatePartitionPoint(gridX, gridZ).rivers())
        {
            final MidpointFractal fractal = edge.fractal();
            if (edge.width >= MIN_RIVER_WIDTH
                && fractal.maybeIntersect(exactGridX, exactGridZ, RIVER_INFLUENCE)
                && (hydrology == null || hydrology.retainsTfcEdge(edge)))
            {
                final float widthInfluence = Mth.map(edge.width, MIN_RIVER_WIDTH, RiverEdge.MAX_WIDTH, 0f, 1f);
                groundwater00 = adjustGroundwaterNearRiver(groundwater00, widthInfluence, fractal, exactGridX, exactGridZ);
                groundwater01 = adjustGroundwaterNearRiver(groundwater01, widthInfluence, fractal, exactGridX, exactGridZ + dG);
                groundwater10 = adjustGroundwaterNearRiver(groundwater10, widthInfluence, fractal, exactGridX + dG, exactGridZ);
                groundwater11 = adjustGroundwaterNearRiver(groundwater11, widthInfluence, fractal, exactGridX + dG, exactGridZ + dG);
            }
        }

        groundwater00 = Math.max(groundwater00, sampleRiverGroundwater(hydrology, blockX, blockZ));
        groundwater01 = Math.max(groundwater01, sampleRiverGroundwater(hydrology, blockX, blockZ + 16));
        groundwater10 = Math.max(groundwater10, sampleRiverGroundwater(hydrology, blockX + 16, blockZ));
        groundwater11 = Math.max(groundwater11, sampleRiverGroundwater(hydrology, blockX + 16, blockZ + 16));

        final LerpFloatLayer baseGroundwaterLayer = new LerpFloatLayer(groundwater00, groundwater01, groundwater10, groundwater11)
            .apply(value -> Mth.clamp(value, 0f, 500f));
        final float localX = (pos.getX() & 15) / 16f;
        final float localZ = (pos.getZ() & 15) / 16f;
        final float baseGroundwater = baseGroundwaterLayer.getValue(localX, localZ);
        if (!includeRainfall)
        {
            return baseGroundwater;
        }
        return Math.min(baseGroundwater + rainfallLayer.getValue(localX, localZ), 500f);
    }

    public static float getRainVariance(long levelSeed, ChunkGenerator generator, BlockPos pos)
    {
        final LerpFloatLayer rainVarianceLayer = getRainVarianceLayer(levelSeed, generator, new ChunkPos(pos));
        if (rainVarianceLayer == null)
        {
            return 0f;
        }

        final float localX = (pos.getX() & 15) / 16f;
        final float localZ = (pos.getZ() & 15) / 16f;
        return rainVarianceLayer.getValue(localX, localZ);
    }

    @Nullable
    public static LerpFloatLayer getRainVarianceLayer(long levelSeed, ChunkGenerator generator, ChunkPos chunkPos)
    {
        if (!(generator instanceof ChunkGeneratorExtension extension))
        {
            return null;
        }

        final ChunkDataProvider provider = extension.chunkDataProvider();
        if (!(provider.generator() instanceof RegionChunkDataGenerator regionGenerator))
        {
            return null;
        }

        final RegionGenerator region = regionGenerator.regionGenerator();
        final int blockX = chunkPos.getMinBlockX();
        final int blockZ = chunkPos.getMinBlockZ();
        final int gridX = Units.blockToGrid(blockX);
        final int gridZ = Units.blockToGrid(blockZ);
        final float variance00 = getPointRainVariance(levelSeed, region, extension.settings().temperatureScale(), gridX, gridZ);
        final float variance01 = getPointRainVariance(levelSeed, region, extension.settings().temperatureScale(), gridX, gridZ + 1);
        final float variance10 = getPointRainVariance(levelSeed, region, extension.settings().temperatureScale(), gridX + 1, gridZ);
        final float variance11 = getPointRainVariance(levelSeed, region, extension.settings().temperatureScale(), gridX + 1, gridZ + 1);
        final double exactGridX = Units.blockToGridExact(blockX);
        final double exactGridZ = Units.blockToGridExact(blockZ);
        final double deltaX = exactGridX - gridX;
        final double deltaZ = exactGridZ - gridZ;
        final double dG = Units.blockToGridExact(16);
        return new LerpFloatLayer(variance00, variance01, variance10, variance11)
            .scaled(deltaX, deltaZ, dG);
    }

    public static boolean isNorthernHemisphere(ChunkGenerator generator, int z)
    {
        if (!(generator instanceof ChunkGeneratorExtension extension))
        {
            return true;
        }
        return isNorthernHemisphere(z, extension.settings().temperatureScale());
    }

    public static boolean isNorthernHemisphere(int z, float hemisphereScale)
    {
        return NTEClimateSeasonModel.isNorthernHemisphere(z, hemisphereScale);
    }

    private static float adjustGroundwaterNearRiver(float currentValue, float widthInfluence, MidpointFractal fractal, double gridX, double gridZ)
    {
        final float distance = (float) fractal.intersectDistance(gridX, gridZ);
        final float distanceInfluence = Mth.clampedMap(distance, 0f, RIVER_INFLUENCE_SQ, 1f, 0f);
        return Math.max(currentValue, distanceInfluence * widthInfluence * 300f);
    }

    private static float sampleRiverGroundwater(@Nullable NTERiverHydrology hydrology, int blockX, int blockZ)
    {
        if (hydrology == null)
        {
            return 0f;
        }
        final NTERiverHydrology.ColumnProfile profile = hydrology.findGraphProfile(blockX, blockZ);
        if (profile == null)
        {
            return 0f;
        }
        final float distanceSq = (float) (profile.normalizedDistanceSq() * profile.channelRadius() * profile.channelRadius());
        final float distanceInfluence = Mth.clampedMap(distanceSq, 0f, RIVER_INFLUENCE_BLOCKS_SQ, 1f, 0f);
        final float widthInfluence = Mth.clampedMap((float) profile.channelRadius(), 1.35f, 18f, 0.15f, 1f);
        return distanceInfluence * widthInfluence * 300f;
    }

    private static float getPointRainVariance(long levelSeed, RegionGenerator generator, int temperatureScale, int gridX, int gridZ)
    {
        final Region region = generator.getOrCreateRegion(gridX, gridZ);
        final Region.Point point = region.requireAt(gridX, gridZ);
        final int index = region.index(gridX, gridZ);
        final byte[] westCoastDistance = getDistanceToWestCoast(region, temperatureScale);
        final Noise2D rainfallVarianceNoise = getRainfallVarianceNoise(levelSeed);
        float rainfallVariance = Mth.clampedMap(westCoastDistance[index] + (float) rainfallVarianceNoise.noise(gridX, gridZ), 0f, 80f, -1f, 1f);
        final float edgeBiasScale = Mth.clampedMap(point.distanceToEdge, 0f, 12f, 1f, 0f);
        rainfallVariance = Mth.lerp(edgeBiasScale, rainfallVariance, 0f);
        return Mth.clamp(rainfallVariance, -1f, 1f);
    }

    private static Noise2D getRainfallVarianceNoise(long levelSeed)
    {
        return RAINFALL_VARIANCE_NOISE.computeIfAbsent(levelSeed, seedValue -> {
            final NTESeed seed = NTESeed.of(seedValue);
            seed.next();
            seed.next();
            seed.next();
            seed.next();
            seed.next();
            return new OpenSimplex2D(seed.next())
                .octaves(2)
                .spread(0.1f)
                .scaled(0f, 20f);
        });
    }

    public static byte[] getDistanceToWestCoast(Region region, int temperatureScale)
    {
        final WestCoastCache cache = WEST_COAST_CACHE.get(region);
        if (cache != null && cache.temperatureScale() == temperatureScale)
        {
            return cache.distanceToWestCoast();
        }

        final byte[] distanceToWestCoast = buildWestCoastDistance(region, temperatureScale);
        WEST_COAST_CACHE.put(region, new WestCoastCache(temperatureScale, distanceToWestCoast));
        return distanceToWestCoast;
    }

    private static byte[] buildWestCoastDistance(Region region, int temperatureScale)
    {
        final Region.Point[] data = region.data();
        final byte[] distanceToWestCoast = new byte[data.length];
        final int sizeX = region.sizeX();
        final int sizeZ = region.sizeZ();

        for (int dx = 0; dx < sizeX; dx++)
        {
            for (int dz = 0; dz < sizeZ; dz++)
            {
                final int index = dx + sizeX * dz;
                final Region.Point point = data[index];
                if (point == null)
                {
                    continue;
                }

                if (dx == 0)
                {
                    distanceToWestCoast[index] = 0;
                    continue;
                }

                final Region.Point lastCenterPoint = data[index - 1];
                if (lastCenterPoint == null)
                {
                    distanceToWestCoast[index] = point.land() ? (byte) (25 + point.distanceToOcean) : 0;
                    continue;
                }

                final int lastCenterValue = distanceToWestCoast[index - 1];
                if (!point.land())
                {
                    distanceToWestCoast[index] = (byte) Math.max(lastCenterValue - 2, 0);
                    continue;
                }

                int sum = 0;
                final int pointZ = region.minZ() + dz;
                final float function = temperatureScale == 0 ? 0f : (float) triangle(Units.GRID_WIDTH_IN_BLOCK / (2f * temperatureScale), pointZ);
                final int start = -2 + (function > 0.1f ? 1 : 0);
                final int end = 2 - (function < -0.1f ? 1 : 0);
                for (int dz2 = start; dz2 <= end; dz2++)
                {
                    final int lastIndex = region.offset(index, -1, dz2);
                    if (lastIndex != -1 && data[lastIndex] != null)
                    {
                        sum += distanceToWestCoast[lastIndex];
                    }
                    else
                    {
                        sum += lastCenterValue;
                    }
                }
                distanceToWestCoast[index] = (byte) (Mth.ceil(sum / (1f + end - start)) + 1);
            }
        }

        final BitSet explored = new BitSet(data.length);
        final ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int index = 0; index < data.length; index++)
        {
            final Region.Point point = data[index];
            if (point != null && point.land())
            {
                explored.set(index);
                queue.add(index);
            }
        }

        while (!queue.isEmpty())
        {
            final int last = queue.removeFirst();
            final int lastDistance = distanceToWestCoast[last];
            final int nextDistance = lastDistance + (lastDistance > 40 ? -1 : 1);

            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dz = -1; dz <= 1; dz++)
                {
                    final int next = region.offset(last, dx, dz);
                    if (next == -1)
                    {
                        continue;
                    }

                    final Region.Point point = data[next];
                    if (point != null && distanceToWestCoast[next] == 0)
                    {
                        if (!explored.get(next))
                        {
                            distanceToWestCoast[next] = (byte) nextDistance;
                            queue.add(next);
                        }
                        explored.set(next);
                    }
                }
            }
        }

        return distanceToWestCoast;
    }

    private static double triangle(double frequency, double value)
    {
        return Math.abs(4f * frequency * value + 1f - 4f * Math.floor(frequency * value + 0.75f)) - 1f;
    }

    private record WestCoastCache(int temperatureScale, byte[] distanceToWestCoast) {}
}
