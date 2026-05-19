package com.newterraearth.tfe.world.terrain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;

import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.biome.BiomeBlendType;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.noise.Noise2D;

import com.newterraearth.tfe.config.NTECommonConfig;
import com.newterraearth.tfe.world.region.NTERegionNoise;

/**
 * Regional relief field for the main terrain subset.
 *
 * Mountain-like biome clusters act as wide uplift sources. Columns blend the
 * nearby contributions with a soft-max style average so adjacent sources do
 * not form hard Voronoi-style seams.
 */
public final class NTETerrainUpliftSampler
{
    private static final long SOURCE_POSITION_SALT_X = 0x52E1F7A4C395B12DL;
    private static final long SOURCE_POSITION_SALT_Z = 0x31B7A41E9D6C08F5L;
    private static final int SOURCE_GRID_SIZE = 512;
    private static final int SOURCE_CANDIDATE_GRID_SIZE = 8;
    private static final int SOURCE_CANDIDATE_STEP = SOURCE_GRID_SIZE / SOURCE_CANDIDATE_GRID_SIZE;
    private static final int MAX_SOURCES_PER_CELL = 5;
    private static final int DEFAULT_SMALL_PLATFORM_RADIUS = 10;
    private static final int LINE_VOLCANO_LENGTH = 900;
    private static final int LINE_VOLCANO_HALF_LENGTH = LINE_VOLCANO_LENGTH / 2;
    private static final int LINE_VOLCANO_DIRECTION_SAMPLE_STEP = 64;
    private static final int SHIELD_VOLCANO_PLATFORM_SAMPLE_STEP = 32;
    private static final int MAX_SHIELD_VOLCANO_PLATFORM_RADIUS = 544;
    private static final double[][] RADIAL_DIRECTIONS = buildRadialDirections(32);
    private static final double OCEAN_EXTENSION_BLOCKS_PER_HEIGHT = 4d;
    private static final int OCEAN_EXTENSION_SAMPLE_STEP = 4;
    private static final int COASTAL_RELIEF_BLEND_DISTANCE = 96;
    private static final double COASTAL_RELIEF_MIN_FACTOR = 0.18d;

    private final long seed;
    private final BiomeSourceExtension biomeSource;
    private final Map<Long, SourceSet> sourceCache;
    private final Map<BiomeExtension, BiomeNoiseSampler> heightSamplerCache;
    private final boolean enabled;
    private final double sourceHeight;
    private final int sourceFalloffDistance;
    private final int smallPlatformRadius;
    private final int sourceRadiusCells;
    private Noise2D activeShieldVolcanoSourceNoise;
    private Noise2D dormantShieldVolcanoSourceNoise;
    private Noise2D extinctShieldVolcanoSourceNoise;
    private Noise2D ancientShieldVolcanoSourceNoise;
    private Noise2D shieldVolcanoIntensitySourceNoise;

    public NTETerrainUpliftSampler(long seed, BiomeSourceExtension biomeSource)
    {
        this.seed = seed;
        this.biomeSource = biomeSource;
        this.sourceCache = new HashMap<>();
        this.heightSamplerCache = new HashMap<>();
        this.enabled = NTECommonConfig.isTerrainUpliftEnabled();
        this.sourceHeight = NTECommonConfig.getTerrainUpliftSourceHeight();
        this.sourceFalloffDistance = NTECommonConfig.getTerrainUpliftSourceFalloffDistance();
        this.smallPlatformRadius = NTECommonConfig.getTerrainUpliftSmallPlatformRadius();
        final int sourceSearchRadius = sourceFalloffDistance + Math.max(MAX_SHIELD_VOLCANO_PLATFORM_RADIUS, smallPlatformRadius + LINE_VOLCANO_HALF_LENGTH);
        this.sourceRadiusCells = Math.max(1, (sourceSearchRadius + SOURCE_GRID_SIZE - 1) / SOURCE_GRID_SIZE);
        NTECommonConfig.logTerrainUpliftConfigOnce("sampler", "seed=" + seed);
    }

    public double sample(int blockX, int blockZ)
    {
        if (!enabled || sourceHeight <= 0d)
        {
            return 0d;
        }

        final int centerCellX = Math.floorDiv(blockX, SOURCE_GRID_SIZE);
        final int centerCellZ = Math.floorDiv(blockZ, SOURCE_GRID_SIZE);
        double weightedUplift = 0d;
        double weightSum = 0d;
        double strongestUplift = 0d;

        for (int cellX = centerCellX - sourceRadiusCells; cellX <= centerCellX + sourceRadiusCells; cellX++)
        {
            for (int cellZ = centerCellZ - sourceRadiusCells; cellZ <= centerCellZ + sourceRadiusCells; cellZ++)
            {
                final SourceSet sourceSet = sourceAt(cellX, cellZ);
                if (!sourceSet.active())
                {
                    continue;
                }

                for (Source source : sourceSet.sources())
                {
                    final double contribution = sourceContribution(source, blockX, blockZ);
                    if (contribution <= 0d)
                    {
                        continue;
                    }

                    strongestUplift = Math.max(strongestUplift, contribution);
                    final double weight = contribution * contribution;
                    weightedUplift += contribution * weight;
                    weightSum += weight;
                }
            }
        }
        if (weightSum <= 0d)
        {
            return 0d;
        }
        return Mth.lerp(0.55d, weightedUplift / weightSum, strongestUplift);
    }

    public double sampleWithOceanExtension(int blockX, int blockZ, double rawUplift)
    {
        if (rawUplift <= 0d)
        {
            return 0d;
        }

        final BiomeExtension biome = sampleBiome(blockX, blockZ);
        if (isOceanExtensionAnchor(biome))
        {
            return sampleCoastalLandRelief(blockX, blockZ, rawUplift);
        }

        final double maxDistance = COASTAL_RELIEF_BLEND_DISTANCE;
        final double landDistance = sampleNearestLandDistance(blockX, blockZ, maxDistance);
        if (!Double.isFinite(landDistance))
        {
            return 0d;
        }
        final double fade = 1d - tfe$smoothStep(Mth.clamp(landDistance / maxDistance, 0d, 1d));
        return rawUplift * COASTAL_RELIEF_MIN_FACTOR * fade * fade;
    }

    public double sampleCoastalLandRelief(int blockX, int blockZ, double rawUplift)
    {
        if (rawUplift <= 0d)
        {
            return 0d;
        }
        final BiomeExtension biome = sampleBiome(blockX, blockZ);
        if (!isOceanExtensionAnchor(biome))
        {
            return 0d;
        }

        final double oceanDistance = sampleNearestOceanDistance(blockX, blockZ, COASTAL_RELIEF_BLEND_DISTANCE);
        if (!Double.isFinite(oceanDistance))
        {
            return rawUplift;
        }
        final double fade = tfe$smoothStep(Mth.clamp(oceanDistance / COASTAL_RELIEF_BLEND_DISTANCE, 0d, 1d));
        return rawUplift * Mth.lerp(fade, COASTAL_RELIEF_MIN_FACTOR, 1d);
    }

    private SourceSet sourceAt(int cellX, int cellZ)
    {
        final long key = cellKey(cellX, cellZ);
        final SourceSet cached = sourceCache.get(key);
        if (cached != null)
        {
            return cached;
        }

        final SourceSet sources = findHighestSourcesInCell(cellX, cellZ);
        sourceCache.put(key, sources);
        return sources;
    }

    private SourceSet findHighestSourcesInCell(int cellX, int cellZ)
    {
        final int cellMinX = cellX * SOURCE_GRID_SIZE;
        final int cellMinZ = cellZ * SOURCE_GRID_SIZE;
        final int sourceLimit = sourceCountForCell(cellX, cellZ);
        final SourceCandidate[] candidates = new SourceCandidate[sourceLimit];
        double bestShieldVolcanoScore = Double.NEGATIVE_INFINITY;
        int bestShieldVolcanoX = cellMinX + SOURCE_GRID_SIZE / 2;
        int bestShieldVolcanoZ = cellMinZ + SOURCE_GRID_SIZE / 2;
        SourceProfile bestShieldVolcanoProfile = null;

        for (int i = 0; i < sourceLimit; i++)
        {
            candidates[i] = SourceCandidate.EMPTY;
        }

        for (int candidateX = 0; candidateX < SOURCE_CANDIDATE_GRID_SIZE; candidateX++)
        {
            for (int candidateZ = 0; candidateZ < SOURCE_CANDIDATE_GRID_SIZE; candidateZ++)
            {
                final int blockX = cellMinX + candidateX * SOURCE_CANDIDATE_STEP + SOURCE_CANDIDATE_STEP / 2;
                final int blockZ = cellMinZ + candidateZ * SOURCE_CANDIDATE_STEP + SOURCE_CANDIDATE_STEP / 2;
                final BiomeExtension biome = sampleBiome(blockX, blockZ);
                final SourceProfile profile = sourceProfile(biome);
                if (profile == null)
                {
                    continue;
                }

                if (profile.shieldVolcano())
                {
                    final double score = shieldVolcanoSourceScore(profile, blockX, blockZ);
                    if (score > bestShieldVolcanoScore)
                    {
                        bestShieldVolcanoScore = score;
                        bestShieldVolcanoX = blockX;
                        bestShieldVolcanoZ = blockZ;
                        bestShieldVolcanoProfile = profile;
                    }
                    continue;
                }

                final double height = sampleHeight(biome, blockX, blockZ);
                insertCandidate(height, blockX, blockZ, profile, candidates);
            }
        }

        int activeCount = 0;
        while (activeCount < sourceLimit && candidates[activeCount].active())
        {
            activeCount++;
        }
        if (activeCount == 0)
        {
            if (Double.isFinite(bestShieldVolcanoScore))
            {
                return new SourceSet(new Source[] {createSource(bestShieldVolcanoProfile, bestShieldVolcanoX, bestShieldVolcanoZ)});
            }
            return SourceSet.EMPTY;
        }

        final boolean hasShieldVolcanoSource = Double.isFinite(bestShieldVolcanoScore);
        final Source[] sources = new Source[Math.min(sourceLimit, activeCount + (hasShieldVolcanoSource ? 1 : 0))];
        int sourceIndex = 0;
        if (hasShieldVolcanoSource)
        {
            sources[sourceIndex++] = createSource(bestShieldVolcanoProfile, bestShieldVolcanoX, bestShieldVolcanoZ);
        }
        for (int i = 0; i < activeCount && sourceIndex < sources.length; i++)
        {
            final SourceCandidate candidate = candidates[i];
            sources[sourceIndex++] = createSource(candidate.profile(), candidate.x(), candidate.z());
        }
        return new SourceSet(sources);
    }

    private Source createSource(SourceProfile profile, int blockX, int blockZ)
    {
        if (profile == null)
        {
            return new Source(blockX, blockZ, smallPlatformRadius, SourceShape.POINT, 1d, 0d, 0.86d);
        }
        if (profile.shape() == SourceShape.LINE)
        {
            final double[] direction = estimateLineVolcanoDirection(blockX, blockZ);
            return new Source(blockX, blockZ, smallPlatformRadius, SourceShape.LINE, direction[0], direction[1], profile.edgeFactor());
        }
        if (profile == SourceProfile.NORMAL)
        {
            return new Source(blockX, blockZ, smallPlatformRadius, SourceShape.POINT, 1d, 0d, profile.edgeFactor());
        }
        final int platformRadius = profile.dynamicPlatform() ? estimateShieldVolcanoPlatformRadius(profile, blockX, blockZ) : profile.platformRadius();
        return new Source(blockX, blockZ, platformRadius, SourceShape.POINT, 1d, 0d, profile.edgeFactor());
    }

    private double sourceContribution(Source source, int blockX, int blockZ)
    {
        final double distance = sourceDistance(source, blockX, blockZ);
        final int platformRadius = source.platformRadius();
        final double edgeFactor = source.edgeFactor();
        final double effectiveRadius = Math.max(1d, platformRadius);
        if (distance <= platformRadius)
        {
            final double t = Mth.clamp(distance / effectiveRadius, 0d, 1d);
            final double smooth = tfe$smoothStep(t);
            return sourceHeight * Mth.lerp(smooth, 1d, edgeFactor);
        }

        final double falloffDistance = distance - platformRadius;
        if (falloffDistance >= sourceFalloffDistance)
        {
            return 0d;
        }
        return sourceHeight * edgeFactor * falloff(falloffDistance / sourceFalloffDistance);
    }

    private static double sourceDistance(Source source, int blockX, int blockZ)
    {
        final double dx = blockX - source.x();
        final double dz = blockZ - source.z();
        if (source.shape() != SourceShape.LINE)
        {
            return Math.sqrt(dx * dx + dz * dz);
        }

        final double along = Mth.clamp(dx * source.directionX() + dz * source.directionZ(), -LINE_VOLCANO_HALF_LENGTH, LINE_VOLCANO_HALF_LENGTH);
        final double closestX = source.directionX() * along;
        final double closestZ = source.directionZ() * along;
        final double sideX = dx - closestX;
        final double sideZ = dz - closestZ;
        return Math.sqrt(sideX * sideX + sideZ * sideZ);
    }

    private double shieldVolcanoSourceScore(SourceProfile profile, int blockX, int blockZ)
    {
        return shieldVolcanoSourceNoise(profile).noise(blockX, blockZ);
    }

    private int estimateShieldVolcanoPlatformRadius(SourceProfile profile, int blockX, int blockZ)
    {
        if (!profile.dynamicPlatform())
        {
            return 0;
        }

        final Noise2D noise = shieldVolcanoSourceNoise(profile);
        final double centerScore = noise.noise(blockX, blockZ);
        if (centerScore <= profile.platformThreshold())
        {
            return profile.minPlatformRadius();
        }

        final double[] distances = new double[RADIAL_DIRECTIONS.length];
        for (int i = 0; i < RADIAL_DIRECTIONS.length; i++)
        {
            final double[] direction = RADIAL_DIRECTIONS[i];
            distances[i] = sampleShieldVolcanoThresholdDistance(noise, blockX, blockZ, direction[0], direction[1], centerScore, profile.platformThreshold(), profile.maxPlatformRadius());
        }
        Arrays.sort(distances);

        final int middle = distances.length / 2;
        final double median = distances.length % 2 == 0 ? 0.5d * (distances[middle - 1] + distances[middle]) : distances[middle];
        return Mth.clamp((int) Math.round(median), profile.minPlatformRadius(), profile.maxPlatformRadius());
    }

    private static double sampleShieldVolcanoThresholdDistance(Noise2D noise, int blockX, int blockZ, double directionX, double directionZ, double centerScore, double threshold, int maxDistance)
    {
        double previousDistance = 0d;
        double previousScore = centerScore;

        for (int distance = SHIELD_VOLCANO_PLATFORM_SAMPLE_STEP; distance <= maxDistance; distance += SHIELD_VOLCANO_PLATFORM_SAMPLE_STEP)
        {
            final int x = blockX + Mth.floor(directionX * distance);
            final int z = blockZ + Mth.floor(directionZ * distance);
            final double score = noise.noise(x, z);
            if (score <= threshold)
            {
                final double scoreDelta = previousScore - score;
                if (scoreDelta <= 1.0e-6)
                {
                    return distance;
                }
                final double t = Mth.clamp((previousScore - threshold) / scoreDelta, 0d, 1d);
                return previousDistance + t * (distance - previousDistance);
            }

            previousDistance = distance;
            previousScore = score;
        }
        return maxDistance;
    }

    private double[] estimateLineVolcanoDirection(int blockX, int blockZ)
    {
        double bestScore = Double.NEGATIVE_INFINITY;
        double[] bestDirection = RADIAL_DIRECTIONS[0];
        for (int i = 0; i < RADIAL_DIRECTIONS.length / 2; i++)
        {
            final double[] direction = RADIAL_DIRECTIONS[i];
            final double score = sampleLineVolcanoRun(blockX, blockZ, direction[0], direction[1]) + sampleLineVolcanoRun(blockX, blockZ, -direction[0], -direction[1]);
            if (score > bestScore)
            {
                bestScore = score;
                bestDirection = direction;
            }
        }
        return bestDirection;
    }

    private double sampleLineVolcanoRun(int blockX, int blockZ, double directionX, double directionZ)
    {
        double score = 0d;
        for (int distance = LINE_VOLCANO_DIRECTION_SAMPLE_STEP; distance <= LINE_VOLCANO_HALF_LENGTH; distance += LINE_VOLCANO_DIRECTION_SAMPLE_STEP)
        {
            final int x = blockX + Mth.floor(directionX * distance);
            final int z = blockZ + Mth.floor(directionZ * distance);
            if (sourceProfile(sampleBiome(x, z)) == SourceProfile.LINE_VOLCANO)
            {
                score += LINE_VOLCANO_DIRECTION_SAMPLE_STEP;
            }
        }
        return score;
    }

    private Noise2D shieldVolcanoSourceNoise(SourceProfile profile)
    {
        return switch (profile)
            {
                case ACTIVE_SHIELD_VOLCANO -> activeShieldVolcanoSourceNoise();
                case DORMANT_SHIELD_VOLCANO -> dormantShieldVolcanoSourceNoise();
                case EXTINCT_SHIELD_VOLCANO -> extinctShieldVolcanoSourceNoise();
                case ANCIENT_SHIELD_VOLCANO -> ancientShieldVolcanoSourceNoise();
                case ICE_SHEET_SHIELD_VOLCANO, GLACIATED_SHIELD_VOLCANO -> shieldVolcanoIntensitySourceNoise();
                case NORMAL, LINE_VOLCANO -> (x, z) -> Double.NEGATIVE_INFINITY;
            };
    }

    private Noise2D activeShieldVolcanoSourceNoise()
    {
        if (activeShieldVolcanoSourceNoise == null)
        {
            activeShieldVolcanoSourceNoise = NTERegionNoise.activeHotSpots(seed);
        }
        return activeShieldVolcanoSourceNoise;
    }

    private Noise2D dormantShieldVolcanoSourceNoise()
    {
        if (dormantShieldVolcanoSourceNoise == null)
        {
            dormantShieldVolcanoSourceNoise = NTERegionNoise.dormantHotSpots(seed);
        }
        return dormantShieldVolcanoSourceNoise;
    }

    private Noise2D extinctShieldVolcanoSourceNoise()
    {
        if (extinctShieldVolcanoSourceNoise == null)
        {
            extinctShieldVolcanoSourceNoise = NTERegionNoise.extinctHotSpots(seed);
        }
        return extinctShieldVolcanoSourceNoise;
    }

    private Noise2D ancientShieldVolcanoSourceNoise()
    {
        if (ancientShieldVolcanoSourceNoise == null)
        {
            ancientShieldVolcanoSourceNoise = NTERegionNoise.ancientHotSpots(seed);
        }
        return ancientShieldVolcanoSourceNoise;
    }

    private Noise2D shieldVolcanoIntensitySourceNoise()
    {
        if (shieldVolcanoIntensitySourceNoise == null)
        {
            shieldVolcanoIntensitySourceNoise = NTERegionNoise.hotSpotIntensity(seed);
        }
        return shieldVolcanoIntensitySourceNoise;
    }

    private static void insertCandidate(double height, int blockX, int blockZ, SourceProfile profile, SourceCandidate[] candidates)
    {
        for (int i = 0; i < candidates.length; i++)
        {
            if (height > candidates[i].score())
            {
                for (int j = candidates.length - 1; j > i; j--)
                {
                    candidates[j] = candidates[j - 1];
                }
                candidates[i] = new SourceCandidate(height, blockX, blockZ, profile);
                return;
            }
        }
    }

    private int sourceCountForCell(int cellX, int cellZ)
    {
        long value = seed ^ ((long) cellX * SOURCE_POSITION_SALT_X) ^ ((long) cellZ * SOURCE_POSITION_SALT_Z);
        value = mix64(value);
        return 1 + (int) Long.remainderUnsigned(value, MAX_SOURCES_PER_CELL);
    }

    private double sampleHeight(BiomeExtension biome, int blockX, int blockZ)
    {
        BiomeNoiseSampler sampler = heightSamplerCache.get(biome);
        if (sampler == null && !heightSamplerCache.containsKey(biome))
        {
            sampler = biome.createNoiseSampler(seed);
            if (sampler != null)
            {
                heightSamplerCache.put(biome, sampler);
            }
        }
        if (sampler == null)
        {
            return Double.NEGATIVE_INFINITY;
        }
        sampler.setColumn(blockX, blockZ);
        return sampler.height();
    }

    private double sampleNearestLandDistance(int blockX, int blockZ, double maxDistance)
    {
        if (isOceanExtensionAnchor(sampleBiome(blockX, blockZ)))
        {
            return 0d;
        }
        return sampleNearestTransitionDistance(blockX, blockZ, maxDistance, false);
    }

    private double sampleNearestOceanDistance(int blockX, int blockZ, double maxDistance)
    {
        if (!isOceanExtensionAnchor(sampleBiome(blockX, blockZ)))
        {
            return 0d;
        }
        return sampleNearestTransitionDistance(blockX, blockZ, maxDistance, true);
    }

    private double sampleNearestTransitionDistance(int blockX, int blockZ, double maxDistance, boolean targetOcean)
    {
        for (int distance = OCEAN_EXTENSION_SAMPLE_STEP; distance <= maxDistance + OCEAN_EXTENSION_SAMPLE_STEP; distance += OCEAN_EXTENSION_SAMPLE_STEP)
        {
            for (double[] direction : RADIAL_DIRECTIONS)
            {
                if (matchesTargetBiome(blockX, blockZ, direction[0], direction[1], distance, targetOcean))
                {
                    return refineTransitionDistance(blockX, blockZ, direction[0], direction[1], distance - OCEAN_EXTENSION_SAMPLE_STEP, distance, targetOcean);
                }
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    private double refineTransitionDistance(int blockX, int blockZ, double directionX, double directionZ, int startDistance, int endDistance, boolean targetOcean)
    {
        final int minDistance = Math.max(0, startDistance + 1);
        for (int distance = minDistance; distance <= endDistance; distance++)
        {
            if (matchesTargetBiome(blockX, blockZ, directionX, directionZ, distance, targetOcean))
            {
                return distance;
            }
        }
        return endDistance;
    }

    private boolean matchesTargetBiome(int blockX, int blockZ, double directionX, double directionZ, int distance, boolean targetOcean)
    {
        final int x = blockX + Mth.floor(directionX * distance);
        final int z = blockZ + Mth.floor(directionZ * distance);
        final boolean isOcean = isOceanExtensionAnchor(sampleBiome(x, z));
        return isOcean == targetOcean;
    }

    private BiomeExtension sampleBiome(int blockX, int blockZ)
    {
        return biomeSource.getBiomeExtensionNoRiver(QuartPos.fromBlock(blockX), QuartPos.fromBlock(blockZ));
    }

    private static SourceProfile sourceProfile(BiomeExtension biome)
    {
        final String path = biome.key().location().getPath();
        if (biome.isSalty() || biome.isShore())
        {
            return null;
        }
        return switch (path)
            {
                case "mountains",
                    "old_mountains",
                    "extreme_doline_mountains",
                    "ice_sheet_mountains",
                    "glaciated_mountains",
                    "glacially_carved_mountains",
                    "tuyas",
                    "ice_sheet_tuyas" -> SourceProfile.NORMAL;
                case "volcanic_mountains" -> SourceProfile.LINE_VOLCANO;
                case "active_shield_volcano" -> SourceProfile.ACTIVE_SHIELD_VOLCANO;
                case "dormant_shield_volcano" -> SourceProfile.DORMANT_SHIELD_VOLCANO;
                case "extinct_shield_volcano" -> SourceProfile.EXTINCT_SHIELD_VOLCANO;
                case "ancient_shield_volcano" -> SourceProfile.ANCIENT_SHIELD_VOLCANO;
                case "ice_sheet_shield_volcano" -> SourceProfile.ICE_SHEET_SHIELD_VOLCANO;
                case "glaciated_shield_volcano" -> SourceProfile.GLACIATED_SHIELD_VOLCANO;
                default -> null;
            };
    }

    private static boolean isOceanExtensionAnchor(BiomeExtension biome)
    {
        return biome.biomeBlendType() != BiomeBlendType.OCEAN;
    }

    private static double tfe$smoothStep(double value)
    {
        final double t = Mth.clamp(value, 0d, 1d);
        return t * t * (3d - 2d * t);
    }

    private static double[][] buildRadialDirections(int count)
    {
        final double[][] directions = new double[count][2];
        for (int i = 0; i < count; i++)
        {
            final double angle = (Math.PI * 2d * i) / count;
            directions[i][0] = Math.cos(angle);
            directions[i][1] = Math.sin(angle);
        }
        return directions;
    }

    private static double falloff(double t)
    {
        if (t <= 0d)
        {
            return 1d;
        }
        if (t >= 1d)
        {
            return 0d;
        }
        final double clamped = Mth.clamp(t, 0d, 1d);
        return 1d - Math.sin(0.5d * Math.PI * clamped);
    }

    private static long cellKey(int cellX, int cellZ)
    {
        return ((long) cellX << 32) ^ (cellZ & 0xffffffffL);
    }

    private static long mix64(long value)
    {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private record Source(int x, int z, int platformRadius, SourceShape shape, double directionX, double directionZ, double edgeFactor) {}

    private record SourceCandidate(double score, int x, int z, SourceProfile profile)
    {
        private static final SourceCandidate EMPTY = new SourceCandidate(Double.NEGATIVE_INFINITY, 0, 0, SourceProfile.NORMAL);

        boolean active()
        {
            return Double.isFinite(score);
        }
    }

    private record SourceSet(Source[] sources)
    {
        private static final SourceSet EMPTY = new SourceSet(new Source[0]);

        boolean active()
        {
            return sources.length > 0;
        }
    }

    private enum SourceShape
    {
        POINT,
        LINE
    }

    private enum SourceProfile
    {
        NORMAL(DEFAULT_SMALL_PLATFORM_RADIUS, DEFAULT_SMALL_PLATFORM_RADIUS, 0d, SourceShape.POINT, 0.82d),
        LINE_VOLCANO(DEFAULT_SMALL_PLATFORM_RADIUS, DEFAULT_SMALL_PLATFORM_RADIUS, 0d, SourceShape.LINE, 0.78d),
        ACTIVE_SHIELD_VOLCANO(96, 288, 0.75d, SourceShape.POINT, 0.94d),
        DORMANT_SHIELD_VOLCANO(192, 544, 0.70d, SourceShape.POINT, 0.90d),
        EXTINCT_SHIELD_VOLCANO(192, 544, 0.60d, SourceShape.POINT, 0.86d),
        ANCIENT_SHIELD_VOLCANO(192, 544, 0.60d, SourceShape.POINT, 0.86d),
        ICE_SHEET_SHIELD_VOLCANO(192, 544, 0.72d, SourceShape.POINT, 0.91d),
        GLACIATED_SHIELD_VOLCANO(192, 544, 0.72d, SourceShape.POINT, 0.91d);

        private final int minPlatformRadius;
        private final int maxPlatformRadius;
        private final double platformThreshold;
        private final SourceShape shape;
        private final double edgeFactor;

        SourceProfile(int minPlatformRadius, int maxPlatformRadius, double platformThreshold, SourceShape shape, double edgeFactor)
        {
            this.minPlatformRadius = minPlatformRadius;
            this.maxPlatformRadius = maxPlatformRadius;
            this.platformThreshold = platformThreshold;
            this.shape = shape;
            this.edgeFactor = edgeFactor;
        }

        int platformRadius()
        {
            return minPlatformRadius;
        }

        int minPlatformRadius()
        {
            return minPlatformRadius;
        }

        int maxPlatformRadius()
        {
            return maxPlatformRadius;
        }

        double platformThreshold()
        {
            return platformThreshold;
        }

        boolean dynamicPlatform()
        {
            return maxPlatformRadius > 0;
        }

        boolean shieldVolcano()
        {
            return this != NORMAL && this != LINE_VOLCANO;
        }

        SourceShape shape()
        {
            return shape;
        }

        double edgeFactor()
        {
            return edgeFactor;
        }
    }
}
