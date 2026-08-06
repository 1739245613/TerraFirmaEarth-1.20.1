package com.newterraearth.tfe.world.surface;

import java.lang.reflect.Field;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.dries007.tfc.world.noise.FastNoiseLite;
import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.NTEClimateSeasonModel;
import com.newterraearth.tfe.world.shore.NTEShoreNoiseHelpers;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

public class ShorelineSurfaceBuilder implements SurfaceBuilder
{
    private static final float DEFAULT_HEMISPHERE_SCALE = 20_000f;
    private static final Field TEMPERATURE_SCALE_FIELD = resolveTemperatureScaleField();
    private static final SurfaceBuilder ROCKY_LAND = (context, startY, endY) -> buildNormalSurface(
        context,
        startY,
        endY,
        NTESurfaceStates.TOP_GRASS_TO_GRAVEL,
        NTESurfaceStates.MID_DIRT_TO_GRAVEL,
        NTESurfaceStates.UNDER_GRAVEL,
        NTESurfaceStates.GRAVEL,
        NTESurfaceStates.GRAVEL,
        NTESurfaceStates.TOP_GRASS_TO_GRAVEL,
        NTESurfaceStates.MID_DIRT_TO_GRAVEL,
        NTESurfaceStates.UNDER_GRAVEL,
        SEA_LEVEL_Y,
        -3
    );
    private static final SurfaceBuilder TERRACE_LAND = ShorelineSurfaceBuilder::buildTerraceLandSurface;
    public static final SurfaceBuilderFactory NORMAL = seed -> new ShorelineSurfaceBuilder(seed, NTESurfaceStates.SHORE_SURFACE, NTESurfaceStates.SHORE_UNDERLAYER, 6, false, false, ROCKY_LAND);
    public static final SurfaceBuilderFactory SANDY = seed -> new ShorelineSurfaceBuilder(seed, NTESurfaceStates.SHORE_SAND, NTESurfaceStates.SHORE_SANDSTONE, 6, false, false, ROCKY_LAND);
    public static final SurfaceBuilderFactory FORCE_RARE_SAND = seed -> new ShorelineSurfaceBuilder(seed, NTESurfaceStates.RARE_SHORE_SAND, NTESurfaceStates.RARE_SHORE_SANDSTONE, 6, false, false, ROCKY_LAND);
    public static final SurfaceBuilderFactory GRAVELLY = seed -> new ShorelineSurfaceBuilder(seed, NTESurfaceStates.GRAVEL, NTESurfaceStates.RAW, 6, false, false, ROCKY_LAND);
    public static final SurfaceBuilderFactory OCEAN = seed -> new ShorelineSurfaceBuilder(seed, NTESurfaceStates.SHORE_SURFACE, NTESurfaceStates.SHORE_UNDERLAYER, 6, false, false, SimpleSurfaceBuilder.OCEAN_MUD.apply(seed));
    public static final SurfaceBuilderFactory SEA_CLIFFS = seed -> new ShorelineSurfaceBuilder(seed, NTESurfaceStates.SHORE_SURFACE, NTESurfaceStates.SHORE_UNDERLAYER, 2, false, false, ROCKY_LAND);
    public static final SurfaceBuilderFactory OLD_SHIELD_VOLCANO = seed -> new ShorelineSurfaceBuilder(seed, NTESurfaceStates.VOLCANIC_SHORE_SAND, NTESurfaceStates.VOLCANIC_SHORE_SANDSTONE, 6, true, false, new ShieldVolcanoVariantSurfaceBuilder(seed, true, false));
    public static final SurfaceBuilderFactory ACTIVE_SHIELD_VOLCANO = seed -> new ShorelineSurfaceBuilder(seed, NTESurfaceStates.VOLCANIC_SHORE_SAND, NTESurfaceStates.VOLCANIC_SHORE_SANDSTONE, 2, false, true, new ShieldVolcanoVariantSurfaceBuilder(seed, false, false));
    public static final SurfaceBuilderFactory MOUNTAINS = seed -> new ShorelineSurfaceBuilder(seed, NTESurfaceStates.GRAVEL, NTESurfaceStates.RAW, 2, false, false, ROCKY_LAND);
    public static final SurfaceBuilderFactory VOLCANIC_MOUNTAINS = seed -> new ShorelineSurfaceBuilder(seed, NTESurfaceStates.GRAVEL, NTESurfaceStates.RAW, 2, false, false, SimpleSurfaceBuilder.ROCKY_VOLCANIC_SOIL.apply(seed));
    public static final SurfaceBuilderFactory ROCKY_SHORE = seed -> new ShorelineSurfaceBuilder(seed, NTESurfaceStates.RAW, NTESurfaceStates.RAW, 6, false, false, SimpleSurfaceBuilder.ROCKY_SHORE.apply(seed));
    public static final SurfaceBuilderFactory TERRACE_CLIFFS = seed -> new ShorelineSurfaceBuilder(seed, NTESurfaceStates.SHORE_SURFACE, NTESurfaceStates.SHORE_UNDERLAYER, 2, false, false, TERRACE_LAND);

    private final long seed;
    private final SurfaceState surface;
    private final SurfaceState subsurface;
    private final int sandHeight;
    private final boolean isShieldVolcano;
    private final boolean isActiveShieldVolcano;
    private final SurfaceBuilder landBuilder;
    private final SurfaceBuilder shieldVolcanoBeachBuilder;
    private final NormalNoise icebergPillarNoise;
    private final NormalNoise icebergPillarRoofNoise;
    private final NormalNoise icebergSurfaceNoise;
    private final Noise2D patternedNoise;
    private final Noise2D tideLevelNoise;
    private final Noise2D lavaFlowNoise;
    private final Noise2D lavaFlowMaterialNoise;

    protected ShorelineSurfaceBuilder(long seed, SurfaceState surface, SurfaceState subsurface, int sandHeight, boolean isShieldVolcano, boolean isActiveShieldVolcano, SurfaceBuilder landBuilder)
    {
        this.seed = seed;
        this.surface = surface;
        this.subsurface = subsurface;
        this.sandHeight = sandHeight;
        this.isShieldVolcano = isShieldVolcano;
        this.isActiveShieldVolcano = isActiveShieldVolcano;
        this.landBuilder = landBuilder;
        this.shieldVolcanoBeachBuilder = isShieldVolcano ? new ShieldVolcanoVariantSurfaceBuilder(seed, false, true) : ROCKY_LAND;
        this.tideLevelNoise = NTEShoreNoiseHelpers.shoreTideLevelNoise(NTESeed.unsafeOf(seed));
        this.lavaFlowNoise = lavaFlow(seed);
        this.lavaFlowMaterialNoise = lavaFlowMaterial(seed);
        this.patternedNoise = seaIceNoise(NTESeed.of(seed).forkStable().next());

        final RandomSource random = NTESeed.of(seed).forkStable().fork();
        this.icebergPillarNoise = NormalNoise.create(random, new NormalNoise.NoiseParameters(-6, 1.0D, 1.0D, 1.0D, 1.0D));
        this.icebergPillarRoofNoise = NormalNoise.create(random, new NormalNoise.NoiseParameters(-3, 1.0D));
        this.icebergSurfaceNoise = NormalNoise.create(random, new NormalNoise.NoiseParameters(-6, 1.0D, 1.0D, 1.0D));
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final BlockPos pos = context.pos();
        final int x = pos.getX();
        final int z = pos.getZ();
        final int tideLevel = (int) tideLevelNoise.noise(x, z);
        final int sandHeightAbsolute = tideLevel + sandHeight;
        final int seaLevel = context.getSeaLevel();

        final int oceanFloorY = startY > seaLevel
            ? startY
            : context.chunk().getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);

        final boolean deepWaterGravel = oceanFloorY < tideLevel - 5;
        if (deepWaterGravel)
        {
            buildDeepWaterGravelSurface(context, startY, endY);
        }
        else if (oceanFloorY <= sandHeightAbsolute)
        {
            if (isShieldVolcano)
            {
                if (isActiveShieldVolcano)
                {
                    buildLavaFlowSurface(context, startY, endY, x, z);
                }
                else
                {
                    shieldVolcanoBeachBuilder.buildSurface(context, startY, endY);
                }
            }
            else
            {
                buildNormalSurface(
                    context,
                    startY,
                    endY,
                    surface,
                    surface,
                    subsurface,
                    surface,
                    surface,
                    surface,
                    surface,
                    subsurface,
                    SEA_LEVEL_Y,
                    -1
                );
            }
        }

        if (!deepWaterGravel)
        {
            landBuilder.buildSurface(context, startY, endY);
        }

        if (startY <= seaLevel)
        {
            frozenOceanExtension(context, startY, endY, oceanFloorY, seaLevel);
        }
    }

    private static void buildDeepWaterGravelSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        int gravelDepth = 1 + (int) (Helpers.hash(98457321L, context.pos()) & 1L);
        boolean foundOceanFloor = false;

        for (int y = startY; y >= endY && gravelDepth > 0; --y)
        {
            final BlockState stateAt = context.getBlockState(y);
            if (context.isDefaultBlock(stateAt))
            {
                foundOceanFloor = true;
                context.setBlockState(y, NTESurfaceStates.GRAVEL);
                gravelDepth--;
            }
            else if (foundOceanFloor)
            {
                break;
            }
        }
    }

    private void buildLavaFlowSurface(SurfaceBuilderContext context, int startY, int endY, int x, int z)
    {
        final double noiseValue = lavaFlowMaterialNoise.noise(x, z);
        final double flowValue = lavaFlowNoise.noise(x, z);

        if (flowValue < 0.40)
        {
            buildNormalSurface(context, startY, endY, surface, surface, subsurface, surface, surface, surface, surface, subsurface, SEA_LEVEL_Y, -1);
        }
        else if (flowValue < 0.50)
        {
            if (noiseValue > 0)
            {
                buildNormalSurface(context, startY, endY, NTESurfaceStates.SNOWY_BASALT_GRAVEL, surface, subsurface, surface, surface, NTESurfaceStates.SNOWY_BASALT_GRAVEL, surface, subsurface, SEA_LEVEL_Y, -1);
            }
            else
            {
                buildNormalSurface(context, startY, endY, surface, surface, subsurface, surface, surface, surface, surface, subsurface, SEA_LEVEL_Y, -1);
            }
        }
        else if (flowValue < 0.75)
        {
            if (noiseValue > 0)
            {
                buildNormalSurface(context, startY, endY, NTESurfaceStates.SNOWY_BASALT_GRAVEL, NTESurfaceStates.BASALT_GRAVEL, NTESurfaceStates.BASALT, NTESurfaceStates.BASALT_GRAVEL, surface, NTESurfaceStates.SNOWY_BASALT_GRAVEL, NTESurfaceStates.BASALT_GRAVEL, NTESurfaceStates.BASALT, SEA_LEVEL_Y, -1);
            }
            else
            {
                buildNormalSurface(context, startY, endY, NTESurfaceStates.SNOWY_BASALT_COBBLE, NTESurfaceStates.BASALT_COBBLE, NTESurfaceStates.BASALT, NTESurfaceStates.BASALT_COBBLE, surface, NTESurfaceStates.SNOWY_BASALT_COBBLE, NTESurfaceStates.BASALT_COBBLE, NTESurfaceStates.BASALT, SEA_LEVEL_Y, -1);
            }
        }
        else if (noiseValue > -0.6)
        {
            buildNormalSurface(context, startY, endY, NTESurfaceStates.SNOWY_BASALT, NTESurfaceStates.BASALT, NTESurfaceStates.BASALT, NTESurfaceStates.BASALT, NTESurfaceStates.BASALT_COBBLE, NTESurfaceStates.SNOWY_BASALT, NTESurfaceStates.BASALT, NTESurfaceStates.BASALT, SEA_LEVEL_Y, -1);
        }
        else
        {
            buildNormalSurface(context, startY, endY, NTESurfaceStates.SNOWY_BASALT_COBBLE, NTESurfaceStates.BASALT_COBBLE, NTESurfaceStates.BASALT, NTESurfaceStates.BASALT, NTESurfaceStates.BASALT_COBBLE, NTESurfaceStates.SNOWY_BASALT_COBBLE, NTESurfaceStates.BASALT_COBBLE, NTESurfaceStates.BASALT, SEA_LEVEL_Y, -1);
        }
    }

    private void frozenOceanExtension(SurfaceBuilderContext context, int startY, int endY, int oceanFloorY, int seaLevel)
    {
        final OverworldClimateModel model = OverworldClimateModel.getIfPresent(context.level());
        if (model == null)
        {
            return;
        }

        final int x = context.pos().getX();
        final int z = context.pos().getZ();
        final float maxAnnualTemperature = getMaxAnnualTemperature(model, z, seaLevel, context.averageTemperature());
        if (maxAnnualTemperature > 2f)
        {
            return;
        }

        final double baseNoise = Math.min(
            Math.abs(icebergSurfaceNoise.getValue(x, 0, z) * 8.25),
            icebergPillarNoise.getValue(x * 1.28, 0, z * 1.28) * 15
        );

        if (baseNoise > 1.8)
        {
            final float temperatureFactor = Mth.clampedMap(maxAnnualTemperature, -1, 2, 1, 0.5f);
            final float depthFactor = Mth.clampedMap(oceanFloorY, seaLevel - 20, seaLevel - 6, 1, 0);
            final double pillarNoise = Math.abs(icebergPillarRoofNoise.getValue(x * 1.17, 0, z * 1.17) * 1.5);

            double icebergMaxY = temperatureFactor * depthFactor * Math.min(
                baseNoise * baseNoise * 1.2,
                Math.ceil(pillarNoise * 30) + 11
            );

            if (icebergMaxY < 2)
            {
                placeSeaIce(context, x, z, seaLevel, maxAnnualTemperature);
                return;
            }

            final BlockState packedIce = Blocks.PACKED_ICE.defaultBlockState();
            final BlockState snow = Blocks.SNOW_BLOCK.defaultBlockState();
            final RandomSource random = context.random();
            final double icebergMinY = seaLevel - icebergMaxY - 7;
            icebergMaxY += seaLevel;

            final int snowDepth = 2 + random.nextInt(4);
            final int snowBoundaryY = seaLevel + 18 + random.nextInt(10);

            int placedSnow = 0;
            for (int y = Math.max(startY, (int) icebergMaxY + 1); y >= endY; --y)
            {
                final BlockState state = context.getBlockState(y);
                if ((state.isAir() && y < icebergMaxY && random.nextDouble() > 0.01)
                    || (isWater(state) && y > icebergMinY && y < seaLevel && icebergMinY != 0 && random.nextDouble() > 0.15))
                {
                    if (placedSnow <= snowDepth && y > snowBoundaryY)
                    {
                        context.setBlockState(y, snow);
                        placedSnow++;
                    }
                    else
                    {
                        context.setBlockState(y, packedIce);
                    }
                }
            }
        }
        else
        {
            placeSeaIce(context, x, z, seaLevel, maxAnnualTemperature);
        }
    }

    private void placeSeaIce(SurfaceBuilderContext context, int x, int z, int seaLevel, float maxAnnualTemperature)
    {
        final boolean placeIce;
        final double iceStart = 1.5;
        final double solidIceStart = -0.5;

        if (maxAnnualTemperature < solidIceStart)
        {
            placeIce = true;
        }
        else if (maxAnnualTemperature > iceStart)
        {
            placeIce = false;
        }
        else
        {
            final double tempFactor = Mth.clampedMap(maxAnnualTemperature, iceStart, solidIceStart, 0.3, 0.04);
            placeIce = patternedNoise.noise(x, z) > tempFactor;
        }

        if (placeIce)
        {
            final int y = seaLevel - 1;
            final BlockState state = context.getBlockState(y);
            if (isWater(state))
            {
                context.setBlockState(y, TFCBlocks.SEA_ICE.get().defaultBlockState());
            }
        }
    }

    private static void buildNormalSurface(SurfaceBuilderContext context, int startY, int endY, SurfaceState topState, SurfaceState midState, SurfaceState underState, SurfaceState underWaterState, SurfaceState thinUnderWaterState, SurfaceState topCaveState, SurfaceState midCaveState, SurfaceState underCaveState, int caveHeight, int subsurfaceMinDepth)
    {
        int surfaceDepth = -1;
        int surfaceY = 0;
        boolean underwaterLayer = false;
        boolean firstLayer = false;
        boolean hasPlacedFirstSurface = false;
        SurfaceState surfaceState = NTESurfaceStates.RAW;

        for (int y = startY; y >= endY; --y)
        {
            final BlockState stateAt = context.getBlockState(y);
            if (stateAt.isAir())
            {
                surfaceDepth = -1;
                if (y <= caveHeight)
                {
                    topState = topCaveState;
                    midState = midCaveState;
                    underState = underCaveState;
                }
                else if (hasPlacedFirstSurface)
                {
                    topState = surfaceState;
                    midState = surfaceState;
                    underState = surfaceState;
                }
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
                        }
                        else if (surfaceDepth == -1)
                        {
                            surfaceDepth = 0;
                            context.setBlockState(y, thinUnderWaterState);
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
                        surfaceDepth = calculateAltitudeSlopeSurfaceDepth(context, surfaceY, subsurfaceMinDepth);
                        if (surfaceDepth < -1)
                        {
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
                    hasPlacedFirstSurface = true;
                    surfaceDepth--;
                    context.setBlockState(y, surfaceState);
                    if (surfaceDepth == 0 && firstLayer)
                    {
                        firstLayer = false;
                        surfaceDepth = calculateAltitudeSlopeSurfaceDepth(context, surfaceY, 0);
                        surfaceState = underwaterLayer ? thinUnderWaterState : underState;
                    }
                }
            }
        }
    }

    private static int calculateAltitudeSlopeSurfaceDepth(SurfaceBuilderContext context, int y, int minimumReturnValue)
    {
        return calculateAltitudeSlopeSurfaceDepth(context, y, minimumReturnValue, 5);
    }

    private static void buildTerraceLandSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        int surfaceDepth = -1;
        int firstSurfaceY = Integer.MIN_VALUE;

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
                    surfaceDepth = 0;
                    if (y < context.getSeaLevel() - 1)
                    {
                        buildTerraceWaveCutSurface(context, y);
                    }
                    else if (firstSurfaceY == Integer.MIN_VALUE)
                    {
                        firstSurfaceY = y;
                        buildTerraceCap(context, y, endY);
                    }
                    else
                    {
                        buildTerraceCliffFace(context, y, firstSurfaceY);
                    }
                }
            }
        }
    }

    private static void buildTerraceCap(SurfaceBuilderContext context, int surfaceY, int endY)
    {
        final int seed = Helpers.hash(472893417L, context.pos());
        final boolean thinTopsoil = context.getSlope() > 10d || (seed & 3) == 0;
        final int soilDepth = surfaceY >= context.getSeaLevel() + 9 && !thinTopsoil ? 2 : 1;

        context.setBlockState(surfaceY, NTESurfaceStates.TOP_GRASS_TO_GRAVEL);
        int depth = 1;
        while (depth <= soilDepth && surfaceY - depth >= endY && context.isDefaultBlock(context.getBlockState(surfaceY - depth)))
        {
            context.setBlockState(surfaceY - depth, NTESurfaceStates.MID_DIRT_TO_GRAVEL);
            depth++;
        }
    }

    private static void buildTerraceCliffFace(SurfaceBuilderContext context, int surfaceY, int topY)
    {
        final int depthFromTop = topY - surfaceY;
        final int seed = Helpers.hash(812394771L, context.pos());
        final int seaLevel = context.getSeaLevel();
        final boolean waveCutTalus = surfaceY <= seaLevel + 3 && depthFromTop >= 5;
        final boolean recessedGravel = surfaceY <= seaLevel + 7 && context.getSlope() > 12d && (seed & 7) <= 2;

        if (waveCutTalus || recessedGravel)
        {
            context.setBlockState(surfaceY, NTESurfaceStates.GRAVEL);
        }
    }

    private static void buildTerraceWaveCutSurface(SurfaceBuilderContext context, int surfaceY)
    {
        final int seaLevel = context.getSeaLevel();
        if (surfaceY >= seaLevel - 5)
        {
            context.setBlockState(surfaceY, NTESurfaceStates.GRAVEL);
        }
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

    private static float getMaxAnnualTemperature(OverworldClimateModel model, int z, int y, float averageTemperature)
    {
        final float hemisphereScale = getHemisphereScale(model);
        final float monthlyTemperature = NTEClimateSeasonModel.seasonalTemperatureAmplitude(z, hemisphereScale);
        return OverworldClimateModel.getAdjustedAverageTempByElevation(y, averageTemperature) + monthlyTemperature;
    }

    private static float getHemisphereScale(OverworldClimateModel model)
    {
        if (TEMPERATURE_SCALE_FIELD != null)
        {
            try
            {
                return TEMPERATURE_SCALE_FIELD.getFloat(model);
            }
            catch (IllegalAccessException ignored)
            {
            }
        }
        return DEFAULT_HEMISPHERE_SCALE;
    }

    private static Field resolveTemperatureScaleField()
    {
        try
        {
            final Field field = OverworldClimateModel.class.getDeclaredField("temperatureScale");
            field.setAccessible(true);
            return field;
        }
        catch (ReflectiveOperationException ignored)
        {
            return null;
        }
    }

    private static boolean isWater(BlockState state)
    {
        return state.getBlock() == TFCBlocks.SALT_WATER.get() || state.getBlock() == Blocks.WATER;
    }

    private static Noise2D seaIceNoise(long seed)
    {
        final int cellSeed = HashCommon.long2int(seed);
        final Noise2D wiggle = new OpenSimplex2D(seed).scaled(-0.04, 0.04).spread(0.12);
        return (x, z) -> seaIceCellDifference(cellSeed, x * 0.03, z * 0.03) + wiggle.noise(x, z);
    }

    private static double seaIceCellDifference(int seed, double x, double z)
    {
        final int primeX = 501125321;
        final int primeY = 1136930381;
        final double jitter = 0.21d;
        final int xr = FastNoiseLite.FastFloor(x);
        final int zr = FastNoiseLite.FastFloor(z);

        double nearestDistance = Double.MAX_VALUE;
        double secondNearestDistance = Double.MAX_VALUE;
        int xPrimed = (xr - 1) * primeX;
        final int zPrimedBase = (zr - 1) * primeY;

        for (int xi = xr - 1; xi <= xr + 1; xi++)
        {
            int zPrimed = zPrimedBase;
            for (int zi = zr - 1; zi <= zr + 1; zi++)
            {
                final int hash = FastNoiseLite.Hash(seed, xPrimed, zPrimed);
                final int idx = hash & (255 << 1);
                final double centerX = xi + FastNoiseLite.RandVecs2D[idx] * jitter;
                final double centerZ = zi + FastNoiseLite.RandVecs2D[idx | 1] * jitter;
                final double newDistance = (centerX - x) * (centerX - x) + (centerZ - z) * (centerZ - z);

                secondNearestDistance = Math.max(Math.min(secondNearestDistance, newDistance), nearestDistance);
                if (newDistance < nearestDistance)
                {
                    nearestDistance = newDistance;
                }
                zPrimed += primeY;
            }
            xPrimed += primeX;
        }
        return secondNearestDistance - nearestDistance;
    }

    private static Noise2D lavaFlow(long seed)
    {
        return new OpenSimplex2D(seed + 23891L).ridged().spread(0.01);
    }

    private static Noise2D lavaFlowMaterial(long seed)
    {
        return new OpenSimplex2D(seed).octaves(2).spread(0.25);
    }

    private static final class ShieldVolcanoVariantSurfaceBuilder implements SurfaceBuilder
    {
        private final boolean hasLavaFlows;
        private final boolean isSandy;
        private final Noise2D lavaFlowNoise;
        private final Noise2D lavaFlowMaterialNoise;

        private ShieldVolcanoVariantSurfaceBuilder(long seed, boolean hasLavaFlows, boolean isSandy)
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
                            else
                            {
                                context.setBlockState(y, underWaterState);
                                if (surfaceDepth == -1)
                                {
                                    surfaceDepth = 0;
                                }
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
                            else
                            {
                                context.setBlockState(y, surfaceDepth == -1 ? underState : topState);
                                if (surfaceDepth == -1)
                                {
                                    surfaceDepth = 0;
                                }
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
    }
}
