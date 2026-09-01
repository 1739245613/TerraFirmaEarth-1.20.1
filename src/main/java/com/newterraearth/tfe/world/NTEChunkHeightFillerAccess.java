package com.newterraearth.tfe.world;

import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.river.RiverBlendType;
import net.dries007.tfc.world.river.RiverNoiseSampler;

import com.newterraearth.tfe.world.river.NTERiverBlendType;
import com.newterraearth.tfe.world.river.NTERiverHydrology;
import com.newterraearth.tfe.world.river.NTERiverNoiseSampler;
import com.newterraearth.tfe.world.shore.NTEShoreBlendType;
import com.newterraearth.tfe.world.shore.NTEShoreNoiseSampler;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureBlendType;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoiseSampler;

public interface NTEChunkHeightFillerAccess
{
    boolean tfe$hasShoreRuntime();

    Map<NTEShoreBlendType, NTEShoreNoiseSampler> tfe$getShoreNoiseSamplers();

    double[] tfe$getShoreBlendWeights();

    Noise2D tfe$getTideHeightNoise();

    Object2DoubleMap<BiomeNoiseSampler> tfe$getColumnBiomeNoiseSamplers();

    double[] tfe$getExactRiverBlendWeights();

    Map<NTERiverBlendType, NTERiverNoiseSampler> tfe$getExactRiverNoiseSamplers();

    Map<NTERiverBlendType, NTERiverNoiseSampler> tfe$getRetainedRiverNoiseSamplers();

    double[] tfe$getSupplementalRiverBlendWeights();

    double[] tfe$getRetainedRiverBlendWeights();

    boolean tfe$usesConfluenceCarvingUnion();

    double[] tfe$getRiverBlendWeights();

    Map<RiverBlendType, RiverNoiseSampler> tfe$getRiverNoiseSamplers();

    Map<NTECenteredFeatureBlendType, NTECenteredFeatureNoiseSampler> tfe$getCenteredFeatureNoiseSamplers();

    double tfe$getTerrainUpliftBaseHeight();

    double tfe$getTerrainUpliftTopHeight();

    double tfe$getTerrainUpliftAmount();

    boolean tfe$isForceSubterraneanCaveRiver();

    int tfe$getBlockX();

    int tfe$getBlockZ();

    boolean tfe$couldBeSalty();

    int tfe$getLocalX();

    int tfe$getLocalZ();

    NTERiverHydrology.ColumnProfile tfe$getRiverHydrologyProfile(int localX, int localZ);

    boolean[] tfe$getNativeDryRiverBanks();

    double tfe$getRiverTerrainHeight(int localX, int localZ);

    int[] tfe$getPreVolcanicHeights();

    int[] tfe$getSurfaceIntegrityDepth();

    void tfe$recordRiverHydrologyProfile(int localX, int localZ);
}
