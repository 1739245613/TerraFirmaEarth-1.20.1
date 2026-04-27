package com.newterraearth.tfe.world.surface;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.rock.Rock;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

import com.newterraearth.tfe.world.NTEBiomeExtensionAccess;
import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.NTESurfaceContext;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureBlendType;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoise;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoiseSampler;

public class NTECinderConeSurfaceBuilder implements SurfaceBuilder
{
    public static SurfaceBuilderFactory create(SurfaceBuilderFactory parent)
    {
        return seed -> new NTECinderConeSurfaceBuilder(parent.apply(seed), seed);
    }

    private final SurfaceBuilder parent;
    private final NTECenteredFeatureNoiseSampler sampler;
    private final Noise2D heightNoise;

    public NTECinderConeSurfaceBuilder(SurfaceBuilder parent, long seed)
    {
        this.parent = parent;

        final NTESeed featureSeed = NTESeed.of(seed);
        this.sampler = NTECenteredFeatureNoise.cinder(featureSeed);
        this.heightNoise = new OpenSimplex2D(featureSeed.next()).octaves(2).spread(0.1f).scaled(-4, 4);
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final NTESurfaceContext.Context surfaceContext = NTESurfaceContext.current();
        final BiomeExtension biome = surfaceContext != null ? surfaceContext.cinderConeBiome() : null;
        if (biome != null && ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureBlendType() == NTECenteredFeatureBlendType.CINDER_CONE)
        {
            final NTEBiomeExtensionAccess access = (NTEBiomeExtensionAccess) (Object) biome;
            final float easing = sampler.calculateEasing(context.pos(), biome);
            if (easing > 0.6f && startY > access.tfe$getCenteredFeatureRockHeight() + heightNoise.noise(context.pos().getX(), context.pos().getZ()))
            {
                buildVolcanicSurface(context, startY, endY, easing);
                return;
            }
        }
        parent.buildSurface(context, startY, endY);
    }

    private void buildVolcanicSurface(SurfaceBuilderContext context, int startY, int endY, float easing)
    {
        final BlockState basalt = TFCBlocks.ROCK_BLOCKS.get(Rock.BASALT).get(Rock.BlockType.RAW).get().defaultBlockState();

        int surfaceDepth = -1;
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
                    surfaceDepth = context.calculateAltitudeSlopeSurfaceDepth(y, 5, 4);
                    surfaceDepth = Mth.clamp((int) (surfaceDepth * (easing - 0.6f) / 0.4f), 2, 11);
                    context.setBlockState(y, basalt);
                }
                else if (surfaceDepth > 0)
                {
                    surfaceDepth--;
                    context.setBlockState(y, basalt);
                }
            }
        }
    }
}
