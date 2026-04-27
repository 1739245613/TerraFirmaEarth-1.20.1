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
