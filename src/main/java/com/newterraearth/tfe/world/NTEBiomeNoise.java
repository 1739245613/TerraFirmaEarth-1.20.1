package com.newterraearth.tfe.world;

import net.minecraft.util.Mth;

import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;

import com.newterraearth.tfe.config.NTECommonConfig;
import com.newterraearth.tfe.world.noise.NTECellular2D;
import com.newterraearth.tfe.world.region.NTERegionNoise;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

public final class NTEBiomeNoise
{
    private NTEBiomeNoise()
    {
    }

    public static Noise2D badlands(long seed)
    {
        return badlands(seed, 22, 19.5f);
    }

    public static Noise2D badlands(long seed, int height, float depth)
    {
        return new OpenSimplex2D(seed)
            .octaves(4)
            .spread(0.025f)
            .scaled(SEA_LEVEL_Y + height, SEA_LEVEL_Y + height + 10)
            .add(new OpenSimplex2D(seed + 1)
                .octaves(4)
                .spread(0.04f)
                .ridged()
                .map(x -> 1.3f * -(x > 0 ? x * x * x : 0.5f * x))
                .scaled(-1f, 0.3f, -1f, 1f)
                .terraces(15)
                .scaled(-depth, 0)
            )
            .map(x -> x < SEA_LEVEL_Y ? SEA_LEVEL_Y - 0.3f * (SEA_LEVEL_Y - x) : x);
    }

    public static Noise2D stairCanyons(long seed)
    {
        final int minHeight = SEA_LEVEL_Y + 4;
        final int plateauHeight = SEA_LEVEL_Y + 22;
        return stairStepCliffs(seed, canyonBaseNoise(seed, minHeight, plateauHeight, 0.05))
            .add(new OpenSimplex2D(seed).octaves(3).spread(0.08).scaled(-5, 5));
    }

    public static Noise2D mesas(long seed)
    {
        final int minHeight = SEA_LEVEL_Y + 4;
        final int plateauHeight = SEA_LEVEL_Y + 22;
        return stairStepCliffs(seed, canyonBaseNoise(seed, minHeight, plateauHeight, 0.12))
            .add(new OpenSimplex2D(seed).octaves(3).spread(0.08).scaled(-5, 5));
    }

    public static Noise2D buttes(long seed)
    {
        final int minHeight = SEA_LEVEL_Y + 4;
        final int plateauHeight = SEA_LEVEL_Y + 22;
        return stairStepCliffs(seed, canyonBaseNoise(seed, minHeight, plateauHeight, 0.18))
            .add(new OpenSimplex2D(seed).octaves(3).spread(0.08).scaled(-5, 5));
    }

    public static Noise2D hoodoos(long seed)
    {
        final int minHeight = SEA_LEVEL_Y + 4;
        final int maxHeight = SEA_LEVEL_Y + 22;
        final Noise2D maxBaseNoise = canyonBaseNoise(seed, minHeight, maxHeight, 0.03);
        final Noise2D minBaseNoise = canyonBaseNoise(seed, minHeight, maxHeight, 0.20);
        final Noise2D hoodooNoise = clampedScaled(new OpenSimplex2D(seed + 1)
            .octaves(3)
            .spread(0.12f)
            .abs(), 0.20, 0.60, minHeight, maxHeight);
        return stairStepCliffs(seed, max(min(maxBaseNoise, hoodooNoise), minBaseNoise), 5, 8, 7)
            .add(new OpenSimplex2D(seed).octaves(3).spread(0.08).scaled(-3, 3));
    }

    public static Noise2D flats(long seed)
    {
        return new OpenSimplex2D(seed)
            .octaves(4)
            .spread(0.03f)
            .scaled(SEA_LEVEL_Y - 12, SEA_LEVEL_Y + 8)
            .clamped(SEA_LEVEL_Y, SEA_LEVEL_Y + 2);
    }

    public static Noise2D saltFlats(long seed)
    {
        return new OpenSimplex2D(seed)
            .octaves(4)
            .spread(0.05f)
            .scaled(SEA_LEVEL_Y - 16, SEA_LEVEL_Y + 10)
            .clamped(SEA_LEVEL_Y - 2, SEA_LEVEL_Y);
    }

    public static Noise2D dunes(long seed, int minHeight, int maxHeight)
    {
        return new OpenSimplex2D(seed)
            .spread(0.02)
            .scaled(-3, 3)
            .add((x, z) -> x / 6 + 20 * Math.sin(z / 240))
            .map(value -> 1.3 * (Math.abs((value % 5) - 1) * ((value % 5) - (value % 1) > 0 ? 0.5 : 2) - 1))
            .clamped(-1, 1)
            .lazyProduct(new OpenSimplex2D(seed)
                .octaves(4)
                .spread(0.1)
                .scaled(-1, 2)
                .clamped(0.4, 1))
            .scaled(SEA_LEVEL_Y + minHeight, SEA_LEVEL_Y + maxHeight);
    }

    public static Noise2D rockyIslands(long seed)
    {
        final Noise2D baseNoise = new OpenSimplex2D(seed)
            .octaves(4)
            .spread(0.14f)
            .map(x -> {
                final double x0 = 0.125f * (x + 1) * (x + 1) * (x + 1);
                return SEA_LEVEL_Y - 15 + 50 * x0;
            });

        final Noise2D cliffNoise = new OpenSimplex2D(seed + 2)
            .octaves(2)
            .spread(0.01f)
            .scaled(-10, 18)
            .map(x -> x > 0 ? x : 0);
        final Noise2D cliffHeightNoise = new OpenSimplex2D(seed + 3)
            .octaves(2)
            .spread(0.01f)
            .scaled(SEA_LEVEL_Y - 5, SEA_LEVEL_Y + 5);

        return (x, z) -> {
            double height = baseNoise.noise(x, z);
            if (height > SEA_LEVEL_Y - 10)
            {
                final double cliffHeight = cliffHeightNoise.noise(x, z) - height;
                if (cliffHeight < 0)
                {
                    final double mappedCliffHeight = Mth.clampedMap(cliffHeight, 0, -1, 0, 1);
                    height += mappedCliffHeight * cliffNoise.noise(x, z);
                }
            }
            return height;
        };
    }

    public static Noise2D mountains(long seed, int baseHeight, int scaleHeight)
    {
        final Noise2D baseNoise = new OpenSimplex2D(seed)
            .octaves(6)
            .spread(0.14f)
            .add(new OpenSimplex2D(seed + 1)
                .octaves(4)
                .spread(0.02f)
                .scaled(-0.7f, 0.7f)
                .ridged()
            )
            .map(x -> {
                final double x0 = 0.125f * (x + 1) * (x + 1) * (x + 1);
                return SEA_LEVEL_Y + baseHeight + scaleHeight * x0;
            });

        final double cliffMinHeight = Math.max(0, NTECommonConfig.getMountainCliffHeightMin());
        final double cliffMaxHeight = Math.max(cliffMinHeight, NTECommonConfig.getMountainCliffHeightMax());
        if (cliffMaxHeight <= 0)
        {
            return baseNoise;
        }
        final double fadeMinRatio = Math.max(0.01, NTECommonConfig.getMountainCliffFadeRatioMin());
        final double fadeMaxRatio = Math.max(fadeMinRatio, NTECommonConfig.getMountainCliffFadeRatioMax());

        final Noise2D cliffLiftNoise = new OpenSimplex2D(seed + 2)
            .octaves(2)
            .spread(0.01f)
            .scaled(-cliffMaxHeight, cliffMaxHeight)
            .map(value -> value > 0 ? Mth.clampedMap(value, 0, cliffMaxHeight, cliffMinHeight, cliffMaxHeight) : 0);
        final Noise2D cliffThresholdNoise = new OpenSimplex2D(seed + 3)
            .octaves(2)
            .spread(0.01f)
            .scaled(140 - 20, 140 + 20);
        final Noise2D cliffFadeRatioNoise = new OpenSimplex2D(seed + 4)
            .octaves(2)
            .spread(0.01f)
            .scaled(fadeMinRatio, fadeMaxRatio);

        return (x, z) -> {
            double height = baseNoise.noise(x, z);
            if (height > 120)
            {
                final double delta = height - cliffThresholdNoise.noise(x, z);
                if (delta > 0)
                {
                    final double cliffHeight = cliffLiftNoise.noise(x, z);
                    if (cliffHeight > 0)
                    {
                        final double fadeHeight = Math.max(1, cliffHeight * cliffFadeRatioNoise.noise(x, z));
                        final double t = Mth.clamp(delta / fadeHeight, 0, 1);
                        height += t * cliffHeight;
                    }
                }
            }
            return height;
        };
    }

    public static Noise2D sharpHills(long seed)
    {
        return sharpHills(seed, -3, 28);
    }

    public static Noise2D sharpHills(long seed, float minHeight, float maxHeight)
    {
        final Noise2D base = new OpenSimplex2D(seed)
            .octaves(4)
            .spread(0.08f);

        final Noise2D lerp = new OpenSimplex2D(seed + 7198234123L)
            .spread(0.013f)
            .scaled(-0.3f, 1.6f)
            .clamped(0, 1);

        final Noise2D lerpMapped = (x, z) -> {
            final double in = base.noise(x, z);
            return Mth.lerp(lerp.noise(x, z), in, sharpHillsMap(in));
        };

        final OpenSimplex2D variance = new OpenSimplex2D(seed + 67981832123L)
            .octaves(3)
            .spread(0.06f)
            .scaled(-0.2f, 0.2f);

        return lerpMapped
            .add(variance)
            .scaled(-0.75f, 0.7f, SEA_LEVEL_Y - minHeight, SEA_LEVEL_Y + maxHeight);
    }

    public static Noise2D towerKarstPlains(long seed)
    {
        return fenglin(seed, BiomeNoise.hills(seed, 4, 8), 40);
    }

    public static Noise2D towerKarstCanyons(long seed)
    {
        return fengcong(seed, BiomeNoise.canyons(seed, -2, 30));
    }

    public static Noise2D towerKarstHills(long seed)
    {
        return fengcong(seed, BiomeNoise.hills(seed, -5, 22));
    }

    public static Noise2D towerKarstHighlands(long seed)
    {
        return fengcong(seed, sharpHills(seed, 0, 20));
    }

    public static Noise2D towerKarstLake(long seed)
    {
        return fenglin(seed, BiomeNoise.hills(seed, -12, -4), 50);
    }

    public static Noise2D towerKarstBay(long seed)
    {
        return fenglin(seed, BiomeNoise.hills(seed, -18, -8), 50);
    }

    public static Noise2D burrenPlateau(long seed)
    {
        return burren(seed, BiomeNoise.hills(seed, 22, 32), 1.4);
    }

    public static Noise2D burrenBadlands(long seed)
    {
        return burren(seed, badlands(seed, 22, 19.5f), 1.0);
    }

    public static Noise2D burrenBadlandsTall(long seed)
    {
        return burren(seed, badlands(seed, 35, 33f), 1.0);
    }

    public static Noise2D burrenPlains(long seed)
    {
        return burren(seed, BiomeNoise.hills(seed, 6, 12), 1.5);
    }

    public static Noise2D burrenRocheMoutonee(long seed)
    {
        return burren(seed, drumlins(seed), 1.5);
    }

    public static Noise2D shilinPlains(long seed)
    {
        return shilin(seed, BiomeNoise.hills(seed, 4, 10), 28);
    }

    public static Noise2D shilinCanyons(long seed)
    {
        return shilin(seed, BiomeNoise.canyons(seed, -2, 30), 26);
    }

    public static Noise2D shilinHills(long seed)
    {
        return shilin(seed, BiomeNoise.hills(seed, -2, 16), 26);
    }

    public static Noise2D shilinHighlands(long seed)
    {
        return shilin(seed, sharpHills(seed, 0, 32), 32);
    }

    public static Noise2D shilinPlateau(long seed)
    {
        return shilin(seed, BiomeNoise.hills(seed, 12, 22), 32);
    }

    public static Noise2D dolinePlains(long seed)
    {
        return bowlDolines(seed, BiomeNoise.hills(seed, 4, 10), 6);
    }

    public static Noise2D dolineHills(long seed)
    {
        return bowlDolines(seed, BiomeNoise.hills(seed, -5, 16), 10);
    }

    public static Noise2D dolineRollingHills(long seed)
    {
        return bowlDolines(seed, BiomeNoise.hills(seed, -5, 28), 18);
    }

    public static Noise2D dolineHighlands(long seed)
    {
        return bowlDolines(seed, sharpHills(seed, -3, 20), 22);
    }

    public static Noise2D dolinePlateau(long seed)
    {
        return bowlDolines(seed, BiomeNoise.hills(seed, 22, 32), 22);
    }

    public static Noise2D rockyPlateau(long seed)
    {
        return max(
            bowlDolines(seed, BiomeNoise.hills(seed, 22, 32), 16),
            BiomeNoise.canyons(seed, 0, 52).spread(1.5)
        );
    }

    public static Noise2D dolineCanyons(long seed)
    {
        return bowlDolines(seed, BiomeNoise.canyons(seed, -2, 34), 15);
    }

    public static Noise2D cenotePlains(long seed)
    {
        return cenotes(seed, BiomeNoise.hills(seed, 4, 10), 11, 8);
    }

    public static Noise2D cenoteHills(long seed)
    {
        return cenotes(seed, BiomeNoise.hills(seed, -5, 16), 16, 10);
    }

    public static Noise2D cenoteRollingHills(long seed)
    {
        return cenotes(seed, BiomeNoise.hills(seed, -5, 28), 22, 14);
    }

    public static Noise2D cenoteCanyons(long seed)
    {
        return cenotes(seed, BiomeNoise.canyons(seed, 2, 28), 18, 10);
    }

    public static Noise2D cenoteHighlands(long seed)
    {
        return cenotes(seed, sharpHills(seed, 0, 24), 20, 10);
    }

    public static Noise2D cenotePlateau(long seed)
    {
        return cenotes(seed, BiomeNoise.hills(seed, 20, 30), 22, 20);
    }

    public static Noise2D extremeDolinePlateau(long seed)
    {
        return tiankeng(seed, BiomeNoise.hills(seed, 24, 34));
    }

    public static Noise2D extremeDolineMountains(long seed)
    {
        return tiankeng(seed, mountains(seed, 16, 40));
    }

    public static Noise2D iceSheet(long seed)
    {
        return iceSheetSurfaceHeight(seed).add(glacialSurfaceTexture(seed));
    }

    public static Noise2D iceSheetEdge(long seed)
    {
        return glacialBase(seed);
    }

    public static Noise2D iceSheetOceanic(long seed)
    {
        return oceanicIceSheetSurfaceHeight(seed).add(glacialSurfaceTexture(seed));
    }

    public static Noise2D iceSheetMountains(long seed, boolean coastal)
    {
        if (coastal)
        {
            return max(
                oceanicIceSheetSurfaceHeight(seed).add(glacialSurfaceTexture(seed)),
                glacialCirquesIceSurfaceHeight(seed),
                glacialCirques(seed)
            );
        }
        return max(
            montaneIceSheetSurfaceHeight(seed).add(glacialSurfaceTexture(seed)),
            addConstant(glacialCirques(seed), 39),
            addConstant(glacialCirquesIceSurfaceHeight(seed), 39)
        );
    }

    public static Noise2D glaciatedMountains(long seed, boolean coastal)
    {
        if (coastal)
        {
            return max(glacialCirques(seed), glacialCirquesIceSurfaceHeight(seed));
        }
        return max(addConstant(glacialCirques(seed), 39), addConstant(glacialCirquesIceSurfaceHeight(seed), 39));
    }

    public static Noise2D glaciallyCarvedMountains(long seed, boolean coastal)
    {
        return coastal ? glacialCirques(seed) : addConstant(glacialCirques(seed), 39);
    }

    public static Noise2D drumlins(long seed)
    {
        return stretchZ(new OpenSimplex2D(seed).octaves(3).spread(0.04f).scaled(SEA_LEVEL_Y - 16, SEA_LEVEL_Y + 32), 2.5);
    }

    public static Noise2D knobAndKettle(long seed)
    {
        return new OpenSimplex2D(seed).octaves(2).spread(0.03f)
            .map(y -> y > 0.3 ? y - 0.3 : y < -0.3 ? y + 0.3 : 0)
            .scaled(-12, 10)
            .add(BiomeNoise.hills(seed, -3, 3));
    }

    public static Noise2D patternedGround(long seed)
    {
        final NTECellular2D cells = new NTECellular2D(seed, 0.25f, 1).spread(0.05);
        return (x, z) -> {
            final NTECellular2D.Cell cell = cells.cell(x, z);
            return cell.f2() - cell.f1() < 0.12 ? -1 : 0;
        };
    }

    public static Noise2D invertedPatternedGround(long seed)
    {
        final Noise2D base = BiomeNoise.hills(seed, -4, 3);
        final NTECellular2D cells = new NTECellular2D(seed, 0.25f, 1).spread(0.05);
        return (x, z) -> {
            final double height = base.noise(x, z);
            final NTECellular2D.Cell cell = cells.cell(x, z);
            final double f2f1 = cell.f2() - cell.f1();
            if (height >= SEA_LEVEL_Y)
            {
                return f2f1 < 0.12 ? 1 : 0;
            }
            if (f2f1 < 0.12)
            {
                return SEA_LEVEL_Y - 1 - height;
            }
            if (f2f1 < 0.22)
            {
                return SEA_LEVEL_Y - 2 - height;
            }
            return SEA_LEVEL_Y - 3 - height;
        };
    }

    public static Noise2D stoneCircles(long seed)
    {
        final NTECellular2D cells = new NTECellular2D(seed, 0.26f, 1).spread(0.09);
        return (x, z) -> {
            final double f1 = cells.cell(x, z).f1();
            return f1 > 0.06 && f1 < 0.13 ? 1 : 0;
        };
    }

    public static Noise2D activeShieldVolcano(long seed)
    {
        return activeShieldVolcano(seed, NTERegionNoise.activeHotSpots(seed));
    }

    public static Noise2D dormantShieldVolcano(long seed)
    {
        return dormantShieldVolcano(seed, NTERegionNoise.dormantHotSpots(seed));
    }

    public static Noise2D extinctShieldVolcano(long seed)
    {
        return extinctShieldVolcano(seed, NTERegionNoise.extinctHotSpots(seed));
    }

    public static Noise2D ancientShieldVolcano(long seed)
    {
        return ancientShieldVolcano(seed, 90, 130, NTERegionNoise.ancientHotSpots(seed));
    }

    public static Noise2D sunkenShieldVolcano(long seed)
    {
        return sunkenShieldVolcano(seed, NTERegionNoise.ancientHotSpots(seed));
    }

    public static Noise2D glaciatedShieldVolcano(long seed)
    {
        return glaciatedShieldVolcano(seed, NTERegionNoise.hotSpotIntensity(seed));
    }

    public static Noise2D shieldVolcanoIceSheetSurface(long seed)
    {
        return shieldVolcanoIceSheetSurface(seed, NTERegionNoise.hotSpotIntensity(seed));
    }

    public static Noise2D shieldVolcanoGlacierSurface(long seed)
    {
        return shieldVolcanoGlacierSurface(seed, NTERegionNoise.hotSpotIntensity(seed));
    }

    public static Noise2D iceSheetShieldVolcanoTerrain(long seed)
    {
        return max(glaciatedShieldVolcano(seed), shieldVolcanoIceSheetSurface(seed).add(glacialSurfaceTexture(seed)));
    }

    public static Noise2D glaciatedShieldVolcanoTerrain(long seed)
    {
        return max(glaciatedShieldVolcano(seed), shieldVolcanoGlacierSurface(seed).add(glacialSurfaceTexture(seed)));
    }

    private static Noise2D connectedValleyBaseNoise(long seed)
    {
        return new OpenSimplex2D(seed).spread(0.0025);
    }

    private static Noise2D canyonBaseNoise(long seed, int minHeight, int maxHeight, double valleyWidth)
    {
        final double valleyEdge = valleyWidth + 0.28;
        return clampedScaled(new OpenSimplex2D(seed + 1)
            .octaves(4)
            .spread(0.03f)
            .abs(), valleyWidth, valleyEdge, minHeight, maxHeight);
    }

    private static Noise2D stairStepCliffs(long seed, Noise2D input)
    {
        return stairStepCliffs(seed, input, 5, 12, 7);
    }

    private static Noise2D stairStepCliffs(long seed, Noise2D input, int minCliffStart, int maxCliffStart, int cliffHeight)
    {
        final Noise2D cliffStartHeightNoise = new OpenSimplex2D(seed + 3).octaves(2).spread(0.008f).scaled(SEA_LEVEL_Y + minCliffStart, SEA_LEVEL_Y + maxCliffStart);
        final Noise2D cliffNoise = new OpenSimplex2D(seed + 7).spread(0.003f).scaled(-cliffHeight, 2d * cliffHeight).clamped(0, cliffHeight);
        final Noise2D doubleCliffNoise = cliffNoise.add(cliffNoise);

        final Noise2D secondCliffStartHeightNoise = cliffStartHeightNoise.add(doubleCliffNoise);
        final Noise2D secondCliffNoise = new OpenSimplex2D(seed + 19).spread(0.003f).scaled(-cliffHeight, 2d * cliffHeight).clamped(0, cliffHeight);

        final Noise2D thirdCliffStartHeightNoise = secondCliffStartHeightNoise.add(doubleCliffNoise);
        final Noise2D thirdCliffNoise = new OpenSimplex2D(seed + 25).spread(0.003f).scaled(-cliffHeight, 2d * cliffHeight).clamped(0, cliffHeight);

        final Noise2D slopeNoise = new OpenSimplex2D(seed + 33).spread(0.008).scaled(-2, 2);
        final Noise2D cliffSlope = cliffNoise.scaled(0, 7, 3, 6).add(slopeNoise);

        return slopedCliffMap(
            slopedCliffMap(
                slopedCliffMap(input, cliffStartHeightNoise, cliffNoise, cliffSlope),
                secondCliffStartHeightNoise, secondCliffNoise, cliffSlope),
            thirdCliffStartHeightNoise, thirdCliffNoise, cliffSlope);
    }

    private static Noise2D lavaFlow(long seed)
    {
        return new OpenSimplex2D(seed + 23891L).ridged().spread(0.01);
    }

    private static Noise2D activeShieldVolcano(long seed, Noise2D hotspot)
    {
        final double edgeElev = SEA_LEVEL_Y + 1;
        final double calderaEdgeElev = SEA_LEVEL_Y + 115;
        final double cliffEdgeElev = SEA_LEVEL_Y + 90;
        final double calderaCenterElev = SEA_LEVEL_Y + 60;

        final Noise2D volcano = hotspot.map(y ->
            y < 0.75 ? Mth.map(y, 0, 0.75, edgeElev, calderaEdgeElev)
                : y < 0.78 ? Mth.map(y, 0.75, 0.78, calderaEdgeElev, cliffEdgeElev)
                : Mth.map(y, 0.78, 1, cliffEdgeElev, calderaCenterElev));

        final Noise2D flows = lavaFlow(seed).map(y -> y < 0.45 ? 0 : 1);
        final OpenSimplex2D warp = new OpenSimplex2D(seed).octaves(4).spread(0.03f).scaled(-100f, 100f);
        final Noise2D surface = new OpenSimplex2D(seed + 1)
            .octaves(4)
            .spread(0.06f)
            .warped(warp)
            .map(x -> x > 0.4 ? x - 0.8f : -x)
            .scaled(-0.4f, 0.8f, -8, 8);

        return volcano.add(flows).add(surface);
    }

    private static Noise2D dormantShieldVolcano(long seed, Noise2D hotspot)
    {
        final double seaElev = SEA_LEVEL_Y + 9;
        final double mountainBaseElev = SEA_LEVEL_Y + 40;
        final double calderaEdgeElev = SEA_LEVEL_Y + 70;
        final double cliffEdgeElev = SEA_LEVEL_Y + 50;
        final double calderaCenterElev = SEA_LEVEL_Y + 15;

        final Noise2D volcano = hotspot.map(y ->
            y < 0.45 ? Mth.map(y, 0, 0.45, seaElev, mountainBaseElev)
                : y < 0.7 ? Mth.map(y, 0.45, 0.7, mountainBaseElev, calderaEdgeElev)
                : y < 0.73 ? Mth.map(y, 0.7, 0.73, calderaEdgeElev, cliffEdgeElev)
                : y < 0.85 ? Mth.map(y, 0.73, 0.85, cliffEdgeElev, calderaCenterElev) : calderaCenterElev);
        final OpenSimplex2D warp = new OpenSimplex2D(seed + 43L).octaves(4).spread(0.03f).scaled(-100f, 100f);
        final Noise2D surface = new OpenSimplex2D(seed + 44L)
            .octaves(4)
            .spread(0.06f)
            .warped(warp)
            .map(x -> x > 0.4 ? x - 0.8f : -x)
            .scaled(-0.4f, 0.8f, -12, 12);

        return volcano.add(surface);
    }

    private static Noise2D extinctShieldVolcano(long seed, Noise2D hotspot)
    {
        final double seaElev = SEA_LEVEL_Y + 6;
        final double mountainBaseElev = SEA_LEVEL_Y + 25;
        final double calderaEdgeElev = SEA_LEVEL_Y + 55;
        final double cliffEdgeElev = SEA_LEVEL_Y + 25;
        final double calderaCenterElev = SEA_LEVEL_Y - 10;

        final Noise2D volcano = hotspot.map(y ->
            y < 0.4 ? Mth.map(y, 0, 0.4, seaElev, mountainBaseElev)
                : y < 0.6 ? Mth.map(y, 0.4, 0.6, mountainBaseElev, calderaEdgeElev)
                : y < 0.62 ? Mth.map(y, 0.6, 0.62, calderaEdgeElev, cliffEdgeElev)
                : y < 0.75 ? Mth.map(y, 0.62, 0.75, cliffEdgeElev, calderaCenterElev) : calderaCenterElev);
        final OpenSimplex2D warp = new OpenSimplex2D(seed + 43L).octaves(4).spread(0.03f).scaled(-100f, 100f);
        final Noise2D surface = new OpenSimplex2D(seed + 44L)
            .octaves(4)
            .spread(0.06f)
            .warped(warp)
            .map(x -> x > 0.4 ? x - 0.8f : -x)
            .scaled(-0.4f, 0.8f, -9, 9);

        return volcano.add(surface);
    }

    private static Noise2D glaciatedShieldVolcano(long seed, Noise2D hotspot)
    {
        final double seaElev = SEA_LEVEL_Y + 15;
        final double mountainBaseElev = SEA_LEVEL_Y + 70;
        final double calderaEdgeElev = SEA_LEVEL_Y + 100;
        final double cliffEdgeElev = SEA_LEVEL_Y + 60;
        final double calderaCenterElev = SEA_LEVEL_Y + 50;

        final Noise2D volcano = hotspot.map(y ->
            y < 0.45 ? Mth.map(y, 0, 0.45, seaElev, mountainBaseElev)
                : y < 0.72 ? Mth.map(y, 0.45, 0.72, mountainBaseElev, calderaEdgeElev)
                : y < 0.74 ? Mth.map(y, 0.72, 0.74, calderaEdgeElev, cliffEdgeElev)
                : y < 0.85 ? Mth.map(y, 0.74, 0.85, cliffEdgeElev, calderaCenterElev) : calderaCenterElev);

        final OpenSimplex2D warp = new OpenSimplex2D(seed + 43L).octaves(4).spread(0.03f).scaled(-100f, 100f);
        final Noise2D surface = new OpenSimplex2D(seed + 44L)
            .octaves(4)
            .spread(0.02f)
            .warped(warp)
            .map(x -> x > 0.4 ? x - 0.8f : -x)
            .scaled(-0.4f, 0.8f, -48, 32);

        return volcano.add(surface);
    }

    private static Noise2D shieldVolcanoIceSheetSurface(long seed, Noise2D hotspot)
    {
        final double edgeElev = 0;
        final double calderaCenterElev = 51;
        return hotspot.map(y -> y < 0.9 ? Mth.map(y, 0.0, 0.9, edgeElev, calderaCenterElev) : calderaCenterElev)
            .add(iceSheetSurfaceHeight(seed));
    }

    private static Noise2D shieldVolcanoGlacierSurface(long seed, Noise2D hotspot)
    {
        final Noise2D base = new OpenSimplex2D(seed).octaves(3).spread(0.05f).scaled(-5, 5);
        final double lowElev = SEA_LEVEL_Y - 60;
        final double edgeElev = SEA_LEVEL_Y + 75;
        final double calderaRimElev = SEA_LEVEL_Y + 92;
        final double calderaCenterElev = SEA_LEVEL_Y + 98;

        return hotspot.map(y ->
            y < 0.40 ? lowElev
                : y < 0.58 ? Mth.map(y, 0.40, 0.58, lowElev, edgeElev)
                : y < 0.72 ? Mth.map(y, 0.58, 0.72, edgeElev, calderaRimElev)
                : y < 0.9 ? Mth.map(y, 0.72, 0.9, calderaRimElev, calderaCenterElev) : calderaCenterElev)
            .add(base);
    }

    private static Noise2D sunkenShieldVolcano(long seed, Noise2D hotspot)
    {
        final Noise2D volcano = hotspot.map(y ->
            y < 0.25 ? 50
                : y < 0.45 ? Mth.map(y, 0.25, 0.45, 50, SEA_LEVEL_Y)
                : y < 0.6 ? Mth.map(y, 0.45, 0.6, SEA_LEVEL_Y, 95)
                : y < 0.62 ? Mth.map(y, 0.6, 0.62, 94, 80)
                : y < 0.75 ? Mth.map(y, 0.62, 0.75, 80, 52) : 52);
        final OpenSimplex2D warp = new OpenSimplex2D(seed + 43L).octaves(4).spread(0.03f).scaled(-100f, 100f);
        final Noise2D surface = new OpenSimplex2D(seed + 44L)
            .octaves(4)
            .spread(0.06f)
            .warped(warp)
            .map(x -> x > 0.4 ? x - 0.8f : -x)
            .scaled(-0.4f, 0.8f, -6, 6);

        final Noise2D scale = new OpenSimplex2D(seed + 789913L).octaves(2).spread(0.008f).scaled(0.45, 1);
        return volcano.lazyProduct(scale).add(surface);
    }

    private static Noise2D ancientShieldVolcano(long seed, double minElevation, double maxElevation, Noise2D hotspot)
    {
        final Noise2D volcano = hotspot.map(y ->
            y < 0.15 ? 90
                : y < 0.6 ? Mth.map(y, 0.15, 0.6, 90, 130)
                : y < 0.63 ? Mth.map(y, 0.6, 0.63, 129, 108)
                : y < 0.7 ? Mth.map(y, 0.63, 0.7, 108, 90) : 90);
        final OpenSimplex2D warp = new OpenSimplex2D(seed + 43L).octaves(4).spread(0.03f).scaled(-100f, 100f);
        final Noise2D surface = new OpenSimplex2D(seed + 44L)
            .octaves(4)
            .spread(0.06f)
            .warped(warp)
            .map(x -> x > 0.4 ? x - 0.8f : -x)
            .scaled(-0.4f, 0.8f, -20, 0);

        final Noise2D valleys = new OpenSimplex2D(seed + 90183L).spread(0.01).ridged().octaves(3).scaled(maxElevation * 2.2, minElevation);
        final Noise2D scale = new OpenSimplex2D(seed + 789913L).octaves(2).spread(0.008f).scaled(0.6, 1);
        return min(scale.lazyProduct(volcano), valleys).add(surface);
    }

    private static Noise2D connectedValleyNoise(long seed)
    {
        return connectedValleyBaseNoise(seed).abs();
    }

    private static Noise2D glacialSurfaceTexture(long seed)
    {
        final Noise2D warp = new OpenSimplex2D(seed + 413L).spread(0.02).scaled(-12, 12);
        return (x, z) -> {
            final double yOfX = Math.min(Helpers.triangle(25f, 18f, 0.035f, (float) (x + warp.noise(x, z))), 0.0);
            final double yOfZ = Math.min(Helpers.triangle(40f, 30f, 0.025f, (float) (z + warp.noise(z, x))), 0.0);
            return Math.min(yOfX, yOfZ);
        };
    }

    public static Noise2D glacialBase(long seed)
    {
        return addConstant(knobAndKettle(seed), 1.5);
    }

    public static Noise2D iceSheetSurfaceHeight(long seed)
    {
        return BiomeNoise.hills(seed, 23, 38);
    }

    public static Noise2D montaneIceSheetSurfaceHeight(long seed)
    {
        return BiomeNoise.hills(seed, 40, 48);
    }

    public static Noise2D oceanicIceSheetSurfaceHeight(long seed)
    {
        return BiomeNoise.hills(seed, 18, 26);
    }

    public static Noise2D glacialCirquesIceSurfaceHeight(long seed)
    {
        return connectedValleyNoise(seed)
            .map(y -> y < 0.38 ? -100 : y < 0.43 ? Mth.map(y, 0.38, 0.43, -50, 0) : Mth.map(y, 0.43, 1, 0, 32))
            .add(BiomeNoise.hills(seed, 15, 23).add(glacialCirquesCliffsScale(seed)));
    }

    private static Noise2D glacialCirquesCliffsStartHeight(long seed)
    {
        return connectedValleyNoise(seed)
            .map(y -> y < 0.43 ? Mth.map(y, 0, 0.43, 32, 0) : Mth.map(y, 0.43, 1, 0, 32))
            .add(BiomeNoise.hills(seed, 18, 26));
    }

    private static Noise2D glacialCirquesCliffsScale(long seed)
    {
        return new OpenSimplex2D(seed + 78267L).spread(0.015).add(glacialValleyShapeNoise(seed)).scaled(-10, 8).clamped(0, 7);
    }

    private static Noise2D glacialValleyShapeNoise(long seed)
    {
        return connectedValleyBaseNoise(seed)
            .map(y -> Math.min(6 * y * y, 0.75 + 0.25 * y))
            .add(new OpenSimplex2D(seed + 5287L).octaves(4).spread(0.06).scaled(-0.2, 0.2));
    }

    public static Noise2D glacialCirques(long seed)
    {
        final Noise2D shape = glacialValleyShapeNoise(seed);
        final Noise2D shapeMap = connectedValleyNoise(seed);

        final double cellScale = 0.010;
        final NTECellular2D cells = new NTECellular2D(seed, 2).spread(cellScale);
        final Noise2D warp = new OpenSimplex2D(seed).spread(0.02).add(shapeMap).scaled(-1, 2, -0.25, 0.2);
        final Noise2D roughPeaks = new OpenSimplex2D(seed).octaves(3).spread(0.08).scaled(0.6, 1.6);

        final Noise2D cliffScale = new OpenSimplex2D(seed + 785267L).spread(0.01).scaled(-12, 15).clamped(0, 10);
        final Noise2D cliffStartHeight = addConstant(oceanicIceSheetSurfaceHeight(seed), -8);

        final Noise2D cirques = (x, z) -> {
            final NTECellular2D.Cell cell = cells.cell(x, z);
            final double f1 = cell.f1();
            final double f2 = cell.f2();
            final double f2f1 = f1 > 0 ? (f2 - f1) : 1;

            final double shapeAtCenter = shapeMap.noise(cell.cx() / cellScale, cell.cy() / cellScale);
            if (shapeAtCenter > 0.60)
            {
                double y = f2f1 + warp.noise(x, z);
                final double rough = roughPeaks.noise(x, z);
                final double scale = Math.min(Helpers.lerp(2 * y, 1.0, rough), rough);
                y = 1 + scale * y;
                return y;
            }

            double y = 1 - (f2f1 - warp.noise(x, z));
            y = 0.5 * (1 + y * y);

            final double shapeAtPoint = shapeMap.noise(x, z);
            final double valleyCloseness = Math.min(shapeAtPoint - shapeAtCenter, 0);
            return y + Mth.clampedMap(f2f1, 0, 0.1, 0, valleyCloseness);
        };

        Noise2D terrain = addConstant(cirques.scaled(0, 1, 12, 64).lazyProduct(shape), SEA_LEVEL_Y - 15);
        terrain = cliffMap(terrain, cliffStartHeight, cliffScale);
        terrain = cliffMap(terrain, glacialCirquesCliffsStartHeight(seed), glacialCirquesCliffsScale(seed));
        return terrain;
    }

    private static Noise2D burren(long seed, Noise2D baseTerrainNoise, double scale)
    {
        final int minHeight = SEA_LEVEL_Y + 2;
        final Noise2D crevices = burrenCrevices(seed).map(y -> y < 0.15 ? -scale : y < 0.4 ? (y - 0.4) * scale : 0);
        return crevices.add(baseTerrainNoise).map(y -> Math.max(y, minHeight));
    }

    private static Noise2D burrenCrevices(long seed)
    {
        return new OpenSimplex2D(seed + 398767567L)
            .octaves(2)
            .spread(0.08f)
            .abs();
    }

    private static Noise2D shilin(long seed, Noise2D baseTerrainNoise, double scale)
    {
        final int minHeight = SEA_LEVEL_Y + 2;
        final Noise2D ridges = shilinRidges(seed);
        final Noise2D bumps = new OpenSimplex2D(seed + 83436545633L).spread(0.16).scaled(0.6, 1.0);
        return max(ridges.lazyProduct(bumps).scaled(SEA_LEVEL_Y, SEA_LEVEL_Y + scale), baseTerrainNoise).map(y -> Math.max(y, minHeight));
    }

    public static Noise2D shilinRidges(long seed)
    {
        final double widthTop = 0.1;
        final double widthBottom = 0.2;

        final Noise2D ridges = new OpenSimplex2D(seed + 398767567L)
            .octaves(2)
            .spread(0.06f)
            .map(y -> {
                final double abs = Math.abs(y);
                return abs < widthTop ? 1 : abs < widthBottom ? 1 + (0.67 * (abs - widthTop) / (widthTop - widthBottom)) : 0;
            });

        final Noise2D cuts = new OpenSimplex2D(seed + 45764379L)
            .octaves(2)
            .spread(0.03f)
            .map(y -> {
                final double abs = Math.abs(y);
                final double mapped = abs < widthTop * 0.65 ? 1 : abs < widthBottom * 1.2 ? 1 + ((abs - widthTop * 0.65) / (widthTop * 0.65 - widthBottom * 1.2)) : 0;
                return 1 - mapped;
            });

        return ridges.lazyProduct(cuts);
    }

    private static Noise2D fengcong(long seed, Noise2D baseTerrainNoise)
    {
        final double scale = 37;
        final Noise2D cones = new OpenSimplex2D(seed)
            .octaves(3)
            .spread(0.06)
            .map(y -> {
                double value = -0.5 * Math.cos(Math.PI * Math.abs(y)) + 0.5;
                value = (Math.max(value, 0.25) - 0.25) / 0.75;
                return scale * value;
            });
        return baseTerrainNoise.add(cones);
    }

    private static Noise2D fenglin(long seed, Noise2D baseTerrainNoise, double scale)
    {
        final Noise2D cliffScale = new OpenSimplex2D(seed + 78535267L).spread(0.06).scaled(0, 0.25);
        final Noise2D cliffStartHeight = new OpenSimplex2D(seed + 390798L).spread(0.06).scaled(0, 0.7);
        final Noise2D cliffBase = new OpenSimplex2D(seed)
            .octaves(2)
            .spread(0.05)
            .map(y -> {
                final double value = Math.abs(y) - 0.45;
                return value > 0 ? Math.sqrt(value / 0.55) : 0;
            });

        return baseTerrainNoise.add(fenglinCliffMap(cliffBase, cliffStartHeight, cliffScale).map(y -> scale * y));
    }

    private static Noise2D bowlDolines(long seed, Noise2D baseTerrainNoise, double scale)
    {
        final Noise2D bowls = new OpenSimplex2D(seed)
            .octaves(3)
            .spread(0.72 / scale)
            .map(y -> {
                double value = -0.5 * Math.cos(Math.PI * y) + 0.5;
                value = Math.max(value, 0.1) - 0.1;
                return -scale * value;
            });
        return baseTerrainNoise.add(bowls);
    }

    private static Noise2D cenotes(long seed, Noise2D baseTerrainNoise, double verticalScale, double horizontalScale)
    {
        final Noise2D cliffScale = new OpenSimplex2D(seed + 78535267L).spread(0.72 / horizontalScale).scaled(0, 0.4);
        final Noise2D cliffStartHeight = new OpenSimplex2D(seed + 390798L).spread(0.72 / horizontalScale).scaled(0, 0.7);
        final Noise2D cliffBase = new OpenSimplex2D(seed)
            .octaves(2)
            .spread(0.6 / horizontalScale)
            .map(y -> {
                final double value = Math.abs(y) - 0.45;
                return value > 0 ? Math.sqrt(value / 0.55) : 0;
            });

        return baseTerrainNoise.add(fenglinCliffMap(cliffBase, cliffStartHeight, cliffScale).map(y -> -verticalScale * y));
    }

    private static Noise2D tiankeng(long seed, Noise2D baseTerrainNoise)
    {
        final Noise2D cliffScale = new OpenSimplex2D(seed + 78535267L).spread(0.04).scaled(0, 0.04);
        final Noise2D cliffStartHeight = new OpenSimplex2D(seed + 390798L).spread(0.04).scaled(0, 0.7);

        final Noise2D wideCliffBase = new OpenSimplex2D(seed)
            .octaves(2)
            .spread(0.02)
            .map(y -> {
                final double value = Math.abs(y) - 0.3;
                return value > 0 ? Math.sqrt(value / 0.7) : 0;
            });

        final Noise2D deepCliffBase = new OpenSimplex2D(seed)
            .octaves(2)
            .spread(0.02)
            .map(y -> {
                final double value = Math.abs(y) - 0.65;
                return value > 0 ? Math.sqrt(value / 0.35) : 0;
            });

        return (x, z) -> {
            final double compare = cliffStartHeight.noise(x, z);
            final double addend = cliffScale.noise(x, z);
            final double wideBase = wideCliffBase.noise(x, z);
            final double deepBase = deepCliffBase.noise(x, z);

            return baseTerrainNoise.noise(x, z)
                - 22 * (wideBase > compare ? wideBase * (1 - addend) + addend : wideBase)
                - 24 * (deepBase > compare ? deepBase * (1 - addend) + addend : deepBase);
        };
    }

    private static Noise2D fenglinCliffMap(Noise2D baseNoise, Noise2D compareNoise, Noise2D addendNoise)
    {
        return (x, z) -> {
            final double base = baseNoise.noise(x, z);
            if (base > compareNoise.noise(x, z))
            {
                final double addend = addendNoise.noise(x, z);
                return base * (1 - addend) + addend;
            }
            return base;
        };
    }

    private static Noise2D addConstant(Noise2D input, double value)
    {
        return (x, z) -> input.noise(x, z) + value;
    }

    private static Noise2D clampedScaled(Noise2D input, double oldMin, double oldMax, double min, double max)
    {
        final double scale = (max - min) / (oldMax - oldMin);
        final double shift = min - oldMin * scale;
        return (x, z) -> Mth.clamp(input.noise(x, z) * scale + shift, min, max);
    }

    private static Noise2D cliffMap(Noise2D base, Noise2D compareNoise, Noise2D addendNoise)
    {
        return (x, z) -> {
            final double value = base.noise(x, z);
            if (value > compareNoise.noise(x, z))
            {
                return value + addendNoise.noise(x, z);
            }
            return value;
        };
    }

    private static Noise2D slopedCliffMap(Noise2D base, Noise2D compareNoise, Noise2D addendNoise, Noise2D slopeNoise)
    {
        return (x, z) -> {
            final double noise = base.noise(x, z);
            final double compare = compareNoise.noise(x, z);
            final double addend = addendNoise.noise(x, z);
            final double slope = slopeNoise.noise(x, z);
            if (noise > compare + addend)
            {
                return noise + addend;
            }
            if (noise > compare)
            {
                return noise + Math.min((noise - compare) * slope, addend);
            }
            return noise;
        };
    }

    private static Noise2D max(Noise2D first, Noise2D second)
    {
        return (x, z) -> Math.max(first.noise(x, z), second.noise(x, z));
    }

    private static Noise2D max(Noise2D first, Noise2D second, Noise2D third)
    {
        return (x, z) -> Math.max(Math.max(first.noise(x, z), second.noise(x, z)), third.noise(x, z));
    }

    private static Noise2D min(Noise2D first, Noise2D second)
    {
        return (x, z) -> Math.min(first.noise(x, z), second.noise(x, z));
    }

    private static Noise2D stretchZ(Noise2D input, double factor)
    {
        return (x, z) -> input.noise(x, z / factor);
    }

    private static double clamp01(double value)
    {
        return Mth.clamp(value, 0d, 1d);
    }

    private static double square(double value)
    {
        return value * value;
    }

    private static double sharpHillsMap(double in)
    {
        final double in0 = 1.0f, in1 = 0.67f, in2 = 0.15f, in3 = -0.15f, in4 = -0.67f, in5 = -1.0f;
        final double out0 = 1.0f, out1 = 0.7f, out2 = 0.5f, out3 = -0.5f, out4 = -0.7f, out5 = -1.0f;

        if (in > in1)
            return Mth.map(in, in1, in0, out1, out0);
        if (in > in2)
            return Mth.map(in, in2, in1, out2, out1);
        if (in > in3)
            return Mth.map(in, in3, in2, out3, out2);
        if (in > in4)
            return Mth.map(in, in4, in3, out4, out3);
        return Mth.map(in, in5, in4, out5, out4);
    }
}
