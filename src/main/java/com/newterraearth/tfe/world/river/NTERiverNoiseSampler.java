package com.newterraearth.tfe.world.river;

import net.dries007.tfc.world.river.RiverInfo;

/**
 * Port of the 1.21 river noise sampler contract.
 */
public interface NTERiverNoiseSampler
{
    NTERiverNoiseSampler NONE = new NTERiverNoiseSampler() {};

    default double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double caveWeight, double thisWeight)
    {
        return heightIn;
    }

    default double noise(int y, double noiseIn)
    {
        return noiseIn;
    }
}
