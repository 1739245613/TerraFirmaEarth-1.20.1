package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

import com.newterraearth.tfe.world.NTEBiomeNoise;

public class ShilinSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory INSTANCE = ShilinSurfaceBuilder::new;

    private final Noise2D ridges;

    public ShilinSurfaceBuilder(long seed)
    {
        this.ridges = NTEBiomeNoise.shilinRidges(seed);
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final double val = ridges.noise(context.pos().getX(), context.pos().getZ());
        if (val > 0.18)
        {
            NormalSurfaceBuilder.ROCKY.buildSurface(context, startY, endY, NTESurfaceStates.SNOWY_RAW, NTESurfaceStates.RAW, NTESurfaceStates.RAW);
        }
        else if (val > 0.09)
        {
            NormalSurfaceBuilder.ROCKY.buildSurface(context, startY, endY, NTESurfaceStates.SNOWY_GRAVEL, NTESurfaceStates.GRAVEL, NTESurfaceStates.GRAVEL);
        }
        else
        {
            NormalSurfaceBuilder.ROCKY.buildSurface(context, startY, endY);
        }
    }
}
