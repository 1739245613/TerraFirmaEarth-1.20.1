package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

public enum DuneSurfaceBuilder implements SurfaceBuilderFactory.Invariant
{
    INSTANCE;

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        context.setSlope(context.getSlope() * (1 - context.weight()));
        NormalSurfaceBuilder.INSTANCE.buildSurface(
            context,
            startY,
            endY,
            NTESurfaceStates.SNOWY_SAND,
            NTESurfaceStates.SAND,
            NTESurfaceStates.SAND,
            NTESurfaceStates.SAND,
            NTESurfaceStates.SAND
        );
    }
}
