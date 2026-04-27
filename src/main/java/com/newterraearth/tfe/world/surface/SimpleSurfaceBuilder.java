package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

public class SimpleSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory ROCKY_SHORE = seed -> new SimpleSurfaceBuilder(NTESurfaceStates.RAW, NTESurfaceStates.RAW, NTESurfaceStates.GRAVEL, true);
    public static final SurfaceBuilderFactory VOLCANIC_SOIL = seed -> new SimpleSurfaceBuilder(NTESurfaceStates.VOLCANIC_TOP_GRASS_TO_LOCAL_GRAVEL, NTESurfaceStates.VOLCANIC_MID_DIRT_TO_LOCAL_GRAVEL, NTESurfaceStates.GRAVEL, true);
    public static final SurfaceBuilderFactory ROCKY_VOLCANIC_SOIL = VOLCANIC_SOIL;
    public static final SurfaceBuilderFactory OCEAN_MUD = seed -> new SimpleSurfaceBuilder(NTESurfaceStates.OCEAN_MUD, NTESurfaceStates.OCEAN_MUD, NTESurfaceStates.OCEAN_MUD, false);

    private final SurfaceState top;
    private final SurfaceState mid;
    private final SurfaceState water;
    private final boolean rockySurfaceBuilder;

    public SimpleSurfaceBuilder(SurfaceState top, SurfaceState mid, SurfaceState water, boolean rockySurfaceBuilder)
    {
        this.top = top;
        this.mid = mid;
        this.water = water;
        this.rockySurfaceBuilder = rockySurfaceBuilder;
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        if (rockySurfaceBuilder)
        {
            NormalSurfaceBuilder.ROCKY.buildSurface(context, startY, endY, top, mid, mid, water, water);
        }
        else
        {
            NormalSurfaceBuilder.INSTANCE.buildSurface(context, startY, endY, top, mid, mid, water, water);
        }
    }
}
