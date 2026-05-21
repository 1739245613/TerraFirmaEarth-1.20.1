package com.newterraearth.tfe.world.river;

import net.minecraft.util.Mth;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.Noise3D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.noise.OpenSimplex3D;
import net.dries007.tfc.world.river.RiverInfo;

import com.newterraearth.tfe.world.NTESeed;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

/**
 * Local port of the 1.21 river runtime, including the river blend types missing from 1.20.
 */
public final class NTERiverNoise
{
    private static final double CAVE_FULL_DELEGATION_WEIGHT = 0.25d;

    private NTERiverNoise()
    {
    }

    private static double caveMouthBlend(double caveWeight)
    {
        final double t = Mth.clamp(caveWeight / CAVE_FULL_DELEGATION_WEIGHT, 0d, 1d);
        return t * t * t;
    }

    private static double caveMouthHeight(RiverInfo info, double heightIn)
    {
        return Math.min(55 + info.normDistSq() * 1.3 * 16, heightIn);
    }

    private static double blendTowardCaveMouth(double riverHeight, RiverInfo info, double heightIn, double caveWeight)
    {
        return Mth.lerp(caveMouthBlend(caveWeight), riverHeight, caveMouthHeight(info, heightIn));
    }

    public static NTERiverNoiseSampler banked(NTESeed seed)
    {
        return new NTERiverNoiseSampler()
        {
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(3).spread(0.05f).scaled(-0.2f, 0.2f);
            final Noise2D bankCutNoise = new OpenSimplex2D(seed.next()).octaves(3).abs().spread(0.025).scaled(0, 1, SEA_LEVEL_Y - 4, SEA_LEVEL_Y + 30);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = info.normDistSq() * 0.8f + distNoise.noise(x, z);
                final double riverHeight = 57 + (distFac < 1.0 ? distFac * 6 : 6);

                final double heightInWeight = Mth.clamp(distFac - 1, 0, 2);
                final double riverWeight = 2 - heightInWeight;

                return height = Math.min((heightIn * heightInWeight + riverHeight * riverWeight) / 2, bankCutNoise.noise(x, z));
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                return y > height ? 0 : noiseIn;
            }
        };
    }

    public static NTERiverNoiseSampler tallBanked(NTESeed seed)
    {
        return new NTERiverNoiseSampler()
        {
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(3).spread(0.05f).scaled(-0.2f, 0.2f);
            final Noise2D surfaceNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.07).scaled(-2, 2);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = info.normDistSq() * 0.8f + distNoise.noise(x, z);
                final double riverHeight = 57 + (distFac < 1.0 ? distFac * 9 : 9) + surfaceNoise.noise(x, z);

                final double heightInWeight = Mth.clamp(distFac - 1, 0, 2);
                final double riverWeight = 2 - heightInWeight;

                return height = (heightIn * heightInWeight + riverHeight * riverWeight) / 2;
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                return y > height ? 0 : noiseIn;
            }
        };
    }

    public static NTERiverNoiseSampler floodplain(NTESeed seed)
    {
        return new NTERiverNoiseSampler()
        {
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-0.2f, 0.2f);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = info.normDistSq() * 0.8f + distNoise.noise(x, z);
                final double riverHeight;
                if (distFac < 1.0)
                {
                    riverHeight = 58.5 + distFac * 3;
                }
                else if (distFac < 2.0)
                {
                    riverHeight = 61.5;
                }
                else
                {
                    final double heightInWeight = Mth.clamp(2 * distFac - 4, 0, 1);
                    final double riverWeight = 1 - heightInWeight;
                    riverHeight = 61.5 * riverWeight + heightIn * heightInWeight;
                }

                return height = Math.min(blendTowardCaveMouth(riverHeight, info, heightIn, caveWeight), heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                return y > height ? 0 : noiseIn;
            }
        };
    }

    public static NTERiverNoiseSampler wide(NTESeed seed)
    {
        return new NTERiverNoiseSampler()
        {
            final Noise2D baseNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-2.5f, 1.5f);
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-0.15f, 0.15f);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = info.normDistSq() * 0.8f + distNoise.noise(x, z);
                final double riverHeight = 58 + distFac * 7 + baseNoise.noise(x, z);

                return height = Math.min(blendTowardCaveMouth(riverHeight, info, heightIn, caveWeight), heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                return y > height ? 0 : noiseIn;
            }
        };
    }

    public static NTERiverNoiseSampler wideDeep(NTESeed seed)
    {
        return new NTERiverNoiseSampler()
        {
            final Noise2D baseNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-2.5f, 1.5f);
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-0.15f, 0.15f);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = info.normDistSq() * 0.8f + distNoise.noise(x, z);
                final double riverHeight = 55 + distFac * 7 + baseNoise.noise(x, z);

                return height = Math.min(blendTowardCaveMouth(riverHeight, info, heightIn, caveWeight), heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                return y > height ? 0 : noiseIn;
            }
        };
    }

    public static NTERiverNoiseSampler canyon(NTESeed seed)
    {
        return new NTERiverNoiseSampler()
        {
            final Noise2D baseNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-7, 3);
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-0.3f, 0.2f);
            final Noise2D lowFreqCliffNoise = new OpenSimplex2D(seed.next()).spread(0.0007f).clamped(0, 1);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = info.normDistSq() * 1.3 + distNoise.noise(x, z);
                final double adjDistFac = distFac > 0.6 ? distFac * 0.4 + 0.8 : distFac;
                final double riverHeight = 55 + Mth.lerp(lowFreqCliffNoise.noise(x, z), distFac, adjDistFac) * 16 + baseNoise.noise(x, z);

                return height = Math.min(blendTowardCaveMouth(riverHeight, info, heightIn, caveWeight), heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                return y > height ? 0 : noiseIn;
            }
        };
    }

    public static NTERiverNoiseSampler tallCanyon(NTESeed seed)
    {
        return new NTERiverNoiseSampler()
        {
            final Noise2D baseNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-7, 3);
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-0.3f, 0.2f);
            final Noise3D cliffNoise = new OpenSimplex3D(seed.next()).octaves(2).spread(0.1f).scaled(0, 3);

            private double distFac;
            private int x;
            private int z;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = info.normDistSq() * 1.3 + distNoise.noise(x, z);
                final double adjDistFac = distFac > 0.32 ? distFac * 0.2 + 1.6 : distFac;
                final double riverHeight = 55 + adjDistFac * 16 + baseNoise.noise(x, z);
                final double widthFactor = Mth.clampedMap(info.widthSq(), 144, 324, 0.7, 1.1);

                this.distFac = Math.max(0, distFac * widthFactor);
                this.x = x;
                this.z = z;

                return Math.min(blendTowardCaveMouth(riverHeight, info, heightIn, caveWeight), heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                return Mth.clampedLerp(rawNoise(y), noiseIn, distFac);
            }

            private double rawNoise(int y)
            {
                if (y > SEA_LEVEL_Y + 35)
                {
                    return 0;
                }
                else if (y > SEA_LEVEL_Y + 20)
                {
                    final double easing = 1 - (y - SEA_LEVEL_Y - 20) / 15f;
                    return easing * cliffNoise.noise(x, y, z);
                }
                else if (y > SEA_LEVEL_Y)
                {
                    return cliffNoise.noise(x, y, z);
                }
                else if (y > SEA_LEVEL_Y - 8)
                {
                    final double easing = (y - SEA_LEVEL_Y + 8) / 8d;
                    return easing * cliffNoise.noise(x, y, z);
                }
                return 0;
            }
        };
    }

    public static NTERiverNoiseSampler talus(NTESeed seed)
    {
        return new NTERiverNoiseSampler()
        {
            final Noise2D baseNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-2.5f, 1.5f);
            final Noise2D cliffHeightNoise = new OpenSimplex2D(seed.next()).octaves(2).spread(0.1f).scaled(3f, 8f);
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-0.15f, 0.15f);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = Math.sqrt(info.normDistSq()) + distNoise.noise(x, z);
                final double talusRiverHeight = 55 + distFac * 12 + baseNoise.noise(x, z) + (distFac > 1.5 ? cliffHeightNoise.noise(x, z) : 0);
                final double canyonRiverHeight = 55 + info.normDistSq() * 1.3 * 16;
                final double riverHeight = Mth.clampedMap(thisWeight, 0.9, 1, canyonRiverHeight, talusRiverHeight);

                return height = Math.min(blendTowardCaveMouth(riverHeight, info, heightIn, caveWeight), heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                return y > height ? 0 : noiseIn;
            }
        };
    }

    public static NTERiverNoiseSampler terraces(NTESeed seed)
    {
        return new NTERiverNoiseSampler()
        {
            final Noise2D baseNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-2.5f, 1.5f);
            final Noise2D cliffHeightNoise = new OpenSimplex2D(seed.next()).octaves(2).spread(0.1f).scaled(4f, 8f);
            final Noise2D cliffBaseNoise = new OpenSimplex2D(seed.next()).octaves(2).spread(0.06f).scaled(SEA_LEVEL_Y - 2, SEA_LEVEL_Y + 4);
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-0.15f, 0.15f);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = Math.sqrt(info.normDistSq()) * 0.85 + distNoise.noise(x, z);
                final double slopedRiverHeight = 54 + distFac * 12 + baseNoise.noise(x, z);
                final double cliffBaseHeight = cliffBaseNoise.noise(x, z);
                final double cliffHeight = cliffHeightNoise.noise(x, z);
                final double lowerTerrace = slopedRiverHeight > cliffBaseHeight ? Mth.clampedMap(slopedRiverHeight, cliffBaseHeight, cliffBaseHeight + 1.1, 0, cliffHeight) : 0;
                final double upperTerrace = slopedRiverHeight > cliffBaseHeight + cliffHeight ? Mth.clampedMap(slopedRiverHeight, cliffBaseHeight + cliffHeight, cliffBaseHeight + cliffHeight + 1.4, 0, cliffHeight + 4) : 0;
                final double terraceRiverHeight = slopedRiverHeight + lowerTerrace + upperTerrace;
                final double canyonRiverHeight = 55 + info.normDistSq() * 1.3 * 16;
                final double riverHeight = Mth.clampedMap(thisWeight, 0.9, 1, canyonRiverHeight, terraceRiverHeight);

                return height = Math.min(blendTowardCaveMouth(riverHeight, info, heightIn, caveWeight), heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                return y > height ? 0 : noiseIn;
            }
        };
    }

    public static NTERiverNoiseSampler cave(NTESeed seed)
    {
        return new NTERiverNoiseSampler()
        {
            final Noise2D carvingCenterNoise = new OpenSimplex2D(seed.next()).octaves(2).spread(0.02f).scaled(SEA_LEVEL_Y - 3, SEA_LEVEL_Y + 3);
            final Noise2D carvingHeightNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.15f).scaled(8, 14);

            double distSquared, weight, height, carvingHeight, carvingCenter;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                distSquared = Mth.clamp(info.normDistSq() * 1.3 - 0.1, 0d, 1d);
                weight = caveWeight;
                height = heightIn;
                carvingHeight = carvingHeightNoise.noise(x, z);
                carvingCenter = carvingCenterNoise.noise(x, z);

                final double maxHeight = carvingCenter + carvingHeight;

                if (caveWeight > 0.75)
                {
                    return heightIn;
                }

                final double canyonMaxHeight = caveMouthHeight(info, heightIn);
                if (caveWeight > 0.5)
                {
                    final double interiorHeight = Mth.map(caveWeight, 0.5d, 0.75d, Math.min(maxHeight, heightIn), heightIn);
                    final double exteriorHeight = Mth.map(caveWeight, 0.5d, 0.75d, Math.min(canyonMaxHeight, heightIn), heightIn);
                    return height = Mth.lerp(distSquared, interiorHeight, exteriorHeight);
                }
                return height = canyonMaxHeight;
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                double vertDistance = (y - carvingCenter) / carvingHeight;
                if (vertDistance > 0)
                {
                    vertDistance = vertDistance * weight * weight;
                }
                final double columnNoise = Math.max(1 - (vertDistance * vertDistance), 0);
                final double noise = Mth.lerp(distSquared, columnNoise, noiseIn);

                return noise * Mth.clampedMap(weight, 0.5, 0.25, 1, 0);
            }
        };
    }
}
