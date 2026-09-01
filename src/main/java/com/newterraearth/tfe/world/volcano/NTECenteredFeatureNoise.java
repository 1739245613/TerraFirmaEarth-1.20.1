package com.newterraearth.tfe.world.volcano;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;

import com.newterraearth.tfe.world.NTEBiomeExtensionAccess;
import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.noise.NTECellular2D;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

/**
 * Local port of the 1.21 centered feature noise runtime.
 */
public final class NTECenteredFeatureNoise
{
    private NTECenteredFeatureNoise()
    {
    }

    public static NTECenteredFeatureNoiseSampler cinder(NTESeed seed)
    {
        return new NTECenteredFeatureNoiseSampler()
        {
            final NTECellular2D cellNoise = new NTECellular2D(seed.seed()).spread(0.009f);
            final Noise2D jitterNoise = new OpenSimplex2D(seed.seed() + 8179234123L).octaves(2).scaled(-0.0016f, 0.0016f).spread(0.128f);

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, BiomeSourceExtension biomeSource)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                final BiomeExtension biome = biomeSource.getBiomeExtension(QuartPos.fromBlock((int) cell.x()), QuartPos.fromBlock((int) cell.y()));
                if (isValidBiome(biome))
                {
                    final int rarity = getRarity(biome);
                    if (checkCellRarity(cell, rarity))
                    {
                        return modifyHeight(cell, x, z, biome, heightIn);
                    }
                }
                return NOT_PRESENT_RETURN;
            }

            @Override
            public BiomeExtension getCenterBiome(int x, int z, BiomeSourceExtension biomeSource)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                return biomeSource.getBiomeExtension(QuartPos.fromBlock((int) cell.x()), QuartPos.fromBlock((int) cell.y()));
            }

            private double modifyHeight(NTECellular2D.Cell cell, int x, int z, BiomeExtension biome, double heightIn)
            {
                final NTEBiomeExtensionAccess access = (NTEBiomeExtensionAccess) (Object) biome;
                final double f1 = cell.f1();
                final double easing = Mth.clamp(calculateClampedEasing((float) f1) + jitterNoise.noise(x, z), 0, 1);
                final double shape = calculateCinderShape(1 - easing);
                final double additionalHeight = shape * access.tfe$getCenteredFeatureScaleHeight();
                final double featureHeight = SEA_LEVEL_Y + access.tfe$getCenteredFeatureBaseHeight() + additionalHeight;
                final double weight = 10f * Mth.clamp((float) cell.f2() - f1, 0f, 0.1f);
                return Mth.lerp(easing * weight, heightIn, 0.2 * featureHeight + 0.8 * Math.max(featureHeight, heightIn + 0.6f * additionalHeight));
            }

            @Override
            public boolean isValidBiome(BiomeExtension biome)
            {
                return ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureBlendType() == NTECenteredFeatureBlendType.CINDER_CONE;
            }

            @Override
            public float calculateEasing(int x, int z, int rarity)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                if (checkCellRarity(cell, rarity))
                {
                    return calculateClampedEasing((float) cell.f1());
                }
                return 0;
            }

            @Override
            public @Nullable BlockPos calculateCenter(int x, int y, int z, int rarity)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                if (checkCellRarity(cell, rarity))
                {
                    return new BlockPos((int) cell.x(), y, (int) cell.y());
                }
                return null;
            }
        };
    }

    public static NTECenteredFeatureNoiseSampler tuffRing(NTESeed seed)
    {
        return new NTECenteredFeatureNoiseSampler()
        {
            final NTECellular2D cellNoise = new NTECellular2D(seed.seed(), 0.2f, 1).spread(0.003f);
            final Noise2D jitterNoise = new OpenSimplex2D(seed.seed() + 1234123L).octaves(2).scaled(-0.032f, 0.032f).spread(0.064f);
            final Noise2D addedCliffNoise = new OpenSimplex2D(seed.seed()).octaves(2).spread(0.1).scaled(-2, 10);
            final Noise2D everywhereNoise = new OpenSimplex2D(seed.next()).octaves(3).spread(0.03).scaled(-10, 10);

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, BiomeSourceExtension biomeSource)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                final BiomeExtension biome = biomeSource.getBiomeExtension(QuartPos.fromBlock((int) cell.x()), QuartPos.fromBlock((int) cell.y()));
                if (isValidBiome(biome))
                {
                    final int rarity = getRarity(biome);
                    if (checkCellRarity(cell, rarity))
                    {
                        return modifyHeight(cell, x, z, biome, heightIn);
                    }
                }
                return NOT_PRESENT_RETURN;
            }

            @Override
            public BiomeExtension getCenterBiome(int x, int z, BiomeSourceExtension biomeSource)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                return biomeSource.getBiomeExtension(QuartPos.fromBlock((int) cell.x()), QuartPos.fromBlock((int) cell.y()));
            }

            private double modifyHeight(NTECellular2D.Cell cell, int x, int z, BiomeExtension biome, double heightIn)
            {
                final NTEBiomeExtensionAccess access = (NTEBiomeExtensionAccess) (Object) biome;
                final double f1 = cell.f1();
                final double easing = Mth.clamp(calculateClampedEasing((float) f1) + jitterNoise.noise(x, z), 0, 1);
                final double shape = calculateTuffRingShape(1 - easing);
                final double ringAdditionalHeight = shape * access.tfe$getCenteredFeatureScaleHeight() + (shape > 0.5 ? addedCliffNoise.noise(x, z) : 0f);
                final double gapAdjustedAdditionalHeight = Math.min(ringAdditionalHeight * getGapVerticalEasing(cell), ringAdditionalHeight);
                final double ringHeight = SEA_LEVEL_Y + access.tfe$getCenteredFeatureBaseHeight() + gapAdjustedAdditionalHeight + everywhereNoise.noise(x, z);
                final double delta = 25 * Mth.clamp(cell.f2() - f1, 0, 0.04);
                return Mth.lerp(delta, heightIn, Math.max(ringHeight, heightIn));
            }

            @Override
            public boolean isValidBiome(BiomeExtension biome)
            {
                return ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureBlendType() == NTECenteredFeatureBlendType.TUFF_RING;
            }

            @Override
            public float calculateEasing(int x, int z, int rarity)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                if (checkCellRarity(cell, rarity))
                {
                    return calculateClampedEasing((float) cell.f1());
                }
                return 0;
            }

            @Override
            public @Nullable BlockPos calculateCenter(int x, int y, int z, int rarity)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                if (checkCellRarity(cell, rarity))
                {
                    return new BlockPos((int) cell.x(), y, (int) cell.y());
                }
                return null;
            }
        };
    }

    public static NTECenteredFeatureNoiseSampler tuya(NTESeed seed)
    {
        return new NTECenteredFeatureNoiseSampler()
        {
            final NTECellular2D cellNoise = new NTECellular2D(seed.seed(), 0.21f, 1).spread(0.0033f);
            final Noise2D jitterNoise = new OpenSimplex2D(seed.seed() + 1234123L).octaves(2).scaled(-0.016f, 0.016f).spread(0.128f);
            final Noise2D addedNoise = new OpenSimplex2D(seed.seed()).octaves(2).spread(0.1).scaled(-2, 3);

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, BiomeSourceExtension biomeSource)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                final BiomeExtension biome = biomeSource.getBiomeExtension(QuartPos.fromBlock((int) cell.x()), QuartPos.fromBlock((int) cell.y()));
                if (isValidBiome(biome))
                {
                    final int rarity = getRarity(biome);
                    if (checkCellRarity(cell, rarity))
                    {
                        return modifyHeight(cell, x, z, biome, heightIn);
                    }
                }
                return NOT_PRESENT_RETURN;
            }

            @Override
            public BiomeExtension getCenterBiome(int x, int z, BiomeSourceExtension biomeSource)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                return biomeSource.getBiomeExtension(QuartPos.fromBlock((int) cell.x()), QuartPos.fromBlock((int) cell.y()));
            }

            private double modifyHeight(NTECellular2D.Cell cell, int x, int z, BiomeExtension biome, double heightIn)
            {
                final NTEBiomeExtensionAccess access = (NTEBiomeExtensionAccess) (Object) biome;
                final double easing = Mth.clamp(calculateClampedEasing((float) cell.f1()) + jitterNoise.noise(x, z), 0, 1);
                final double shape = access.tfe$getCenteredFeatureIce() ? calculateTuyaIcyShape(1 - easing) : calculateTuyaShape(1 - easing);
                final double additionalHeight = shape * access.tfe$getCenteredFeatureScaleHeight() + addedNoise.noise(x, z);
                final double tuyaHeight = SEA_LEVEL_Y + access.tfe$getCenteredFeatureBaseHeight() + additionalHeight;
                return Mth.lerp(easing, heightIn, 0.5f * (tuyaHeight + Math.max(tuyaHeight, heightIn + 0.4f * additionalHeight)));
            }

            @Override
            public boolean isValidBiome(BiomeExtension biome)
            {
                return ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureBlendType() == NTECenteredFeatureBlendType.TUYA;
            }

            @Override
            public float calculateEasing(int x, int z, int rarity)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                if (checkCellRarity(cell, rarity))
                {
                    return calculateClampedEasing((float) cell.f1());
                }
                return 0;
            }

            @Override
            public @Nullable BlockPos calculateCenter(int x, int y, int z, int rarity)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                if (checkCellRarity(cell, rarity))
                {
                    return new BlockPos((int) cell.x(), y, (int) cell.y());
                }
                return null;
            }
        };
    }

    /** 4.2.9 atoll reef geometry and integrity shaping. */
    public static NTECenteredFeatureNoiseSampler atoll(NTESeed seed)
    {
        return new NTECenteredFeatureNoiseSampler()
        {
            final double minThickness = -0.3, maxThickness = 0.4, thicknessAmplitude = maxThickness - minThickness;
            final NTECellular2D cellNoise = new NTECellular2D(seed.seed(), 0.5f, 2).spread(0.0023f);
            final Noise2D heightNoise = new OpenSimplex2D(seed.seed()).octaves(4).spread(0.03f).scaled(SEA_LEVEL_Y - 3, SEA_LEVEL_Y + 7);
            final Noise2D rimWarpNoise = new OpenSimplex2D(seed.seed() + 1431L).octaves(3).scaled(0f, 0.3f).spread(0.029f);
            final Noise2D unscaledThicknessNoise = new OpenSimplex2D(seed.seed() + 131L).octaves(3).spread(0.02f);

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, BiomeSourceExtension biomeSource)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                final BiomeExtension biome = biomeSource.getBiomeExtension(QuartPos.fromBlock((int) cell.x()), QuartPos.fromBlock((int) cell.y()));
                if (!isValidBiome(biome) || !checkCellFrequency(cell, getFrequency(biome)))
                {
                    return NOT_PRESENT_RETURN;
                }
                final NTEBiomeExtensionAccess access = (NTEBiomeExtensionAccess) (Object) biome;
                final double integrity = getAtollIntegrity(cell);
                final double unclampedThickness = Mth.map(unscaledThicknessNoise.noise(x, z), -1, 1, minThickness, maxThickness);
                final float easing = calculateAtollEasing(cell, x, z);
                final double lagoonHeight = 0.8 + 0.12 * hashDouble(cell.noise(), 15413);
                final double reefHeight = reefShape(easing, lagoonHeight, unclampedThickness, integrity);
                double maxHeight = heightNoise.noise(x, z);
                if (integrity < 1) maxHeight = Mth.clampedMap(unscaledThicknessNoise.noise(x, z) + integrity, -1, 1, lagoonHeight * maxHeight, maxHeight);
                return Math.max(heightIn, Mth.map(reefHeight, 0, 1, SEA_LEVEL_Y - 60, maxHeight));
            }

            @Override
            public BiomeExtension getCenterBiome(int x, int z, BiomeSourceExtension biomeSource)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                return biomeSource.getBiomeExtension(QuartPos.fromBlock((int) cell.x()), QuartPos.fromBlock((int) cell.y()));
            }

            @Override
            public boolean isValidBiome(BiomeExtension biome)
            {
                return ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureBlendType() == NTECenteredFeatureBlendType.ATOLL;
            }

            @Override
            public float calculateEasing(int x, int z, int rarity)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                return checkCellRarity(cell, rarity) ? calculateAtollEasing(cell, x, z) : 0;
            }

            @Override
            public @Nullable BlockPos calculateCenter(int x, int y, int z, int rarity)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                return checkCellRarity(cell, rarity) ? new BlockPos((int) cell.x(), y, (int) cell.y()) : null;
            }

            @Override
            public NTECellular2D getCellularNoise()
            {
                return cellNoise;
            }

            private float calculateAtollEasing(NTECellular2D.Cell cell, int x, int z)
            {
                final double difference = cell.f2() - cell.f1();
                final float easing = (float) (Math.sqrt(cell.f2()) - Math.sqrt(cell.f1()) + rimWarpNoise.noise(x, z));
                return difference > 0.18 ? easing : easing * (float) Mth.map(difference, 0, 0.18, 0, 1);
            }

            private double reefShape(double easing, double lagoonDepth, double thickness, double integrity)
            {
                final double edgeDist = 0.47;
                final double beachDist = edgeDist + 0.11;
                final double clampedThickness = Mth.clamp(thickness, 0, 0.3);
                final double lagoonDist = beachDist + 0.19 + clampedThickness;
                final double baseShape;
                if (easing < edgeDist) return Mth.map(easing, 0, edgeDist, 0, lagoonDepth);
                if (easing < beachDist) baseShape = Mth.map(easing, edgeDist, beachDist, lagoonDepth, 1);
                else baseShape = Mth.clampedMap(easing, beachDist, lagoonDist, 1, lagoonDepth);
                return integrity >= 1 ? baseShape : Mth.clampedMap(thickness, minThickness - thicknessAmplitude * integrity, maxThickness - thicknessAmplitude * integrity, lagoonDepth, baseShape);
            }

            private double getAtollIntegrity(NTECellular2D.Cell cell)
            {
                return Math.min(0.4 + hashDouble(cell.noise(), 523), 1);
            }
        };
    }

    /** 4.2.9 stratovolcano geometry with size-dependent shape variants. */
    public static NTECenteredFeatureNoiseSampler stratovolcano(NTESeed seed)
    {
        return new NTECenteredFeatureNoiseSampler()
        {
            final NTECellular2D cellNoise = new NTECellular2D(seed.seed(), 2).spread(0.0024f);

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, BiomeSourceExtension biomeSource)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                final BiomeExtension biome = biomeSource.getBiomeExtension(QuartPos.fromBlock((int) cell.x()), QuartPos.fromBlock((int) cell.y()));
                if (!isValidBiome(biome) || !checkCellFrequency(cell, getFrequency(biome)))
                {
                    return NOT_PRESENT_RETURN;
                }
                final NTEBiomeExtensionAccess access = (NTEBiomeExtensionAccess) (Object) biome;
                final double maxDiam = Math.sqrt(Math.min(1, maxSafeDiameterSquared(cell, cellNoise)));
                final NTEVolcanoVariant variant = getVolcanoVariant(cell);
                return variant.getHeight(heightIn, x, z, maxDiam, access.tfe$getCenteredFeatureScaleHeight(), access.tfe$getCenteredFeatureBaseHeight(), cell);
            }

            @Override
            public BiomeExtension getCenterBiome(int x, int z, BiomeSourceExtension biomeSource)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                return biomeSource.getBiomeExtension(QuartPos.fromBlock((int) cell.x()), QuartPos.fromBlock((int) cell.y()));
            }

            @Override
            public boolean isValidBiome(BiomeExtension biome)
            {
                return ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureBlendType() == NTECenteredFeatureBlendType.STRATOVOLCANO;
            }

            @Override
            public float calculateEasing(int x, int z, int rarity)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                return checkCellRarity(cell, rarity) ? calculateClampedEasing((float) cell.f1()) : 0;
            }

            @Override
            public float calculateEasing(BlockPos pos, BiomeExtension biome)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(pos.getX(), pos.getZ());
                return checkCellFrequency(cell, getFrequency(biome)) ? calculateClampedEasing((float) cell.f1()) : 0;
            }

            @Override
            public @Nullable BlockPos calculateCenter(BlockPos pos, BiomeExtension biome)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(pos.getX(), pos.getZ());
                return checkCellFrequency(cell, getFrequency(biome)) ? new BlockPos((int) cell.x(), pos.getY(), (int) cell.y()) : null;
            }

            @Override
            public @Nullable BlockPos calculateCenter(int x, int y, int z, int rarity)
            {
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                return checkCellRarity(cell, rarity) ? new BlockPos((int) cell.x(), y, (int) cell.y()) : null;
            }

            @Override
            public NTECellular2D getCellularNoise()
            {
                return cellNoise;
            }

            @Override
            public @Nullable NTEVolcanoVariant getVolcanoVariant(NTECellular2D.Cell cell)
            {
                return NTEVolcanoVariants.forCell(seed, cell, cellNoise);
            }
        };
    }

    public static double maxSafeDiameterSquared(NTECellular2D.Cell cell, NTECellular2D cellNoise)
    {
        double f2 = Math.min(cellNoise.cell(cell.x() + 1, cell.y()).f2(), cellNoise.cell(cell.x() - 1, cell.y()).f2());
        f2 = Math.min(f2, cellNoise.cell(cell.x(), cell.y() + 1).f2());
        return Math.min(f2, cellNoise.cell(cell.x(), cell.y() - 1).f2());
    }

    public static double getAtollIntegrity(NTECellular2D.Cell cell)
    {
        return Math.min(0.4 + hashDouble(cell.noise(), 523), 1);
    }

    public static double hashDouble(double input, int index)
    {
        final long inputBits = Double.doubleToLongBits(input);
        long bits = inputBits + index;
        bits ^= bits >>> 33;
        bits *= 0xff51afd7ed558ccdL;
        bits ^= bits >>> 33;
        bits *= 0xc4ceb9fe1a85ec53L;
        bits ^= bits >>> 33;
        return (bits >>> 11) * 0x1.0p-53;
    }

    private static float calculateClampedEasing(float f1)
    {
        return Mth.clamp(Mth.map(f1, 0, 0.23f, 1, 0), 0, 1);
    }

    private static double calculateCinderShape(double t)
    {
        if (t > 0.025)
        {
            return (5 / (9 * t + 1) - 0.5) * 0.279173646008;
        }
        final double a = t * 9 + 0.05;
        return (8 * a * a + 2.97663265306) * 0.279173646008;
    }

    private static double calculateTuffRingShape(double t)
    {
        return t < 0.03f ? 0
            : t < 0.10f ? t * 7f - 0.21f
            : t < 0.15f ? t * 6.6667f
            : t < 0.20f ? 1 - (t - 0.15f) * 6.6667f
            : 1.5f - 5 * t;
    }

    private static double calculateTuyaShape(double t)
    {
        return t < 0.015f ? Mth.map(t, 0f, 0.015f, 0.8f, 1f)
            : t < 0.125f ? Mth.map(t, 0.025f, 0.125f, 1f, 0.75f)
            : Mth.clampedMap(t, 0.125f, 0.16f, 0.75f, 0f);
    }

    private static double calculateTuyaIcyShape(double t)
    {
        return t < 0.015f ? Mth.map(t, 0f, 0.015f, 0.8f, 1f)
            : t < 0.125f ? Mth.map(t, 0.025f, 0.125f, 1f, 0.75f)
            : t < 0.16 ? Mth.map(t, 0.125f, 0.16f, 0.75f, 0f)
            : Mth.clampedMap(t, 0.16f, 0.19f, 0f, 0.5f);
    }

    private static double getGapVerticalEasing(NTECellular2D.Cell cell)
    {
        final double gapSize = ((1 + cell.noise()) * 100) % 1;
        double gapVerticalEasing = 1;
        if (gapSize > 0)
        {
            final double aGap = 4 * (Math.abs(cell.noise() * 10000) % 1);
            final double angleToGap = Math.abs(cell.angle() - aGap);
            if (angleToGap < gapSize)
            {
                final double angleToGapEdge = Math.abs(Math.min(angleToGap + gapSize, angleToGap - gapSize));
                gapVerticalEasing = Mth.clampedMap(angleToGapEdge, 0, Math.max(0.3, 0.6 * gapSize), 1, 0);
            }
        }
        return gapVerticalEasing;
    }
}
