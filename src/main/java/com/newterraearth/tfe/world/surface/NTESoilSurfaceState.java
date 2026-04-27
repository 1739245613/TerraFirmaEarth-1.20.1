package com.newterraearth.tfe.world.surface;

import java.util.List;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;

import com.newterraearth.tfe.common.NTEBlocks;
import com.newterraearth.tfe.world.NTE121ClimateHelpers;
import com.newterraearth.tfe.world.NTESurfaceContext;
import com.newterraearth.tfe.world.soil.NTESoil;
import com.newterraearth.tfe.world.soil.NTESoilBlockType;

public class NTESoilSurfaceState implements SurfaceState
{
    public static final Noise2D PATCH_NOISE = new OpenSimplex2D(18273952837592L).octaves(2).spread(0.012f);

    private static final Noise2D FLOODPLAIN_NOISE = new OpenSimplex2D(81234781234L).octaves(3).spread(0.006f);
    private static final Noise2D FLUVISOL_CLUSTER_NOISE = new OpenSimplex2D(67584930211L).octaves(2).spread(0.0065f);
    private static final Noise2D RAIN_VARIANCE_NOISE = new OpenSimplex2D(91827364512L).octaves(2).spread(0.0012f);

    public static SurfaceState buildSnowableSurface(SurfaceState snow, SurfaceState typical)
    {
        final ImmutableList<SurfaceState> regions = ImmutableList.of(
            snow,
            snow,
            transition(snow, typical),
            typical,
            typical,
            typical,
            typical,
            typical,
            typical,
            typical,
            typical,
            typical,
            typical,
            typical,
            typical,
            typical,
            typical,
            typical
        );
        return new NTESoilSurfaceState(regions);
    }

    public static SurfaceState buildSurfaceType(NTESoilBlockType type, SurfaceState dry)
    {
        final ImmutableList<SurfaceState> regions = ImmutableList.of(
            NTESurfaceStates.SNOW_BLOCK,
            NTESurfaceStates.SNOW_BLOCK,
            transition(NTESurfaceStates.SNOW_BLOCK, dry),
            dry,
            transition(dry, NTESurfaceStates.COARSE_ARIDISOL_BASE),
            NTESurfaceStates.COARSE_ARIDISOL_BASE,
            transition(NTESurfaceStates.COARSE_ARIDISOL_BASE, soil(type, NTESoil.ARIDISOL)),
            soil(type, NTESoil.ARIDISOL),
            blobTransition(soil(type, NTESoil.ARIDISOL), transitioningSoil(type)),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type)
        );
        return needsPostProcessing(type) ? new NeedsPostProcessing(regions) : new NTESoilSurfaceState(regions);
    }

    public static SurfaceState buildMidType(NTESoilBlockType type, SurfaceState dry)
    {
        final ImmutableList<SurfaceState> regions = ImmutableList.of(
            NTESurfaceStates.PACKED_ICE,
            blobTransition(NTESurfaceStates.PACKED_ICE, dry),
            dry,
            dry,
            transition(dry, NTESurfaceStates.COARSE_ARIDISOL_BASE),
            NTESurfaceStates.COARSE_ARIDISOL_BASE,
            transition(NTESurfaceStates.COARSE_ARIDISOL_BASE, soil(type, NTESoil.ARIDISOL)),
            soil(type, NTESoil.ARIDISOL),
            blobTransition(soil(type, NTESoil.ARIDISOL), transitioningSoil(type)),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type)
        );
        return needsPostProcessing(type) ? new NeedsPostProcessing(regions) : new NTESoilSurfaceState(regions);
    }

    public static SurfaceState buildVolcanicSurfaceType(NTESoilBlockType type, SurfaceState dry)
    {
        final ImmutableList<SurfaceState> regions = ImmutableList.of(
            NTESurfaceStates.SNOW,
            NTESurfaceStates.SNOW,
            transition(NTESurfaceStates.SNOW, dry),
            dry,
            transition(dry, NTESurfaceStates.COARSE_ANDISOL_BASE),
            NTESurfaceStates.COARSE_ANDISOL_BASE,
            transition(NTESurfaceStates.COARSE_ANDISOL_BASE, soil(type, NTESoil.ANDISOL)),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL)
        );
        return needsPostProcessing(type) ? new NeedsPostProcessing(regions) : new NTESoilSurfaceState(regions);
    }

    public static SurfaceState buildVolcanicMidType(NTESoilBlockType type, SurfaceState dry)
    {
        final ImmutableList<SurfaceState> regions = ImmutableList.of(
            NTESurfaceStates.PACKED_ICE,
            blobTransition(NTESurfaceStates.PACKED_ICE, dry),
            dry,
            dry,
            transition(dry, NTESurfaceStates.COARSE_ANDISOL_BASE),
            NTESurfaceStates.COARSE_ANDISOL_BASE,
            transition(NTESurfaceStates.COARSE_ANDISOL_BASE, soil(type, NTESoil.ANDISOL)),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL),
            soil(type, NTESoil.ANDISOL)
        );
        return needsPostProcessing(type) ? new NeedsPostProcessing(regions) : new NTESoilSurfaceState(regions);
    }

    public static SurfaceState buildUnderType()
    {
        final ImmutableList<SurfaceState> regions = ImmutableList.of(
            NTESurfaceStates.RAW,
            NTESurfaceStates.RAW,
            blobTransition(NTESurfaceStates.RAW, NTESurfaceStates.GRAVEL),
            NTESurfaceStates.GRAVEL,
            NTESurfaceStates.GRAVEL,
            NTESurfaceStates.GRAVEL,
            NTESurfaceStates.GRAVEL,
            NTESurfaceStates.GRAVEL,
            NTESurfaceStates.GRAVEL,
            NTESurfaceStates.GRAVEL,
            NTESurfaceStates.GRAVEL,
            NTESurfaceStates.GRAVEL,
            NTESurfaceStates.GRAVEL,
            NTESurfaceStates.GRAVEL,
            NTESurfaceStates.GRAVEL,
            NTESurfaceStates.GRAVEL,
            NTESurfaceStates.GRAVEL,
            NTESurfaceStates.GRAVEL
        );
        return new NTESoilSurfaceState(regions);
    }

    public static SurfaceState buildDryDirt(NTESoilBlockType type)
    {
        final ImmutableList<SurfaceState> regions = ImmutableList.of(
            NTESurfaceStates.SNOW_BLOCK,
            NTESurfaceStates.SNOW_BLOCK,
            transition(NTESurfaceStates.SNOW_BLOCK, soil(type, NTESoil.ARIDISOL)),
            soil(type, NTESoil.ARIDISOL),
            soil(type, NTESoil.ARIDISOL),
            soil(type, NTESoil.ARIDISOL),
            soil(type, NTESoil.ARIDISOL),
            soil(type, NTESoil.ARIDISOL),
            blobTransition(soil(type, NTESoil.ARIDISOL), transitioningSoil(type)),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type),
            transitioningSoil(type)
        );
        return new NTESoilSurfaceState(regions);
    }

    public static SurfaceState soil(NTESoilBlockType type, NTESoil soil)
    {
        return context -> NTEBlocks.getBlock(soil, type).get().defaultBlockState();
    }

    public static SurfaceState transitioningSoil(NTESoilBlockType blockType)
    {
        return transitioningSoil(blockType, NTESoil.ENTISOL, NTESoil.OXISOL, 16f, 16.7f);
    }

    public static SurfaceState transitioningSoil(NTESoilBlockType blockType, NTESoil coldSoilType, NTESoil hotSoilType, float transitionStartTemp, float transitionEndTemp)
    {
        return context -> {
            if (shouldUseFluvisol(context))
            {
                return NTEBlocks.getBlock(NTESoil.FLUVISOL, blockType).get().defaultBlockState();
            }

            final float temperature = adjustedTemperature(context);
            final BlockState coldBlock = NTEBlocks.getBlock(coldSoilType, blockType).get().defaultBlockState();
            if (temperature < transitionStartTemp)
            {
                return coldBlock;
            }

            final BlockState hotBlock = NTEBlocks.getBlock(hotSoilType, blockType).get().defaultBlockState();
            if (temperature > transitionEndTemp)
            {
                return hotBlock;
            }

            return selectTransitioningSoil(context, coldBlock, hotBlock, temperature, transitionStartTemp, transitionEndTemp);
        };
    }

    private static boolean needsPostProcessing(NTESoilBlockType type)
    {
        return type == NTESoilBlockType.GRASS || type == NTESoilBlockType.CLAY_GRASS;
    }

    private static boolean shouldUseFluvisol(SurfaceBuilderContext context)
    {
        final float baseGroundwater = baseGroundwater(context);
        final float averageGroundwater = groundWater(context);
        final float variance = Math.abs(rainVariance(context));
        if (baseGroundwater <= 32f || averageGroundwater <= 110f || variance <= 0.65f)
        {
            return false;
        }

        final BlockPos pos = context.pos();
        final double cluster = FLUVISOL_CLUSTER_NOISE.noise(pos.getX(), pos.getZ());
        final double threshold = 0.22d
            - Mth.clampedMap(baseGroundwater, 32f, 96f, 0d, 0.18d)
            - Mth.clampedMap(averageGroundwater, 110f, 260f, 0d, 0.16d)
            - Mth.clampedMap(variance, 0.65f, 2.5f, 0d, 0.10d);
        return cluster > threshold;
    }

    private static BlockState selectTransitioningSoil(SurfaceBuilderContext context, BlockState coldBlock, BlockState hotBlock, float temperature, float transitionStartTemp, float transitionEndTemp)
    {
        final BlockPos pos = context.pos();
        final float midpoint = 0.5f * (transitionStartTemp + transitionEndTemp);
        final float halfWidth = Math.max(0.05f, 0.5f * (transitionEndTemp - transitionStartTemp));
        final double selector = PATCH_NOISE.noise(pos.getX(), pos.getZ())
            + Mth.clampedMap(temperature, midpoint - halfWidth, midpoint + halfWidth, -0.35d, 0.35d);
        return selector > 0d ? hotBlock : coldBlock;
    }

    private static float groundWater(SurfaceBuilderContext context)
    {
        final NTESurfaceContext.Context surfaceContext = NTESurfaceContext.current();
        if (surfaceContext != null)
        {
            final float groundwater = surfaceContext.averageGroundwater(context.pos());
            if (groundwater != Float.NEGATIVE_INFINITY)
            {
                return groundwater;
            }
        }
        return approximateGroundWater(context);
    }

    private static float baseGroundwater(SurfaceBuilderContext context)
    {
        final NTESurfaceContext.Context surfaceContext = NTESurfaceContext.current();
        if (surfaceContext != null)
        {
            final float groundwater = surfaceContext.baseGroundwater(context.pos());
            if (groundwater != Float.NEGATIVE_INFINITY)
            {
                return groundwater;
            }
        }
        return approximateBaseGroundwater(context);
    }

    private static float rainVariance(SurfaceBuilderContext context)
    {
        final NTESurfaceContext.Context surfaceContext = NTESurfaceContext.current();
        if (surfaceContext != null)
        {
            return surfaceContext.rainVariance(context.getSeed(), context.pos());
        }
        return approximateRainVariance(context);
    }

    private static float approximateGroundWater(SurfaceBuilderContext context)
    {
        return context.rainfall() + approximateBaseGroundwater(context);
    }

    private static float approximateBaseGroundwater(SurfaceBuilderContext context)
    {
        final int y = context.pos().getY();
        final int seaLevel = context.getSeaLevel();
        final float rainfall = context.rainfall();
        final float valleyBoost = y <= seaLevel + 4 ?
            Mth.clampedMap(seaLevel + 4 - y, 0, 24, 0, 90) :
            Mth.clampedMap(y - seaLevel - 4, 0, 72, 0, -60);
        final float floodBoost = (float) Mth.clampedMap((float) FLOODPLAIN_NOISE.noise(context.pos().getX(), context.pos().getZ()), -0.5f, 0.7f, 0f, 65f);
        final float slopePenalty = (float) Mth.clamp(context.getSlope() * 14d, 0d, 85d);
        return rainfall * 0.08f + valleyBoost + floodBoost - slopePenalty;
    }

    private static float approximateRainVariance(SurfaceBuilderContext context)
    {
        return (float) RAIN_VARIANCE_NOISE.noise(context.pos().getX(), context.pos().getZ());
    }

    private static float adjustedTemperature(SurfaceBuilderContext context)
    {
        return context.averageTemperature() - Mth.clamp((context.pos().getY() - context.getSeaLevel()) * 0.16225f, 0f, 17.822f);
    }

    private static SurfaceState transition(SurfaceState first, SurfaceState second)
    {
        return context -> (Helpers.hash(729375982L, context.pos()) & 127) > 63 ?
            first.getState(context) : second.getState(context);
    }

    private static SurfaceState blobTransition(SurfaceState first, SurfaceState second)
    {
        return context -> {
            final BlockPos pos = context.pos();
            return PATCH_NOISE.noise(pos.getX(), pos.getZ()) > 0 ?
                first.getState(context) : second.getState(context);
        };
    }

    private final List<SurfaceState> regions;

    private NTESoilSurfaceState(List<SurfaceState> regions)
    {
        this.regions = regions;
    }

    @Override
    public BlockState getState(SurfaceBuilderContext context)
    {
        final int rainIndex = (int) Mth.clampedMap(groundWater(context), 35, 450, 3, regions.size() - 0.01f);
        final int tempIndex = (int) Mth.clampedMap(adjustedTemperature(context), -19, -4, 0, regions.size() - 0.01f);
        return regions.get(Math.min(rainIndex, tempIndex)).getState(context);
    }

    static class NeedsPostProcessing extends NTESoilSurfaceState
    {
        private NeedsPostProcessing(List<SurfaceState> regions)
        {
            super(regions);
        }

        @Override
        public void setState(SurfaceBuilderContext context)
        {
            context.chunk().setBlockState(context.pos(), getState(context), false);
            context.chunk().markPosForPostprocessing(context.pos());
        }
    }
}
