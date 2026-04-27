package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceStates;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

public enum CoastalVolcanicSurfaceBuilder implements SurfaceBuilderFactory.Invariant
{
    INSTANCE;

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        if (startY <= context.getSeaLevel() + 2)
        {
            NormalSurfaceBuilder.ROCKY.buildSurface(
                context,
                startY,
                endY,
                SurfaceStates.GRAVEL,
                SurfaceStates.RAW,
                SurfaceStates.RAW,
                SurfaceStates.GRAVEL,
                SurfaceStates.GRAVEL
            );
        }
        else
        {
            VolcanicRockySurfaceBuilder.INSTANCE.buildSurface(context, startY, endY);
        }
    }
}
