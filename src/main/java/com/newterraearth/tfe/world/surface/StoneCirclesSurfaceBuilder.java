package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

import com.newterraearth.tfe.world.NTEBiomeNoise;

public class StoneCirclesSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory INSTANCE = StoneCirclesSurfaceBuilder::new;

    private final NormalSurfaceBuilder surfaceBuilder;
    private final Noise2D edgeNoise;

    public StoneCirclesSurfaceBuilder(long seed)
    {
        this.surfaceBuilder = NormalSurfaceBuilder.ROCKY;
        this.edgeNoise = NTEBiomeNoise.stoneCircles(seed);
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        if (edgeNoise.noise(context.pos().getX(), context.pos().getZ()) * context.weight() <= 0.60)
        {
            surfaceBuilder.buildSurface(
                context,
                startY,
                endY,
                NTESurfaceStates.SNOWY_SAND_AND_GRAVEL,
                NTESurfaceStates.SAND_AND_GRAVEL,
                NTESurfaceStates.GRAVEL
            );
        }
        else
        {
            surfaceBuilder.buildSurface(
                context,
                startY,
                endY,
                NTESurfaceStates.SNOWY_COBBLE,
                NTESurfaceStates.MORAINE,
                NTESurfaceStates.GRAVEL
            );
        }
    }
}
