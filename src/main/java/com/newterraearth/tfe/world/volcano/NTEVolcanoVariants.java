package com.newterraearth.tfe.world.volcano;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.BlockPos;

import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.rock.Rock;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.biome.BiomeExtension;

import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.NTEBiomeExtensionAccess;
import com.newterraearth.tfe.world.noise.NTECellular2D;
import com.newterraearth.tfe.world.noise.NTECellular1D;
import com.newterraearth.tfe.world.surface.NTESurfaceStates;
import com.newterraearth.tfe.common.NTERock;
import com.newterraearth.tfe.world.NTESurfaceContext;
import com.newterraearth.tfe.world.shore.NTEShoreNoiseHelpers;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

/**
 * Port of the 4.2.9 stratovolcano variant registry. The formulas retain the
 * named profiles, cell-size selection and the major crater / ridge / dome
 * surface behavior while using the 1.20 compatibility APIs.
 */
public final class NTEVolcanoVariants
{
    private NTEVolcanoVariants()
    {
    }

    public static NTEVolcanoVariant forCell(NTESeed seed, NTECellular2D.Cell cell, NTECellular2D cellNoise)
    {
        final double maxDiam = Math.sqrt(Math.min(1, NTECenteredFeatureNoise.maxSafeDiameterSquared(cell, cellNoise)));
        final double noise = hashDouble(cell.noise(), 317);
        if (maxDiam >= 1)
        {
            return noise > 0.4 ? kelimutu(seed) : craterLake(seed);
        }
        if (maxDiam >= 0.7)
        {
            if (noise > 0.7) return kelimutu(seed);
            if (noise > 0.45) return craterLake(seed);
            if (noise > 0.2) return tahoma(seed);
            return fuji(seed);
        }
        if (maxDiam < 0.4)
        {
            return batholith(seed);
        }
        return maxDiam < 0.55 && noise > 0.5 ? batholith(seed) : fuji(seed);
    }

    public static NTEVolcanoVariant fuji(NTESeed seed)
    {
        final Noise2D ridgeWarp = new OpenSimplex2D(seed.seed() + 23L).octaves(2).scaled(-0.4f, 0.4f).spread(0.09f);
        final Noise2D skirt = new OpenSimplex2D(seed.seed() + 2982L).octaves(3).spread(0.09).scaled(-0.05, 0.05);
        final Noise2D footprint = new OpenSimplex2D(seed.seed() + 6143L).octaves(2).spread(0.0028).scaled(-0.06, 0.06);
        return new Profile("fuji")
        {
            @Override
            public double getLandHeight(double heightIn, int x, int z, double maxDiam, double scale, double base, NTECellular2D.Cell cell)
            {
                final double crater = 0.04 + 0.1 * hashDouble(cell.noise(), 10);
                final double baseR = radius(cell, maxDiam, 0.45);
                final double r = distortOceanicFootprint(baseR, heightIn, x, z, cell, footprint, 0.58, 0.94);
                double shape = maxDiam * simpleWithSkirt(r, crater, 0.9, cell.f1(), cell.f2(), 2);
                shape *= 1 - 0.1 * circumferential(cell, crater, 0.2, 0.9, 1, r, 3, (int) (maxDiam * 16), ridgeWarp.noise(x, z));
                if (r > 0.65) shape += skirt.noise(x, z) * Mth.clampedMap(r, 0.65, 1.2, 0, 1);
                return Math.max(scale(shape, base, scale), heightIn);
            }

            @Override
            public boolean buildSurface(SurfaceBuilderContext context, int oceanFloorHeight, int preVolcanicHeight, NTECenteredFeatureNoiseSampler sampler)
            {
                if (oceanFloorHeight <= preVolcanicHeight + 2) return false;
                final BlockPos pos = context.pos();
                final int x = pos.getX(), z = pos.getZ();
                final NTECellular2D cellNoise = sampler.getCellularNoise();
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                final double maxDiam = Math.min(1, Math.sqrt(NTECenteredFeatureNoise.maxSafeDiameterSquared(cell, cellNoise)));
                final BiomeExtension biome = NTESurfaceContext.current().stratovolcanoBiome();
                final int unerodedHeight = (int) getUnerodedHeight(maxDiam, ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureScaleHeight(), ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureBaseHeight(), cell);
                final double baseR = Mth.map(Mth.sqrt((float) cell.f1()), 0, maxDiam * 0.45, 0, 1);
                final double r = distortOceanicFootprint(baseR, preVolcanicHeight, x, z, cell, footprint, 0.58, 0.94);
                if (r > 1.2) return false;
                final double crater = 0.04 + 0.1 * hashDouble(cell.noise(), 10);
                if (r <= crater || (hashDouble(cell.noise(), 3199) < 0.6 && r <= 2 * crater))
                {
                    buildStrataSurfaceOnly(context, unerodedHeight, preVolcanicHeight, oceanFloorHeight, cell.noise(), seed);
                }
                else
                {
                    buildNormalSurfaceWithStrata(context, unerodedHeight, preVolcanicHeight, oceanFloorHeight, 0, cell.noise(), seed, NTESurfaceStates.VOLCANIC_TOP_GRASS_TO_TUFF_GRAVEL, NTESurfaceStates.VOLCANIC_MID_DIRT_TO_TUFF_GRAVEL, Fluids.LAVA.getSource().defaultFluidState().createLegacyBlock());
                }
                return true;
            }

            private double getUnerodedHeight(double maxDiam, double scale, double base, NTECellular2D.Cell cell)
            {
                final double r = Mth.map(Mth.sqrt((float) cell.f1()), 0, maxDiam * 0.45, 0, 1);
                final double crater = 0.04 + 0.1 * hashDouble(cell.noise(), 10);
                return scale(maxDiam * calculateCone(r, crater), base, scale);
            }
        };
    }

    public static NTEVolcanoVariant craterLake(NTESeed seed)
    {
        final Noise2D ridgeWarp = new OpenSimplex2D(seed.seed() + 23L).octaves(2).scaled(-0.4f, 0.4f).spread(0.09f);
        final Noise2D rimWarp = new OpenSimplex2D(seed.seed() + 1431L).octaves(2).scaled(-0.08f, 0.08f).spread(0.03f);
        final Noise2D texture = new OpenSimplex2D(seed.seed() + 24482L).octaves(3).spread(0.06).scaled(0.85, 1.08);
        final Noise2D skirt = new OpenSimplex2D(seed.seed() + 2982L).octaves(3).spread(0.09).scaled(-0.09, 0.09);
        final Noise2D footprint = new OpenSimplex2D(seed.seed() + 26143L).octaves(2).spread(0.0028).scaled(-0.05, 0.05);
        return new Profile("crater_lake")
        {
            @Override
            public double getHeight(double heightIn, int x, int z, double maxDiam, double scale, double base, NTECellular2D.Cell cell)
            {
                return Math.max(getLandHeight(heightIn, x, z, maxDiam, scale, base, cell), getFluidHeight(heightIn, x, z, maxDiam, scale, base, cell));
            }

            @Override
            public double getLandHeight(double heightIn, int x, int z, double maxDiam, double scale, double base, NTECellular2D.Cell cell)
            {
                final double crater = 0.5 + rimWarp.noise(x, z);
                final double r = distortOceanicFootprint(radius(cell, maxDiam, 0.45), heightIn, x, z, cell, footprint, 0.68, 0.97);
                double shape = 0.42 * simpleWithSkirt(r, crater, 2, cell.f1(), cell.f2(), 5);
                shape *= 1 - 0.12 * circumferential(cell, crater, crater + 0.06, 0.95, 1, r, 24, (int) (maxDiam * 32), ridgeWarp.noise(x, z));
                shape *= 1 - 0.08 * circumferential(cell, crater * 0.4, crater * 0.8, crater * 0.8, crater, r, 24, (int) (maxDiam * 32), ridgeWarp.noise(x, z));
                if (r < crater)
                {
                    final double noise = cell.noise();
                    final double apex = 0.42 * (0.65 + 0.45 * hashDouble(noise, 8));
                    final double ox = -45 + 90 * hashDouble(noise, 6);
                    final double oz = -45 + 90 * hashDouble(noise, 7);
                    shape = Math.max(shape, offsetCone(cell.x() + ox, cell.y() + oz, x, z, apex, apex * maxDiam * 300, noise, ridgeWarp));
                    heightIn = SEA_LEVEL_Y + 10 + base;
                }
                else if (r > 1) shape += skirt.noise(x, z) * Mth.clampedMap(r, 1, 1.2, 0, 1);
                return Math.max(scale(shape * texture.noise(x, z), base, scale), heightIn);
            }

            @Override
            public double getFluidHeight(double heightIn, int x, int z, double maxDiam, double scale, double base, NTECellular2D.Cell cell)
            {
                final double crater = 0.5 + rimWarp.noise(x, z);
                return radius(cell, maxDiam, 0.45) < crater ? scale(0.2, base, scale) : 0;
            }

            @Override
            public boolean buildSurface(SurfaceBuilderContext context, int oceanFloorHeight, int preVolcanicHeight, NTECenteredFeatureNoiseSampler sampler)
            {
                final BlockPos pos = context.pos();
                final int x = pos.getX(), z = pos.getZ();
                final NTECellular2D cellNoise = sampler.getCellularNoise();
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                final double noise = cell.noise();
                final double maxDiam = Math.min(1, Math.sqrt(NTECenteredFeatureNoise.maxSafeDiameterSquared(cell, cellNoise)));
                final BiomeExtension biome = NTESurfaceContext.current().stratovolcanoBiome();
                final double scale = ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureScaleHeight();
                final double base = ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureBaseHeight();
                final int landHeight = (int) Math.round(getLandHeight(preVolcanicHeight, x, z, maxDiam, scale, base, cell));
                final int waterHeight = (int) Math.round(getFluidHeight(preVolcanicHeight, x, z, maxDiam, scale, base, cell));
                final int unerodedHeight = (int) getUnerodedHeight(maxDiam, scale, base, cell);
                final double baseR = Mth.map(Mth.sqrt((float) cell.f1()), 0, maxDiam * 0.45, 0, 1);
                final double mainCrater = 0.5 + rimWarp.noise(x, z);
                if (baseR > 1.2 * mainCrater && oceanFloorHeight <= preVolcanicHeight + 2) return false;
                double r2 = Double.MAX_VALUE;
                if (baseR < 1.2 * mainCrater)
                {
                    preVolcanicHeight = (int) Mth.clampedMap(baseR, mainCrater + 0.1, mainCrater, preVolcanicHeight, SEA_LEVEL_Y + 10);
                    if (baseR < mainCrater)
                    {
                        final double cx = cell.x() - 45 + 90 * hashDouble(noise, 6);
                        final double cz = cell.y() - 45 + 90 * hashDouble(noise, 7);
                        r2 = (x - cx) * (x - cx) + (z - cz) * (z - cz);
                    }
                }
                if (r2 < 81)
                {
                    buildStrataSurfaceOnly(context, unerodedHeight, preVolcanicHeight, oceanFloorHeight, noise, seed);
                }
                else
                {
                    final double featurePlacementHash = hashDouble(noise, 3199);
                    final BlockState fluid = featurePlacementHash < 0.83 ? Fluids.WATER.getSource().defaultFluidState().createLegacyBlock() : TFCFluids.SPRING_WATER.getSource().defaultFluidState().createLegacyBlock();
                    buildNormalSurfaceWithStrata(context, unerodedHeight, preVolcanicHeight, Math.max(landHeight, Math.min(oceanFloorHeight, context.getSeaLevel())), waterHeight, noise, seed, NTESurfaceStates.VOLCANIC_TOP_GRASS_TO_TUFF_GRAVEL, NTESurfaceStates.VOLCANIC_MID_DIRT_TO_TUFF_GRAVEL, fluid);
                }
                return true;
            }

            private double getUnerodedHeight(double maxDiam, double scale, double base, NTECellular2D.Cell cell)
            {
                final double r = Mth.map(Mth.sqrt((float) cell.f1()), 0, maxDiam * 0.45, 0, 1);
                return scale(maxDiam * 0.42 * calculateCone(r, 0.5), base, scale);
            }

            private double offsetCone(double cx, double cz, int x, int z, double apex, double radius, double noise, Noise2D ridge)
            {
                final double r = Mth.map(Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)), 0, radius, 0, 1);
                final double crater = 0.03 + 0.03 * hashDouble(noise, 10);
                double shape = simpleNoSkirt(r, crater, 1.1);
                shape *= 0.9 + 0.1 * circumferentialOffset(crater, 0.2, 0.9, 1, r, diamondAngle(x - cx, z - cz), (int) (3 + hashDouble(noise, 1313) * apex * 12), ridge.noise(x, z), noise);
                return Mth.map(shape, 0, 1, 0, apex);
            }
        };
    }

    public static NTEVolcanoVariant tahoma(NTESeed seed)
    {
        final Noise2D ridgeWarp = new OpenSimplex2D(seed.seed() + 23L).octaves(2).scaled(-0.5f, 0.5f).spread(0.03f);
        final Noise2D skirt = new OpenSimplex2D(seed.seed() + 2982L).octaves(3).spread(0.09).scaled(-0.05, 0.05);
        final Noise2D footprint = new OpenSimplex2D(seed.seed() + 16143L).octaves(2).spread(0.0035).scaled(-0.06, 0.06);
        final Noise2D texture = new OpenSimplex2D(seed.seed() + 248582L).octaves(3).spread(0.06).scaled(0.94, 1.06);
        return new Profile("tahoma")
        {
            @Override
            public double getLandHeight(double heightIn, int x, int z, double maxDiam, double scale, double base, NTECellular2D.Cell cell)
            {
                final double noise = cell.noise();
                final double crater = 0.1 + 0.15 * hashDouble(noise, 1013);
                final double transition = crater + 0.3;
                final double r = distortOceanicFootprint(radius(cell, maxDiam, 0.45), heightIn, x, z, cell, footprint, 0.60, 0.95);
                double shape = maxDiam * simpleWithSkirt(r, crater, 0.9, cell.f1(), cell.f2(), 2);
                final double inner = r > transition ? 0 : circumferential(cell, crater, crater + 0.15, transition - 0.15, transition, r, 3, (int) (maxDiam * 8), ridgeWarp.noise(x, z));
                final double outer = r < transition - 0.15 ? 0 : circumferential(cell, transition - 0.15, transition, 0.85, 1, r, 6, 2 * (int) (maxDiam * 8), ridgeWarp.noise(x, z));
                shape -= Mth.clampedMap(r, crater, crater + 0.3, 0, 0.12) * (1.6 + inner + outer);
                if (r < 0.65)
                {
                    shape *= 1 - 0.52 * crater * variableRim(cell, crater * 0.5, crater, crater * 2, r, 1 + (int) (hashDouble(noise, 978) * 3), x, z, texture);
                    if (r < crater)
                    {
                        final double innerScale = 1.1 * (0.65 + 0.35 * hashDouble(noise, 8973)) * crater;
                        final double innerCone = maxDiam * (1 - 0.9 * crater) + innerScale * simpleNoSkirt(Mth.clampedMap(r, 0, 0.9 * crater, 0, 1), crater, 1);
                        shape = Math.max(shape, innerCone);
                    }
                }
                else shape += skirt.noise(x, z) * Mth.clampedMap(r, 0.65, 1.2, 0, 1);
                return Math.max(scale(shape * texture.noise(x, z), base, scale), heightIn);
            }

            @Override
            public boolean buildSurface(SurfaceBuilderContext context, int oceanFloorHeight, int preVolcanicHeight, NTECenteredFeatureNoiseSampler sampler)
            {
                if (oceanFloorHeight <= preVolcanicHeight + 2) return false;
                final BlockPos pos = context.pos();
                final int x = pos.getX(), z = pos.getZ();
                final NTECellular2D cellNoise = sampler.getCellularNoise();
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                final double maxDiam = Math.min(1, Math.sqrt(NTECenteredFeatureNoise.maxSafeDiameterSquared(cell, cellNoise)));
                final BiomeExtension biome = NTESurfaceContext.current().stratovolcanoBiome();
                final double scale = ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureScaleHeight();
                final double base = ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureBaseHeight();
                final int unerodedHeight = (int) getUnerodedHeight(maxDiam, scale, base, cell);
                final double crater = 0.7 * (0.20 + 0.25 * hashDouble(cell.noise(), 1013));
                final double r = distortOceanicFootprint(radius(cell, maxDiam, 0.45), preVolcanicHeight, x, z, cell, footprint, 0.60, 0.95);
                if (r > 1.2) return false;
                if (r <= crater)
                {
                    buildStrataSurfaceOnly(context, unerodedHeight, preVolcanicHeight, oceanFloorHeight, cell.noise(), seed);
                }
                else
                {
                    buildNormalSurfaceWithStrata(context, unerodedHeight, preVolcanicHeight, oceanFloorHeight, 0, cell.noise(), seed, NTESurfaceStates.VOLCANIC_TOP_GRASS_TO_TUFF_GRAVEL, NTESurfaceStates.VOLCANIC_MID_DIRT_TO_TUFF_GRAVEL, Fluids.LAVA.getSource().defaultFluidState().createLegacyBlock());
                }
                return true;
            }

            private double getUnerodedHeight(double maxDiam, double scale, double base, NTECellular2D.Cell cell)
            {
                final double r = Mth.map(Mth.sqrt((float) cell.f1()), 0, maxDiam * 0.45, 0, 1);
                final double crater = 0.20 + 0.25 * hashDouble(cell.noise(), 1013);
                return scale(maxDiam * calculateCone(r, crater), base, scale);
            }
        };
    }

    public static NTEVolcanoVariant batholith(NTESeed seed)
    {
        final Noise2D texture = new OpenSimplex2D(seed.seed() + 24852L).octaves(4).spread(0.2).scaled(0.8, 1.2);
        final Noise2D warp = new OpenSimplex2D(seed.seed() + 133L).octaves(2).scaled(0f, 0.006f).spread(0.05f);
        return new Profile("batholith")
        {
            @Override
            public double getLandHeight(double heightIn, int x, int z, double maxDiam, double scale, double base, NTECellular2D.Cell cell)
            {
                final double maxR = maxDiam * 0.5 - 0.08 * hashDouble(cell.noise(), 1398);
                final double erosion = hashDouble(cell.noise(), 1348) > 0.6 ? warp.noise(x, z) : 0;
                final double r2 = Mth.map(cell.f1() + erosion, 0, maxR * maxR, 0, 1);
                double shape = maxDiam * (0.8 + 0.4 * hashDouble(cell.noise(), 83)) * (1 - r2);
                shape *= texture.noise(x, z);
                return Math.max(scale(shape, base, scale), heightIn);
            }

            @Override
            public boolean buildSurface(SurfaceBuilderContext context, int oceanFloorHeight, int preVolcanicHeight, NTECenteredFeatureNoiseSampler sampler)
            {
                if (oceanFloorHeight <= preVolcanicHeight + 2) return false;
                final boolean granite = sampler.getCellularNoise().cell(context.pos().getX(), context.pos().getZ()).noise() > 0;
                if (granite)
                {
                    buildNormalBatholithSurface(context, oceanFloorHeight, preVolcanicHeight, NTESurfaceStates.VOLCANIC_TOP_GRASS_TO_GRANITE_GRAVEL, NTESurfaceStates.VOLCANIC_MID_DIRT_TO_GRANITE_GRAVEL, NTESurfaceStates.GRANITE_GRAVEL, NTESurfaceStates.GRANITE_GRAVEL, NTESurfaceStates.GRANITE_GRAVEL, NTESurfaceStates.GRANITE);
                }
                else
                {
                    buildNormalBatholithSurface(context, oceanFloorHeight, preVolcanicHeight, NTESurfaceStates.VOLCANIC_TOP_GRASS_TO_DIORITE_GRAVEL, NTESurfaceStates.VOLCANIC_MID_DIRT_TO_DIORITE_GRAVEL, NTESurfaceStates.DIORITE_GRAVEL, NTESurfaceStates.DIORITE_GRAVEL, NTESurfaceStates.DIORITE_GRAVEL, NTESurfaceStates.DIORITE);
                }
                return true;
            }
        };
    }

    public static NTEVolcanoVariant kelimutu(NTESeed seed)
    {
        final Noise2D ridgeWarp = new OpenSimplex2D(seed.seed() + 23L).octaves(2).scaled(-0.4f, 0.4f).spread(0.09f);
        final Noise2D texture = new OpenSimplex2D(seed.seed() + 24482L).octaves(3).spread(0.06).scaled(0.85, 1.08);
        final Noise2D skirt = new OpenSimplex2D(seed.seed() + 2982L).octaves(3).spread(0.09).scaled(-0.05, 0.05);
        final Noise2D footprint = new OpenSimplex2D(seed.seed() + 36143L).octaves(2).spread(0.003).scaled(-0.06, 0.06);
        return new Profile("kelimutu")
        {
            @Override
            public double getHeight(double heightIn, int x, int z, double maxDiam, double scale, double base, NTECellular2D.Cell cell)
            {
                return Math.max(getLandHeight(heightIn, x, z, maxDiam, scale, base, cell), getFluidHeight(heightIn, x, z, maxDiam, scale, base, cell));
            }

            @Override
            public double getLandHeight(double heightIn, int x, int z, double maxDiam, double scale, double base, NTECellular2D.Cell cell)
            {
                final double noise = cell.noise();
                final double r = distortOceanicFootprint(radius(cell, maxDiam, 0.45), heightIn, x, z, cell, footprint, 0.60, 0.92);
                final double crater0 = 0.06 + 0.06 * hashDouble(noise, 67);
                final double rim0 = 0.65;
                double shape = rim0 * truncated(r, crater0);
                shape *= 1 - 0.12 * circumferential(cell, crater0, crater0 + 0.06, 0.95, 1, r, 6, (int) (maxDiam * 12), ridgeWarp.noise(x, z));
                final double ox1 = offset(noise, 68), oz1 = offset(noise, 69);
                final double crater1 = 0.06 + 0.08 * (Math.abs(ox1) + Math.abs(oz1));
                final double rim1 = rim0 * (0.8 + 0.2 * hashDouble(noise, 70));
                shape = Math.max(shape, offsetTruncated(shape, cell.x() + signedOffset(ox1), cell.y() + signedOffset(oz1), x, z, rim1, rim1 * maxDiam * 300, crater1, 3 + (int) (rim1 * 8 + hashDouble(noise, 101) * 4), noise, ridgeWarp));
                double craterShape = rim0 * crater(r / crater0, crater0, 0.6 + hashDouble(noise, 74));
                craterShape = offsetCrater(craterShape, cell.x() + signedOffset(ox1), cell.y() + signedOffset(oz1), x, z, 0, rim1, rim1 * maxDiam * 300, crater1, 1.3 + hashDouble(noise, 70));
                if (hashDouble(noise, 1066) > 0.3)
                {
                    final double ox2 = offset(noise, 71), oz2 = offset(noise, 72);
                    final double crater2 = 0.06 + 0.08 * (Math.abs(ox2) + Math.abs(oz2));
                    final double rim2 = rim0 * (0.7 + 0.3 * hashDouble(noise, 73));
                    shape = Math.max(shape, offsetTruncated(shape, cell.x() + signedOffset(ox2, 73), cell.y() + signedOffset(oz2, 74), x, z, rim2, rim2 * maxDiam * 300, crater2, 3 + (int) (rim2 * 8 + hashDouble(noise, 101) * 4), noise, ridgeWarp));
                    craterShape = offsetCrater(craterShape, cell.x() + signedOffset(ox2), cell.y() + signedOffset(oz2), x, z, 0, rim2, rim2 * maxDiam * 300, crater2, 1.3 + hashDouble(noise, 73));
                }
                shape = Math.min(shape, craterShape) * texture.noise(x, z);
                if (r > 0.7) shape += skirt.noise(x, z) * Mth.clampedMap(r, 0.7, 1, 0, 1);
                return Math.max(scale(shape, base, scale), Math.min(heightIn, scale(craterShape, base, scale)));
            }

            @Override
            public double getFluidHeight(double heightIn, int x, int z, double maxDiam, double scale, double base, NTECellular2D.Cell cell)
            {
                return 0;
            }

            @Override
            public boolean buildSurface(SurfaceBuilderContext context, int oceanFloorHeight, int preVolcanicHeight, NTECenteredFeatureNoiseSampler sampler)
            {
                final BlockPos pos = context.pos();
                final int x = pos.getX(), z = pos.getZ();
                final NTECellular2D cellNoise = sampler.getCellularNoise();
                final NTECellular2D.Cell cell = cellNoise.cell(x, z);
                final double noise = cell.noise();
                final double maxDiam = Math.min(1, Math.sqrt(NTECenteredFeatureNoise.maxSafeDiameterSquared(cell, cellNoise)));
                final BiomeExtension biome = NTESurfaceContext.current().stratovolcanoBiome();
                final double scale = ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureScaleHeight();
                final double base = ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureBaseHeight();
                final int unerodedHeight = (int) getUnerodedHeight(maxDiam, scale, base, cell);
                final double baseR = Mth.map(Mth.sqrt((float) cell.f1()), 0, maxDiam * 0.45, 0, 1);
                final double surfaceR = distortOceanicFootprint(baseR, preVolcanicHeight, x, z, cell, footprint, 0.60, 0.92);
                if (surfaceR > 1.2) return false;
                final double crater0 = 0.06 + 0.06 * hashDouble(noise, 67);
                boolean insideCrater = false;
                if (baseR < 2 * crater0)
                {
                    insideCrater = baseR < crater0;
                    preVolcanicHeight = Math.min(preVolcanicHeight, (int) Mth.clampedMap(baseR, 2 * crater0, 1.6 * crater0, oceanFloorHeight - 1, oceanFloorHeight - 15));
                }
                final double rim0 = 0.65;
                if (!insideCrater)
                {
                    final double ox = 2 * (0.5 - hashDouble(noise, 68));
                    final double oz = 2 * (0.5 - hashDouble(noise, 69));
                    final double rim1 = rim0 * (0.8 + 0.2 * hashDouble(noise, 70));
                    final double crater1 = 0.06 + 0.08 * (Math.abs(ox) + Math.abs(oz));
                    final double cx = cell.x() + (ox > 0 ? 20 + ox * 70 : -20 + ox * 70);
                    final double cz = cell.y() + (oz > 0 ? 20 + oz * 70 : -20 + oz * 70);
                    final double r1 = Mth.map(Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)), 0, rim1 * maxDiam * 300, 0, 1);
                    insideCrater = r1 < crater1;
                    if (r1 < 2 * crater1) preVolcanicHeight = Math.min(preVolcanicHeight, (int) Mth.clampedMap(r1, 2 * crater1, 1.6 * crater1, oceanFloorHeight - 1, oceanFloorHeight - 15));
                }
                if (!insideCrater && hashDouble(noise, 1066) > 0.3)
                {
                    final double ox = 2 * (0.5 - hashDouble(noise, 71));
                    final double oz = 2 * (0.5 - hashDouble(noise, 72));
                    final double rim2 = rim0 * (0.7 + 0.3 * hashDouble(noise, 73));
                    final double crater2 = 0.06 + 0.08 * (Math.abs(ox) + Math.abs(oz));
                    final double cx = cell.x() + (ox > 0 ? 20 + ox * 70 : -20 + ox * 73);
                    final double cz = cell.y() + (oz > 0 ? 20 + oz * 70 : -20 + oz * 74);
                    final double r2 = Mth.map(Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)), 0, rim2 * maxDiam * 300, 0, 1);
                    insideCrater = r2 < crater2;
                    if (r2 < 4 * crater2) preVolcanicHeight = Math.min(preVolcanicHeight, (int) Mth.clampedMap(r2, 2 * crater2, 1.6 * crater2, oceanFloorHeight, oceanFloorHeight - 15));
                }
                if (oceanFloorHeight <= preVolcanicHeight + 2) return false;
                if (insideCrater) buildStrataSurfaceOnly(context, unerodedHeight, preVolcanicHeight, oceanFloorHeight, noise, seed);
                else buildNormalSurfaceWithStrata(context, unerodedHeight, preVolcanicHeight, oceanFloorHeight, 0, noise, seed, NTESurfaceStates.VOLCANIC_TOP_GRASS_TO_TUFF_GRAVEL, NTESurfaceStates.VOLCANIC_MID_DIRT_TO_TUFF_GRAVEL, Fluids.WATER.getSource().defaultFluidState().createLegacyBlock());
                return true;
            }

            private double getUnerodedHeight(double maxDiam, double scale, double base, NTECellular2D.Cell cell)
            {
                final double r = Mth.map(Mth.sqrt((float) cell.f1()), 0, maxDiam * 0.45, 0, 1);
                return scale(maxDiam * 0.65 * calculateCone(r, 0.06 + 0.06 * hashDouble(cell.noise(), 67)), base, scale);
            }

            private double offset(double noise, int hash)
            {
                return 2 * (0.5 - hashDouble(noise, hash));
            }

            private double signedOffset(double value)
            {
                return signedOffset(value, 70);
            }

            private double signedOffset(double value, double multiplier)
            {
                return value > 0 ? 20 + value * multiplier : -20 + value * multiplier;
            }

            private double offsetTruncated(double shapeIn, double cx, double cz, int x, int z, double rim, double radius, double crater, int ridges, double noise, Noise2D ridgeWarp)
            {
                final double r = Mth.map(Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)), 0, radius, 0, 1);
                double shape = rim * truncated(r, crater);
                shape *= 1 - 0.15 * circumferentialOffset(crater, 0.2, 0.9, 1, r, diamondAngle(x - cx, z - cz), ridges, ridgeWarp.noise(x, z), noise);
                return Math.max(shape, shapeIn);
            }

            private double offsetCrater(double shapeIn, double cx, double cz, int x, int z, double baseHeight, double rim, double radius, double craterSize, double depth)
            {
                final double r = Mth.map(Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)), 0, craterSize * radius, 0, 1);
                double shape = rim * crater(r, craterSize, depth);
                shape = Mth.map(shape, 0, 1, baseHeight, rim);
                return Math.min(shape, shapeIn);
            }
        };
    }

    private abstract static class Profile implements NTEVolcanoVariant
    {
        private final String name;

        private Profile(String name)
        {
            this.name = name;
        }

        @Override
        public String name()
        {
            return name;
        }
    }

    private static double radius(NTECellular2D.Cell cell, double maxDiam, double radiusScale)
    {
        return Mth.map(Mth.sqrt((float) cell.f1()), 0, maxDiam * radiusScale, 0, 1);
    }

    private static double scale(double shape, double base, double height)
    {
        return SEA_LEVEL_Y + base + shape * height;
    }

    private static double simpleWithSkirt(double r, double crater, double depth, double f1, double f2, double skirtSlope)
    {
        return r >= 1 ? (1 - r) * Mth.clampedMap(f2 - f1, 0, 0.1, skirtSlope, 1) : simple(r, crater, depth);
    }

    private static double simpleNoSkirt(double r, double crater, double depth)
    {
        return r >= 1 ? 0 : simple(r, crater, depth);
    }

    private static double calculateCone(double r, double crater)
    {
        return Mth.map(r, 1, crater, 0, 1);
    }

    private static double simple(double r, double crater, double depth)
    {
        if (r > crater)
        {
            return hyperbolicSection(Mth.map(r, crater, 1, 0, 1), 1, 1);
        }
        final double base = 1 - depth * crater;
        return hyperbolicSection(crater - r, crater, depth * crater) + base;
    }

    private static double truncated(double r, double crater)
    {
        if (r > 1) return 1 - r;
        return r > crater ? hyperbolicSection(Mth.map(r, crater, 1, 0, 1), 1, 1) : 1;
    }

    private static double crater(double r, double crater, double depth)
    {
        final double d = crater * depth;
        return (1 - d) + d * r * r;
    }

    private static double circumferential(NTECellular2D.Cell cell, double inner0, double inner1, double outer1, double outer0, double r, int minRidges, int addedRidges, double warp)
    {
        final int ridges = (int) (hashDouble(cell.noise(), 213) * addedRidges) + minRidges;
        return circumferentialOffset(inner0, inner1, outer1, outer0, r, cell.angle(), ridges, warp, cell.noise(), minRidges, minRidges + addedRidges);
    }

    private static double circumferentialOffset(double inner0, double inner1, double outer1, double outer0, double r, double angle, int ridges, double warp, double noiseIn)
    {
        return circumferentialOffset(inner0, inner1, outer1, outer0, r, angle, ridges, warp, noiseIn, 3, 10);
    }

    private static double circumferentialOffset(double inner0, double inner1, double outer1, double outer0, double r, double angle, int ridges, double warp, double noiseIn, int minRidges, int maxRidges)
    {
        final double noise = hashDouble(noiseIn, 213);
        double a = angle + warp / ridges;
        a = a >= 4 ? a - 4 : a < 0 ? a + 4 : a;
        final double fluvial = Math.abs((a * 0.5 * ridges % 2) - 1);
        final double easing = r <= inner1 ? Mth.clampedMap(r, inner0, inner1, 0, 1) : Mth.clampedMap(r, outer0, outer1, 0, 1);
        final double ridgeScale = Mth.clampedMap(ridges, minRidges, maxRidges, 1.5, 0.5);
        return (fluvial - 1) * (2 - noise) * easing * ridgeScale;
    }

    private static double variableRim(NTECellular2D.Cell cell, double inner, double crater, double outer, double r, int peaks, int x, int z, Noise2D texture)
    {
        if (r < inner || r > outer) return 0;
        final double a = cell.angle() + hashDouble(cell.noise(), 43);
        final double peak = Math.abs((a * 0.5 * peaks % 2) - 1);
        final double easing = r <= crater ? Mth.map(r, inner, crater, 0, 1) : Mth.map(r, crater, outer, 1, 0);
        return peak * easing * texture.noise(x, z);
    }

    private static double distortOceanicFootprint(double r, double terrainHeight, int x, int z, NTECellular2D.Cell cell, Noise2D footprintNoise, double warpStart, double warpFull)
    {
        final double underwater = Mth.clampedMap(SEA_LEVEL_Y - terrainHeight, 0, 18, 0, 1);
        if (underwater <= 0) return r;
        double slope = Mth.clampedMap(r, warpStart, warpFull, 0, 1);
        slope = slope * slope * (3 - 2 * slope);
        if (slope <= 0) return r;
        final double angle = Math.atan2(z - cell.y(), x - cell.x());
        final double n = cell.noise();
        final double scale = 1 + (0.02 + 0.015 * hashDouble(n, 6104)) * Math.cos(2 * angle)
            + (0.035 + 0.02 * hashDouble(n, 6105)) * Math.cos(3 * angle)
            + (0.01 + 0.01 * hashDouble(n, 6106)) * Math.cos(5 * angle)
            + footprintNoise.noise(x, z);
        return Mth.lerp(underwater * slope, r, r / Mth.clamp(scale, 0.86, 1.18));
    }

    public static void buildStrataSurfaceOnly(SurfaceBuilderContext context, int unerodedY, int baseY, int landHeight, double cellNoiseIn, NTESeed seed)
    {
        final double rockTypeNoise = hashDouble(cellNoiseIn, 856);
        final NTECellular1D cells = new NTECellular1D(seed.seed()).spread(0.25);
        for (int y = landHeight; y >= baseY; --y)
        {
            if (context.isDefaultBlock(context.getBlockState(y)))
            {
                context.setBlockState(y, getStratifiedStoneBlock(unerodedY, y, cells, rockTypeNoise, Rock.BlockType.RAW));
            }
        }
    }

    public static void buildNormalSurfaceWithStrata(SurfaceBuilderContext context, int unerodedY, int baseY, int landHeight, int waterHeight, double cellNoiseIn, NTESeed seed, SurfaceState topState, SurfaceState midState, BlockState fluidState)
    {
        final double rockTypeNoise = hashDouble(cellNoiseIn, 856);
        final NTECellular1D cells = new NTECellular1D(seed.seed()).spread(0.25);
        int surfaceDepth = -1;
        SurfaceState surfaceState = null;
        boolean isUnderWater = false;
        final int shoreLevel = 4 + (int) NTEShoreNoiseHelpers.shoreTideLevelNoise(NTESeed.unsafeOf(seed.seed())).noise(context.pos().getX(), context.pos().getZ());
        if (waterHeight > landHeight)
        {
            for (int y = waterHeight; y >= landHeight; --y) context.setBlockState(y, fluidState);
            context.setBlockState(landHeight, getStratifiedStoneBlock(unerodedY, landHeight, cells, rockTypeNoise, Rock.BlockType.GRAVEL));
            for (int y = landHeight - 1; y >= landHeight - 5; --y) context.setBlockState(y, getStratifiedStoneBlock(unerodedY, y, cells, rockTypeNoise, Rock.BlockType.RAW));
            isUnderWater = true;
        }
        for (int y = landHeight; y >= baseY; --y)
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
                    if (y < shoreLevel || isUnderWater)
                    {
                        surfaceDepth = altitudeDepth(context, y, -1);
                        if (surfaceDepth < -1) { surfaceDepth = 0; context.setBlockState(y, getStratifiedStoneBlock(unerodedY, y, cells, rockTypeNoise, Rock.BlockType.RAW)); }
                        else if (surfaceDepth == -1) { surfaceDepth = 0; context.setBlockState(y, getStratifiedStoneBlock(unerodedY, y, cells, rockTypeNoise, Rock.BlockType.GRAVEL)); }
                        else { context.setBlockState(y, getStratifiedStoneBlock(unerodedY, y, cells, rockTypeNoise, Rock.BlockType.GRAVEL)); }
                    }
                    else
                    {
                        surfaceDepth = altitudeDepth(context, y, -3);
                        if (surfaceDepth < -1) { surfaceDepth = 0; context.setBlockState(y, getStratifiedStoneBlock(unerodedY, y, cells, rockTypeNoise, Rock.BlockType.RAW)); }
                        else if (surfaceDepth == -1) { surfaceDepth = 0; context.setBlockState(y, getStratifiedStoneBlock(unerodedY, y, cells, rockTypeNoise, Rock.BlockType.GRAVEL)); }
                        else { context.setBlockState(y, topState); }
                        surfaceState = midState;
                    }
                }
                else if (surfaceDepth > 0)
                {
                    surfaceDepth--;
                    if (surfaceState == null) context.setBlockState(y, getStratifiedStoneBlock(unerodedY, y, cells, rockTypeNoise, Rock.BlockType.GRAVEL));
                    else context.setBlockState(y, surfaceState);
                }
                else
                {
                    context.setBlockState(y, getStratifiedStoneBlock(unerodedY, y, cells, rockTypeNoise, Rock.BlockType.RAW));
                }
            }
        }
    }

    public static BlockState getStratifiedStoneBlock(int unerodedY, int y, NTECellular1D cells, double rockTypeNoiseIn, Rock.BlockType blockType)
    {
        final NTECellular1D.Cell cell = cells.cell(unerodedY - y);
        if (cell.noise() > 0.3) return NTERock.TUFF.getBlock(blockType).get().defaultBlockState();
        if (cell.noise() < -0.8) return TFCBlocks.ROCK_BLOCKS.get(Rock.BASALT).get(blockType).get().defaultBlockState();
        final double rockTypeNoise = hashDouble(rockTypeNoiseIn, 9673);
        final Rock rock = rockTypeNoise > 0.67 ? Rock.ANDESITE : rockTypeNoise > 0.33 ? Rock.DACITE : Rock.RHYOLITE;
        return TFCBlocks.ROCK_BLOCKS.get(rock).get(blockType).get().defaultBlockState();
    }

    public static void buildNormalBatholithSurface(SurfaceBuilderContext context, int startY, int endY, SurfaceState topState, SurfaceState midState, SurfaceState underState, SurfaceState underWaterState, SurfaceState thinUnderWaterState, SurfaceState rockState)
    {
        int surfaceDepth = -1;
        boolean underwaterLayer = false;
        boolean firstLayer = false;
        SurfaceState surfaceState = net.dries007.tfc.world.surface.SurfaceStates.RAW;
        for (int y = startY; y >= endY; --y)
        {
            final BlockState stateAt = context.getBlockState(y);
            if (stateAt.isAir()) surfaceDepth = -1;
            else if (context.isDefaultBlock(stateAt))
            {
                if (surfaceDepth == -1)
                {
                    firstLayer = true;
                    if (y < context.getSeaLevel() - 1)
                    {
                        surfaceDepth = altitudeDepth(context, y, -1);
                        if (surfaceDepth < -1) { surfaceDepth = 0; context.setBlockState(y, rockState); }
                        else if (surfaceDepth == -1) { surfaceDepth = 0; context.setBlockState(y, thinUnderWaterState); }
                        else context.setBlockState(y, underWaterState);
                        surfaceState = underWaterState;
                        underwaterLayer = true;
                    }
                    else
                    {
                        surfaceDepth = altitudeDepth(context, y, -3);
                        if (surfaceDepth < -1) { surfaceDepth = 0; context.setBlockState(y, rockState); }
                        else if (surfaceDepth == -1) { surfaceDepth = 0; context.setBlockState(y, underState); }
                        else context.setBlockState(y, topState);
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
                        surfaceDepth = altitudeDepth(context, y, 0);
                        surfaceState = underwaterLayer ? thinUnderWaterState : underState;
                    }
                }
                else context.setBlockState(y, rockState);
            }
        }
    }

    private static int altitudeDepth(SurfaceBuilderContext context, int y, int minimum)
    {
        final double slopeFactor = 1 - Mth.clamp(context.getSlope() / 15d, 0, 1);
        final double seaFactor = y < context.getSeaLevel() ? Mth.clampedMap((context.getSeaLevel() - y) / 15d, 0, 0.4, 1, 1.4) : 1;
        final int maxDepth = y < context.getSeaLevel() + 7 ? 5 : (int) Mth.clampedMap(y, context.getSeaLevel() + 7, context.getSeaLevel() + 67, 5, 2);
        return Mth.clamp((int) Mth.lerp(slopeFactor * seaFactor, minimum, maxDepth), minimum, maxDepth);
    }

    private static double hashDouble(double input, int index)
    {
        long bits = Double.doubleToLongBits(input) + index;
        bits ^= bits >>> 33;
        bits *= 0xff51afd7ed558ccdL;
        bits ^= bits >>> 33;
        bits *= 0xc4ceb9fe1a85ec53L;
        bits ^= bits >>> 33;
        return (bits >>> 11) * 0x1.0p-53;
    }

    private static double diamondAngle(double x, double y)
    {
        if (y >= 0) return x >= 0 ? y / (x + y) : 1 - x / (-x + y);
        return x < 0 ? 2 - y / (-x - y) : 3 + x / (x - y);
    }

    private static double hyperbolicSection(double x, double xIntercept, double yIntercept)
    {
        return yIntercept * ((2 / (x / xIntercept + 1)) - 1);
    }
}
