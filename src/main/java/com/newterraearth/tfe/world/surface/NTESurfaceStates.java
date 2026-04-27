package com.newterraearth.tfe.world.surface;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blocks.SandstoneBlockType;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.rock.Rock;
import net.dries007.tfc.common.blocks.soil.SandBlockType;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;

import com.newterraearth.tfe.common.NTEBlocks;
import com.newterraearth.tfe.common.NTERock;
import com.newterraearth.tfe.world.soil.NTESoil;
import com.newterraearth.tfe.world.soil.NTESoilBlockType;

public final class NTESurfaceStates
{
    private static final Noise2D SAND_VARIANT_NOISE = new OpenSimplex2D(36263276L).octaves(5).spread(0.0003f).abs();
    private static final Noise2D BEACH_MIX_NOISE = new OpenSimplex2D(124154L).octaves(3).spread(0.00002f);

    public static final SurfaceState RAW = context -> context.getRock().raw().defaultBlockState();
    public static final SurfaceState COBBLE = context -> context.getRock().cobble().defaultBlockState();
    public static final SurfaceState GRAVEL = context -> context.getRock().gravel().defaultBlockState();
    public static final SurfaceState SAND = context -> context.getRock().sand().defaultBlockState();
    public static final SurfaceState SANDSTONE = context -> context.getRock().sandstone().defaultBlockState();
    public static final SurfaceState BASALT = context -> TFCBlocks.ROCK_BLOCKS.get(Rock.BASALT).get(Rock.BlockType.RAW).get().defaultBlockState();
    public static final SurfaceState BASALT_COBBLE = context -> TFCBlocks.ROCK_BLOCKS.get(Rock.BASALT).get(Rock.BlockType.COBBLE).get().defaultBlockState();
    public static final SurfaceState BASALT_GRAVEL = context -> TFCBlocks.ROCK_BLOCKS.get(Rock.BASALT).get(Rock.BlockType.GRAVEL).get().defaultBlockState();
    public static final SurfaceState TUFF = context -> NTERock.TUFF.getBlock(Rock.BlockType.RAW).get().defaultBlockState();
    public static final SurfaceState TUFF_GRAVEL = context -> NTERock.TUFF.getBlock(Rock.BlockType.GRAVEL).get().defaultBlockState();
    public static final SurfaceState BASALT_MORAINE = context -> (Helpers.hash(729375982L, context.pos()) & 127) > 96 ?
        BASALT_COBBLE.getState(context) :
        BASALT_GRAVEL.getState(context);
    public static final SurfaceState MORAINE = context -> (Helpers.hash(729375982L, context.pos()) & 127) > 96 ?
        COBBLE.getState(context) :
        GRAVEL.getState(context);

    public static final SurfaceState SAND_AND_GRAVEL = context -> (Helpers.hash(728275914L, context.pos()) & 127) > 48 ?
        context.getRock().sand().defaultBlockState() :
        context.getRock().gravel().defaultBlockState();

    public static final SurfaceState SNOW = context -> Blocks.SNOW_BLOCK.defaultBlockState();
    public static final SurfaceState SNOW_BLOCK = SNOW;
    public static final SurfaceState ICE = context -> Blocks.ICE.defaultBlockState();
    public static final SurfaceState PACKED_ICE = context -> Blocks.PACKED_ICE.defaultBlockState();
    public static final SurfaceState BLUE_ICE = context -> Blocks.BLUE_ICE.defaultBlockState();

    public static final SurfaceState COARSE_ARIDISOL_BASE = NTESoilSurfaceState.soil(NTESoilBlockType.COARSE_DIRT, NTESoil.ARIDISOL);
    public static final SurfaceState COARSE_ANDISOL_BASE = NTESoilSurfaceState.soil(NTESoilBlockType.COARSE_DIRT, NTESoil.ANDISOL);
    public static final SurfaceState OCEAN_MUD = NTESoilSurfaceState.soil(NTESoilBlockType.MUD, NTESoil.FLUVISOL);
    public static final SurfaceState HARDENED_CLAY = block(NTEBlocks.HARDENED_CLAY);
    public static final SurfaceState HALITE = block(NTEBlocks.HALITE);

    public static final SurfaceState RIVER_SAND = context -> context.getSeaLevelRock().sand().defaultBlockState();
    public static final SurfaceState YELLOW_SAND = block(TFCBlocks.SAND.get(SandBlockType.YELLOW));
    public static final SurfaceState YELLOW_SANDSTONE = block(TFCBlocks.SANDSTONE.get(SandBlockType.YELLOW).get(SandstoneBlockType.RAW));
    public static final SurfaceState RED_SAND = block(TFCBlocks.SAND.get(SandBlockType.RED));
    public static final SurfaceState RED_SANDSTONE = block(TFCBlocks.SANDSTONE.get(SandBlockType.RED).get(SandstoneBlockType.RAW));
    public static final SurfaceState BROWN_SAND = block(TFCBlocks.SAND.get(SandBlockType.BROWN));
    public static final SurfaceState BROWN_SANDSTONE = block(TFCBlocks.SANDSTONE.get(SandBlockType.BROWN).get(SandstoneBlockType.RAW));
    public static final SurfaceState WHITE_SAND = block(TFCBlocks.SAND.get(SandBlockType.WHITE));
    public static final SurfaceState WHITE_SANDSTONE = block(TFCBlocks.SANDSTONE.get(SandBlockType.WHITE).get(SandstoneBlockType.RAW));
    public static final SurfaceState BLACK_SAND = block(TFCBlocks.SAND.get(SandBlockType.BLACK));
    public static final SurfaceState BLACK_SANDSTONE = block(TFCBlocks.SANDSTONE.get(SandBlockType.BLACK).get(SandstoneBlockType.RAW));
    public static final SurfaceState GREEN_SAND = block(TFCBlocks.SAND.get(SandBlockType.GREEN));
    public static final SurfaceState GREEN_SANDSTONE = block(TFCBlocks.SANDSTONE.get(SandBlockType.GREEN).get(SandstoneBlockType.RAW));
    public static final SurfaceState PINK_SAND = block(TFCBlocks.SAND.get(SandBlockType.PINK));
    public static final SurfaceState PINK_SANDSTONE = block(TFCBlocks.SANDSTONE.get(SandBlockType.PINK).get(SandstoneBlockType.RAW));

    public static final SurfaceState RARE_SHORE_SAND = new SurfaceState()
    {
        @Override
        public BlockState getState(SurfaceBuilderContext context)
        {
            final float rainfall = context.rainfall();
            final float temperature = context.averageTemperature();
            if (rainfall > 300f && temperature > 15f)
            {
                return PINK_SAND.getState(context);
            }
            if (rainfall > 360f && BEACH_MIX_NOISE.noise(context.pos().getX(), context.pos().getZ()) > 0.2f)
            {
                return GREEN_SAND.getState(context);
            }
            if (rainfall > 300f)
            {
                return BLACK_SAND.getState(context);
            }
            return WHITE_SAND.getState(context);
        }
    };

    public static final SurfaceState SHORE_SAND = new SurfaceState()
    {
        @Override
        public BlockState getState(SurfaceBuilderContext context)
        {
            final float variantNoiseValue = (float) SAND_VARIANT_NOISE.noise(context.pos().getX(), context.pos().getZ());
            if (variantNoiseValue > 0.55f) return RARE_SHORE_SAND.getState(context);
            if (variantNoiseValue > 0.2f) return YELLOW_SAND.getState(context);
            if (variantNoiseValue > 0.1f) return BROWN_SAND.getState(context);
            return RED_SAND.getState(context);
        }
    };

    public static final SurfaceState RARE_SHORE_SANDSTONE = new SurfaceState()
    {
        @Override
        public BlockState getState(SurfaceBuilderContext context)
        {
            final float rainfall = context.rainfall();
            final float temperature = context.averageTemperature();
            if (rainfall > 300f && temperature > 15f)
            {
                return PINK_SANDSTONE.getState(context);
            }
            if (rainfall > 360f && BEACH_MIX_NOISE.noise(context.pos().getX(), context.pos().getZ()) > 0.2f)
            {
                return GREEN_SANDSTONE.getState(context);
            }
            if (rainfall > 300f)
            {
                return BLACK_SANDSTONE.getState(context);
            }
            return WHITE_SANDSTONE.getState(context);
        }
    };

    public static final SurfaceState VOLCANIC_SHORE_SAND = new SurfaceState()
    {
        @Override
        public BlockState getState(SurfaceBuilderContext context)
        {
            return context.rainfall() > 360f ? GREEN_SAND.getState(context) : BLACK_SAND.getState(context);
        }
    };

    public static final SurfaceState VOLCANIC_SHORE_SANDSTONE = new SurfaceState()
    {
        @Override
        public BlockState getState(SurfaceBuilderContext context)
        {
            return context.rainfall() > 360f ? GREEN_SANDSTONE.getState(context) : BLACK_SANDSTONE.getState(context);
        }
    };

    public static final SurfaceState SHORE_SANDSTONE = new SurfaceState()
    {
        @Override
        public BlockState getState(SurfaceBuilderContext context)
        {
            final float variantNoiseValue = (float) SAND_VARIANT_NOISE.noise(context.pos().getX(), context.pos().getZ());
            if (variantNoiseValue > 0.8f) return RARE_SHORE_SANDSTONE.getState(context);
            if (variantNoiseValue > 0.4f) return YELLOW_SANDSTONE.getState(context);
            if (variantNoiseValue > 0.2f) return RED_SANDSTONE.getState(context);
            return BROWN_SANDSTONE.getState(context);
        }
    };

    public static final SurfaceState SHORE_SURFACE = new SurfaceState()
    {
        @Override
        public BlockState getState(SurfaceBuilderContext context)
        {
            final float beachMix = (float) BEACH_MIX_NOISE.noise(context.pos().getX(), context.pos().getZ());
            final double gravelCutoff = Mth.clampedMap(context.averageTemperature(), -15, 25, -0.7, 0.7);
            return (beachMix > gravelCutoff ? GRAVEL : SHORE_SAND).getState(context);
        }
    };

    public static final SurfaceState SHORE_UNDERLAYER = new SurfaceState()
    {
        @Override
        public BlockState getState(SurfaceBuilderContext context)
        {
            final float beachMix = (float) BEACH_MIX_NOISE.noise(context.pos().getX(), context.pos().getZ());
            final double gravelCutoff = Mth.clampedMap(context.averageTemperature(), -15, 25, -0.7, 0.7);
            return (beachMix > gravelCutoff ? RAW : SHORE_SANDSTONE).getState(context);
        }
    };

    public static final SurfaceState TOP_GRASS_TO_GRAVEL = NTESoilSurfaceState.buildSurfaceType(NTESoilBlockType.GRASS, GRAVEL);
    public static final SurfaceState TOP_GRASS_TO_SAND = NTESoilSurfaceState.buildSurfaceType(NTESoilBlockType.GRASS, SAND);
    public static final SurfaceState MID_DIRT_TO_GRAVEL = NTESoilSurfaceState.buildMidType(NTESoilBlockType.DIRT, GRAVEL);
    public static final SurfaceState MID_DIRT_TO_SAND = NTESoilSurfaceState.buildMidType(NTESoilBlockType.DIRT, SAND);
    public static final SurfaceState VOLCANIC_TOP_GRASS_TO_GRAVEL = NTESoilSurfaceState.buildVolcanicSurfaceType(NTESoilBlockType.GRASS, BASALT_GRAVEL);
    public static final SurfaceState VOLCANIC_MID_DIRT_TO_GRAVEL = NTESoilSurfaceState.buildVolcanicMidType(NTESoilBlockType.DIRT, BASALT_GRAVEL);
    public static final SurfaceState VOLCANIC_TOP_GRASS_TO_LOCAL_GRAVEL = NTESoilSurfaceState.buildVolcanicSurfaceType(NTESoilBlockType.GRASS, GRAVEL);
    public static final SurfaceState VOLCANIC_MID_DIRT_TO_LOCAL_GRAVEL = NTESoilSurfaceState.buildVolcanicMidType(NTESoilBlockType.DIRT, GRAVEL);
    public static final SurfaceState UNDER_GRAVEL = NTESoilSurfaceState.buildUnderType();
    public static final SurfaceState MUD = NTESoilSurfaceState.buildSurfaceType(NTESoilBlockType.MUD, GRAVEL);
    public static final SurfaceState SNOWY_RAW = NTESoilSurfaceState.buildSnowableSurface(SNOW, RAW);
    public static final SurfaceState SNOWY_COBBLE = NTESoilSurfaceState.buildSnowableSurface(SNOW, COBBLE);
    public static final SurfaceState SNOWY_GRAVEL = NTESoilSurfaceState.buildSnowableSurface(SNOW, GRAVEL);
    public static final SurfaceState SNOWY_SAND = NTESoilSurfaceState.buildSnowableSurface(SNOW, SAND);
    public static final SurfaceState SNOWY_MORAINE = NTESoilSurfaceState.buildSnowableSurface(SNOW, MORAINE);
    public static final SurfaceState SNOWY_BASALT = NTESoilSurfaceState.buildSnowableSurface(SNOW, BASALT);
    public static final SurfaceState SNOWY_BASALT_COBBLE = NTESoilSurfaceState.buildSnowableSurface(SNOW, BASALT_COBBLE);
    public static final SurfaceState SNOWY_BASALT_GRAVEL = NTESoilSurfaceState.buildSnowableSurface(SNOW, BASALT_GRAVEL);
    public static final SurfaceState SNOWY_BASALT_MORAINE = NTESoilSurfaceState.buildSnowableSurface(SNOW, BASALT_MORAINE);
    public static final SurfaceState SNOWY_SAND_AND_GRAVEL = NTESoilSurfaceState.buildSnowableSurface(SNOW, SAND_AND_GRAVEL);

    private NTESurfaceStates()
    {
    }

    private static SurfaceState block(Supplier<? extends Block> block)
    {
        return context -> block.get().defaultBlockState();
    }
}
