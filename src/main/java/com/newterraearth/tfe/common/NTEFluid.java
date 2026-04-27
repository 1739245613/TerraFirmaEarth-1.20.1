package com.newterraearth.tfe.common;

import java.util.Locale;

public enum NTEFluid
{
    CANOLA_OIL(0xFFBA8507),
    CANOLA_OIL_WATER(0xFF9F6B07);

    private final String id;
    private final int color;

    NTEFluid(int color)
    {
        this.id = name().toLowerCase(Locale.ROOT);
        this.color = color;
    }

    public String getId()
    {
        return id;
    }

    public int getColor()
    {
        return color;
    }
}
