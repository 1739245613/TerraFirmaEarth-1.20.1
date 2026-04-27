package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

import com.newterraearth.tfe.world.NTESurfaceContext;

public final class RockyPlateauSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory INSTANCE = RockyPlateauSurfaceBuilder::new;

    private final Noise2D plateauTopNoise;

    public RockyPlateauSurfaceBuilder(long seed)
    {
        this.plateauTopNoise = BiomeNoise.hills(seed, 22, 32);
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final NTESurfaceContext.Context surfaceContext = NTESurfaceContext.current();
        final float baseGroundwater = surfaceContext != null
            ? surfaceContext.baseGroundwater(context.pos())
            : Float.NEGATIVE_INFINITY;

        if (context.weight() > 0.9d && startY < 86 && baseGroundwater == 0f)
        {
            NormalSurfaceBuilder.ROCKY.buildSurface(context, startY, endY, NTESurfaceStates.HALITE, NTESurfaceStates.HARDENED_CLAY, NTESurfaceStates.RAW);
        }
        else if (startY - 2 > plateauTopNoise.noise(context.pos().getX(), context.pos().getZ()))
        {
            NormalSurfaceBuilder.ROCKY.buildSurface(context, startY, endY, NTESurfaceStates.RAW, NTESurfaceStates.RAW, NTESurfaceStates.RAW);
        }
        else
        {
            NormalSurfaceBuilder.ROCKY.buildSurface(context, startY, endY, NTESurfaceStates.TOP_GRASS_TO_SAND, NTESurfaceStates.MID_DIRT_TO_SAND, NTESurfaceStates.RAW);
        }
    }
}
