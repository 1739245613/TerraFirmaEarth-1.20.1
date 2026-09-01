package com.newterraearth.tfe.world.surface;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.util.Helpers;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

import com.newterraearth.tfe.world.NTEBiomeExtensionAccess;
import com.newterraearth.tfe.world.NTESurfaceContext;
import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureBlendType;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoise;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoiseSampler;
import com.newterraearth.tfe.world.noise.NTECellular2D;

/** 4.2.9 atoll reef surface treatment. */
public final class NTEAtollSurfaceBuilder implements SurfaceBuilder
{
    public static SurfaceBuilderFactory create(SurfaceBuilderFactory parent)
    {
        return seed -> new NTEAtollSurfaceBuilder(parent.apply(seed), seed);
    }

    private final SurfaceBuilder parent;
    private final NTECenteredFeatureNoiseSampler sampler;
    private final net.dries007.tfc.world.noise.Noise2D heightNoise;

    private NTEAtollSurfaceBuilder(SurfaceBuilder parent, long seed)
    {
        this.parent = parent;
        this.sampler = NTECenteredFeatureNoise.atoll(NTESeed.of(seed));
        this.heightNoise = new net.dries007.tfc.world.noise.OpenSimplex2D(NTESeed.of(seed).next()).octaves(2).spread(0.1f).scaled(-4, 4);
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final NTESurfaceContext.Context surfaceContext = NTESurfaceContext.current();
        final BiomeExtension biome = surfaceContext.atollBiome();
        if (((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureBlendType() != NTECenteredFeatureBlendType.ATOLL)
        {
            parent.buildSurface(context, startY, endY);
            return;
        }

        final float easing = sampler.calculateEasing(context.pos(), biome);
        if (easing <= 0)
        {
            parent.buildSurface(context, startY, endY);
            return;
        }

        final double randomHeight = heightNoise.noise(context.pos().getX(), context.pos().getZ());
        final int seaLevel = context.getSeaLevel();
        final int preAtollHeight = surfaceContext.preVolcanicHeight(context.pos());
        final int volcanoHeight = (int) Mth.clampedMap(easing, 0.4, 0.7, seaLevel - 70, seaLevel - 8 + randomHeight);
        final int maxDepth = Math.max(preAtollHeight, volcanoHeight);
        final NTECellular2D.Cell cell = sampler.getCellularNoise().cell(context.pos().getX(), context.pos().getZ());
        final SurfaceState grassState, sandState;
        if (NTECenteredFeatureNoise.hashDouble(cell.noise(), 6324) > 0.7)
        {
            grassState = context.rainfall() > 375 ? NTESurfaceStates.ATOLL_GRASS_TO_PINK_SAND : NTESurfaceStates.ATOLL_GRASS_TO_YELLOW_SAND;
            sandState = context.rainfall() > 375 ? NTESurfaceStates.PINK_SAND : NTESurfaceStates.YELLOW_SAND;
        }
        else
        {
            grassState = NTESurfaceStates.ATOLL_GRASS_TO_WHITE_SAND;
            sandState = NTESurfaceStates.WHITE_SAND;
        }
        final SurfaceState rockState = NTECenteredFeatureNoise.hashDouble(cell.noise(), 624) > 0.7 ? NTESurfaceStates.DOLOMITE : NTESurfaceStates.LIMESTONE;
        final int oceanFloorY = context.chunk().getHeight(Heightmap.Types.OCEAN_FLOOR_WG, context.pos().getX(), context.pos().getZ());
        if (oceanFloorY <= maxDepth + 2)
        {
            parent.buildSurface(context, startY, endY);
            return;
        }
        int surfaceDepth = -1;
        for (int y = startY; y >= maxDepth; --y)
        {
            final BlockState state = context.getBlockState(y);
            if (state.isAir()) surfaceDepth = -1;
            else if (y == seaLevel - 1 && NTECenteredFeatureNoise.getAtollIntegrity(cell) >= 1 && easing > 0.58 && state.is(TFCFluids.SALT_WATER.createSourceBlock().getBlock()))
            {
                context.setBlockState(y, Fluids.WATER.getSource().defaultFluidState().createLegacyBlock());
            }
            else if (context.isDefaultBlock(state))
            {
                if (surfaceDepth == -1)
                {
                    surfaceDepth = 0;
                    if (y > seaLevel + 2) context.setBlockState(y, grassState);
                    else if (y > seaLevel - 11 + randomHeight) context.setBlockState(y, sandState);
                    else context.setBlockState(y, rockState);
                }
                else if (surfaceDepth < 5 + randomHeight)
                {
                    if (y > seaLevel - 11 + randomHeight) { context.setBlockState(y, sandState); surfaceDepth++; }
                    else { context.setBlockState(y, rockState); surfaceDepth = 20; }
                }
                else context.setBlockState(y, rockState);
            }
        }
    }
}
