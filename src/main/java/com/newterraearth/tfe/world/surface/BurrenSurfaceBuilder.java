package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

public class BurrenSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory INSTANCE = BurrenSurfaceBuilder::new;

    private final Noise2D crevices;

    public BurrenSurfaceBuilder(long seed)
    {
        this.crevices = new OpenSimplex2D(seed + 398767567L)
            .octaves(2)
            .spread(0.08f)
            .abs();
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        if (crevices.noise(context.pos().getX(), context.pos().getZ()) + 0.3 * context.weight() <= 0.40)
        {
            NormalSurfaceBuilder.ROCKY.buildSurface(context, startY, endY, NTESurfaceStates.TOP_GRASS_TO_GRAVEL, NTESurfaceStates.RAW, NTESurfaceStates.RAW);
        }
        else
        {
            NormalSurfaceBuilder.ROCKY.buildSurface(context, startY, endY, NTESurfaceStates.SNOWY_RAW, NTESurfaceStates.RAW, NTESurfaceStates.RAW);
        }
    }
}
