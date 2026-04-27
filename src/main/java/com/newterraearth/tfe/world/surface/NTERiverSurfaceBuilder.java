package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.surface.SurfaceStates;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

public class NTERiverSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory INSTANCE = NTERiverSurfaceBuilder::new;

    private final long seed;

    protected NTERiverSurfaceBuilder(long seed)
    {
        this.seed = seed;
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final BiomeExtension biome = context.originalBiome();
        if (biome.isShore())
        {
            biome.createSurfaceBuilder(seed).buildSurface(context, startY, endY);
        }
        else if (!biome.hasSandyRiverShores())
        {
            NormalSurfaceBuilder.INSTANCE.buildSurface(context, startY, endY);
        }
        else
        {
            SurfaceState state = SurfaceStates.GRAVEL;
            if (context.getSlope() < 2)
            {
                state = NTESurfaceStates.TOP_GRASS_TO_GRAVEL;
            }
            else if (context.getSlope() < 5)
            {
                state = SurfaceStates.RIVER_SAND;
            }
            NormalSurfaceBuilder.INSTANCE.buildSurface(context, startY, endY, state, SurfaceStates.GRAVEL, SurfaceStates.GRAVEL);
        }
    }
}
