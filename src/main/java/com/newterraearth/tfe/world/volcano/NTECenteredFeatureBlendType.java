package com.newterraearth.tfe.world.volcano;

import java.util.function.Function;

import com.newterraearth.tfe.world.NTESeed;

public enum NTECenteredFeatureBlendType
{
    NONE(seed -> NTECenteredFeatureNoiseSampler.NONE),
    CINDER_CONE(NTECenteredFeatureNoise::cinder),
    TUYA(NTECenteredFeatureNoise::tuya),
    TUFF_RING(NTECenteredFeatureNoise::tuffRing);

    public static final NTECenteredFeatureBlendType[] ALL = values();
    public static final int SIZE = ALL.length;

    private final Function<NTESeed, NTECenteredFeatureNoiseSampler> factory;

    NTECenteredFeatureBlendType(Function<NTESeed, NTECenteredFeatureNoiseSampler> factory)
    {
        this.factory = factory;
    }

    public NTECenteredFeatureNoiseSampler createNoiseSampler(NTESeed seed)
    {
        return factory.apply(seed);
    }
}
