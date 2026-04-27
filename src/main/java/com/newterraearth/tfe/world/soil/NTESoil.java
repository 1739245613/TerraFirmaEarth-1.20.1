package com.newterraearth.tfe.world.soil;

import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blocks.soil.SoilBlockType;

public enum NTESoil
{
    ALFISOL("alfisol", "silt", SoilBlockType.Variant.SILT, false),
    ENTISOL("entisol", "loam", SoilBlockType.Variant.LOAM, false),
    ARIDISOL("aridisol", "sandy_loam", SoilBlockType.Variant.SANDY_LOAM, false),
    ANDISOL("andisol", "silty_loam", SoilBlockType.Variant.SILTY_LOAM, false),
    FLUVISOL("fluvisol", "fluvisol", null, true),
    MOLLISOL("mollisol", "mollisol", null, true),
    PODZOL("podzol", "podzol", null, true),
    OXISOL("oxisol", "oxisol", null, true);

    private final String serializedName;
    private final String blockPath;
    @Nullable
    private final SoilBlockType.Variant nativeVariant;
    private final boolean addonFamily;

    NTESoil(String serializedName, String blockPath, @Nullable SoilBlockType.Variant nativeVariant, boolean addonFamily)
    {
        this.serializedName = serializedName;
        this.blockPath = blockPath;
        this.nativeVariant = nativeVariant;
        this.addonFamily = addonFamily;
    }

    public String serializedName()
    {
        return serializedName;
    }

    public String blockPath()
    {
        return blockPath;
    }

    public boolean isAddonFamily()
    {
        return addonFamily;
    }

    public boolean isNativeFamily()
    {
        return nativeVariant != null;
    }

    @Nullable
    public SoilBlockType.Variant nativeVariant()
    {
        return nativeVariant;
    }
}
