package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.surface.SurfaceStates;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

public enum KarstSurfaceBuilder implements SurfaceBuilderFactory.Invariant
{
    TOWER,
    SHILIN,
    DOLINE,
    CENOTE;

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final boolean exposed = context.getSlope() > 3.8 || startY > context.getSeaLevel() + 30;
        final boolean humid = context.rainfall() > 300f;

        final SurfaceState top;
        final SurfaceState mid;
        final SurfaceState under;

        switch (this)
        {
            case TOWER -> {
                top = exposed ? SurfaceStates.RAW : humid ? SurfaceStates.GRASS : SurfaceStates.DIRT;
                mid = exposed ? SurfaceStates.RAW : SurfaceStates.GRAVEL;
                under = SurfaceStates.RAW;
            }
            case SHILIN -> {
                top = exposed ? SurfaceStates.GRAVEL : SurfaceStates.GRASS;
                mid = exposed ? SurfaceStates.RAW : SurfaceStates.DIRT;
                under = SurfaceStates.RAW;
            }
            case CENOTE -> {
                top = exposed ? SurfaceStates.GRAVEL : SurfaceStates.GRASS;
                mid = SurfaceStates.DIRT;
                under = SurfaceStates.GRAVEL;
            }
            default -> {
                top = exposed ? SurfaceStates.GRAVEL : SurfaceStates.GRASS;
                mid = exposed ? SurfaceStates.RAW : SurfaceStates.DIRT;
                under = SurfaceStates.GRAVEL;
            }
        }

        NormalSurfaceBuilder.ROCKY.buildSurface(
            context,
            startY,
            endY,
            top,
            mid,
            under,
            SurfaceStates.GRAVEL,
            SurfaceStates.GRAVEL
        );
    }
}
