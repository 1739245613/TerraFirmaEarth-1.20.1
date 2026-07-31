package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.surface.SurfaceStates;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import com.newterraearth.tfe.common.NTEBlocks;
import com.newterraearth.tfe.world.NTESurfaceContext;
import com.newterraearth.tfe.world.river.NTERiverHydrology;
import com.newterraearth.tfe.world.soil.NTESoil;
import com.newterraearth.tfe.world.soil.NTESoilBlockType;

public class NTERiverSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory INSTANCE = NTERiverSurfaceBuilder::new;

    private final long seed;

    protected NTERiverSurfaceBuilder(long seed)
    {
        this.seed = seed;
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final NTESurfaceContext.Context surfaceContext = NTESurfaceContext.current();
        final NTERiverHydrology.ColumnProfile riverProfile = surfaceContext == null
            ? null
            : surfaceContext.riverProfile(context.pos());
        if (riverProfile != null
            && riverProfile.surfaceVisible()
            && riverProfile.inWaterCore()
            && riverProfile.bedBlockY() < riverProfile.waterBlockY())
        {
            context.originalBiome().createSurfaceBuilder(seed).buildSurface(context, startY, endY);
            demoteSubmergedOrganicSurface(context, riverProfile, startY, endY);
            return;
        }

        final BiomeExtension biome = context.originalBiome();
        if (biome.isShore())
        {
            biome.createSurfaceBuilder(seed).buildSurface(context, startY, endY);
        }
        else if (!biome.hasSandyRiverShores())
        {
            NormalSurfaceBuilder.INSTANCE.buildSurface(context, startY, endY);
        }
        else
        {
            SurfaceState state = SurfaceStates.GRAVEL;
            if (context.getSlope() < 2)
            {
                state = NTESurfaceStates.TOP_GRASS_TO_GRAVEL;
            }
            else if (context.getSlope() < 5)
            {
                state = SurfaceStates.RIVER_SAND;
            }
            NormalSurfaceBuilder.INSTANCE.buildSurface(context, startY, endY, state, SurfaceStates.GRAVEL, SurfaceStates.GRAVEL);
        }
    }

    /**
     * The original biome owns the river-bed material. At raised water levels
     * its surface builder can still regard the bed as dry because TFC compares
     * against the global sea level. Only remove living/organic top blocks here;
     * sand, gravel, mud, rock and the biome's normal soil layering are retained.
     */
    private static void demoteSubmergedOrganicSurface(
        SurfaceBuilderContext context,
        NTERiverHydrology.ColumnProfile profile,
        int startY,
        int endY
    )
    {
        final int topY = Math.min(startY, profile.waterBlockY() - 1);
        final int bottomY = Math.max(endY, profile.bedBlockY() - 3);
        for (int y = topY; y >= bottomY; y--)
        {
            final BlockState replacement = nonOrganicSoil(context.getBlockState(y));
            if (replacement != null)
            {
                context.setBlockState(y, replacement);
            }
        }
    }

    @Nullable
    private static BlockState nonOrganicSoil(BlockState state)
    {
        final Block block = state.getBlock();
        for (NTESoil soil : NTESoil.values())
        {
            if (block == NTEBlocks.getBlock(soil, NTESoilBlockType.GRASS).get()
                || block == NTEBlocks.getBlock(soil, NTESoilBlockType.DUFF).get())
            {
                return NTEBlocks.getBlock(soil, NTESoilBlockType.DIRT).get().defaultBlockState();
            }
            if (block == NTEBlocks.getBlock(soil, NTESoilBlockType.CLAY_GRASS).get()
                || block == NTEBlocks.getBlock(soil, NTESoilBlockType.CLAY_DUFF).get())
            {
                return NTEBlocks.getBlock(soil, NTESoilBlockType.CLAY).get().defaultBlockState();
            }
        }
        return null;
    }
}
