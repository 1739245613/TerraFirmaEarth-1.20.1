package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

import com.newterraearth.tfe.world.NTESurfaceContext;

public enum MudFlatsSurfaceBuilder implements SurfaceBuilderFactory.Invariant
{
    INSTANCE;

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final NTESurfaceContext.Context surfaceContext = NTESurfaceContext.current();
        final float baseGroundwater = surfaceContext != null
            ? surfaceContext.baseGroundwater(context.pos())
            : Float.NEGATIVE_INFINITY;

        if (startY < 66 && baseGroundwater < 25f)
        {
            NormalSurfaceBuilder.INSTANCE.buildSurface(
                context,
                startY,
                endY,
                NTESurfaceStates.HARDENED_CLAY,
                NTESurfaceStates.HARDENED_CLAY,
                NTESurfaceStates.HARDENED_CLAY,
                NTESurfaceStates.MUD,
                NTESurfaceStates.MUD
            );
        }
        else
        {
            NormalSurfaceBuilder.INSTANCE.buildSurface(
                context,
                startY,
                endY,
                NTESurfaceStates.TOP_GRASS_TO_SAND,
                NTESurfaceStates.MID_DIRT_TO_SAND,
                NTESurfaceStates.UNDER_GRAVEL
            );
        }
    }
}
