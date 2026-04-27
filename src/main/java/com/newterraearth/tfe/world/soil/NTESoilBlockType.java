package com.newterraearth.tfe.world.soil;

public enum NTESoilBlockType
{
    DIRT("dirt"),
    GRASS("grass"),
    DUFF("duff"),
    GRASS_PATH("grass_path"),
    CLAY("clay"),
    CLAY_GRASS("clay_grass"),
    CLAY_DUFF("clay_duff"),
    ROOTED_DIRT("rooted_dirt"),
    COARSE_DIRT("coarse_dirt"),
    FARMLAND("farmland"),
    MUD("mud"),
    MUD_BRICKS("mud_bricks"),
    DRYING_BRICKS("drying_bricks"),
    MUDDY_ROOTS("muddy_roots");

    private final String directoryName;

    NTESoilBlockType(String directoryName)
    {
        this.directoryName = directoryName;
    }

    public String directoryName()
    {
        return directoryName;
    }

    public String id(NTESoil soil)
    {
        return directoryName + "/" + soil.blockPath();
    }

    public boolean requiresAddonRegistration(NTESoil soil)
    {
        return soil.isAddonFamily() || this == COARSE_DIRT || this == DUFF || this == CLAY_DUFF;
    }
}
