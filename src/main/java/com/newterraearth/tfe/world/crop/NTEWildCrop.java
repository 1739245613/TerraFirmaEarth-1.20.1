package com.newterraearth.tfe.world.crop;

import java.util.Locale;

public enum NTEWildCrop
{
    ALFALFA,
    CANOLA,
    CASSAVA,
    LENTIL,
    PEANUT,
    RADISH;

    public String serializedName()
    {
        return name().toLowerCase(Locale.ROOT);
    }

    public String id()
    {
        return "wild_crop/" + serializedName();
    }
}
