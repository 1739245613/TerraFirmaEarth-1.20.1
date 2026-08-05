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

    private static double caveMouthHeight(RiverInfo info, NTERiverHydrology.ColumnProfile profile, double heightIn)
    {
        final double baseHeight = bedY(profile, 55d);
        return Math.min(baseHeight + radialDistanceSq(info, profile) * 1.3d * 16d, heightIn);
    }

    private static double blendTowardCaveMouth(double riverHeight, RiverInfo info, NTERiverHydrology.ColumnProfile profile, double heightIn, double caveWeight)
    {
        return Mth.lerp(caveMouthBlend(caveWeight), riverHeight, caveMouthHeight(info, profile, heightIn));
    }

    static double radialDistanceSq(RiverInfo info, NTERiverHydrology.ColumnProfile profile)
    {
        if (profile == null)
        {
            return info.normDistSq();
        }
        if (info == null)
        {
            return profile.normalizedDistanceSq();
        }
        // Geometry and river type must use one shape handoff. Water ownership
        // can transfer earlier, but the dry shoulder retains the creek slope
        // until it reaches the receiver's actual wet center.
        return Mth.lerp(
            NTERiverHydrology.receiverBankShapeBlendWeight(profile),
            profile.normalizedDistanceSq(),
            info.normDistSq()
        );
    }

    private static double bedY(NTERiverHydrology.ColumnProfile profile, double fallback)
    {
        return profile == null
            ? fallback
            : Mth.lerp(
                NTERiverHydrology.receiverBankShapeBlendWeight(profile),
                profile.centerBedY(),
                fallback
            );
    }

    private static double waterY(NTERiverHydrology.ColumnProfile profile, double fallback)
    {
        return profile == null
            ? fallback
            : Mth.lerp(
                NTERiverHydrology.receiverBankShapeBlendWeight(profile),
                profile.waterSurfaceY(),
                fallback
            );
    }

    public static NTERiverNoiseSampler banked(NTESeed seed)
    {
        return new NTERiverNoiseSampler()
        {
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(3).spread(0.05f).scaled(-0.2f, 0.2f);
            final Noise2D bankCutNoise = new OpenSimplex2D(seed.next()).octaves(3).abs().spread(0.025).scaled(0, 1);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, NTERiverHydrology.ColumnProfile profile, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = radialDistanceSq(info, profile) * 0.8f + distNoise.noise(x, z);
                final double channelRise = waterY(profile, 58d) - bedY(profile, 57d) + 1.4d;
                final double riverHeight = bedY(profile, 57d) + (distFac < 1.0 ? distFac * channelRise : channelRise);

                final double heightInWeight = Mth.clamp(distFac - 1, 0, 2);
                final double riverWeight = 2 - heightInWeight;

                height = Math.min(
                    (heightIn * heightInWeight + riverHeight * riverWeight) / 2,
                    profile == null ? Mth.clampedMap(bankCutNoise.noise(x, z), 0d, 1d, SEA_LEVEL_Y - 4d, SEA_LEVEL_Y + 30d) : profile.waterSurfaceY() + 4d + bankCutNoise.noise(x, z) * 26d
                );
                return height;
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
            public double setColumnAndSampleHeight(RiverInfo info, NTERiverHydrology.ColumnProfile profile, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = radialDistanceSq(info, profile) * 0.8f + distNoise.noise(x, z);
                final double channelRise = waterY(profile, 58d) - bedY(profile, 57d) + 4d;
                final double riverHeight = bedY(profile, 57d) + (distFac < 1.0 ? distFac * channelRise : channelRise) + surfaceNoise.noise(x, z);

                final double heightInWeight = Mth.clamp(distFac - 1, 0, 2);
                final double riverWeight = 2 - heightInWeight;

                height = (heightIn * heightInWeight + riverHeight * riverWeight) / 2;
                return height;
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
            public double setColumnAndSampleHeight(RiverInfo info, NTERiverHydrology.ColumnProfile profile, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = radialDistanceSq(info, profile) * 0.8f + distNoise.noise(x, z);
                final double riverHeight;
                if (distFac < 1.0)
                {
                    riverHeight = bedY(profile, 58.5d) + distFac * (waterY(profile, 60d) - bedY(profile, 58.5d) + 1.5d);
                }
                else if (distFac < 2.0)
                {
                    riverHeight = waterY(profile, 60d) + 1.5d;
                }
                else
                {
                    final double heightInWeight = Mth.clamp(2 * distFac - 4, 0, 1);
                    final double riverWeight = 1 - heightInWeight;
                    riverHeight = (waterY(profile, 60d) + 1.5d) * riverWeight + heightIn * heightInWeight;
                }

                height = Math.min(blendTowardCaveMouth(riverHeight, info, profile, heightIn, caveWeight), heightIn);
                return height;
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
            public double setColumnAndSampleHeight(RiverInfo info, NTERiverHydrology.ColumnProfile profile, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = radialDistanceSq(info, profile) * 0.8f + distNoise.noise(x, z);
                final double riverHeight = bedY(profile, 58d) + distFac * 7d + baseNoise.noise(x, z);

                height = Math.min(blendTowardCaveMouth(riverHeight, info, profile, heightIn, caveWeight), heightIn);
                return height;
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
            public double setColumnAndSampleHeight(RiverInfo info, NTERiverHydrology.ColumnProfile profile, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = radialDistanceSq(info, profile) * 0.8f + distNoise.noise(x, z);
                final double riverHeight = bedY(profile, 55d) + distFac * 7d + baseNoise.noise(x, z);

                height = Math.min(blendTowardCaveMouth(riverHeight, info, profile, heightIn, caveWeight), heightIn);
                return height;
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
            public double setColumnAndSampleHeight(RiverInfo info, NTERiverHydrology.ColumnProfile profile, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = radialDistanceSq(info, profile) * 1.3 + distNoise.noise(x, z);
                final double adjDistFac = distFac > 0.6 ? distFac * 0.4 + 0.8 : distFac;
                final double riverHeight = bedY(profile, 55d) + Mth.lerp(lowFreqCliffNoise.noise(x, z), distFac, adjDistFac) * 16 + baseNoise.noise(x, z);

                height = Math.min(blendTowardCaveMouth(riverHeight, info, profile, heightIn, caveWeight), heightIn);
                return height;
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
            private double waterSurface;
            private int x;
            private int z;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, NTERiverHydrology.ColumnProfile profile, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = radialDistanceSq(info, profile) * 1.3 + distNoise.noise(x, z);
                final double adjDistFac = distFac > 0.32 ? distFac * 0.2 + 1.6 : distFac;
                final double riverHeight = bedY(profile, 55d) + adjDistFac * 16 + baseNoise.noise(x, z);
                final double widthSq = profile == null ? info.widthSq() : Mth.square(profile.channelRadius());
                final double widthFactor = Mth.clampedMap(widthSq, 144, 324, 0.7, 1.1);

                this.distFac = Math.max(0, distFac * widthFactor);
                this.waterSurface = waterY(profile, SEA_LEVEL_Y);
                this.x = x;
                this.z = z;

                return Math.min(blendTowardCaveMouth(riverHeight, info, profile, heightIn, caveWeight), heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                return Mth.clampedLerp(rawNoise(y), noiseIn, distFac);
            }

            private double rawNoise(int y)
            {
                if (y > waterSurface + 35)
                {
                    return 0;
                }
                else if (y > waterSurface + 20)
                {
                    final double easing = 1 - (y - waterSurface - 20) / 15f;
                    return easing * cliffNoise.noise(x, y, z);
                }
                else if (y > waterSurface)
                {
                    return cliffNoise.noise(x, y, z);
                }
                else if (y > waterSurface - 8)
                {
                    final double easing = (y - waterSurface + 8) / 8d;
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
            public double setColumnAndSampleHeight(RiverInfo info, NTERiverHydrology.ColumnProfile profile, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = Math.sqrt(radialDistanceSq(info, profile)) + distNoise.noise(x, z);
                final double talusRiverHeight = bedY(profile, 55d) + distFac * 12 + baseNoise.noise(x, z) + (distFac > 1.5 ? cliffHeightNoise.noise(x, z) : 0);
                final double canyonRiverHeight = bedY(profile, 55d) + radialDistanceSq(info, profile) * 1.3 * 16;
                final double riverHeight = Mth.clampedMap(thisWeight, 0.9, 1, canyonRiverHeight, talusRiverHeight);

                height = Math.min(blendTowardCaveMouth(riverHeight, info, profile, heightIn, caveWeight), heightIn);
                return height;
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
            final Noise2D cliffBaseNoise = new OpenSimplex2D(seed.next()).octaves(2).spread(0.06f).scaled(-2d, 4d);
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-0.15f, 0.15f);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, NTERiverHydrology.ColumnProfile profile, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                final double distFac = Math.sqrt(radialDistanceSq(info, profile)) * 0.85 + distNoise.noise(x, z);
                final double slopedRiverHeight = bedY(profile, 54d) + distFac * 12 + baseNoise.noise(x, z);
                final double cliffBaseHeight = waterY(profile, SEA_LEVEL_Y) + cliffBaseNoise.noise(x, z);
                final double cliffHeight = cliffHeightNoise.noise(x, z);
                final double lowerTerrace = slopedRiverHeight > cliffBaseHeight ? Mth.clampedMap(slopedRiverHeight, cliffBaseHeight, cliffBaseHeight + 1.1, 0, cliffHeight) : 0;
                final double upperTerrace = slopedRiverHeight > cliffBaseHeight + cliffHeight ? Mth.clampedMap(slopedRiverHeight, cliffBaseHeight + cliffHeight, cliffBaseHeight + cliffHeight + 1.4, 0, cliffHeight + 4) : 0;
                final double terraceRiverHeight = slopedRiverHeight + lowerTerrace + upperTerrace;
                final double canyonRiverHeight = bedY(profile, 55d) + radialDistanceSq(info, profile) * 1.3 * 16;
                final double riverHeight = Mth.clampedMap(thisWeight, 0.9, 1, canyonRiverHeight, terraceRiverHeight);

                height = Math.min(blendTowardCaveMouth(riverHeight, info, profile, heightIn, caveWeight), heightIn);
                return height;
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
            final Noise2D carvingCenterNoise = new OpenSimplex2D(seed.next()).octaves(2).spread(0.02f).scaled(-3, 3);
            final Noise2D carvingHeightNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.15f).scaled(8, 14);

            double distSquared, weight, height, carvingHeight, carvingCenter;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, NTERiverHydrology.ColumnProfile profile, int x, int z, double heightIn, double caveWeight, double thisWeight)
            {
                distSquared = Mth.clamp(radialDistanceSq(info, profile) * 1.3 - 0.1, 0d, 1d);
                weight = caveWeight;
                height = heightIn;
                carvingHeight = carvingHeightNoise.noise(x, z);
                carvingCenter = profile == null
                    ? SEA_LEVEL_Y + carvingCenterNoise.noise(x, z)
                    : profile.waterSurfaceY() + carvingCenterNoise.noise(x, z);

                final double maxHeight = carvingCenter + carvingHeight;

                if (caveWeight > 0.75)
                {
                    return heightIn;
                }

                final double canyonMaxHeight = caveMouthHeight(info, profile, heightIn);
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
