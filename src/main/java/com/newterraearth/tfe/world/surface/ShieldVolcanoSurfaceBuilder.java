package com.newterraearth.tfe.world.surface;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

public class ShieldVolcanoSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory ACTIVE = seed -> new ShieldVolcanoSurfaceBuilder(seed, true, false);
    public static final SurfaceBuilderFactory DORMANT = seed -> new ShieldVolcanoSurfaceBuilder(seed, false, false);
    public static final SurfaceBuilderFactory SHORE = seed -> new ShieldVolcanoSurfaceBuilder(seed, false, true);

    private final boolean hasLavaFlows;
    private final boolean isSandy;
    private final Noise2D lavaFlowNoise;
    private final Noise2D lavaFlowMaterialNoise;

    private ShieldVolcanoSurfaceBuilder(long seed, boolean hasLavaFlows, boolean isSandy)
    {
        this.hasLavaFlows = hasLavaFlows;
        this.isSandy = isSandy;
        this.lavaFlowNoise = lavaFlow(seed);
        this.lavaFlowMaterialNoise = lavaFlowMaterial(seed);
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final int x = context.pos().getX();
        final int z = context.pos().getZ();

        final SurfaceState top;
        final SurfaceState mid;
        final SurfaceState under;
        final SurfaceState underwater;

        if (isSandy)
        {
            top = NTESurfaceStates.VOLCANIC_SHORE_SAND;
            mid = NTESurfaceStates.VOLCANIC_SHORE_SAND;
            under = NTESurfaceStates.VOLCANIC_SHORE_SANDSTONE;
            underwater = NTESurfaceStates.VOLCANIC_SHORE_SAND;
        }
        else
        {
            top = NTESurfaceStates.VOLCANIC_TOP_GRASS_TO_GRAVEL;
            mid = NTESurfaceStates.VOLCANIC_MID_DIRT_TO_GRAVEL;
            under = NTESurfaceStates.BASALT_GRAVEL;
            underwater = NTESurfaceStates.BASALT_GRAVEL;
        }

        if (!hasLavaFlows)
        {
            buildSurface(context, startY, endY, top, mid, under, underwater);
            return;
        }

        final double noiseValue = lavaFlowMaterialNoise.noise(x, z);
        final double flowValue = lavaFlowNoise.noise(x, z);

        if (flowValue < 0.40)
        {
            buildSurface(context, startY, endY, top, mid, under, underwater);
        }
        else if (flowValue < 0.50)
        {
            if (noiseValue > 0)
            {
                buildSurface(context, startY, endY, NTESurfaceStates.SNOWY_BASALT_GRAVEL, NTESurfaceStates.BASALT_GRAVEL, NTESurfaceStates.BASALT, NTESurfaceStates.BASALT_GRAVEL);
            }
            else
            {
                buildSurface(context, startY, endY, top, mid, under, underwater);
            }
        }
        else if (flowValue < 0.75)
        {
            if (noiseValue > 0)
            {
                buildSurface(context, startY, endY, NTESurfaceStates.SNOWY_BASALT_GRAVEL, NTESurfaceStates.BASALT_GRAVEL, NTESurfaceStates.BASALT, NTESurfaceStates.BASALT_GRAVEL);
            }
            else
            {
                buildSurface(context, startY, endY, NTESurfaceStates.SNOWY_BASALT_COBBLE, NTESurfaceStates.BASALT_COBBLE, NTESurfaceStates.BASALT, NTESurfaceStates.BASALT_COBBLE);
            }
        }
        else if (noiseValue > -0.6)
        {
            buildSurface(context, startY, endY, NTESurfaceStates.SNOWY_BASALT, NTESurfaceStates.BASALT, NTESurfaceStates.BASALT, NTESurfaceStates.BASALT_COBBLE);
        }
        else
        {
            buildSurface(context, startY, endY, NTESurfaceStates.SNOWY_BASALT_COBBLE, NTESurfaceStates.BASALT_COBBLE, NTESurfaceStates.BASALT, NTESurfaceStates.BASALT_COBBLE);
        }
    }

    private void buildSurface(SurfaceBuilderContext context, int startY, int endY, SurfaceState topState, SurfaceState midState, SurfaceState underState, SurfaceState underWaterState)
    {
        int surfaceDepth = -1;
        int surfaceY = 0;
        boolean underwaterLayer = false;
        boolean firstLayer = false;
        SurfaceState surfaceState = NTESurfaceStates.BASALT;

        int basaltDepth = (int) (20 * context.weight());

        for (int y = startY; y >= endY; --y)
        {
            final BlockState stateAt = context.getBlockState(y);
            if (stateAt.isAir())
            {
                surfaceDepth = -1;
            }
            else if (context.isDefaultBlock(stateAt))
            {
                if (surfaceDepth == -1)
                {
                    surfaceY = y;
                    firstLayer = true;
                    if (y < context.getSeaLevel() - 1)
                    {
                        surfaceDepth = calculateAltitudeSlopeSurfaceDepth(context, surfaceY, -1);
                        if (surfaceDepth < -1)
                        {
                            surfaceDepth = 0;
                            context.setBlockState(y, NTESurfaceStates.BASALT);
                        }
                        else if (surfaceDepth == -1)
                        {
                            surfaceDepth = 0;
                            context.setBlockState(y, underWaterState);
                        }
                        else
                        {
                            context.setBlockState(y, underWaterState);
                        }
                        surfaceState = underWaterState;
                        underwaterLayer = true;
                    }
                    else
                    {
                        surfaceDepth = calculateAltitudeSlopeSurfaceDepth(context, surfaceY, -3);
                        if (surfaceDepth < -1)
                        {
                            context.setBlockState(y, NTESurfaceStates.BASALT);
                            surfaceDepth = 0;
                        }
                        else if (surfaceDepth == -1)
                        {
                            surfaceDepth = 0;
                            context.setBlockState(y, underState);
                        }
                        else
                        {
                            context.setBlockState(y, topState);
                        }
                        surfaceState = midState;
                        underwaterLayer = false;
                    }
                }
                else if (surfaceDepth > 0)
                {
                    surfaceDepth--;
                    context.setBlockState(y, surfaceState);
                    if (surfaceDepth == 0 && firstLayer)
                    {
                        firstLayer = false;
                        surfaceDepth = calculateAltitudeSlopeSurfaceDepth(context, surfaceY, 0);
                        if (underwaterLayer)
                        {
                            surfaceState = underState;
                        }
                    }
                }
                else if (basaltDepth > 0)
                {
                    context.setBlockState(y, NTESurfaceStates.BASALT);
                    basaltDepth--;
                }
            }
        }
    }

    private static int calculateAltitudeSlopeSurfaceDepth(SurfaceBuilderContext context, int y, int minimumReturnValue)
    {
        return calculateAltitudeSlopeSurfaceDepth(context, y, minimumReturnValue, 5);
    }

    private static int calculateAltitudeSlopeSurfaceDepth(SurfaceBuilderContext context, int y, int minimumReturnValue, int maxDepth)
    {
        final double slopeFactor = 1 - Mth.clamp(context.getSlope() / 15d, 0, 1);
        final double seaLevelFactor = y < context.getSeaLevel()
            ? Mth.clampedMap((context.getSeaLevel() - y) / 15d, 0, 0.4, 1, 1.4)
            : 1;
        final int maxElevationDepth = y < context.getSeaLevel() + 7
            ? maxDepth
            : (int) Mth.clampedMap(y, context.getSeaLevel() + 7, context.getSeaLevel() + 67, maxDepth, 2);

        return Mth.clamp((int) Mth.lerp(slopeFactor * seaLevelFactor, minimumReturnValue, maxElevationDepth), minimumReturnValue, maxElevationDepth);
    }

    private static Noise2D lavaFlow(long seed)
    {
        return new OpenSimplex2D(seed + 23891L).ridged().spread(0.01);
    }

    private static Noise2D lavaFlowMaterial(long seed)
    {
        return new OpenSimplex2D(seed).octaves(2).spread(0.25);
    }
}
