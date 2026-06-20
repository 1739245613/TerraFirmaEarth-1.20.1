package com.newterraearth.tfe.mixin;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Beardifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.dries007.tfc.world.ChunkBaseBlockSource;
import net.dries007.tfc.world.ChunkNoiseFiller;
import net.dries007.tfc.world.MutableDensityFunctionContext;
import net.dries007.tfc.world.TFCAquifer;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.dries007.tfc.world.noise.ChunkNoiseSamplingSettings;
import net.dries007.tfc.world.noise.TrilinearInterpolator;
import net.dries007.tfc.world.river.RiverInfo;

import com.newterraearth.tfe.world.NTEChunkBaseBlockSourceAccess;
import com.newterraearth.tfe.world.NTEChunkHeightFillerAccess;
import com.newterraearth.tfe.world.river.NTERiverBlendType;
import com.newterraearth.tfe.world.river.NTERiverNoiseSampler;
import com.newterraearth.tfe.world.shore.NTEShoreBlendType;
import com.newterraearth.tfe.world.shore.NTEShoreNoiseSampler;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

@Mixin(value = ChunkNoiseFiller.class, remap = false)
public abstract class ChunkNoiseFillerMixin
{
    @Shadow private int[] surfaceHeight;
    @Shadow private BiomeExtension[] localBiomes;
    @Shadow private BiomeExtension[] localBiomesNoRivers;
    @Shadow private double[] localBiomeWeights;
    @Shadow private ChunkBaseBlockSource baseBlockSource;
    @Shadow private TrilinearInterpolator noiseCaves;
    @Shadow private TrilinearInterpolator noodleToggle;
    @Shadow private TrilinearInterpolator noodleThickness;
    @Shadow private TrilinearInterpolator noodleRidgeA;
    @Shadow private TrilinearInterpolator noodleRidgeB;
    @Shadow private Beardifier beardifier;
    @Shadow private MutableDensityFunctionContext mutableDensityFunctionContext;
    @Shadow private TFCAquifer aquifer;
    @Shadow private ChunkNoiseSamplingSettings settings;

    /**
     * @author Codex
     * @reason Port the 1.21 shore density pass after river carving noise.
     */
    @Overwrite
    private double calculateNoiseAtHeight(int y, double heightNoiseValue)
    {
        final NTEChunkHeightFillerAccess access = (NTEChunkHeightFillerAccess) this;
        final Object2DoubleMap<net.dries007.tfc.world.BiomeNoiseSampler> columnBiomeNoiseSamplers = access.tfe$getColumnBiomeNoiseSamplers();
        final double[] riverBlendWeights = access.tfe$getExactRiverBlendWeights();

        double noise = 0;
        for (Object2DoubleMap.Entry<net.dries007.tfc.world.BiomeNoiseSampler> entry : columnBiomeNoiseSamplers.object2DoubleEntrySet())
        {
            final net.dries007.tfc.world.BiomeNoiseSampler sampler = entry.getKey();
            noise += sampler.noise(y) * entry.getDoubleValue();
        }

        final double initialNoise = tfe$applyTerrainUpliftLayerNoise(y, noise, access);
        noise = 0;
        for (NTERiverBlendType type : NTERiverBlendType.ALL)
        {
            final double weight = riverBlendWeights[type.ordinal()];
            if (type == NTERiverBlendType.NONE)
            {
                noise += weight * initialNoise;
            }
            else if (weight > 0)
            {
                final NTERiverNoiseSampler sampler = access.tfe$getExactRiverNoiseSamplers().get(type);
                if (sampler != null)
                {
                    noise += weight * sampler.noise(y, initialNoise);
                }
            }
        }
        final double riverAdjustedNoise = noise;

        if (access.tfe$hasShoreRuntime())
        {
            final double[] shoreBlendWeights = access.tfe$getShoreBlendWeights();
            noise = 0;
            for (NTEShoreBlendType type : NTEShoreBlendType.ALL)
            {
                final double weight = shoreBlendWeights[type.ordinal()];
                if (type == NTEShoreBlendType.NONE)
                {
                    noise += weight * riverAdjustedNoise;
                }
                else if (weight > 0)
                {
                    final NTEShoreNoiseSampler sampler = access.tfe$getShoreNoiseSamplers().get(type);
                    if (sampler != null)
                    {
                        noise += weight * sampler.noise(tfe$shoreNoiseY(type, y, access), riverAdjustedNoise);
                    }
                }
            }
        }
        noise = tfe$protectTerrainUpliftLayerAfterShore(y, noise, access);

        noise = net.dries007.tfc.world.BiomeNoiseSampler.AIR_THRESHOLD - noise;
        if (y > heightNoiseValue)
        {
            noise -= (y - heightNoiseValue) * 0.2f;
        }

        return Mth.clamp(noise, -1, 1);
    }

    @Unique
    private static double tfe$applyTerrainUpliftLayerNoise(int y, double initialNoise, NTEChunkHeightFillerAccess access)
    {
        if (!tfe$isTerrainUpliftLayer(y, access))
        {
            return initialNoise;
        }

        return Math.min(initialNoise, net.dries007.tfc.world.BiomeNoiseSampler.SOLID);
    }

    @Unique
    private static boolean tfe$isTerrainUpliftLayer(int y, NTEChunkHeightFillerAccess access)
    {
        return access.tfe$getTerrainUpliftAmount() > 0d
            && y > access.tfe$getTerrainUpliftBaseHeight()
            && y <= access.tfe$getTerrainUpliftTopHeight();
    }

    @Unique
    private static double tfe$protectTerrainUpliftLayerAfterShore(int y, double noise, NTEChunkHeightFillerAccess access)
    {
        if (!tfe$isTerrainUpliftLayer(y, access))
        {
            return noise;
        }

        if (tfe$hasExactRiverDensity(access))
        {
            return noise;
        }
        if (tfe$hasTerrainCarvingShoreDensity(access))
        {
            return noise;
        }

        return Math.min(noise, net.dries007.tfc.world.BiomeNoiseSampler.SOLID);
    }

    @Unique
    private static int tfe$shoreNoiseY(NTEShoreBlendType type, int y, NTEChunkHeightFillerAccess access)
    {
        if (!tfe$isTerrainCarvingShoreType(type))
        {
            return y;
        }

        if (!tfe$isTerrainUpliftAffectedHeight(y, access))
        {
            return y;
        }

        return Mth.floor(y - access.tfe$getTerrainUpliftAmount());
    }

    @Unique
    private static boolean tfe$isTerrainUpliftAffectedHeight(int y, NTEChunkHeightFillerAccess access)
    {
        return access.tfe$getTerrainUpliftAmount() > 0d
            && y <= access.tfe$getTerrainUpliftTopHeight();
    }

    @Unique
    private static boolean tfe$hasTerrainCarvingShoreDensity(NTEChunkHeightFillerAccess access)
    {
        if (!access.tfe$hasShoreRuntime())
        {
            return false;
        }

        final double[] weights = access.tfe$getShoreBlendWeights();
        for (NTEShoreBlendType type : NTEShoreBlendType.ALL)
        {
            if (tfe$isTerrainCarvingShoreType(type) && weights[type.ordinal()] > 1.0e-4d)
            {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static boolean tfe$isTerrainCarvingShoreType(NTEShoreBlendType type)
    {
        return type == NTEShoreBlendType.SANDY
            || type == NTEShoreBlendType.DUNES
            || type == NTEShoreBlendType.SEA_STACKS
            || type == NTEShoreBlendType.ROCKY_SHORES
            || type == NTEShoreBlendType.EMBAYMENTS
            || type == NTEShoreBlendType.UPPER_TERRACE
            || type == NTEShoreBlendType.LOWER_TERRACE
            || type == NTEShoreBlendType.SETBACK_CLIFFS;
    }

    @Unique
    private static boolean tfe$hasExactRiverDensity(NTEChunkHeightFillerAccess access)
    {
        if (access.tfe$isForceSubterraneanCaveRiver())
        {
            return true;
        }

        final double[] weights = access.tfe$getExactRiverBlendWeights();
        for (NTERiverBlendType type : NTERiverBlendType.ALL)
        {
            if (type != NTERiverBlendType.NONE && weights[type.ordinal()] > 1.0e-4d)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @author Codex
     * @reason Keep the shore-ported density pass aligned with native cave noise behavior.
     */
    @Overwrite
    private BlockState calculateBlockStateAtNoise(int y, double terrainNoise)
    {
        double terrainAndCaveNoise = terrainNoise;
        final NTEChunkHeightFillerAccess access = (NTEChunkHeightFillerAccess) this;
        final int blockX = access.tfe$getBlockX();
        final int blockZ = access.tfe$getBlockZ();

        if (noodleToggle.sample() >= 0)
        {
            final double thickness = Mth.clampedMap(noodleThickness.sample(), -1, 1, 0.05, 0.1);
            final double ridgeA = Math.abs(1.5 * noodleRidgeA.sample()) - thickness;
            final double ridgeB = Math.abs(1.5 * noodleRidgeB.sample()) - thickness;
            final double ridge = Math.max(ridgeA, ridgeB);

            terrainAndCaveNoise = Math.min(terrainAndCaveNoise, ridge);
        }

        terrainAndCaveNoise = Math.min(terrainAndCaveNoise, noiseCaves.sample());

        mutableDensityFunctionContext.cursor().set(blockX, y, blockZ);
        terrainAndCaveNoise += beardifier.compute(mutableDensityFunctionContext);

        final BlockState aquiferState = aquifer.sampleState(blockX, y, blockZ, terrainAndCaveNoise);
        if (aquiferState != null)
        {
            return aquiferState;
        }
        return baseBlockSource.getBaseBlock(blockX, y, blockZ);
    }

    /**
     * @author Codex
     * @reason Port 1.21 shore saltwater edge handling into local biome caching.
     */
    @Overwrite
    protected void updateLocalCaches(Object2DoubleMap<BiomeExtension> biomeWeights, BiomeExtension biomeAt, @Nullable RiverInfo info, double height)
    {
        final NTEChunkHeightFillerAccess access = (NTEChunkHeightFillerAccess) this;
        final int localX = access.tfe$getLocalX();
        final int localZ = access.tfe$getLocalZ();
        final int localIndex = localX + 16 * localZ;
        final boolean couldBeSalty = access.tfe$couldBeSalty();

        localBiomesNoRivers[localIndex] = biomeAt;
        if (height <= SEA_LEVEL_Y + 1 && info != null && info.normDistSq() < 1.1 && biomeAt.hasRivers())
        {
            biomeAt = TFCBiomes.RIVER;
        }

        localBiomes[localIndex] = biomeAt;
        final double biomeWeightAt = biomeWeights.getOrDefault(biomeAt, 0.5);
        localBiomeWeights[localIndex] = biomeWeightAt;
        surfaceHeight[localIndex] = (int) height;

        ((NTEChunkBaseBlockSourceAccess) baseBlockSource).tfe$useAccurateBiome(localX, localZ, biomeAt, biomeWeightAt, couldBeSalty);
    }

}
