package com.newterraearth.tfe.world.surface;

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

public class NTETuyaSurfaceBuilder implements SurfaceBuilder
{
    public static SurfaceBuilderFactory create(SurfaceBuilderFactory parent)
    {
        return seed -> new NTETuyaSurfaceBuilder(parent.apply(seed), seed);
    }

    private final SurfaceBuilder parent;
    private final NTECenteredFeatureNoiseSampler sampler;
    private final Noise2D heightNoise;

    public NTETuyaSurfaceBuilder(SurfaceBuilder parent, long seed)
    {
        this.parent = parent;

        final NTESeed featureSeed = NTESeed.of(seed);
        this.sampler = NTECenteredFeatureNoise.tuya(featureSeed);
        this.heightNoise = new OpenSimplex2D(featureSeed.next()).octaves(2).spread(0.1f).scaled(-4, 4);
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final NTESurfaceContext.Context surfaceContext = NTESurfaceContext.current();
        final BiomeExtension biome = surfaceContext != null ? surfaceContext.tuyaBiome() : null;
        if (biome != null && ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureBlendType() == NTECenteredFeatureBlendType.TUYA)
        {
            final float easing = sampler.calculateEasing(context.pos(), biome);
            if (1 - easing < 0.16f)
            {
                buildVolcanicSurface(context, startY, endY, biome, (int) heightNoise.noise(context.pos().getX(), context.pos().getZ()));
                return;
            }
        }
        parent.buildSurface(context, startY, endY);
    }

    private void buildVolcanicSurface(SurfaceBuilderContext context, int startY, int endY, BiomeExtension biome, int noise)
    {
        final NTEBiomeExtensionAccess access = (NTEBiomeExtensionAccess) (Object) biome;
        final BlockState basalt = TFCBlocks.ROCK_BLOCKS.get(Rock.BASALT).get(Rock.BlockType.RAW).get().defaultBlockState();

        int surfaceDepth = -1;
        for (int y = startY; y >= endY; --y)
        {
            final BlockState stateAt = context.getBlockState(y);
            if (stateAt.isAir())
            {
                surfaceDepth = -1;
            }
            else if (context.isDefaultBlock(stateAt) && y > access.tfe$getCenteredFeatureRockHeight() + noise)
            {
                if (surfaceDepth == -1)
                {
                    surfaceDepth = 40;
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
