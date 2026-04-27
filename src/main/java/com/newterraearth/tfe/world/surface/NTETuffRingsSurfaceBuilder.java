package com.newterraearth.tfe.world.surface;

import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

import com.newterraearth.tfe.world.NTEBiomeExtensionAccess;
import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.NTESurfaceContext;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureBlendType;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoise;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoiseSampler;

public class NTETuffRingsSurfaceBuilder implements SurfaceBuilder
{
    public static SurfaceBuilderFactory create(SurfaceBuilderFactory parent)
    {
        return seed -> new NTETuffRingsSurfaceBuilder(parent.apply(seed), seed);
    }

    private final SurfaceBuilder parent;
    private final NTECenteredFeatureNoiseSampler sampler;

    public NTETuffRingsSurfaceBuilder(SurfaceBuilder parent, long seed)
    {
        this.parent = parent;
        this.sampler = NTECenteredFeatureNoise.tuffRing(NTESeed.of(seed));
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final NTESurfaceContext.Context surfaceContext = NTESurfaceContext.current();
        final BiomeExtension biome = surfaceContext != null ? surfaceContext.tuffRingBiome() : null;
        if (biome != null && ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureBlendType() == NTECenteredFeatureBlendType.TUFF_RING)
        {
            final float easing = sampler.calculateEasing(context.pos(), biome);
            if (easing > 0.6f)
            {
                if (startY < context.getSeaLevel() + 3)
                {
                    buildTuffSurface(context, startY, endY, NTESurfaceStates.VOLCANIC_SHORE_SAND, NTESurfaceStates.VOLCANIC_SHORE_SAND, NTESurfaceStates.TUFF, NTESurfaceStates.TUFF_GRAVEL);
                }
                else
                {
                    buildTuffSurface(context, startY, endY, NTESurfaceStates.VOLCANIC_TOP_GRASS_TO_LOCAL_GRAVEL, NTESurfaceStates.VOLCANIC_MID_DIRT_TO_LOCAL_GRAVEL, NTESurfaceStates.TUFF, NTESurfaceStates.TUFF_GRAVEL);
                }
                return;
            }
        }
        parent.buildSurface(context, startY, endY);
    }

    private void buildTuffSurface(SurfaceBuilderContext context, int startY, int endY, SurfaceState topState, SurfaceState midState, SurfaceState underState, SurfaceState underWaterState)
    {
        int surfaceDepth = -1;
        int surfaceY = 0;
        boolean underwaterLayer = false;
        boolean firstLayer = false;
        SurfaceState surfaceState = underState;
        int tuffDepth = (int) (20 * context.weight());

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
                        surfaceDepth = context.calculateAltitudeSlopeSurfaceDepth(surfaceY, 5, -1);
                        if (surfaceDepth < -1)
                        {
                            surfaceDepth = 0;
                            context.setBlockState(y, NTESurfaceStates.TUFF);
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
                        surfaceDepth = context.calculateAltitudeSlopeSurfaceDepth(surfaceY, 5, -3);
                        if (surfaceDepth < -1)
                        {
                            context.setBlockState(y, NTESurfaceStates.TUFF);
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
                        surfaceDepth = context.calculateAltitudeSlopeSurfaceDepth(surfaceY, 5, 0);
                        if (!underwaterLayer)
                        {
                            surfaceState = underState;
                        }
                    }
                }
                else if (tuffDepth > 0)
                {
                    context.setBlockState(y, NTESurfaceStates.TUFF);
                    tuffDepth--;
                }
            }
        }
    }
}
