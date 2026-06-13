package com.newterraearth.tfe.world.shore;

import net.minecraft.util.Mth;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.Noise3D;
import net.dries007.tfc.world.noise.OpenSimplex2D;

import com.newterraearth.tfe.world.NTEBiomeExtensionAccess;
import com.newterraearth.tfe.world.NTESeed;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

public final class NTEShoreNoise
{
    private NTEShoreNoise()
    {
    }

    public static NTEShoreNoiseSampler sandyBeach(NTESeed seed)
    {
        return new NTEShoreNoiseSampler()
        {
            private final Noise2D tideNoise = NTEShoreNoiseHelpers.shoreTideLevelNoise(seed);

            private double sandHeight;
            private double weight;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                sandHeight = simpleBeach(tideNoise, x, z, heightIn, landWeight, oceanWeight);
                weight = thisWeight;
                return sandHeight;
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                if (yIn <= sandHeight || weight > 0.5)
                {
                    return 0;
                }

                final double heightMultiplier = Mth.clamp((yIn - sandHeight) / 8, 0, 1);
                return heightMultiplier * Mth.map(weight, 0, 0.5, 0.7, 0);
            }
        };
    }

    public static NTEShoreNoiseSampler setbackCliffs(NTESeed seed)
    {
        return new NTEShoreNoiseSampler()
        {
            private final Noise2D bankNoise = new OpenSimplex2D(seed.seed()).spread(0.03).abs().scaled(-1, 8);
            private final Noise3D cliffNoise = NTEShoreNoiseHelpers.cliffNoise(seed);
            private final Noise2D tideNoise = NTEShoreNoiseHelpers.shoreTideLevelNoise(seed);

            private final double cliffBaseWeight = 0.25;
            private final double cliffTopWeight = 0.45;

            private double bankHeight;
            private double landWeight;
            private int x;
            private int z;
            private double yTop;
            private double sandHeight;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                this.x = x;
                this.z = z;
                this.landWeight = landWeight;
                this.sandHeight = simpleBeachNoLandBlend(tideNoise, x, z, oceanWeight);
                this.yTop = heightIn;
                final double tideLevelAtOcean = tideNoise.noise(x, z) - 4;

                final double shoreBankHeight = bankNoise.noise(x, z) + sandHeight;
                if (landWeight >= cliffBaseWeight)
                {
                    bankHeight = shoreBankHeight;
                    return heightIn;
                }

                if (oceanWeight > 0)
                {
                    bankHeight = Mth.clampedMap(oceanWeight, 0, 0.25, shoreBankHeight, tideLevelAtOcean);
                }
                else
                {
                    bankHeight = shoreBankHeight;
                }
                bankHeight = Mth.clampedMap(thisWeight + landWeight / 2, 0.5, 1, sandHeight, bankHeight);
                return bankHeight;
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                if (yIn <= bankHeight)
                {
                    return 0;
                }
                if (landWeight >= cliffBaseWeight)
                {
                    final double percentHeight = Mth.clampedMap(yIn, bankHeight, yTop, 0.30, 1);
                    final double percentDepth = Mth.clampedMap(landWeight, cliffBaseWeight, cliffTopWeight, 0, 1);
                    final double cliffNoiseValue = edgeFunction(percentHeight) * edgeFunction(percentDepth) * cliffNoise.noise(x, yIn, z);
                    return (percentHeight - cliffNoiseValue - percentDepth) * 3;
                }
                return 0.7;
            }

            private double edgeFunction(double input)
            {
                return Math.min(3 - 6 * Math.abs(input - 0.5), 1);
            }
        };
    }

    public static NTEShoreNoiseSampler dunes(NTESeed seed)
    {
        return new NTEShoreNoiseSampler()
        {
            private final OpenSimplex2D warpNoise = new OpenSimplex2D(seed.seed()).scaled(-10, 10).spread(0.05);
            private final Noise2D duneNoise = new OpenSimplex2D(seed.seed()).spread(0.03).abs().scaled(-2, 6.5).warped(warpNoise);
            private final Noise2D tideNoise = NTEShoreNoiseHelpers.shoreTideLevelNoise(seed);

            private double duneHeight;
            private double sandHeight;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                sandHeight = simpleBeach(tideNoise, x, z, heightIn, landWeight, oceanWeight);
                final double tideLevelAtOcean = tideNoise.noise(x, z) - 4;

                final double fullDuneHeight = duneNoise.noise(x, z) + sandHeight;
                if (oceanWeight > 0)
                {
                    duneHeight = Mth.clampedMap(oceanWeight, 0, 0.25, fullDuneHeight, tideLevelAtOcean);
                }
                else
                {
                    duneHeight = fullDuneHeight;
                }
                duneHeight = Mth.clampedMap(thisWeight, 0.5, 1, sandHeight, duneHeight);
                return duneHeight;
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                if (yIn <= duneHeight)
                {
                    return Mth.clampedMap(yIn, sandHeight, duneHeight, -0.5, 0);
                }
                return 1.3;
            }
        };
    }

    public static NTEShoreNoiseSampler embayments(NTESeed seed)
    {
        return new NTEShoreNoiseSampler()
        {
            private final Noise2D rockShelfIntensity = new OpenSimplex2D(seed.seed() + 5252L).octaves(3).spread(0.03).scaled(-0.8, 1);
            private final Noise2D steepnessNoise = new OpenSimplex2D(seed.seed() + 224L).octaves(2).spread(0.11).scaled(-0.7, 0.7, 0, 0.3).clamped(0, 0.3);
            private final Noise2D roughnessNoise = new OpenSimplex2D(seed.seed()).octaves(3).spread(0.09).scaled(-3, 3);
            private final Noise2D tidepoolNoise = new OpenSimplex2D(seed.seed() + 492L).octaves(3).spread(0.09).scaled(-1, 0, -5, 0).clamped(-5, 0);
            private final Noise3D caveNoise = NTEShoreNoiseHelpers.cliffNoise(seed).scaled(0, 0.12);
            private final Noise2D tideNoise = NTEShoreNoiseHelpers.shoreTideLevelNoise(seed);

            private final double topShelfEdge = 0.7;
            private final double midShelfEdge = 0.35;

            private int x;
            private int z;
            private double tideLevel;
            private double sandHeight;
            private double shelfProgress;
            private double height;
            private double midShelfHeight;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                tideLevel = tideNoise.noise(x, z);
                sandHeight = simpleBeach(tideLevel, heightIn, landWeight, oceanWeight);
                this.x = x;
                this.z = z;

                final double roughness = roughnessNoise.noise(x, z) + tidepoolNoise.noise(x, z);
                final double topShelfHeight = SEA_LEVEL_Y + roughness + 15;
                midShelfHeight = SEA_LEVEL_Y + roughness + 9;

                final double baseRockShelfNoise = rockShelfIntensity.noise(x, z);
                if (oceanWeight > 0.10)
                {
                    shelfProgress = Mth.clampedMap(oceanWeight, 0.1, 0.3, baseRockShelfNoise, baseRockShelfNoise - oceanWeight * 3);
                }
                else
                {
                    final double landInfluence = Mth.clampedMap(landWeight, 0.4, 0, baseRockShelfNoise + landWeight * 2, baseRockShelfNoise);
                    shelfProgress = Mth.clampedMap(oceanWeight, 0, 0.1, baseRockShelfNoise + landInfluence, baseRockShelfNoise);
                }

                final double steepness = steepnessNoise.noise(x, z);
                height = shelfProgress > topShelfEdge + steepness ? topShelfHeight
                    : shelfProgress > topShelfEdge ? Mth.map(shelfProgress, topShelfEdge, topShelfEdge + steepness, midShelfHeight, topShelfHeight)
                    : shelfProgress > midShelfEdge + steepness ? midShelfHeight
                    : shelfProgress > midShelfEdge ? Mth.map(shelfProgress, midShelfEdge, midShelfEdge + steepness, sandHeight, midShelfHeight)
                    : sandHeight;
                return height;
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                if (yIn <= sandHeight)
                {
                    return -0.6;
                }
                if (height <= midShelfHeight)
                {
                    final double cliffCurveDepth = caveNoise.noise(x, yIn, z);
                    final double cliffProgress = widthFunction(true, yIn - tideLevel, midShelfHeight - tideLevel, midShelfEdge + cliffCurveDepth, midShelfEdge);
                    return 0.4 + 3 * (cliffProgress - shelfProgress);
                }
                if (yIn <= height)
                {
                    return 0;
                }
                return 0.7;
            }
        };
    }

    public static NTEShoreNoiseSampler rockyShores(NTESeed seed)
    {
        return new NTEShoreNoiseSampler()
        {
            private final Noise2D rockShelfIntensity = new OpenSimplex2D(seed.seed() + 5252L).octaves(3).spread(0.03).scaled(-0.8, 1);
            private final Noise2D steepnessNoise = new OpenSimplex2D(seed.seed() + 224L).octaves(2).spread(0.11).scaled(-0.7, 0.7, 0, 0.3).clamped(0, 0.3);
            private final Noise2D roughnessNoise = new OpenSimplex2D(seed.seed()).octaves(3).spread(0.09).scaled(-3, 3);
            private final Noise2D tidepoolNoise = new OpenSimplex2D(seed.seed() + 492L).octaves(3).spread(0.09).scaled(-1, 0, -5, 0).clamped(-5, 0);
            private final Noise2D tideNoise = NTEShoreNoiseHelpers.shoreTideLevelNoise(seed);

            private final Cellular2D cellularNoise = new Cellular2D(seed.seed() + 323L).spread(0.022);
            private final Noise2D punchbowlCarvingNoise = (x, z) -> {
                final Cellular2D.Cell cell = cellularNoise.cell(x, z);
                final double punchBowlRarity = 0.25;
                final double punchBowlDiameter = 0.5;
                if (cell.noise() > punchBowlRarity && cell.f2() >= punchBowlDiameter)
                {
                    return 1;
                }
                return cell.f1();
            };
            private final Noise2D punchbowlSizeNoise = new OpenSimplex2D(seed.seed()).octaves(3).spread(0.06).scaled(-0.7, 0.7, -0.05, 0.15).clamped(-0.05, 0.15);

            private final double topShelfEdge = 0.7;
            private final double midShelfEdge = 0.35;
            private final double lowShelfEdge = 0.10;

            private int x;
            private int z;
            private double oceanEdgeHeight;
            private double shelfProgress;
            private double height;
            private double topShelfHeight;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                final double tideLevel = tideNoise.noise(x, z);
                oceanEdgeHeight = tideLevel - 4;
                this.x = x;
                this.z = z;

                final double roughness = roughnessNoise.noise(x, z) + tidepoolNoise.noise(x, z);
                topShelfHeight = SEA_LEVEL_Y + roughness + 15;
                final double midShelfHeight = SEA_LEVEL_Y + roughness + 9;
                final double lowShelfHeight = SEA_LEVEL_Y + roughness + 3;

                final double baseRockShelfNoise = rockShelfIntensity.noise(x, z);
                if (oceanWeight > 0.10)
                {
                    shelfProgress = Mth.clampedMap(oceanWeight, 0.1, 0.3, baseRockShelfNoise, baseRockShelfNoise - oceanWeight * 3);
                }
                else
                {
                    final double landInfluence = Mth.clampedMap(landWeight, 0.4, 0, baseRockShelfNoise + landWeight * 2, baseRockShelfNoise);
                    shelfProgress = Mth.clampedMap(oceanWeight, 0, 0.1, baseRockShelfNoise + landInfluence, baseRockShelfNoise);
                }

                final double steepness = steepnessNoise.noise(x, z);
                height = shelfProgress > topShelfEdge + steepness ? topShelfHeight
                    : shelfProgress > topShelfEdge ? Mth.map(shelfProgress, topShelfEdge, topShelfEdge + steepness, midShelfHeight, topShelfHeight)
                    : shelfProgress > midShelfEdge + steepness ? midShelfHeight
                    : shelfProgress > midShelfEdge ? Mth.map(shelfProgress, midShelfEdge, midShelfEdge + steepness, lowShelfHeight, midShelfHeight)
                    : shelfProgress > lowShelfEdge - steepness ? lowShelfHeight
                    : oceanEdgeHeight;
                return height;
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                if (yIn <= oceanEdgeHeight)
                {
                    return 0;
                }
                if (yIn >= height)
                {
                    return 0.7;
                }

                double returnNoise = 0;
                final double lowShelfHeight = topShelfHeight - 12;
                if (height <= lowShelfHeight)
                {
                    final double cliffProgress = widthFunction(true, yIn - oceanEdgeHeight, lowShelfHeight - oceanEdgeHeight, lowShelfEdge + 0.11, lowShelfEdge);
                    returnNoise = 0.4 + 3 * (cliffProgress - shelfProgress);
                }

                final double punchbowlRadius = punchbowlCarvingNoise.noise(x, z);
                if (punchbowlRadius < 0.25)
                {
                    final double intensityShift = punchbowlSizeNoise.noise(x, z);
                    final double edgeIntensity = widthFunction(true, yIn - oceanEdgeHeight, topShelfHeight - oceanEdgeHeight, 0.25 - intensityShift, 0.12 - intensityShift);
                    returnNoise = Math.max(returnNoise, 3 * (edgeIntensity - punchbowlRadius));
                }
                return returnNoise;
            }
        };
    }

    public static NTEShoreNoiseSampler classic(NTESeed seed)
    {
        return new NTEShoreNoiseSampler()
        {
            private final Noise2D shoreNoise = new OpenSimplex2D(seed.seed() + 8719234132L).octaves(2).spread(0.003f).scaled(-0.1, 1.1);

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                final int cliffHeightAdjustment = shoreBaseHeightOf(biome);
                final double cliffInfluence = Mth.clamp(
                    shoreNoise.noise(x, z) + Mth.map(heightIn, cliffHeightAdjustment, cliffHeightAdjustment + 20, 0, 0.6),
                    0.0,
                    1.0
                );
                final double adjustedCliffInfluence = 1.0 - (1.0 - cliffInfluence) * (1.0 - cliffInfluence);
                final double x2 = Mth.lerp(adjustedCliffInfluence, 0.8, 0.515);
                final double y2 = 1.15 - 0.3 * x2;

                final double adjustedShoreWeight = shoreWeight < x2
                    ? Mth.map(shoreWeight, 0.5, x2, 0.5, y2)
                    : Mth.map(shoreWeight, x2, 1.0, y2, 1.0);

                final double normalWeight = 1.0 - shoreWeight;
                final double adjustedNormalWeight = 1.0 - adjustedShoreWeight;
                final double adjustedHeight = Math.max(
                    (adjustedShoreWeight / shoreWeight) * shoreHeight + (adjustedNormalWeight / normalWeight) * normalHeight,
                    cliffHeightAdjustment
                );

                if (adjustedHeight < heightIn)
                {
                    heightIn = adjustedHeight;
                }
                return heightIn;
            }
        };
    }

    public static NTEShoreNoiseSampler seaStacks(NTESeed seed)
    {
        return new NTEShoreNoiseSampler()
        {
            private final double minStackDensity = 0.85;
            private final double noiseScale = 3;
            private final Cellular2D cellularNoise = new Cellular2D(seed.seed() + 323L).spread(0.05);
            private final Noise2D seaStackDistributionNoise = new OpenSimplex2D(seed.seed() + 5424L).octaves(2).map(y -> 1 - Math.abs(y)).spread(0.015);
            private final Noise2D f2MinusF1Noise = (x, z) -> {
                final Cellular2D.Cell cell = cellularNoise.cell(x, z);
                final double f1 = cell.f1();
                final double f2 = cell.f2();
                return f1 > 0 ? f2 - f1 : 1;
            };
            private final Noise2D f1Noise = (x, z) -> {
                final Cellular2D.Cell cell = cellularNoise.cell(x, z);
                final double centerX = cell.x();
                final double centerZ = cell.y();
                final double stackDensity = seaStackDistributionNoise.noise(centerX, centerZ);
                if (stackDensity < minStackDensity)
                {
                    return noiseScale;
                }
                return cell.f1();
            };
            private final Noise3D cliffNoise = NTEShoreNoiseHelpers.cliffNoise(seed);
            private final Noise2D tideNoise = NTEShoreNoiseHelpers.shoreTideLevelNoise(seed);

            private double stackNoiseValue;
            private double sandHeight;
            private double landWeight;
            private double tideLevel;
            private double oceanWeightFactor;
            private int x;
            private int z;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                this.x = x;
                this.z = z;
                this.landWeight = landWeight;
                this.oceanWeightFactor = Mth.clampedMap(oceanWeight, 0.1, 0.25, 1, 0);
                tideLevel = tideNoise.noise(x, z);

                final double typicalBeachSandHeight = simpleBeach(tideLevel, heightIn, landWeight, oceanWeight);
                sandHeight = Mth.clampedMap(thisWeight, 0.5, 0.8, typicalBeachSandHeight, Math.min(typicalBeachSandHeight, tideLevel - 1));

                final double f2MinusF1 = f2MinusF1Noise.noise(x, z);
                final double outputMin = Mth.clampedMap(seaStackDistributionNoise.noise(x, z), 0.2, 0.6, 3, 1);
                stackNoiseValue = f1Noise.noise(x, z) * Mth.clampedMap(Math.abs(f2MinusF1), 0, 0.25, outputMin, 1);
                return Mth.clampedMap(oceanWeight, 0, 0.5, heightIn, shoreHeight / shoreWeight);
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                if (yIn <= sandHeight)
                {
                    return 0;
                }

                final double overhangHeight = SEA_LEVEL_Y + 14;
                final double stackBaseHeight = tideLevel + 1;
                final double y = yIn - stackBaseHeight;
                final double cliffNoiseModifier = 0.04 * Math.abs(cliffNoise.noise(x, y, z));
                final double stackMinWidth = (0.06 + cliffNoiseModifier) * oceanWeightFactor;
                final double stackMaxWidth = (0.18 + cliffNoiseModifier) * oceanWeightFactor;
                final double cliffBorderTopWeight = 0.22 + cliffNoiseModifier;
                final double cliffBorderBaseWeight = 0.26 + cliffNoiseModifier;
                final double height = overhangHeight - stackBaseHeight;
                final double stackWidth = widthFunction(false, y, height, stackMinWidth, stackMaxWidth);
                final double stackOutput = Mth.clamp((stackNoiseValue - stackWidth) * 10, 0, 1) * Mth.clampedMap(yIn, stackBaseHeight, overhangHeight, noiseScale, 0.75);

                if (landWeight >= cliffBorderTopWeight)
                {
                    final double cliffBorderWeight = widthFunction(true, y, height, cliffBorderBaseWeight, cliffBorderTopWeight);
                    return Math.min(Mth.clamp((cliffBorderWeight - landWeight) * 10, 0, 1) * noiseScale, stackOutput);
                }
                return stackOutput;
            }
        };
    }

    public static NTEShoreNoiseSampler upperTerrace(NTESeed seed)
    {
        return new NTEShoreNoiseSampler()
        {
            private final double noiseScale = 3;
            private final Noise3D cliffNoise = NTEShoreNoiseHelpers.cliffNoise(seed);
            private final Noise2D erosionNoise = new OpenSimplex2D(seed.seed() + 5192371L).octaves(2).spread(0.045f);
            private final Noise2D faceDetailNoise = new OpenSimplex2D(seed.seed() + 7483921L).octaves(2).spread(0.055f);
            private final Noise2D lowerTerraceNoise = NTEShoreNoiseHelpers.lowerTerraceNoise(seed);
            private final Noise2D tideNoise = NTEShoreNoiseHelpers.shoreTideLevelNoise(seed);
            private final Noise2D upperTerraceNoise = NTEShoreNoiseHelpers.upperTerraceNoise(seed);

            private double oceanWeight;
            private double landWeight;
            private double lowerWallFloorHeight;
            private int x;
            private int z;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                this.x = x;
                this.z = z;
                this.oceanWeight = oceanWeight;
                this.landWeight = landWeight;
                this.lowerWallFloorHeight = simpleBeach(tideNoise, x, z, heightIn, landWeight, oceanWeight);
                return upperTerraceNoise.noise(x, z);
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                final double lowerHeight = lowerTerraceNoise.noise(x, z);
                if (yIn <= lowerHeight)
                {
                    return lowerWallNoise(yIn, lowerHeight);
                }

                final double solidFloorHeight = Math.max(lowerHeight, SEA_LEVEL_Y + 6d);
                if (yIn <= solidFloorHeight)
                {
                    return 0;
                }

                final double overhangHeight = SEA_LEVEL_Y + 25;
                final double cliffBaseHeight = SEA_LEVEL_Y + 11;
                final double y = yIn - cliffBaseHeight;
                final double cliffNoiseModifier = 0.04 * Math.abs(cliffNoise.noise(x, y, z));
                final double height = overhangHeight - cliffBaseHeight;
                final double heightFactor = Mth.clamp((yIn - solidFloorHeight) / Math.max(1d, overhangHeight - solidFloorHeight), 0d, 1d);
                final double lowerGuard = Mth.clampedMap(yIn, solidFloorHeight + 1d, solidFloorHeight + 5d, 0d, 1d);
                final double upperGuard = 1d - Mth.clampedMap(yIn, overhangHeight - 8d, overhangHeight - 1d, 0d, 1d);
                final double detailGate = lowerGuard * upperGuard * Mth.clampedMap(oceanWeight, 0.10d, 0.35d, 0d, 1d);
                final double detailRoll = Mth.clampedMap(faceDetailNoise.noise(x, z), -0.70d, 0.85d, 0d, 1d);
                final double detailDepth = detailRoll < 0.24d ? 0d : detailRoll < 0.74d ? 0.020d : 0.040d;
                final double detailCenter = 0.24d + 0.58d * Mth.clampedMap(faceDetailNoise.noise(x + 41, z - 31), -1d, 1d, 0d, 1d);
                final double detailWidth = 0.12d + 0.10d * Mth.clampedMap(faceDetailNoise.noise(x - 19, z + 53), -1d, 1d, 0d, 1d);
                final double faceRelief = detailDepth * detailGate * smoothBand(heightFactor, detailCenter, detailWidth);
                final double broadErosion = 0.012d * detailGate * Mth.clampedMap(erosionNoise.noise(x, z), -0.55d, 0.85d, 0d, 1d);
                final double leaningRetreat = 0.006d * detailGate * (1d - heightFactor);
                final double cliffBorderTopWeight = 0.32 + cliffNoiseModifier + broadErosion + faceRelief * 0.65d;
                final double cliffBorderBaseWeight = 0.36 + cliffNoiseModifier + broadErosion + faceRelief + leaningRetreat;

                if (landWeight >= cliffBorderTopWeight)
                {
                    final double cliffBorderWeight = widthFunction(true, y, height, cliffBorderBaseWeight, cliffBorderTopWeight);
                    return Mth.clamp((cliffBorderWeight - landWeight) * 10, 0, 1) * noiseScale;
                }
                return Mth.clampedMap(yIn, solidFloorHeight, solidFloorHeight + 6, 0, noiseScale);
            }

            private double lowerWallNoise(int yIn, double lowerHeight)
            {
                final double solidFloorHeight = lowerWallFloorHeight;
                if (yIn <= solidFloorHeight || lowerHeight <= solidFloorHeight + 3d)
                {
                    return 0;
                }

                final double wallHeight = lowerHeight - solidFloorHeight;
                final double wallY = yIn - solidFloorHeight;
                final double wallProgress = Mth.clamp(wallY / wallHeight, 0d, 1d);
                final double floorGuard = Mth.clampedMap(yIn, solidFloorHeight + 0.25d, solidFloorHeight + 1.5d, 0d, 1d);
                final double topGuard = 1d - Mth.clampedMap(yIn, lowerHeight - 2d, lowerHeight, 0d, 1d);
                final double wallGate = floorGuard * topGuard * Mth.clampedMap(oceanWeight, 0.10d, 0.35d, 0d, 1d);
                if (wallGate <= 0d)
                {
                    return 0;
                }

                final double detailRoll = Mth.clampedMap(faceDetailNoise.noise(x + 101, z - 73), -0.75d, 0.90d, 0d, 1d);
                final double erosionHeightBlocks = 3d + 7d * Mth.clampedMap(faceDetailNoise.noise(x - 53, z + 89), -1d, 1d, 0d, 1d);
                final double outerHeight = Mth.clamp(erosionHeightBlocks / wallHeight, 0.18d, 0.68d);
                final double innerHeight = outerHeight * (0.42d + 0.26d * Mth.clampedMap(faceDetailNoise.noise(x + 29, z + 47), -1d, 1d, 0d, 1d));
                final double outerDepth = detailRoll < 0.12d ? 0d : detailRoll < 0.78d ? 0.060d : 0.068d;
                final double innerDepth = detailRoll > 0.60d ? 0.036d : 0d;
                final double outerTexture = Mth.clampedMap(cliffNoise.noise(x, wallY * 0.75d, z), -1d, 1d, 0.75d, 1.15d);
                final double innerTexture = Mth.clampedMap(cliffNoise.noise(x + 17, wallY * 0.85d, z - 11), -1d, 1d, 0.55d, 1.00d);
                final double outerRelief = outerDepth * bottomUpBand(wallProgress, outerHeight) * outerTexture;
                final double innerRelief = innerDepth * bottomUpBand(wallProgress, innerHeight) * innerTexture;
                final double faceRelief = wallGate * (outerRelief + innerRelief);
                final double erosion = 0.010d * wallGate * Mth.clampedMap(erosionNoise.noise(x - 23, z + 61), -0.55d, 0.85d, 0d, 1d);
                final double leaningRetreat = 0.004d * wallGate * (1d - wallProgress);
                final double cliffBorderTopWeight = Math.max(0.205d, 0.265d - erosion - faceRelief * 0.75d);
                final double cliffBorderBaseWeight = Math.max(0.192d, 0.245d - erosion - faceRelief - leaningRetreat);
                final double cliffBorderWeight = widthFunction(false, wallY, wallHeight, cliffBorderBaseWeight, cliffBorderTopWeight);

                if (oceanWeight >= cliffBorderWeight)
                {
                    return Mth.clamp((oceanWeight - cliffBorderWeight) * 16d, 0d, 1d) * noiseScale;
                }
                return 0;
            }
        };
    }

    public static NTEShoreNoiseSampler lowerTerrace(NTESeed seed)
    {
        return new NTEShoreNoiseSampler()
        {
            private final double noiseScale = 1;
            private final Noise3D cliffNoise = NTEShoreNoiseHelpers.cliffNoise(seed);
            private final Noise2D erosionNoise = new OpenSimplex2D(seed.seed() + 918273L).octaves(2).spread(0.055f);
            private final Noise2D faceDetailNoise = new OpenSimplex2D(seed.seed() + 283719L).octaves(2).spread(0.060f);
            private final Noise2D lowerTerraceNoise = NTEShoreNoiseHelpers.lowerTerraceNoise(seed);
            private final Noise2D tideNoise = NTEShoreNoiseHelpers.shoreTideLevelNoise(seed);

            private double oceanWeight;
            private double sandHeight;
            private int x;
            private int z;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                this.x = x;
                this.z = z;
                this.oceanWeight = oceanWeight;
                this.sandHeight = simpleBeach(tideNoise, x, z, heightIn, landWeight, oceanWeight);
                return lowerTerraceNoise.noise(x, z);
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                final double solidFloorHeight = sandHeight;
                if (yIn <= solidFloorHeight)
                {
                    return 0;
                }

                final double overhangHeight = SEA_LEVEL_Y + 11;
                final double cliffBaseHeight = SEA_LEVEL_Y - 2;
                final double y = yIn - cliffBaseHeight;
                final double cliffNoiseModifier = 0.12 * Math.abs(cliffNoise.noise(x, y, z));
                final double height = overhangHeight - cliffBaseHeight;
                final double wallHeight = Math.max(1d, overhangHeight - solidFloorHeight);
                final double heightFactor = Mth.clamp((yIn - solidFloorHeight) / wallHeight, 0d, 1d);
                final double lowerGuard = Mth.clampedMap(yIn, solidFloorHeight + 0.25d, solidFloorHeight + 1.5d, 0d, 1d);
                final double detailRoll = Mth.clampedMap(faceDetailNoise.noise(x, z), -0.65d, 0.85d, 0d, 1d);
                final double erosionHeightBlocks = 2d + 5d * Mth.clampedMap(faceDetailNoise.noise(x + 17, z - 29), -1d, 1d, 0d, 1d);
                final double outerHeight = Mth.clamp(erosionHeightBlocks / wallHeight, 0.16d, 0.62d);
                final double innerHeight = outerHeight * (0.40d + 0.24d * Mth.clampedMap(faceDetailNoise.noise(x - 37, z + 11), -1d, 1d, 0d, 1d));
                final double outerDepth = detailRoll < 0.30d ? 0d : detailRoll < 0.80d ? 0.014d : 0.020d;
                final double innerDepth = detailRoll > 0.70d ? 0.010d : 0d;
                final double outerTexture = Mth.clampedMap(cliffNoise.noise(x, y * 0.75d, z), -1d, 1d, 0.75d, 1.15d);
                final double innerTexture = Mth.clampedMap(cliffNoise.noise(x + 13, y * 0.85d, z - 7), -1d, 1d, 0.55d, 1.00d);
                final double faceRelief = lowerGuard * (
                    outerDepth * bottomUpBand(heightFactor, outerHeight) * outerTexture +
                        innerDepth * bottomUpBand(heightFactor, innerHeight) * innerTexture
                );
                final double erosion = 0.012d * lowerGuard * Mth.clampedMap(erosionNoise.noise(x, z), -0.5d, 0.8d, 0d, 1d);
                final double leaningRetreat = 0.006d * lowerGuard * (1d - heightFactor);
                final double cliffBorderTopWeight = 0.26 - cliffNoiseModifier - erosion;
                final double cliffBorderBaseWeight = 0.22 - cliffNoiseModifier - erosion - faceRelief - leaningRetreat;
                final double cliffBorderWeight = widthFunction(false, y, height, cliffBorderBaseWeight, cliffBorderTopWeight);

                if (oceanWeight >= cliffBorderWeight)
                {
                    return Mth.clamp((oceanWeight - cliffBorderWeight) * 20, 0, 1) * noiseScale;
                }
                return 0;
            }
        };
    }

    private static int shoreBaseHeightOf(BiomeExtension biome)
    {
        return ((NTEBiomeExtensionAccess) (Object) biome).tfe$getShoreBaseHeight();
    }

    private static double widthFunction(boolean inverted, double y, double height, double baseWidth, double topWidth)
    {
        final double curve = baseWidth + (y * y / (height * height)) * (topWidth - baseWidth);
        return inverted ? Math.max(curve, topWidth) : Math.min(curve, topWidth);
    }

    private static double smoothBand(double progress, double center, double halfWidth)
    {
        final double distance = Math.abs(progress - center);
        final double weight = 1d - Mth.clamp(distance / halfWidth, 0d, 1d);
        return weight * weight * (3d - 2d * weight);
    }

    private static double bottomUpBand(double progress, double height)
    {
        final double fadeStart = height * 0.62d;
        final double fade = 1d - Mth.clampedMap(progress, fadeStart, height, 0d, 1d);
        return fade * fade * (3d - 2d * fade);
    }

    private static double simpleBeach(Noise2D tideNoise, int x, int z, double heightIn, double landWeight, double oceanWeight)
    {
        return simpleBeach(tideNoise.noise(x, z), heightIn, landWeight, oceanWeight);
    }

    private static double simpleBeach(double tideLevel, double heightIn, double landWeight, double oceanWeight)
    {
        final double simpleShoreLandHeight = tideLevel + 2;
        final double simpleShoreSelfHeight = tideLevel - 2;
        final double simpleShoreOceanHeight = tideLevel - 4;

        if (oceanWeight > 0.10)
        {
            return Mth.clampedMap(oceanWeight, 0.1, 0.25, simpleShoreSelfHeight, simpleShoreOceanHeight);
        }

        final double landDerivedHeight = landWeight < 0.3
            ? Mth.map(landWeight, 0.3, 0, Math.min(heightIn, simpleShoreLandHeight), simpleShoreSelfHeight)
            : Mth.clampedMap(landWeight, 0.3, 0.5, Math.min(heightIn, simpleShoreLandHeight), heightIn);
        return Mth.clampedMap(oceanWeight, 0, 0.1, landDerivedHeight, simpleShoreSelfHeight);
    }

    private static double simpleBeachNoLandBlend(Noise2D tideNoise, int x, int z, double oceanWeight)
    {
        final double tideLevel = tideNoise.noise(x, z);
        final double simpleShoreSelfHeight = tideLevel - 1;
        final double simpleShoreOceanHeight = tideLevel - 4;
        return Mth.clampedMap(oceanWeight, 0, 0.5, simpleShoreSelfHeight, simpleShoreOceanHeight);
    }
}
