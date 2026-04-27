package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

public final class GrassyDunesSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory INSTANCE = GrassyDunesSurfaceBuilder::new;

    private final Noise2D grassHeightVariationNoise;

    public GrassyDunesSurfaceBuilder(long seed)
    {
        grassHeightVariationNoise = new OpenSimplex2D(seed)
            .octaves(2)
            .scaled(SEA_LEVEL_Y + 8, SEA_LEVEL_Y + 14)
            .spread(0.08f);
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final double heightVariation = grassHeightVariationNoise.noise(context.pos().getX(), context.pos().getZ());
        final double trueSlope = context.getSlope();

        context.setSlope(trueSlope * (1 - context.weight()));

        if (startY > heightVariation && trueSlope < 5)
        {
            NormalSurfaceBuilder.INSTANCE.buildSurface(
                context,
                startY,
                endY,
                NTESurfaceStates.TOP_GRASS_TO_SAND,
                NTESurfaceStates.SAND,
                NTESurfaceStates.SAND,
                NTESurfaceStates.SAND,
                NTESurfaceStates.SAND
            );
        }
        else
        {
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
}
