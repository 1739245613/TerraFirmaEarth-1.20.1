package com.newterraearth.tfe.mixin;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.ChunkHeightFiller;
import net.dries007.tfc.world.biome.BiomeBlendType;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.river.RiverBlendType;
import net.dries007.tfc.world.river.RiverInfo;
import net.dries007.tfc.world.river.RiverNoiseSampler;

import com.newterraearth.tfe.world.NTEBiomeExtensionAccess;
import com.newterraearth.tfe.world.NTEChunkHeightFillerAccess;
import com.newterraearth.tfe.world.NTEChunkShoreContext;
import com.newterraearth.tfe.debug.NTERuntimeTrace;
import com.newterraearth.tfe.world.river.NTERiverBlendType;
import com.newterraearth.tfe.world.river.NTERiverNoiseSampler;
import com.newterraearth.tfe.world.shore.NTEShoreBlendType;
import com.newterraearth.tfe.world.shore.NTEShoreNoiseSampler;
import com.newterraearth.tfe.world.terrain.NTETerrainUpliftSampler;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureBlendType;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoiseSampler;

@Mixin(value = ChunkHeightFiller.class, remap = false)
public abstract class ChunkHeightFillerMixin implements NTEChunkHeightFillerAccess
{
    @Shadow protected Map<BiomeExtension, BiomeNoiseSampler> biomeNoiseSamplers;
    @Shadow protected Object2DoubleMap<BiomeNoiseSampler> columnBiomeNoiseSamplers;
    @Shadow protected BiomeSourceExtension biomeSource;
    @Shadow protected Map<RiverBlendType, RiverNoiseSampler> riverNoiseSamplers;
    @Shadow protected double[] riverBlendWeights;
    @Shadow protected int seaLevel;
    @Shadow protected int blockX;
    @Shadow protected int blockZ;
    @Shadow protected int localX;
    @Shadow protected int localZ;

    @Shadow protected abstract @Nullable RiverInfo sampleRiverInfo(boolean useCache);

    @Shadow protected abstract void updateLocalCaches(Object2DoubleMap<BiomeExtension> biomeWeights, BiomeExtension biomeAt, @Nullable RiverInfo info, double height);

    @Unique private boolean tfe$hasShoreRuntime;
    @Unique private Map<NTERiverBlendType, NTERiverNoiseSampler> tfe$exactRiverNoiseSamplers = Collections.emptyMap();
    @Unique private double[] tfe$exactRiverBlendWeights = new double[NTERiverBlendType.SIZE];
    @Unique private Map<NTEShoreBlendType, NTEShoreNoiseSampler> tfe$shoreNoiseSamplers = Collections.emptyMap();
    @Unique private double[] tfe$shoreBlendWeights = new double[NTEShoreBlendType.SIZE];
    @Unique private Noise2D tfe$tideHeightNoise;
    @Unique private Map<NTECenteredFeatureBlendType, NTECenteredFeatureNoiseSampler> tfe$centeredFeatureNoiseSamplers = Collections.emptyMap();
    @Unique private NTETerrainUpliftSampler tfe$terrainUpliftSampler;
    @Unique private double tfe$terrainUpliftBaseHeight;
    @Unique private double tfe$terrainUpliftTopHeight;
    @Unique private double tfe$terrainUpliftAmount;
    @Unique private boolean tfe$forceSubterraneanCaveRiver;
    @Unique private boolean tfe$couldBeSalty;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void tfe$init(Object2DoubleMap<BiomeExtension>[] sampledBiomeWeights, BiomeSourceExtension biomeSource, Map<BiomeExtension, BiomeNoiseSampler> biomeNoiseSamplers, Map<RiverBlendType, RiverNoiseSampler> riverNoiseSamplers, Noise2D shoreSampler, int seaLevel, CallbackInfo ci)
    {
        final NTEChunkShoreContext.Context context = NTEChunkShoreContext.current();
        if (context != null)
        {
            tfe$hasShoreRuntime = true;
            tfe$exactRiverNoiseSamplers = context.riverNoiseSamplers();
            tfe$shoreNoiseSamplers = context.shoreNoiseSamplers();
            tfe$tideHeightNoise = context.tideHeightNoise();
            tfe$centeredFeatureNoiseSamplers = context.centeredFeatureNoiseSamplers();
            tfe$terrainUpliftSampler = context.terrainUpliftSampler();
        }
        else
        {
            tfe$hasShoreRuntime = false;
            tfe$exactRiverNoiseSamplers = Collections.emptyMap();
            tfe$shoreNoiseSamplers = Collections.emptyMap();
            tfe$tideHeightNoise = null;
            tfe$centeredFeatureNoiseSamplers = Collections.emptyMap();
            tfe$terrainUpliftSampler = null;
        }
        tfe$exactRiverBlendWeights = new double[NTERiverBlendType.SIZE];
        tfe$shoreBlendWeights = new double[NTEShoreBlendType.SIZE];
        tfe$couldBeSalty = false;
    }

    @Override
    public boolean tfe$hasShoreRuntime()
    {
        return tfe$hasShoreRuntime;
    }

    @Override
    public Map<NTEShoreBlendType, NTEShoreNoiseSampler> tfe$getShoreNoiseSamplers()
    {
        return tfe$shoreNoiseSamplers;
    }

    @Override
    public double[] tfe$getShoreBlendWeights()
    {
        return tfe$shoreBlendWeights;
    }

    @Override
    public Noise2D tfe$getTideHeightNoise()
    {
        return tfe$tideHeightNoise;
    }

    @Override
    public Object2DoubleMap<BiomeNoiseSampler> tfe$getColumnBiomeNoiseSamplers()
    {
        return columnBiomeNoiseSamplers;
    }

    @Override
    public double[] tfe$getExactRiverBlendWeights()
    {
        return tfe$exactRiverBlendWeights;
    }

    @Override
    public Map<NTERiverBlendType, NTERiverNoiseSampler> tfe$getExactRiverNoiseSamplers()
    {
        return tfe$exactRiverNoiseSamplers;
    }

    @Override
    public double[] tfe$getRiverBlendWeights()
    {
        return riverBlendWeights;
    }

    @Override
    public Map<RiverBlendType, RiverNoiseSampler> tfe$getRiverNoiseSamplers()
    {
        return riverNoiseSamplers;
    }

    @Override
    public Map<NTECenteredFeatureBlendType, NTECenteredFeatureNoiseSampler> tfe$getCenteredFeatureNoiseSamplers()
    {
        return tfe$centeredFeatureNoiseSamplers;
    }

    @Override
    public double tfe$getTerrainUpliftBaseHeight()
    {
        return tfe$terrainUpliftBaseHeight;
    }

    @Override
    public double tfe$getTerrainUpliftTopHeight()
    {
        return tfe$terrainUpliftTopHeight;
    }

    @Override
    public double tfe$getTerrainUpliftAmount()
    {
        return tfe$terrainUpliftAmount;
    }

    @Override
    public boolean tfe$isForceSubterraneanCaveRiver()
    {
        return tfe$forceSubterraneanCaveRiver;
    }

    @Override
    public int tfe$getBlockX()
    {
        return blockX;
    }

    @Override
    public int tfe$getBlockZ()
    {
        return blockZ;
    }

    @Override
    public boolean tfe$couldBeSalty()
    {
        return tfe$couldBeSalty;
    }

    @Override
    public int tfe$getLocalX()
    {
        return localX;
    }

    @Override
    public int tfe$getLocalZ()
    {
        return localZ;
    }

    /**
     * @author Codex
     * @reason Port 1.21 shore blend weighting and tide-aware shoreline height adjustment into 1.20.
     */
    @Overwrite
    protected double sampleColumnHeightAndBiome(Object2DoubleMap<BiomeExtension> biomeWeights, boolean useCache)
    {
        if (!tfe$hasShoreRuntime)
        {
            return tfe$sampleColumnHeightAndBiome120(biomeWeights, useCache);
        }

        columnBiomeNoiseSamplers.clear();

        double height = 0;
        double normalHeight = 0;
        double shoreHeight = 0;
        double shoreWeight = 0;
        double oceanWeight = 0;

        BiomeExtension biomeAt = null;
        BiomeExtension normalBiomeAt = null;
        BiomeExtension shoreBiomeAt = null;
        BiomeExtension oceanBiomeAt = null;
        double maxNormalWeight = 0;
        double maxShoreWeight = 0;
        double maxOceanWeight = 0;
        tfe$couldBeSalty = false;
        final boolean trace = NTERuntimeTrace.isTargetColumn(blockX, blockZ);
        for (Object2DoubleMap.Entry<BiomeExtension> entry : biomeWeights.object2DoubleEntrySet())
        {
            final double biomeWeight = entry.getDoubleValue();
            final BiomeExtension biome = entry.getKey();
            final BiomeNoiseSampler sampler = biomeNoiseSamplers.get(biome);

            if (biome.isSalty())
            {
                tfe$couldBeSalty = true;
            }

            assert sampler != null : "Non-existent sampler for biome: " + biome.key();

            if (columnBiomeNoiseSamplers.containsKey(sampler))
            {
                columnBiomeNoiseSamplers.mergeDouble(sampler, biomeWeight, Double::sum);
            }
            else
            {
                sampler.setColumn(blockX, blockZ);
                columnBiomeNoiseSamplers.put(sampler, biomeWeight);
            }

            final double biomeHeight = biomeWeight * sampler.height();
            height += biomeHeight;

            if (biome.isShore())
            {
                shoreHeight += biomeHeight;
                shoreWeight += biomeWeight;
                if (maxShoreWeight < biomeWeight)
                {
                    shoreBiomeAt = biome;
                    maxShoreWeight = biomeWeight;
                }
            }
            else if (biome.biomeBlendType() == BiomeBlendType.OCEAN)
            {
                oceanWeight += biomeWeight;
                if (maxOceanWeight < biomeWeight)
                {
                    oceanBiomeAt = biome;
                    maxOceanWeight = biomeWeight;
                }
            }
            else
            {
                normalHeight += biomeHeight;
                if (maxNormalWeight < biomeWeight)
                {
                    normalBiomeAt = biome;
                    maxNormalWeight = biomeWeight;
                }
            }
        }

        biomeAt = normalBiomeAt;
        tfe$computeInitialShoreWeights(biomeWeights);
        final double baseHeight = height;
        if (trace)
        {
            tfe$trace("base", biomeWeights, baseHeight, normalHeight, shoreHeight, shoreWeight, oceanWeight, 0d, 0d, null);
        }

        final double landWeight = 1 - oceanWeight - shoreWeight;
        if (shoreWeight > 0 && shoreBiomeAt != null)
        {
            height = tfe$adjustHeightForShoreContributions(height, oceanWeight, landWeight, shoreWeight, maxShoreWeight, shoreBiomeAt, shoreHeight, normalHeight);
            if (shoreWeight > 0.5)
            {
                biomeAt = shoreBiomeAt;
            }
        }

        if (biomeAt == null)
        {
            biomeAt = oceanBiomeAt;
        }

        final double shoreAdjustedHeight = height;
        if (trace)
        {
            tfe$trace("shore", biomeWeights, height, normalHeight, shoreHeight, shoreWeight, oceanWeight, landWeight, maxShoreWeight, shoreBiomeAt);
        }

        if (oceanWeight >= 0.25d && tfe$tideHeightNoise != null)
        {
            final double tideAdjustedSeaEdgeHeight = tfe$tideHeightNoise.noise(blockX, blockZ) - 4d;
            height = Mth.clampedMap(landWeight, 0.32d, 0.36d, Math.min(height, tideAdjustedSeaEdgeHeight), height);
        }

        assert biomeAt != null;

        height = tfe$adjustHeightForCenteredFeatures(height);
        final double centeredFeatureHeight = height;
        final RiverInfo info = sampleRiverInfo(useCache);
        tfe$computeInitialExactRiverWeights(biomeWeights);
        final double terrainUplift = tfe$sampleTerrainUplift(biomeAt, biomeWeights);
        final double terrainUpliftBaseHeight = height;
        height += terrainUplift;
        final double initialCaveWeight = tfe$adjustExactRiverWeightsForCaves();
        final double caveTransitionTerrainUplift = terrainUplift * tfe$caveTransitionTerrainUpliftProtection(initialCaveWeight);
        tfe$forceSubterraneanCaveRiver = false;
        height = tfe$adjustHeightForExactRiverContributions(height, info, initialCaveWeight, caveTransitionTerrainUplift);
        if (trace)
        {
            tfe$traceFinal(biomeWeights, baseHeight, shoreAdjustedHeight, centeredFeatureHeight, terrainUpliftBaseHeight, terrainUplift, initialCaveWeight, caveTransitionTerrainUplift, info, height);
        }
        tfe$recordTerrainUpliftLayer(terrainUpliftBaseHeight, height, terrainUplift);

        if (useCache)
        {
            updateLocalCaches(biomeWeights, biomeAt, info, height);
        }

        return height;
    }

    @Unique
    private double tfe$sampleColumnHeightAndBiome120(Object2DoubleMap<BiomeExtension> biomeWeights, boolean useCache)
    {
        columnBiomeNoiseSamplers.clear();

        double height = 0;
        double normalHeight = 0;
        double shoreHeight = 0;
        double shoreWeight = 0;

        BiomeExtension biomeAt = null;
        BiomeExtension normalBiomeAt = null;
        BiomeExtension shoreBiomeAt = null;
        double maxNormalWeight = 0;
        double maxShoreWeight = 0;
        tfe$couldBeSalty = false;
        for (Object2DoubleMap.Entry<BiomeExtension> entry : biomeWeights.object2DoubleEntrySet())
        {
            final double biomeWeight = entry.getDoubleValue();
            final BiomeExtension biome = entry.getKey();
            final BiomeNoiseSampler sampler = biomeNoiseSamplers.get(biome);

            if (biome.isSalty())
            {
                tfe$couldBeSalty = true;
            }

            assert sampler != null : "Non-existent sampler for biome: " + biome.key();

            if (columnBiomeNoiseSamplers.containsKey(sampler))
            {
                columnBiomeNoiseSamplers.mergeDouble(sampler, biomeWeight, Double::sum);
            }
            else
            {
                sampler.setColumn(blockX, blockZ);
                columnBiomeNoiseSamplers.put(sampler, biomeWeight);
            }

            final double biomeHeight = biomeWeight * sampler.height();
            height += biomeHeight;

            if (biome.isShore())
            {
                shoreHeight += biomeHeight;
                shoreWeight += biomeWeight;
                if (maxShoreWeight < biomeWeight)
                {
                    shoreBiomeAt = biome;
                    maxShoreWeight = biomeWeight;
                }
            }
            else
            {
                normalHeight += biomeHeight;
                if (maxNormalWeight < biomeWeight)
                {
                    normalBiomeAt = biome;
                    maxNormalWeight = biomeWeight;
                }
            }
        }

        biomeAt = normalBiomeAt;
        if (biomeAt == null)
        {
            biomeAt = shoreBiomeAt;
        }
        final double baseHeight = height;

        if (shoreWeight > 0.5 && shoreBiomeAt != null)
        {
            final double cliffInfluence = Mth.clamp(
                Mth.map(height, seaLevel, seaLevel + 20, 0, 0.6),
                0.0,
                1.0
            );
            final double adjustedCliffInfluence = 1.0 - (1.0 - cliffInfluence) * (1.0 - cliffInfluence);
            final double x2 = Mth.lerp(adjustedCliffInfluence, 0.8, 0.515);
            final double y2 = 1.15 - 0.3 * x2;
            final double adjustedShoreWeight = shoreWeight < x2
                ? Mth.map(shoreWeight, 0.5, x2, 0.5, y2)
                : Mth.map(shoreWeight, x2, 1.0, y2, 1.0);

            final double normalWeight = 1.0 - shoreWeight;
            final double adjustedNormalWeight = 1.0 - adjustedShoreWeight;
            final double adjustedHeight = Math.max(
                (adjustedShoreWeight / shoreWeight) * shoreHeight + (adjustedNormalWeight / normalWeight) * normalHeight,
                seaLevel
            );

            if (adjustedHeight < height)
            {
                height = adjustedHeight;
            }

            biomeAt = shoreBiomeAt;
        }
        final double shoreAdjustedHeight = height;

        assert biomeAt != null;

        height = tfe$adjustHeightForCenteredFeatures(height);
        final double centeredFeatureHeight = height;
        final RiverInfo info = sampleRiverInfo(useCache);
        tfe$computeInitialExactRiverWeights(biomeWeights);
        final double terrainUplift = tfe$sampleTerrainUplift(biomeAt, biomeWeights);
        final double terrainUpliftBaseHeight = height;
        height += terrainUplift;
        final double initialCaveWeight = tfe$adjustExactRiverWeightsForCaves();
        final double caveTransitionTerrainUplift = terrainUplift * tfe$caveTransitionTerrainUpliftProtection(initialCaveWeight);
        tfe$forceSubterraneanCaveRiver = false;
        height = tfe$adjustHeightForExactRiverContributions(height, info, initialCaveWeight, caveTransitionTerrainUplift);
        tfe$recordTerrainUpliftLayer(terrainUpliftBaseHeight, height, terrainUplift);

        if (useCache)
        {
            updateLocalCaches(biomeWeights, biomeAt, info, height);
        }
        return height;
    }

    @Unique
    private void tfe$computeInitialShoreWeights(Object2DoubleMap<BiomeExtension> biomeWeights)
    {
        Arrays.fill(tfe$shoreBlendWeights, 0d);
        for (Object2DoubleMap.Entry<BiomeExtension> entry : biomeWeights.object2DoubleEntrySet())
        {
            final NTEShoreBlendType blendType = ((NTEBiomeExtensionAccess) (Object) entry.getKey()).tfe$getShoreBlendType();
            tfe$shoreBlendWeights[blendType.ordinal()] += entry.getDoubleValue();
        }
    }

    @Unique
    private double tfe$adjustHeightForShoreContributions(double height, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
    {
        double shoreBlendHeight = 0d;
        for (NTEShoreBlendType type : NTEShoreBlendType.ALL)
        {
            final double weight = tfe$shoreBlendWeights[type.ordinal()];
            final NTEShoreNoiseSampler sampler = tfe$shoreNoiseSamplers.get(type);
            if (type == NTEShoreBlendType.NONE)
            {
                shoreBlendHeight += weight * height;
            }
            else if (weight > 0 && sampler != null)
            {
                shoreBlendHeight += weight * sampler.setColumnAndSampleHeight(height, blockX, blockZ, oceanWeight, landWeight, shoreWeight, thisWeight, biome, shoreHeight, normalHeight);
            }
        }
        return shoreBlendHeight;
    }

    @Unique
    private double tfe$adjustHeightForCenteredFeatures(double heightIn)
    {
        double centeredFeatureHeight = NTECenteredFeatureNoiseSampler.NOT_PRESENT_RETURN;
        for (NTECenteredFeatureBlendType type : NTECenteredFeatureBlendType.ALL)
        {
            if (type == NTECenteredFeatureBlendType.NONE)
            {
                continue;
            }
            final NTECenteredFeatureNoiseSampler sampler = tfe$centeredFeatureNoiseSamplers.get(type);
            if (sampler != null)
            {
                final double sampledHeight = sampler.setColumnAndSampleHeight(heightIn, blockX, blockZ, biomeSource);
                if (sampledHeight > centeredFeatureHeight)
                {
                    centeredFeatureHeight = sampledHeight;
                }
            }
        }
        return centeredFeatureHeight == NTECenteredFeatureNoiseSampler.NOT_PRESENT_RETURN ? heightIn : centeredFeatureHeight;
    }

    @Unique
    private double tfe$sampleTerrainUplift(BiomeExtension biomeAt, Object2DoubleMap<BiomeExtension> biomeWeights)
    {
        if (tfe$terrainUpliftSampler == null)
        {
            return 0d;
        }

        final double rawUplift = tfe$terrainUpliftSampler.sample(blockX, blockZ);
        if (rawUplift <= 0d)
        {
            return 0d;
        }

        double uplift = tfe$terrainUpliftSampler.sampleWithOceanExtension(blockX, blockZ, rawUplift);
        if (uplift <= 0d)
        {
            return 0d;
        }

        final double protectedWaterWeight = tfe$lakeUpliftSuppression(biomeWeights);
        return uplift * (1d - Mth.clamp(protectedWaterWeight, 0d, 1d));
    }

    @Unique
    private void tfe$recordTerrainUpliftLayer(double baseHeight, double topHeight, double terrainUplift)
    {
        if (terrainUplift <= 0d || topHeight <= baseHeight)
        {
            tfe$terrainUpliftBaseHeight = 0d;
            tfe$terrainUpliftTopHeight = 0d;
            tfe$terrainUpliftAmount = 0d;
            return;
        }
        tfe$terrainUpliftBaseHeight = baseHeight;
        tfe$terrainUpliftTopHeight = topHeight;
        tfe$terrainUpliftAmount = topHeight - baseHeight;
    }

    @Unique
    private static double tfe$lakeUpliftSuppression(Object2DoubleMap<BiomeExtension> biomeWeights)
    {
        double weight = 0d;
        for (Object2DoubleMap.Entry<BiomeExtension> entry : biomeWeights.object2DoubleEntrySet())
        {
            if (entry.getKey().biomeBlendType() == BiomeBlendType.LAKE)
            {
                weight += entry.getDoubleValue();
            }
        }
        return tfe$smoothStep(Mth.clampedMap(weight, 0.08d, 0.86d, 0d, 1d));
    }

    @Unique
    private static double tfe$smoothStep(double value)
    {
        final double t = Mth.clamp(value, 0d, 1d);
        return t * t * (3d - 2d * t);
    }

    @Unique
    private static double tfe$caveTransitionTerrainUpliftProtection(double initialCaveWeight)
    {
        return tfe$smoothStep(Mth.clampedMap(initialCaveWeight, 0.40d, 0.75d, 0d, 1d));
    }

    @Unique
    private void tfe$computeInitialExactRiverWeights(Object2DoubleMap<BiomeExtension> biomeWeights)
    {
        Arrays.fill(tfe$exactRiverBlendWeights, 0d);
        for (Object2DoubleMap.Entry<BiomeExtension> entry : biomeWeights.object2DoubleEntrySet())
        {
            final NTERiverBlendType blendType = ((NTEBiomeExtensionAccess) (Object) entry.getKey()).tfe$getRiverBlendType();
            tfe$exactRiverBlendWeights[blendType.ordinal()] += entry.getDoubleValue();
        }
    }

    @Unique
    private double tfe$adjustExactRiverWeightsForCaves()
    {
        final double initialCaveWeight = tfe$exactRiverBlendWeights[NTERiverBlendType.CAVE.ordinal()];
        if (initialCaveWeight > 0)
        {
            final double totalWeight = 1.0 - tfe$exactRiverBlendWeights[NTERiverBlendType.NONE.ordinal()];
            final double adjustedCaveWeight = initialCaveWeight < 0.25
                ? Mth.map(initialCaveWeight, 0.0, 0.25, 0, 0.1 * totalWeight)
                : totalWeight;

            for (NTERiverBlendType type : NTERiverBlendType.ALL)
            {
                final double weight = tfe$exactRiverBlendWeights[type.ordinal()];
                tfe$exactRiverBlendWeights[type.ordinal()] = weight * (1.0 - adjustedCaveWeight) / (1.0 - initialCaveWeight);
            }
            tfe$exactRiverBlendWeights[NTERiverBlendType.CAVE.ordinal()] = adjustedCaveWeight;
        }
        return initialCaveWeight;
    }

    @Unique
    private double tfe$adjustHeightForExactRiverContributions(double height, @Nullable RiverInfo info, double initialCaveWeight, double caveTransitionTerrainUplift)
    {
        if (info != null)
        {
            double riverBlendHeight = 0d;
            for (NTERiverBlendType type : NTERiverBlendType.ALL)
            {
                final double weight = tfe$exactRiverBlendWeights[type.ordinal()];
                final NTERiverNoiseSampler sampler = tfe$exactRiverNoiseSamplers.get(type);
                if (type == NTERiverBlendType.NONE)
                {
                    riverBlendHeight += weight * height;
                }
                else if (weight > 0 && sampler != null)
                {
                    final boolean caveTransition = type == NTERiverBlendType.CAVE && caveTransitionTerrainUplift > 0d;
                    final double sampleHeight = caveTransition ? height - caveTransitionTerrainUplift : height;
                    final double sampledHeight = sampler.setColumnAndSampleHeight(info, blockX, blockZ, sampleHeight, initialCaveWeight, weight);
                    riverBlendHeight += weight * (caveTransition ? sampledHeight + caveTransitionTerrainUplift : sampledHeight);
                }
            }
            return riverBlendHeight;
        }

        Arrays.fill(tfe$exactRiverBlendWeights, 0);
        tfe$exactRiverBlendWeights[NTERiverBlendType.NONE.ordinal()] = 1.0;
        return height;
    }

    @Unique
    private void tfe$trace(String stage, Object2DoubleMap<BiomeExtension> biomeWeights, double height, double normalHeight, double shoreHeight, double shoreWeight, double oceanWeight, double landWeight, double maxShoreWeight, @Nullable BiomeExtension shoreBiomeAt)
    {
        System.out.printf(
            "[TFE][RuntimeTrace][terrain_cut][%s] x=%d z=%d h=%.3f normalH=%.3f shoreH=%.3f landW=%.3f shoreW=%.3f oceanW=%.3f maxShoreW=%.3f shoreBiome=%s biomeWeights=%s shoreWeights=%s%n",
            stage,
            blockX,
            blockZ,
            height,
            normalHeight,
            shoreHeight,
            landWeight,
            shoreWeight,
            oceanWeight,
            maxShoreWeight,
            tfe$biomeName(shoreBiomeAt),
            tfe$formatBiomeWeights(biomeWeights),
            tfe$formatShoreWeights()
        );
    }

    @Unique
    private void tfe$traceFinal(Object2DoubleMap<BiomeExtension> biomeWeights, double baseHeight, double shoreAdjustedHeight, double centeredFeatureHeight, double terrainUpliftBaseHeight, double terrainUplift, double initialCaveWeight, double caveTransitionTerrainUplift, @Nullable RiverInfo info, double finalHeight)
    {
        System.out.printf(
            "[TFE][RuntimeTrace][terrain_cut][final] x=%d z=%d base=%.3f shore=%.3f centered=%.3f upliftBase=%.3f uplift=%.3f caveInitial=%.3f caveTransitionUplift=%.3f final=%.3f river=%s exactWeights=%s biomeWeights=%s noRiverBiome=%s%n",
            blockX,
            blockZ,
            baseHeight,
            shoreAdjustedHeight,
            centeredFeatureHeight,
            terrainUpliftBaseHeight,
            terrainUplift,
            initialCaveWeight,
            caveTransitionTerrainUplift,
            finalHeight,
            tfe$formatRiverInfo(info),
            tfe$formatExactRiverWeights(),
            tfe$formatBiomeWeights(biomeWeights),
            tfe$biomeName(biomeSource.getBiomeExtensionNoRiver(net.minecraft.core.QuartPos.fromBlock(blockX), net.minecraft.core.QuartPos.fromBlock(blockZ)))
        );
    }

    @Unique
    private static String tfe$biomeName(@Nullable BiomeExtension biome)
    {
        return biome == null ? "null" : biome.key().location().getPath();
    }

    @Unique
    private static String tfe$formatRiverInfo(@Nullable RiverInfo info)
    {
        if (info == null)
        {
            return "null";
        }
        return String.format("normDistSq=%.3f widthSq=%.3f", info.normDistSq(), info.widthSq());
    }

    @Unique
    private static String tfe$formatBiomeWeights(Object2DoubleMap<BiomeExtension> biomeWeights)
    {
        return biomeWeights.object2DoubleEntrySet().stream()
            .sorted((a, b) -> Double.compare(b.getDoubleValue(), a.getDoubleValue()))
            .map(entry -> tfe$biomeName(entry.getKey()) + "=" + String.format("%.3f", entry.getDoubleValue()))
            .collect(Collectors.joining(","));
    }

    @Unique
    private String tfe$formatExactRiverWeights()
    {
        return Arrays.stream(NTERiverBlendType.ALL)
            .filter(type -> tfe$exactRiverBlendWeights[type.ordinal()] > 1.0e-6d)
            .map(type -> type.name() + "=" + String.format("%.3f", tfe$exactRiverBlendWeights[type.ordinal()]))
            .collect(Collectors.joining(","));
    }

    @Unique
    private String tfe$formatShoreWeights()
    {
        return Arrays.stream(NTEShoreBlendType.ALL)
            .filter(type -> tfe$shoreBlendWeights[type.ordinal()] > 1.0e-6d)
            .map(type -> type.name() + "=" + String.format("%.3f", tfe$shoreBlendWeights[type.ordinal()]))
            .collect(Collectors.joining(","));
    }
}
