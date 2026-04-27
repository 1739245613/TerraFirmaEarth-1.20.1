package com.newterraearth.tfe.world.surface;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blocks.SandstoneBlockType;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.rock.Rock;
import net.dries007.tfc.common.blocks.soil.SandBlockType;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

import com.newterraearth.tfe.common.NTERock;
import com.newterraearth.tfe.world.NTE121ClimateHelpers;
import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.NTESurfaceContext;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

public final class NTEBadlandsSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory NORMAL = seed -> new NTEBadlandsSurfaceBuilder(false, false, -1, seed);
    public static final SurfaceBuilderFactory MESAS = seed -> new NTEBadlandsSurfaceBuilder(true, false, -3, seed);
    public static final SurfaceBuilderFactory HOODOOS = seed -> new NTEBadlandsSurfaceBuilder(true, false, -7, seed);
    public static final SurfaceBuilderFactory WARPED = seed -> new NTEBadlandsSurfaceBuilder(true, true, -20, seed);

    private static final int PRIMARY_SIZE = 8;
    private static final int SECONDARY_SIZE = 5;
    private static final int UNCOMMON_SIZE = 3;
    private static final int LAYER_SIZE = PRIMARY_SIZE + SECONDARY_SIZE + UNCOMMON_SIZE;

    private static final Set<Rock> KARST_ROCKS = EnumSet.of(Rock.LIMESTONE, Rock.DOLOMITE, Rock.CHALK, Rock.MARBLE);
    private static final Set<Rock> MAFIC_ROCKS = EnumSet.of(Rock.GABBRO, Rock.BASALT);
    private static final Map<Block, Rock> ROCK_BY_RAW_BLOCK = new IdentityHashMap<>();

    static
    {
        for (Rock rock : Rock.values())
        {
            ROCK_BY_RAW_BLOCK.put(TFCBlocks.ROCK_BLOCKS.get(rock).get(Rock.BlockType.RAW).get(), rock);
        }
    }

    private static void fillBlocks(RandomSource random, BlockState[] sandLayers, BlockState[] sandstoneLayers, BlockState primaryStone, BlockState primarySand, BlockState secondaryStone, BlockState secondarySand, BlockState uncommonStone, BlockState uncommonSand)
    {
        fill(random, sandLayers, primarySand, secondarySand, uncommonSand);
        fill(random, sandstoneLayers, primaryStone, secondaryStone, uncommonStone);
    }

    private static void fillSand(RandomSource random, BlockState[] sandLayers, BlockState[] sandstoneLayers, SandBlockType primary, SandBlockType secondary, SandBlockType uncommon)
    {
        fill(random, sandLayers, sand(primary), sand(secondary), sand(uncommon));
        fill(random, sandstoneLayers, sandstone(primary), sandstone(secondary), sandstone(uncommon));
    }

    private static void fill(RandomSource random, BlockState[] layers, BlockState primary, BlockState secondary, BlockState uncommon)
    {
        Arrays.fill(layers, 0, PRIMARY_SIZE, primary);
        Arrays.fill(layers, PRIMARY_SIZE, PRIMARY_SIZE + SECONDARY_SIZE, secondary);
        Arrays.fill(layers, PRIMARY_SIZE + SECONDARY_SIZE, LAYER_SIZE, uncommon);
        shuffleArray(layers, random);
    }

    private static BlockState sand(SandBlockType color)
    {
        return TFCBlocks.SAND.get(color).get().defaultBlockState();
    }

    private static BlockState sandstone(SandBlockType color)
    {
        return TFCBlocks.SANDSTONE.get(color).get(SandstoneBlockType.RAW).get().defaultBlockState();
    }

    private static BlockState gravel(Rock rock)
    {
        return TFCBlocks.ROCK_BLOCKS.get(rock).get(Rock.BlockType.GRAVEL).get().defaultBlockState();
    }

    private static BlockState rock(Rock rock)
    {
        return TFCBlocks.ROCK_BLOCKS.get(rock).get(Rock.BlockType.RAW).get().defaultBlockState();
    }

    private static BlockState addonGravel(NTERock rock)
    {
        return rock.getBlock(Rock.BlockType.GRAVEL).get().defaultBlockState();
    }

    private static BlockState addonRock(NTERock rock)
    {
        return rock.getBlock(Rock.BlockType.RAW).get().defaultBlockState();
    }

    private static <T> void shuffleArray(T[] array, RandomSource random)
    {
        for (int i = array.length - 1; i > 0; i--)
        {
            final int j = random.nextInt(i + 1);
            final T value = array[i];
            array[i] = array[j];
            array[j] = value;
        }
    }

    private final boolean inverted;
    private final boolean dippingStrata;
    private final int soilMinDepth;
    private final BlockState[] sandLayers0;
    private final BlockState[] sandLayers1;
    private final BlockState[] sandLayersKarst;
    private final BlockState[] sandLayersVolcanic;
    private final BlockState[] sandstoneLayers0;
    private final BlockState[] sandstoneLayers1;
    private final BlockState[] sandstoneLayersKarst;
    private final BlockState[] sandstoneLayersVolcanic;
    private final float[] layerThresholds;
    private final Noise2D grassHeightVariationNoise;
    private final Noise2D sandHeightOffsetNoise;
    private final Noise2D sandStyleNoise;

    public NTEBadlandsSurfaceBuilder(boolean inverted, boolean dippingStrata, int soilMinDepth, long seed)
    {
        this.inverted = inverted;
        this.dippingStrata = dippingStrata;
        this.soilMinDepth = soilMinDepth;

        final RandomSource random = NTESeed.of(seed).fork();

        sandHeightOffsetNoise = dippingStrata
            ? new OpenSimplex2D(random.nextLong()).octaves(2).scaled(-250, 250).spread(0.005)
            : new OpenSimplex2D(random.nextLong()).octaves(2).scaled(0, 6).spread(0.0014f);

        sandLayers0 = new BlockState[LAYER_SIZE];
        sandLayers1 = new BlockState[LAYER_SIZE];
        sandLayersKarst = new BlockState[LAYER_SIZE];
        sandLayersVolcanic = new BlockState[LAYER_SIZE];

        sandstoneLayers0 = new BlockState[LAYER_SIZE];
        sandstoneLayers1 = new BlockState[LAYER_SIZE];
        sandstoneLayersKarst = new BlockState[LAYER_SIZE];
        sandstoneLayersVolcanic = new BlockState[LAYER_SIZE];

        layerThresholds = new float[LAYER_SIZE];

        fillSand(random, sandLayers0, sandstoneLayers0, SandBlockType.RED, SandBlockType.BROWN, SandBlockType.YELLOW);
        fillSand(random, sandLayers1, sandstoneLayers1, SandBlockType.BROWN, SandBlockType.YELLOW, SandBlockType.WHITE);
        fillSand(random, sandLayersKarst, sandstoneLayersKarst, SandBlockType.RED, SandBlockType.YELLOW, SandBlockType.WHITE);
        fillBlocks(random, sandLayersVolcanic, sandstoneLayersVolcanic, sandstone(SandBlockType.BLACK), sand(SandBlockType.BLACK), addonRock(NTERock.TUFF), addonGravel(NTERock.TUFF), sandstone(SandBlockType.RED), sand(SandBlockType.RED));

        for (int i = 0; i < LAYER_SIZE; i++)
        {
            layerThresholds[i] = random.nextFloat();
        }

        grassHeightVariationNoise = new OpenSimplex2D(random.nextLong()).octaves(2).scaled(SEA_LEVEL_Y + 14, SEA_LEVEL_Y + 18).spread(0.5f);
        sandStyleNoise = new OpenSimplex2D(random.nextLong()).octaves(2).scaled(-0.3f, 1.3f).spread(0.0003f);
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final double heightVariation = grassHeightVariationNoise.noise(context.pos().getX(), context.pos().getZ());
        final double weightVariation = (1.0 - context.weight()) * 23.0;
        final double rainfallVariation = Mth.clampedMap(getGroundwater(context), 100, 500, 0, 22);

        final Rock rock = ROCK_BY_RAW_BLOCK.get(context.getRock().raw());
        final boolean karst = rock != null && KARST_ROCKS.contains(rock);
        final boolean volcanic = rock != null && MAFIC_ROCKS.contains(rock);

        if (inverted)
        {
            final int shift = dippingStrata ? -10 : -16;
            buildSandstoneSurface(context, startY, endY, karst, volcanic, (int) (shift + heightVariation + weightVariation + rainfallVariation));
        }
        else if (startY - 5 > heightVariation - weightVariation - rainfallVariation)
        {
            NormalSurfaceBuilder.INSTANCE.buildSurface(context, startY, endY, NTESurfaceStates.TOP_GRASS_TO_SAND, NTESurfaceStates.MID_DIRT_TO_SAND, NTESurfaceStates.UNDER_GRAVEL);
        }
        else
        {
            buildSandySurface(context, startY, endY, karst, volcanic);
        }
    }

    private void buildSandySurface(SurfaceBuilderContext context, int startHeight, int minSurfaceHeight, boolean karst, boolean volcanic)
    {
        final float style = (float) sandStyleNoise.noise(context.pos().getX(), context.pos().getZ());
        final int height = (int) sandHeightOffsetNoise.noise(context.pos().getX(), context.pos().getZ());

        int surfaceDepth = -1;
        for (int y = startHeight; y >= minSurfaceHeight; --y)
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
                    if (y < context.getSeaLevel() - 1)
                    {
                        context.setBlockState(y, NTESurfaceStates.SAND);
                    }
                    else
                    {
                        context.setBlockState(y, sampleLayer(getKarstOrVolcanicSandLayer(sandLayers0, karst, volcanic), getKarstOrVolcanicSandLayer(sandLayers1, karst, volcanic), y + height, style));
                        surfaceDepth = 3;
                    }
                }
                else if (surfaceDepth > 0)
                {
                    surfaceDepth--;
                    context.setBlockState(y, sampleLayer(getKarstOrVolcanicSandstoneLayer(sandstoneLayers0, karst, volcanic), getKarstOrVolcanicSandstoneLayer(sandstoneLayers1, karst, volcanic), y + height, style));
                }
            }
        }
    }

    private void buildSandstoneSurface(SurfaceBuilderContext context, int startHeight, int minSurfaceHeight, boolean karst, boolean volcanic, int sandstoneBaseHeight)
    {
        final float style = (float) sandStyleNoise.noise(context.pos().getX(), context.pos().getZ());
        final int height = (int) sandHeightOffsetNoise.noise(context.pos().getX(), context.pos().getZ());

        int surfaceDepth = -1;
        int sandstoneDepth = startHeight - sandstoneBaseHeight;
        for (int y = startHeight; y >= minSurfaceHeight; --y)
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
                    if (y < context.getSeaLevel() - 1)
                    {
                        context.setBlockState(y, NTESurfaceStates.SAND);
                    }
                    else
                    {
                        surfaceDepth = calculateAltitudeSlopeSurfaceDepth(context, y, soilMinDepth);
                        if (surfaceDepth < 0)
                        {
                            if (sandstoneDepth > 0)
                            {
                                context.setBlockState(y, sampleLayer(getKarstOrVolcanicSandstoneLayer(sandstoneLayers0, karst, volcanic), getKarstOrVolcanicSandstoneLayer(sandstoneLayers1, karst, volcanic), y + height, style));
                            }
                            surfaceDepth = 0;
                        }
                        else
                        {
                            context.setBlockState(y, NTESurfaceStates.TOP_GRASS_TO_SAND);
                            sandstoneDepth -= surfaceDepth;
                        }
                    }
                }
                else if (surfaceDepth > 0)
                {
                    surfaceDepth--;
                    context.setBlockState(y, NTESurfaceStates.MID_DIRT_TO_SAND);
                }
                else if (sandstoneDepth > 0)
                {
                    sandstoneDepth--;
                    context.setBlockState(y, sampleLayer(getKarstOrVolcanicSandstoneLayer(sandstoneLayers0, karst, volcanic), getKarstOrVolcanicSandstoneLayer(sandstoneLayers1, karst, volcanic), y + height, style));
                }
            }
        }
    }

    private BlockState[] getKarstOrVolcanicSandLayer(BlockState[] defaultLayer, boolean karst, boolean volcanic)
    {
        return karst ? sandLayersKarst : volcanic ? sandLayersVolcanic : defaultLayer;
    }

    private BlockState[] getKarstOrVolcanicSandstoneLayer(BlockState[] defaultLayer, boolean karst, boolean volcanic)
    {
        return karst ? sandstoneLayersKarst : volcanic ? sandstoneLayersVolcanic : defaultLayer;
    }

    private float getGroundwater(SurfaceBuilderContext context)
    {
        final NTESurfaceContext.Context surfaceContext = NTESurfaceContext.current();
        if (surfaceContext == null)
        {
            return context.rainfall();
        }
        return surfaceContext.baseGroundwater(context.pos()) + context.rainfall();
    }

    private BlockState sampleLayer(BlockState[] layers0, BlockState[] layers1, int y, float threshold)
    {
        final int height = dippingStrata ? y / 4 : y;
        final int index = Math.floorMod(height, LAYER_SIZE);
        return (layerThresholds[index] < threshold ? layers0 : layers1)[index];
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
}
