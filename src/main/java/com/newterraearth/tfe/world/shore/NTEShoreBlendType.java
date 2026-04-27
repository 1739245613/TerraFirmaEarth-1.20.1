package com.newterraearth.tfe.world.shore;

import java.util.function.Function;

import com.newterraearth.tfe.world.NTESeed;

public enum NTEShoreBlendType
{
    NONE(seed -> NTEShoreNoiseSampler.NONE),
    CLASSIC(NTEShoreNoise::classic),
    SANDY(NTEShoreNoise::sandyBeach),
    EMBAYMENTS(NTEShoreNoise::embayments),
    UPPER_TERRACE(NTEShoreNoise::upperTerrace),
    LOWER_TERRACE(NTEShoreNoise::lowerTerrace),
    SETBACK_CLIFFS(NTEShoreNoise::setbackCliffs),
    DUNES(NTEShoreNoise::dunes),
    ROCKY_SHORES(NTEShoreNoise::rockyShores),
    SEA_STACKS(NTEShoreNoise::seaStacks);

    public static final NTEShoreBlendType[] ALL = values();
    public static final int SIZE = ALL.length;

    private final Function<NTESeed, NTEShoreNoiseSampler> factory;

    NTEShoreBlendType(Function<NTESeed, NTEShoreNoiseSampler> factory)
    {
        this.factory = factory;
    }

    public NTEShoreNoiseSampler createNoiseSampler(NTESeed seed)
    {
        return factory.apply(seed);
    }
}
