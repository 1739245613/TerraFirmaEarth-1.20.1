package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

import com.newterraearth.tfe.world.NTEBiomeNoise;

public class PatternedGroundSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory INSTANCE = PatternedGroundSurfaceBuilder::new;

    private final NormalSurfaceBuilder surfaceBuilder;
    private final Noise2D edgeNoise;

    public PatternedGroundSurfaceBuilder(long seed)
    {
        this.surfaceBuilder = NormalSurfaceBuilder.INSTANCE;
        this.edgeNoise = NTEBiomeNoise.patternedGround(seed);
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        if (edgeNoise.noise(context.pos().getX(), context.pos().getZ()) * context.weight() >= -0.60)
        {
            surfaceBuilder.buildSurface(context, startY, endY);
        }
        else
        {
            surfaceBuilder.buildSurface(
                context,
                startY,
                endY,
                NTESurfaceStates.MUD,
                NTESurfaceStates.MUD,
                NTESurfaceStates.GRAVEL,
                NTESurfaceStates.MUD,
                NTESurfaceStates.MUD
            );
        }
    }
}
