package com.newterraearth.tfe.world.river;

import java.util.function.Function;

import net.dries007.tfc.world.river.RiverBlendType;

import com.newterraearth.tfe.world.NTESeed;

public enum NTERiverBlendType
{
    NONE(seed -> NTERiverNoiseSampler.NONE),
    BANKED(NTERiverNoise::banked),
    TALL_BANKED(NTERiverNoise::tallBanked),
    FLOODPLAIN(NTERiverNoise::floodplain),
    WIDE(NTERiverNoise::wide),
    WIDE_DEEP(NTERiverNoise::wideDeep),
    CANYON(NTERiverNoise::canyon),
    TALL_CANYON(NTERiverNoise::tallCanyon),
    TALUS(NTERiverNoise::talus),
    TERRACES(NTERiverNoise::terraces),
    CAVE(NTERiverNoise::cave);

    public static final NTERiverBlendType[] ALL = values();
    public static final int SIZE = ALL.length;

    private final Function<NTESeed, NTERiverNoiseSampler> factory;

    NTERiverBlendType(Function<NTESeed, NTERiverNoiseSampler> factory)
    {
        this.factory = factory;
    }

    public NTERiverNoiseSampler createNoiseSampler(NTESeed seed)
    {
        return factory.apply(seed);
    }

    public static NTERiverBlendType fromLegacy(RiverBlendType type)
    {
        return switch (type)
        {
            case NONE -> NONE;
            case WIDE -> WIDE;
            case CANYON -> CANYON;
            case TALL_CANYON -> TALL_CANYON;
            case CAVE -> CAVE;
        };
    }
}
