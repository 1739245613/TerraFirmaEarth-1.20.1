package com.newterraearth.tfe.common.entity;

import java.util.Locale;

/** Fish introduced by TFC after the 1.20 dependency version. */
public enum NTEFish
{
    ARCTIC_CHAR(0x5C728A, 0xD6E6EF, 0.7f, 0.4f),
    BURBOT(0x6B5A4A, 0xC0A27B, 0.7f, 0.4f),
    MUKSUN(0x5E7787, 0xD7D1B2, 0.7f, 0.4f),
    NORTHERN_PIKE(0x516F3A, 0xB8B870, 0.7f, 0.4f),
    PACU(0x6B5137, 0xD59A62, 0.7f, 0.4f),
    PEACOCK_BASS(0x315F48, 0xD2A34A, 0.7f, 0.4f),
    RED_PIRANHA(0x8B302A, 0xD9A14B, 0.7f, 0.4f),
    SPOTTED_GUDGEON(0x6C6759, 0xC7B883, 0.7f, 0.4f),
    TILAPIA(0x4B5964, 0xB4C2BD, 0.7f, 0.4f);

    private final String serializedName;
    private final int eggColor1;
    private final int eggColor2;
    private final float width;
    private final float height;

    NTEFish(int eggColor1, int eggColor2, float width, float height)
    {
        this.serializedName = name().toLowerCase(Locale.ROOT);
        this.eggColor1 = eggColor1;
        this.eggColor2 = eggColor2;
        this.width = width;
        this.height = height;
    }

    public String getSerializedName()
    {
        return serializedName;
    }

    public int getEggColor1()
    {
        return eggColor1;
    }

    public int getEggColor2()
    {
        return eggColor2;
    }

    public float getWidth()
    {
        return width;
    }

    public float getHeight()
    {
        return height;
    }
}
