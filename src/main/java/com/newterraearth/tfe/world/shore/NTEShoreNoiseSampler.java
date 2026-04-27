package com.newterraearth.tfe.world.shore;

import net.dries007.tfc.world.biome.BiomeExtension;

public interface NTEShoreNoiseSampler
{
    NTEShoreNoiseSampler NONE = new NTEShoreNoiseSampler() {};

    default double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
    {
        return heightIn;
    }

    default double noise(int y, double noiseIn)
    {
        return noiseIn;
    }
}
