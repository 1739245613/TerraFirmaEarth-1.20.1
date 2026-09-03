/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;

/**
 * 1.21 TFC's alternate normal surface profile used by river valleys.
 *
 * The sandy profile keeps a climate-sensitive grass top and dirt sub-layer;
 * only the dry-side transition becomes sand. It is intentionally different
 * from the shoreline sand builder, which is reserved for beaches and tidal
 * terrain.
 */
public final class NormalAlternateSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory SANDY = seed -> new NormalAlternateSurfaceBuilder(false);
    public static final SurfaceBuilderFactory SANDY_ROCKY = seed -> new NormalAlternateSurfaceBuilder(true);

    private final boolean rocky;

    private NormalAlternateSurfaceBuilder(boolean rocky)
    {
        this.rocky = rocky;
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final NormalSurfaceBuilder builder = rocky ? NormalSurfaceBuilder.ROCKY : NormalSurfaceBuilder.INSTANCE;
        builder.buildSurface(
            context,
            startY,
            endY,
            NTESurfaceStates.TOP_GRASS_TO_SAND,
            NTESurfaceStates.MID_DIRT_TO_SAND,
            NTESurfaceStates.UNDER_GRAVEL
        );
    }
}
