package com.newterraearth.tfe.world.surface;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

import com.newterraearth.tfe.world.NTE121ClimateHelpers;
import com.newterraearth.tfe.world.NTEBiomeNoise;
import com.newterraearth.tfe.world.NTESurfaceContext;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

public class IceSheetSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory NORMAL = seed -> new IceSheetSurfaceBuilder(seed, NTEBiomeNoise.glacialBase(seed), NTEBiomeNoise.iceSheetSurfaceHeight(seed), true, false, false);
    public static final SurfaceBuilderFactory EDGE = seed -> new IceSheetSurfaceBuilder(seed, addConstant(NTEBiomeNoise.glacialBase(seed), 1.6), NTEBiomeNoise.iceSheetSurfaceHeight(seed), true, false, false);
    public static final SurfaceBuilderFactory EDGE_LAKE = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.lake(seed), NTEBiomeNoise.iceSheetSurfaceHeight(seed), false, false, false);
    public static final SurfaceBuilderFactory HIDDEN_LAKE = seed -> new IceSheetSurfaceBuilder(seed, glacialOceanicBase(), NTEBiomeNoise.iceSheetSurfaceHeight(seed), false, false, false);
    public static final SurfaceBuilderFactory ICE_SHEET_MOUNTAINS = seed -> new IceSheetSurfaceBuilder(seed, addConstant(NTEBiomeNoise.glacialCirques(seed), 39), max(NTEBiomeNoise.montaneIceSheetSurfaceHeight(seed), addConstant(NTEBiomeNoise.glacialCirquesIceSurfaceHeight(seed), 39)), false, true, false);
    public static final SurfaceBuilderFactory GLACIATED_MOUNTAINS = seed -> new IceSheetSurfaceBuilder(seed, addConstant(NTEBiomeNoise.glacialCirques(seed), 39), addConstant(NTEBiomeNoise.glacialCirquesIceSurfaceHeight(seed), 39), false, true, false);
    public static final SurfaceBuilderFactory OCEANIC = seed -> new IceSheetSurfaceBuilder(seed, glacialOceanicBase(), NTEBiomeNoise.oceanicIceSheetSurfaceHeight(seed), false, false, true);
    public static final SurfaceBuilderFactory ICE_SHEET_OCEANIC_MOUNTAINS = seed -> new IceSheetSurfaceBuilder(seed, NTEBiomeNoise.glacialCirques(seed), max(NTEBiomeNoise.oceanicIceSheetSurfaceHeight(seed), NTEBiomeNoise.glacialCirquesIceSurfaceHeight(seed)), false, true, true);
    public static final SurfaceBuilderFactory GLACIATED_OCEANIC_MOUNTAINS = seed -> new IceSheetSurfaceBuilder(seed, NTEBiomeNoise.glacialCirques(seed), NTEBiomeNoise.glacialCirquesIceSurfaceHeight(seed), false, true, true);

    public static final SurfaceBuilderFactory FLAT = NORMAL;
    public static final SurfaceBuilderFactory MOUNTAINS = ICE_SHEET_MOUNTAINS;
    public static final SurfaceBuilderFactory GLACIATED = GLACIATED_MOUNTAINS;
    public static final SurfaceBuilderFactory GLACIATED_OCEANIC = GLACIATED_OCEANIC_MOUNTAINS;

    private final Noise2D iceSurfaceNoise;
    private final Noise2D baseNoise;
    private final boolean hasMoraines;
    private final boolean hasStonyPeaks;
    private final boolean isShoreBiome;
    private final SurfaceBuilder shoreSurfaceBuilder;

    public IceSheetSurfaceBuilder(long seed, Noise2D baseNoise, Noise2D iceSurfaceNoise, boolean hasMoraines, boolean hasStonyPeaks, boolean isShoreBiome)
    {
        this.baseNoise = baseNoise;
        this.iceSurfaceNoise = iceSurfaceNoise;
        this.hasMoraines = hasMoraines;
        this.hasStonyPeaks = hasStonyPeaks;
        this.isShoreBiome = isShoreBiome;
        this.shoreSurfaceBuilder = isShoreBiome ? ShorelineSurfaceBuilder.MOUNTAINS.apply(seed) : null;
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final int seaLevel = context.getSeaLevel();
        final int x = context.pos().getX();
        final int z = context.pos().getZ();

        final int glacierBaseHeight = (int) Math.ceil(baseNoise.noise(x, z));
        final int glacierSurfaceHeight = (int) Math.ceil(iceSurfaceNoise.noise(x, z));

        final int iceDepth;
        if (hasMoraines && getBaseGroundwater(context) <= 20f)
        {
            final double moraineCrestHeight = Math.min(0.5 * (glacierSurfaceHeight + glacierBaseHeight), glacierBaseHeight + 18);
            iceDepth = Math.max((int) ((startY - moraineCrestHeight) * 2), 0);
        }
        else
        {
            iceDepth = 35;
        }

        if (hasStonyPeaks && startY > glacierSurfaceHeight + 2.5)
        {
            NormalSurfaceBuilder.ROCKY.buildSurface(context, startY, endY);
        }
        else if (startY < glacierBaseHeight - 1.5)
        {
            NormalSurfaceBuilder.INSTANCE.buildSurface(context, startY, endY);
        }
        else if (isShoreBiome && startY <= seaLevel)
        {
            shoreSurfaceBuilder.buildSurface(context, startY, endY);
        }
        else
        {
            placeIceSurface(context, startY, endY, glacierBaseHeight, glacierSurfaceHeight, seaLevel, iceDepth, NTESurfaceStates.SNOW, NTESurfaceStates.PACKED_ICE, NTESurfaceStates.BLUE_ICE, NTESurfaceStates.SNOWY_MORAINE, NTESurfaceStates.MORAINE, null);
        }
    }

    static void placeIceSurface(SurfaceBuilderContext context, int startY, int endY, int glacierBaseHeight, int glacierSurfaceHeight, int seaLevel, int initialIceDepth, SurfaceState snowState, SurfaceState iceState, SurfaceState blueIceState, SurfaceState moraineTopState, SurfaceState moraineState, SurfaceState baseState)
    {
        int surfaceDepth = -1;
        int iceDepth = initialIceDepth;
        final int minY = Math.max(endY, glacierBaseHeight - (baseState == null ? 2 : 22));

        for (int y = startY; y >= minY; --y)
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
                    final int surfaceY = y;
                    surfaceDepth = context.calculateAltitudeSlopeSurfaceDepth(surfaceY, 5, -3);
                    if (surfaceDepth <= -1)
                    {
                        if (iceDepth < 1)
                        {
                            context.setBlockState(y, moraineState);
                        }
                        else if (y <= glacierBaseHeight)
                        {
                            iceDepth = 0;
                        }
                        else
                        {
                            context.setBlockState(y, iceState);
                        }
                    }
                    else if (iceDepth == 0 || y <= seaLevel || y < glacierBaseHeight)
                    {
                        context.setBlockState(y, moraineState);
                        iceDepth = 0;
                    }
                    else
                    {
                        context.setBlockState(y, snowState);
                    }
                    surfaceDepth = 1;
                }
                else if (iceDepth > 0 && y > glacierBaseHeight)
                {
                    iceDepth--;
                    context.setBlockState(y, y < glacierSurfaceHeight - 16 ? blueIceState : iceState);
                }
                else if (baseState != null && y <= glacierBaseHeight)
                {
                    context.setBlockState(y, baseState);
                }
                else
                {
                    context.setBlockState(y, y == startY ? moraineTopState : moraineState);
                }
            }
        }
    }

    static float getBaseGroundwater(SurfaceBuilderContext context)
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

    private static float approximateBaseGroundwater(SurfaceBuilderContext context)
    {
        final int y = context.pos().getY();
        final int seaLevel = context.getSeaLevel();
        final float rainfall = context.rainfall();
        final float valleyBoost = y <= seaLevel + 4 ?
            (float) Mth.clampedMap(seaLevel + 4 - y, 0, 24, 0, 40) :
            (float) Mth.clampedMap(y - seaLevel - 4, 0, 72, 0, -40);
        final float slopePenalty = (float) Mth.clamp(context.getSlope() * 10d, 0d, 45d);
        return rainfall * 0.08f + valleyBoost - slopePenalty;
    }

    private static Noise2D addConstant(Noise2D input, double value)
    {
        return (x, z) -> input.noise(x, z) + value;
    }

    private static Noise2D max(Noise2D first, Noise2D second)
    {
        return (x, z) -> Math.max(first.noise(x, z), second.noise(x, z));
    }

    private static Noise2D glacialOceanicBase()
    {
        return (x, z) -> SEA_LEVEL_Y - 4;
    }
}
