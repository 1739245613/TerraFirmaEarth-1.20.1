package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

/**
 * Legacy compatibility alias kept for older addon-side references.
 * Volcanic mountain biomes now have full 1.21-style andisol surface states,
 * so this delegates to the same rocky volcanic soil builder used by 1.21.
 */
public enum VolcanicRockySurfaceBuilder implements SurfaceBuilderFactory.Invariant
{
    INSTANCE;

    private static final SimpleSurfaceBuilder EXACT = new SimpleSurfaceBuilder(
        NTESurfaceStates.VOLCANIC_TOP_GRASS_TO_LOCAL_GRAVEL,
        NTESurfaceStates.VOLCANIC_MID_DIRT_TO_LOCAL_GRAVEL,
        NTESurfaceStates.GRAVEL,
        true
    );

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        EXACT.buildSurface(context, startY, endY);
    }
}
