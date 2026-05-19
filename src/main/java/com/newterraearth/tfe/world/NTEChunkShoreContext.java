package com.newterraearth.tfe.world;

import java.util.Map;

import net.dries007.tfc.world.noise.Noise2D;

import com.newterraearth.tfe.world.river.NTERiverBlendType;
import com.newterraearth.tfe.world.river.NTERiverNoiseSampler;
import com.newterraearth.tfe.world.shore.NTEShoreBlendType;
import com.newterraearth.tfe.world.shore.NTEShoreNoiseSampler;
import com.newterraearth.tfe.world.terrain.NTETerrainUpliftSampler;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureBlendType;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoiseSampler;

public final class NTEChunkShoreContext
{
    public record Context(
        Map<NTEShoreBlendType, NTEShoreNoiseSampler> shoreNoiseSamplers,
        Noise2D tideHeightNoise,
        Map<NTERiverBlendType, NTERiverNoiseSampler> riverNoiseSamplers,
        Map<NTECenteredFeatureBlendType, NTECenteredFeatureNoiseSampler> centeredFeatureNoiseSamplers,
        NTETerrainUpliftSampler terrainUpliftSampler
    ) {}

    public interface Scope extends AutoCloseable
    {
        @Override
        void close();
    }

    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private NTEChunkShoreContext()
    {
    }

    public static Scope open(
        Map<NTEShoreBlendType, NTEShoreNoiseSampler> shoreNoiseSamplers,
        Noise2D tideHeightNoise,
        Map<NTERiverBlendType, NTERiverNoiseSampler> riverNoiseSamplers,
        Map<NTECenteredFeatureBlendType, NTECenteredFeatureNoiseSampler> centeredFeatureNoiseSamplers,
        NTETerrainUpliftSampler terrainUpliftSampler
    )
    {
        CURRENT.set(new Context(shoreNoiseSamplers, tideHeightNoise, riverNoiseSamplers, centeredFeatureNoiseSamplers, terrainUpliftSampler));
        return CURRENT::remove;
    }

    public static Context current()
    {
        return CURRENT.get();
    }
}
