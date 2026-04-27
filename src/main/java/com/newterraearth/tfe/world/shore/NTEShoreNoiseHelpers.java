package com.newterraearth.tfe.world.shore;

import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.Noise3D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.noise.OpenSimplex3D;

import com.newterraearth.tfe.world.NTESeed;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

public final class NTEShoreNoiseHelpers
{
    private NTEShoreNoiseHelpers()
    {
    }

    public static Noise3D cliffNoise(NTESeed seed)
    {
        return new OpenSimplex3D(seed.seed()).octaves(2).spread(0.1f);
    }

    public static Noise2D lowerTerraceNoise(NTESeed seed)
    {
        return BiomeNoise.hills(seed.seed(), 7, 15);
    }

    public static Noise2D upperTerraceNoise(NTESeed seed)
    {
        return BiomeNoise.hills(seed.seed(), 18, 30);
    }

    public static Noise2D shoreTideLevelNoise(NTESeed seed)
    {
        return new OpenSimplex2D(seed.seed())
            .octaves(3)
            .spread(0.005f)
            .scaled(SEA_LEVEL_Y - 6, SEA_LEVEL_Y + 6)
            .clamped(SEA_LEVEL_Y, SEA_LEVEL_Y + 4)
            .add(new OpenSimplex2D(seed.seed()).spread(0.03));
    }
}
