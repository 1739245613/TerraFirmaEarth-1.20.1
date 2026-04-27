package com.newterraearth.tfe.debug;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.IntPredicate;
import java.util.function.LongFunction;
import java.util.function.Predicate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.jetbrains.annotations.Nullable;
import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.ChunkBiomeSampler;
import net.dries007.tfc.world.FastConcurrentCache;
import net.dries007.tfc.world.biome.BiomeBlendType;
import net.dries007.tfc.world.biome.BiomeBuilder;
import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.RegionPartition;
import net.dries007.tfc.world.region.RiverEdge;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.layer.SmoothLayer;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.UniformLayer;
import net.dries007.tfc.world.layer.ZoomLayer;
import net.dries007.tfc.world.layer.MoreShoresLayer;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.layer.framework.AdjacentTransformLayer;
import net.dries007.tfc.world.layer.framework.AreaContext;
import net.dries007.tfc.world.layer.framework.ConcurrentArea;
import net.dries007.tfc.world.river.RiverBlendType;
import net.dries007.tfc.world.river.RiverInfo;
import net.dries007.tfc.world.river.MidpointFractal;
import net.dries007.tfc.world.settings.Settings;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;

import com.newterraearth.tfe.world.NTEBiomeNoise;
import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.NTELayerIds;
import com.newterraearth.tfe.mixin.TFCLayersMixin;
import com.newterraearth.tfe.world.layer.NTEIceSheetEdgeLayer;
import com.newterraearth.tfe.world.noise.NTECellular2D;
import com.newterraearth.tfe.world.region.NTERegionNoise;
import com.newterraearth.tfe.world.river.NTERiverBlendType;
import com.newterraearth.tfe.world.river.NTERiverNoiseSampler;
import com.newterraearth.tfe.world.shore.NTEShoreBlendType;
import com.newterraearth.tfe.world.shore.NTEShoreNoiseHelpers;
import com.newterraearth.tfe.world.shore.NTEShoreNoiseSampler;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureBlendType;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoise;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoiseSampler;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

/**
 * Primarily pure-JVM diagnostics for the giant shield-volcano chain.
 * The static and bootstrap-free summaries do not require mixin application.
 * The "actual local runtime" summary reconstructs the current addon biome and
 * height path in plain JVM so it can be compared without ModLauncher.
 */
public final class VolcanoDiagnosticMain
{
    private static final double HOTSPOT_THRESHOLD = 0.65;
    private static final double EXPANSION_THRESHOLD = 0.15;
    private static final sun.misc.Unsafe UNSAFE = getUnsafe();
    private static volatile boolean minecraftBootstrapped = false;
    private static volatile Settings defaultSettings = null;

    private VolcanoDiagnosticMain()
    {
    }

    public static void main(String[] args)
    {
        ensureForgeModListStub();
        final Config config = Config.parse(args);
        final List<Long> seeds = config.seeds();

        System.out.printf(
            "Volcano diagnostics: seeds=%s gridRadius=%d sampleStep=%d top=%d%n",
            seeds,
            config.gridRadius(),
            config.sampleStep(),
            config.top()
        );

        for (long seed : seeds)
        {
            System.out.println();
            System.out.printf("=== Seed %d ===%n", seed);
            final Analysis analysis = analyzeSeed(seed, config.gridRadius(), config.sampleStep());
            printSummary(analysis, config.sampleStep(), config.top());
        }
    }

    private static Analysis analyzeSeed(long seed, int gridRadius, int sampleStep)
    {
        final Grid grid = new Grid(gridRadius);
        final Cellular2D plateNoise = NTERegionNoise.plateRegions(seed).spread(128);
        final Noise2D hotSpotIntensity = NTERegionNoise.hotSpotIntensity(seed).spread(128);
        final Noise2D hotSpotAge = NTERegionNoise.hotSpotAge(seed).spread(128);

        final ArrayDeque<Integer> queue = new ArrayDeque<>();

        for (int z = -gridRadius; z <= gridRadius; z++)
        {
            for (int x = -gridRadius; x <= gridRadius; x++)
            {
                final int index = grid.index(x, z);
                final double intensity = hotSpotIntensity.noise(shift(x), shift(z));
                grid.intensities[index] = intensity;

                final Cellular2D.Cell cell = plateNoise.cell(x, z);
                final double edgeDistance = Math.abs(cell.f1() - cell.f2());
                if (intensity > HOTSPOT_THRESHOLD && edgeDistance > 0.05)
                {
                    final byte age = (byte) ((int) hotSpotAge.noise(shift(x), shift(z)));
                    if (age > 0)
                    {
                        grid.ages[index] = age;
                        queue.add(index);
                    }
                }
            }
        }

        while (!queue.isEmpty())
        {
            final int index = queue.removeFirst();
            final byte age = grid.ages[index];
            final int x = grid.x(index);
            final int z = grid.z(index);

            for (int dz = -1; dz <= 1; dz++)
            {
                for (int dx = -1; dx <= 1; dx++)
                {
                    if (dx == 0 && dz == 0)
                    {
                        continue;
                    }

                    final int nx = x + dx;
                    final int nz = z + dz;
                    if (!grid.inBounds(nx, nz))
                    {
                        continue;
                    }

                    final int nextIndex = grid.index(nx, nz);
                    if (grid.ages[nextIndex] != 0)
                    {
                        continue;
                    }

                    final double intensity = grid.intensities[nextIndex];
                    if (intensity > EXPANSION_THRESHOLD)
                    {
                        grid.ages[nextIndex] = age;
                        queue.add(nextIndex);
                    }
                    else
                    {
                        final double buffer = hotSpotIntensity.noise(shift(nx) - dx, shift(nz) - dz);
                        if (buffer > EXPANSION_THRESHOLD)
                        {
                            grid.ages[nextIndex] = age;
                        }
                    }
                }
            }
        }

        return analyzeComponents(seed, grid, sampleStep);
    }

    private static Analysis analyzeComponents(long seed, Grid grid, int sampleStep)
    {
        final boolean[] visited = new boolean[grid.ages.length];
        final int[] componentIds = new int[grid.ages.length];
        Arrays.fill(componentIds, -1);
        final List<Component> components = new ArrayList<>();

        for (int index = 0; index < grid.ages.length; index++)
        {
            final byte age = grid.ages[index];
            if (age == 0 || visited[index])
            {
                continue;
            }

            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            int count = 0;
            double maxIntensity = Double.NEGATIVE_INFINITY;
            int peakIndex = index;
            final int componentId = components.size();

            final ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(index);
            visited[index] = true;
            componentIds[index] = componentId;

            while (!queue.isEmpty())
            {
                final int current = queue.removeFirst();
                final int x = grid.x(current);
                final int z = grid.z(current);
                final double intensity = grid.intensities[current];

                count++;
                if (intensity > maxIntensity)
                {
                    maxIntensity = intensity;
                    peakIndex = current;
                }
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);

                for (int dz = -1; dz <= 1; dz++)
                {
                    for (int dx = -1; dx <= 1; dx++)
                    {
                        if (dx == 0 && dz == 0)
                        {
                            continue;
                        }

                        final int nx = x + dx;
                        final int nz = z + dz;
                        if (!grid.inBounds(nx, nz))
                        {
                            continue;
                        }

                        final int next = grid.index(nx, nz);
                        if (!visited[next] && grid.ages[next] == age)
                        {
                            visited[next] = true;
                            componentIds[next] = componentId;
                            queue.add(next);
                        }
                    }
                }
            }

            final HeightMetrics heights = sampleHeights(seed, age, minX, maxX, minZ, maxZ, sampleStep);
            components.add(new Component(
                componentId,
                age,
                count,
                minX,
                maxX,
                minZ,
                maxZ,
                grid.x(peakIndex),
                grid.z(peakIndex),
                maxIntensity,
                heights
            ));
        }

        components.sort(Comparator.comparingDouble(Component::score).reversed());
        return new Analysis(seed, grid, componentIds, components);
    }

    private static HeightMetrics sampleHeights(long seed, byte age, int minGridX, int maxGridX, int minGridZ, int maxGridZ, int sampleStep)
    {
        final HeightSampler sampler = switch (age)
        {
            case 1 -> activeShieldVolcanoSampler(seed);
            case 2 -> dormantShieldVolcanoSampler(seed);
            case 3 -> extinctShieldVolcanoSampler(seed);
            case 4 -> ancientShieldVolcanoSampler(seed);
            default -> throw new IllegalArgumentException("Unsupported hotspot age: " + age);
        };
        return sampleHeights(sampler, minGridX, maxGridX, minGridZ, maxGridZ, sampleStep);
    }

    private static HeightMetrics sampleSunkenHeights(long seed, int minGridX, int maxGridX, int minGridZ, int maxGridZ, int sampleStep)
    {
        return sampleHeights(sunkenShieldVolcanoSampler(seed), minGridX, maxGridX, minGridZ, maxGridZ, sampleStep);
    }

    private static HeightMetrics sampleHeights(HeightSampler sampler, int minGridX, int maxGridX, int minGridZ, int maxGridZ, int sampleStep)
    {
        final int expandGrid = 2;
        final int minBlockX = (minGridX - expandGrid) * Units.GRID_WIDTH_IN_BLOCK;
        final int maxBlockX = (maxGridX + expandGrid + 1) * Units.GRID_WIDTH_IN_BLOCK;
        final int minBlockZ = (minGridZ - expandGrid) * Units.GRID_WIDTH_IN_BLOCK;
        final int maxBlockZ = (maxGridZ + expandGrid + 1) * Units.GRID_WIDTH_IN_BLOCK;

        double maxHeight = Double.NEGATIVE_INFINITY;
        double minHeight = Double.POSITIVE_INFINITY;
        int maxHeightX = minBlockX;
        int maxHeightZ = minBlockZ;
        int above40 = 0;
        int above70 = 0;
        int above90 = 0;

        for (int z = minBlockZ; z <= maxBlockZ; z += sampleStep)
        {
            for (int x = minBlockX; x <= maxBlockX; x += sampleStep)
            {
                final double height = sampler.height(x, z);
                if (height > maxHeight)
                {
                    maxHeight = height;
                    maxHeightX = x;
                    maxHeightZ = z;
                }
                minHeight = Math.min(minHeight, height);
                if (height >= SEA_LEVEL_Y + 40)
                {
                    above40++;
                }
                if (height >= SEA_LEVEL_Y + 70)
                {
                    above70++;
                }
                if (height >= SEA_LEVEL_Y + 90)
                {
                    above90++;
                }
            }
        }

        return new HeightMetrics(
            maxHeight,
            minHeight,
            maxHeightX,
            maxHeightZ,
            equivalentDiameter(above40, sampleStep),
            equivalentDiameter(above70, sampleStep),
            equivalentDiameter(above90, sampleStep)
        );
    }

    private static double equivalentDiameter(int sampleCount, int sampleStep)
    {
        if (sampleCount <= 0)
        {
            return 0;
        }
        final double area = sampleCount * sampleStep * sampleStep;
        return 2d * Math.sqrt(area / Math.PI);
    }

    private static void printSummary(Analysis analysis, int sampleStep, int top)
    {
        final long active = analysis.components.stream().filter(c -> c.age == 1).count();
        final long dormant = analysis.components.stream().filter(c -> c.age == 2).count();
        final long extinct = analysis.components.stream().filter(c -> c.age == 3).count();
        final long ancient = analysis.components.stream().filter(c -> c.age == 4).count();

        System.out.printf("Hotspot components: active=%d dormant=%d extinct=%d ancient=%d%n", active, dormant, extinct, ancient);

        for (byte age = 1; age <= 4; age++)
        {
            final byte ageValue = age;
            final List<Component> candidates = analysis.components.stream()
                .filter(component -> component.age == ageValue)
                .limit(top)
                .toList();

            if (candidates.isEmpty())
            {
                continue;
            }

            System.out.printf("Top %s candidates:%n", ageName(ageValue));
            for (Component component : candidates)
            {
                System.out.printf(
                    "  points=%d bbox=%dx%d grid (~%dx%d blocks) hotspot≈(%d,%d) peak≈(%d,%d) peakIntensity=%.3f maxY=%.1f minY=%.1f diam@+40=%.0f diam@+70=%.0f diam@+90=%.0f%n",
                    component.count,
                    component.gridWidth(),
                    component.gridHeight(),
                    component.gridWidth() * Units.GRID_WIDTH_IN_BLOCK,
                    component.gridHeight() * Units.GRID_WIDTH_IN_BLOCK,
                    component.peakGridX * Units.GRID_WIDTH_IN_BLOCK,
                    component.peakGridZ * Units.GRID_WIDTH_IN_BLOCK,
                    component.heights.maxHeightX,
                    component.heights.maxHeightZ,
                    component.maxIntensity,
                    component.heights.maxHeight,
                    component.heights.minHeight,
                    component.heights.diameterAt40,
                    component.heights.diameterAt70,
                    component.heights.diameterAt90
                );
            }
        }

        final Component bestSunken = analysis.components.stream()
            .filter(component -> component.age == 4)
            .max(Comparator.comparingDouble(component -> sampleSunkenHeights(
                analysis.seed,
                component.minGridX,
                component.maxGridX,
                component.minGridZ,
                component.maxGridZ,
                sampleStep
            ).diameterAt40))
            .orElse(null);
        if (bestSunken != null)
        {
            final HeightMetrics sunken = sampleSunkenHeights(
                analysis.seed,
                bestSunken.minGridX,
                bestSunken.maxGridX,
                bestSunken.minGridZ,
                bestSunken.maxGridZ,
                sampleStep
            );
            System.out.printf(
                "Best sunken candidate: hotspot≈(%d,%d) peak≈(%d,%d) maxY=%.1f minY=%.1f diam@+40=%.0f diam@+70=%.0f diam@+90=%.0f%n",
                bestSunken.peakGridX * Units.GRID_WIDTH_IN_BLOCK,
                bestSunken.peakGridZ * Units.GRID_WIDTH_IN_BLOCK,
                sunken.maxHeightX,
                sunken.maxHeightZ,
                sunken.maxHeight,
                sunken.minHeight,
                sunken.diameterAt40,
                sunken.diameterAt70,
                sunken.diameterAt90
            );
        }

        printRuntimeBiomeSummary(analysis, sampleStep, top);
        printActualLocalRuntimeSummary(analysis, sampleStep, top);
    }

    private static void printRuntimeBiomeSummary(Analysis analysis, int sampleStep, int top)
    {
        final RuntimeBiomeContext context = RuntimeBiomeContext.create(analysis.seed);
        System.out.println("Bootstrap-free continent-aligned candidates:");

        for (byte age = 1; age <= 4; age++)
        {
            final byte ageValue = age;
            final HeightSampler sampler = switch (ageValue)
            {
                case 1 -> activeShieldVolcanoSampler(analysis.seed);
                case 2 -> dormantShieldVolcanoSampler(analysis.seed);
                case 3 -> extinctShieldVolcanoSampler(analysis.seed);
                case 4 -> ancientShieldVolcanoSampler(analysis.seed);
                default -> throw new IllegalArgumentException("Unsupported hotspot age: " + ageValue);
            };

            final List<Component> candidates = analysis.components.stream()
                .filter(component -> component.age == ageValue)
                .limit(top)
                .toList();

            boolean printed = false;
            for (Component component : candidates)
            {
                final RuntimeBiomeMetrics metrics = sampleRuntimeBiomeCandidate(analysis, context, component, sampler, ageValue, false, sampleStep);
                if (metrics == null)
                {
                    continue;
                }

                if (!printed)
                {
                    System.out.printf("  %s:%n", ageName(ageValue));
                    printed = true;
                }

                System.out.printf(
                    "    biomeSamples=%d peak~(%d,%d) maxY=%.1f minY=%.1f diam@+40=%.0f diam@+70=%.0f diam@+90=%.0f%n",
                    metrics.sampleCount,
                    metrics.heights.maxHeightX,
                    metrics.heights.maxHeightZ,
                    metrics.heights.maxHeight,
                    metrics.heights.minHeight,
                    metrics.heights.diameterAt40,
                    metrics.heights.diameterAt70,
                    metrics.heights.diameterAt90
                );
            }

            if (!printed)
            {
                System.out.printf("  %s: no matching runtime region cells found in top hotspot components%n", ageName(ageValue));
            }
        }

        final List<Component> ancientComponents = analysis.components.stream()
            .filter(component -> component.age == 4)
            .limit(top)
            .toList();
        final HeightSampler sunkenSampler = sunkenShieldVolcanoSampler(analysis.seed);
        RuntimeBiomeMetrics bestSunken = null;
        for (Component component : ancientComponents)
        {
            final RuntimeBiomeMetrics metrics = sampleRuntimeBiomeCandidate(analysis, context, component, sunkenSampler, (byte) 4, true, sampleStep);
            if (metrics != null && (bestSunken == null || metrics.heights.maxHeight > bestSunken.heights.maxHeight))
            {
                bestSunken = metrics;
            }
        }

        if (bestSunken != null)
        {
            System.out.printf(
                "  sunken: biomeSamples=%d peak~(%d,%d) maxY=%.1f minY=%.1f diam@+40=%.0f diam@+70=%.0f diam@+90=%.0f%n",
                bestSunken.sampleCount,
                bestSunken.heights.maxHeightX,
                bestSunken.heights.maxHeightZ,
                bestSunken.heights.maxHeight,
                bestSunken.heights.minHeight,
                bestSunken.heights.diameterAt40,
                bestSunken.heights.diameterAt70,
                bestSunken.heights.diameterAt90
            );
        }
        else
        {
            System.out.println("  sunken: no matching runtime region cells found in top ancient hotspot components");
        }
    }

    private static void printActualLocalRuntimeSummary(Analysis analysis, int sampleStep, int top)
    {
        System.out.println("Layered local biome candidates (column height vs terrain density surface):");

        final LocalRuntimeContext context;
        try
        {
            context = LocalRuntimeContext.create(analysis.seed, analysis.grid);
        }
        catch (Throwable t)
        {
            final String reason = describeThrowable(t);
            System.out.printf("  unavailable in plain JVM task: %s%n", reason);
            t.printStackTrace(System.out);
            return;
        }

        final int blockStep = Math.max(8, sampleStep);
        for (byte age = 1; age <= 4; age++)
        {
            final byte ageValue = age;
            final List<Component> candidates = analysis.components.stream()
                .filter(component -> component.age == ageValue)
                .limit(top)
                .toList();

            boolean printed = false;
            for (Component component : candidates)
            {
                final LocalRuntimeMetrics metrics = sampleActualLocalRuntimeCandidate(analysis, context, component, blockStep);
                if (metrics == null)
                {
                    continue;
                }

                if (!printed)
                {
                    System.out.printf("  %s:%n", ageName(ageValue));
                    printed = true;
                }

                System.out.printf(
                    "    biome=%s samples=%d columnPeak=(%d,%d) columnMaxY=%.1f terrainPeak=(%d,%d) terrainMaxY=%.1f terrainMinY=%.1f terrDiam@+40=%.0f terrDiam@+70=%.0f terrDiam@+90=%.0f%n",
                    metrics.biomeName,
                    metrics.sampleCount,
                    metrics.columnHeights.maxHeightX,
                    metrics.columnHeights.maxHeightZ,
                    metrics.columnHeights.maxHeight,
                    metrics.terrainHeights.maxHeightX,
                    metrics.terrainHeights.maxHeightZ,
                    metrics.terrainHeights.maxHeight,
                    metrics.terrainHeights.minHeight,
                    metrics.terrainHeights.diameterAt40,
                    metrics.terrainHeights.diameterAt70,
                    metrics.terrainHeights.diameterAt90
                );
            }

            if (!printed)
            {
                System.out.printf("  %s: no shield-volcano biome samples found in top hotspot components%n", ageName(ageValue));
            }
        }
    }

    private static RuntimeBiomeMetrics sampleRuntimeBiomeCandidate(Analysis analysis, RuntimeBiomeContext context, Component component, HeightSampler sampler, byte targetAge, boolean sunken, int sampleStep)
    {
        final int expandGrid = 2;
        final int minBlockX = (component.minGridX - expandGrid) * Units.GRID_WIDTH_IN_BLOCK;
        final int maxBlockX = (component.maxGridX + expandGrid + 1) * Units.GRID_WIDTH_IN_BLOCK;
        final int minBlockZ = (component.minGridZ - expandGrid) * Units.GRID_WIDTH_IN_BLOCK;
        final int maxBlockZ = (component.maxGridZ + expandGrid + 1) * Units.GRID_WIDTH_IN_BLOCK;
        final int blockStep = Math.max(8, sampleStep);

        double maxHeight = Double.NEGATIVE_INFINITY;
        double minHeight = Double.POSITIVE_INFINITY;
        int maxHeightX = minBlockX;
        int maxHeightZ = minBlockZ;
        int above40 = 0;
        int above70 = 0;
        int above90 = 0;
        int sampleCount = 0;

        for (int z = minBlockZ; z <= maxBlockZ; z += blockStep)
        {
            for (int x = minBlockX; x <= maxBlockX; x += blockStep)
            {
                final int gridX = Units.blockToGrid(x);
                final int gridZ = Units.blockToGrid(z);
                if (!matchesRuntimeBiome(analysis.grid, context, gridX, gridZ, targetAge, sunken))
                {
                    continue;
                }

                sampleCount++;
                final double height = sampler.height(x, z);
                if (height > maxHeight)
                {
                    maxHeight = height;
                    maxHeightX = x;
                    maxHeightZ = z;
                }
                minHeight = Math.min(minHeight, height);
                if (height >= SEA_LEVEL_Y + 40)
                {
                    above40++;
                }
                if (height >= SEA_LEVEL_Y + 70)
                {
                    above70++;
                }
                if (height >= SEA_LEVEL_Y + 90)
                {
                    above90++;
                }
            }
        }

        if (sampleCount == 0)
        {
            return null;
        }

        return new RuntimeBiomeMetrics(
            sampleCount,
            new HeightMetrics(
                maxHeight,
                minHeight,
                maxHeightX,
                maxHeightZ,
                equivalentDiameter(above40, blockStep),
                equivalentDiameter(above70, blockStep),
                equivalentDiameter(above90, blockStep)
            )
        );
    }

    private static boolean matchesRuntimeBiome(Grid grid, RuntimeBiomeContext context, int gridX, int gridZ, byte targetAge, boolean sunken)
    {
        if (!grid.inBounds(gridX, gridZ))
        {
            return false;
        }

        final byte age = grid.ageAt(gridX, gridZ);
        if (age != targetAge)
        {
            return false;
        }
        if (targetAge != 4)
        {
            return true;
        }

        final boolean land = context.isLand(gridX, gridZ);
        return sunken ? !land : land;
    }

    private static @Nullable LocalRuntimeMetrics sampleActualLocalRuntimeCandidate(Analysis analysis, LocalRuntimeContext context, Component component, int blockStep)
    {
        final int expandGrid = 2;
        final int minBlockX = (component.minGridX - expandGrid) * Units.GRID_WIDTH_IN_BLOCK;
        final int maxBlockX = (component.maxGridX + expandGrid + 1) * Units.GRID_WIDTH_IN_BLOCK;
        final int minBlockZ = (component.minGridZ - expandGrid) * Units.GRID_WIDTH_IN_BLOCK;
        final int maxBlockZ = (component.maxGridZ + expandGrid + 1) * Units.GRID_WIDTH_IN_BLOCK;

        double columnMaxHeight = Double.NEGATIVE_INFINITY;
        double columnMinHeight = Double.POSITIVE_INFINITY;
        int columnMaxHeightX = minBlockX;
        int columnMaxHeightZ = minBlockZ;
        int columnAbove40 = 0;
        int columnAbove70 = 0;
        int columnAbove90 = 0;

        double maxHeight = Double.NEGATIVE_INFINITY;
        double minHeight = Double.POSITIVE_INFINITY;
        int maxHeightX = minBlockX;
        int maxHeightZ = minBlockZ;
        int above40 = 0;
        int above70 = 0;
        int above90 = 0;
        int sampleCount = 0;
        final Map<String, Integer> biomeCounts = new HashMap<>();

        for (int z = minBlockZ; z <= maxBlockZ; z += blockStep)
        {
            for (int x = minBlockX; x <= maxBlockX; x += blockStep)
            {
                final BiomeExtension biome = context.biomeAtBlock(x, z);
                if (!matchesExpectedShieldVolcanoBiome(analysis, context, component, x, z, biome))
                {
                    continue;
                }

                sampleCount++;
                biomeCounts.merge(biome.key().location().toString(), 1, Integer::sum);

                final double columnHeight = context.sampleHeight(x, z);
                if (columnHeight > columnMaxHeight)
                {
                    columnMaxHeight = columnHeight;
                    columnMaxHeightX = x;
                    columnMaxHeightZ = z;
                }
                columnMinHeight = Math.min(columnMinHeight, columnHeight);
                if (columnHeight >= SEA_LEVEL_Y + 40)
                {
                    columnAbove40++;
                }
                if (columnHeight >= SEA_LEVEL_Y + 70)
                {
                    columnAbove70++;
                }
                if (columnHeight >= SEA_LEVEL_Y + 90)
                {
                    columnAbove90++;
                }

                final double height = context.sampleTerrainSurfaceHeight(x, z);
                if (height > maxHeight)
                {
                    maxHeight = height;
                    maxHeightX = x;
                    maxHeightZ = z;
                }
                minHeight = Math.min(minHeight, height);
                if (height >= SEA_LEVEL_Y + 40)
                {
                    above40++;
                }
                if (height >= SEA_LEVEL_Y + 70)
                {
                    above70++;
                }
                if (height >= SEA_LEVEL_Y + 90)
                {
                    above90++;
                }
            }
        }

        if (sampleCount == 0)
        {
            return null;
        }

        final String biomeName = biomeCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("unknown");

        return new LocalRuntimeMetrics(
            biomeName,
            sampleCount,
            new HeightMetrics(
                columnMaxHeight,
                columnMinHeight,
                columnMaxHeightX,
                columnMaxHeightZ,
                equivalentDiameter(columnAbove40, blockStep),
                equivalentDiameter(columnAbove70, blockStep),
                equivalentDiameter(columnAbove90, blockStep)
            ),
            new HeightMetrics(
                maxHeight,
                minHeight,
                maxHeightX,
                maxHeightZ,
                equivalentDiameter(above40, blockStep),
                equivalentDiameter(above70, blockStep),
                equivalentDiameter(above90, blockStep)
            )
        );
    }

    private static boolean matchesExpectedShieldVolcanoBiome(Analysis analysis, LocalRuntimeContext context, Component component, int blockX, int blockZ, BiomeExtension biome)
    {
        final int gridX = Units.blockToGrid(blockX);
        final int gridZ = Units.blockToGrid(blockZ);
        if (!context.grid.inBounds(gridX, gridZ)
            || context.grid.ageAt(gridX, gridZ) != component.age
            || analysis.componentIdAt(gridX, gridZ) != component.id)
        {
            return false;
        }

        final Region.Point point = context.biomeSource.regionGenerator.getOrCreateRegionPoint(gridX, gridZ);
        return matchesBiomePath(biome, expectedShieldVolcanoBiomePath(component.age, point));
    }

    private static String expectedShieldVolcanoBiomePath(byte age, Region.Point point)
    {
        if (age <= 3)
        {
            final float maxIceSheetTemp = -14f + 0.006f * point.rainfall;
            if (point.land() && point.temperature < maxIceSheetTemp)
            {
                return "ice_sheet_shield_volcano";
            }
            if (point.temperature < maxIceSheetTemp + 4f)
            {
                return "glaciated_shield_volcano";
            }
        }

        return switch (age)
        {
            case 1 -> "active_shield_volcano";
            case 2 -> "dormant_shield_volcano";
            case 3 -> "extinct_shield_volcano";
            case 4 -> point.land() ? "ancient_shield_volcano" : "sunken_shield_volcano";
            default -> "";
        };
    }

    private static boolean matchesBiomePath(BiomeExtension biome, String suffix)
    {
        final String path = biome.key().location().getPath();
        return path.equals(suffix) || path.endsWith("_" + suffix);
    }

    private static synchronized Settings loadDefaultSettings()
    {
        ensureMinecraftBootstrap();

        if (defaultSettings != null)
        {
            return defaultSettings;
        }

        // These match the bundled overworld preset. rockLayerSettings is intentionally left null because
        // the runtime diagnostic only evaluates the region and biome chain up to CHOOSE_BIOMES.
        defaultSettings = new Settings(false, 4000, 0, 0, 20000, 0f, 20000, 0f, null, 0.5f, 0.5f);
        return defaultSettings;
    }

    private static synchronized void ensureMinecraftBootstrap()
    {
        if (!minecraftBootstrapped)
        {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            minecraftBootstrapped = true;
        }
    }

    private static HeightSampler activeShieldVolcanoSampler(long seed)
    {
        final Noise2D base = NTEBiomeNoise.activeShieldVolcano(seed);
        final NTECellular2D cellNoise = new NTECellular2D(seed).spread(0.009f);
        final Noise2D jitterNoise = new OpenSimplex2D(seed + 8179234123L).octaves(2).scaled(-0.0016f, 0.0016f).spread(0.128f);
        return (x, z) -> applyCinderFeature(base.noise(x, z), x, z, cellNoise, jitterNoise, 4, 15, 25);
    }

    private static HeightSampler dormantShieldVolcanoSampler(long seed)
    {
        return createTuffRingSampler(seed, NTEBiomeNoise.dormantShieldVolcano(seed), 2, 0, 36);
    }

    private static HeightSampler extinctShieldVolcanoSampler(long seed)
    {
        return createTuffRingSampler(seed, NTEBiomeNoise.extinctShieldVolcano(seed), 2, 0, 26);
    }

    private static HeightSampler ancientShieldVolcanoSampler(long seed)
    {
        return createTuffRingSampler(seed, NTEBiomeNoise.ancientShieldVolcano(seed), 3, -16, 30);
    }

    private static HeightSampler sunkenShieldVolcanoSampler(long seed)
    {
        return createTuffRingSampler(seed, NTEBiomeNoise.sunkenShieldVolcano(seed), 2, -8, 24);
    }

    private static HeightSampler createTuffRingSampler(long seed, Noise2D base, int rarity, int baseHeight, int scaleHeight)
    {
        final NTESeed featureSeed = NTESeed.of(seed);
        final long stableSeed = featureSeed.seed();
        final NTECellular2D cellNoise = new NTECellular2D(stableSeed, 0.2f, 1).spread(0.003f);
        final Noise2D jitterNoise = new OpenSimplex2D(stableSeed + 1234123L).octaves(2).scaled(-0.032f, 0.032f).spread(0.064f);
        final Noise2D addedCliffNoise = new OpenSimplex2D(stableSeed).octaves(2).spread(0.1).scaled(-2, 10);
        final Noise2D everywhereNoise = new OpenSimplex2D(featureSeed.next()).octaves(3).spread(0.03).scaled(-10, 10);
        return (x, z) -> applyTuffRingFeature(
            base.noise(x, z),
            x,
            z,
            cellNoise,
            jitterNoise,
            addedCliffNoise,
            everywhereNoise,
            rarity,
            baseHeight,
            scaleHeight
        );
    }

    private static double applyCinderFeature(double heightIn, int x, int z, NTECellular2D cellNoise, Noise2D jitterNoise, int rarity, int baseHeight, int scaleHeight)
    {
        final NTECellular2D.Cell cell = cellNoise.cell(x, z);
        if (!checkCellRarity(cell, rarity))
        {
            return heightIn;
        }

        final double f1 = cell.f1();
        final double easing = Mth.clamp(calculateClampedEasing((float) f1) + jitterNoise.noise(x, z), 0, 1);
        final double shape = calculateCinderShape(1 - easing);
        final double additionalHeight = shape * scaleHeight;
        final double featureHeight = SEA_LEVEL_Y + baseHeight + additionalHeight;
        final double weight = 10f * Mth.clamp((float) cell.f2() - (float) f1, 0f, 0.1f);
        return Mth.lerp(easing * weight, heightIn, 0.2 * featureHeight + 0.8 * Math.max(featureHeight, heightIn + 0.6f * additionalHeight));
    }

    private static double applyTuffRingFeature(double heightIn, int x, int z, NTECellular2D cellNoise, Noise2D jitterNoise, Noise2D addedCliffNoise, Noise2D everywhereNoise, int rarity, int baseHeight, int scaleHeight)
    {
        final NTECellular2D.Cell cell = cellNoise.cell(x, z);
        if (!checkCellRarity(cell, rarity))
        {
            return heightIn;
        }

        final double f1 = cell.f1();
        final double easing = Mth.clamp(calculateClampedEasing((float) f1) + jitterNoise.noise(x, z), 0, 1);
        final double shape = calculateTuffRingShape(1 - easing);
        final double ringAdditionalHeight = shape * scaleHeight + (shape > 0.5 ? addedCliffNoise.noise(x, z) : 0f);
        final double gapAdjustedAdditionalHeight = Math.min(ringAdditionalHeight * getGapVerticalEasing(cell), ringAdditionalHeight);
        final double ringHeight = SEA_LEVEL_Y + baseHeight + gapAdjustedAdditionalHeight + everywhereNoise.noise(x, z);
        final double delta = 25 * Mth.clamp(cell.f2() - f1, 0, 0.04);
        return Mth.lerp(delta, heightIn, Math.max(ringHeight, heightIn));
    }

    private static boolean checkCellRarity(NTECellular2D.Cell cell, int rarity)
    {
        return rarity > 0 && Math.abs(cell.noise()) <= 1.0 / rarity;
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

    private static String ageName(byte age)
    {
        return switch (age)
        {
            case 1 -> "active";
            case 2 -> "dormant";
            case 3 -> "extinct";
            case 4 -> "ancient";
            default -> "unknown";
        };
    }

    private static double shift(int point)
    {
        return point + 0.5;
    }

    private static String describeThrowable(Throwable throwable)
    {
        final StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        boolean first = true;
        while (current != null)
        {
            if (!first)
            {
                builder.append(" -> caused by ");
            }
            builder.append(current.getClass().getSimpleName());
            if (current.getMessage() != null && !current.getMessage().isBlank())
            {
                builder.append(": ").append(current.getMessage());
            }
            current = current.getCause();
            first = false;
        }
        return builder.toString();
    }

    private static sun.misc.Unsafe getUnsafe()
    {
        try
        {
            final Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (sun.misc.Unsafe) field.get(null);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Unable to access Unsafe for bootstrap-free RegionGenerator diagnostics", e);
        }
    }

    private static synchronized void ensureForgeModListStub()
    {
        if (net.minecraftforge.fml.ModList.get() != null)
        {
            return;
        }

        final net.minecraftforge.fml.ModList modList = net.minecraftforge.fml.ModList.of(List.of(), List.of());
        try
        {
            final Method method = net.minecraftforge.fml.ModList.class.getDeclaredMethod("setLoadedMods", List.class);
            method.setAccessible(true);
            method.invoke(modList, List.of());
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Unable to initialize empty Forge ModList for plain JVM diagnostics", e);
        }
    }

    private static double triangle(double frequency, double value)
    {
        return Math.abs(4f * frequency * value + 1f - 4f * Mth.floor(frequency * value + 0.75f)) - 1f;
    }

    private static Noise2D baseNoise(boolean axisIsX, float scale, float constant)
    {
        final float frequency = Units.GRID_WIDTH_IN_BLOCK / (2f * scale);
        return scale == 0 ? (x, z) -> constant : axisIsX
            ? (x, z) -> triangle(frequency, x)
            : (x, z) -> triangle(frequency, z);
    }

    private static RegionGenerator createBootstrapFreeRegionGenerator(Settings settings, RandomSource random)
    {
        try
        {
            final long seed = random.nextLong();
            final Cellular2D cellNoise = new Cellular2D(random.nextLong()).spread(1f / Units.CELL_WIDTH_IN_GRID);

            final float min = settings.continentalness() * 10f - 2.5f;
            final Noise2D continentNoise = cellNoise.then(cell -> 1 - cell.f1() / (0.37f + cell.f2()))
                .lazyProduct(new OpenSimplex2D(random.nextLong())
                    .spread(0.24f)
                    .scaled(min, 8.7f)
                    .octaves(4));

            final Noise2D temperatureNoise = baseNoise(false, settings.temperatureScale(), settings.temperatureConstant())
                .scaled(-20f, 30f)
                .add(new OpenSimplex2D(random.nextInt())
                    .octaves(2)
                    .spread(0.15f)
                    .scaled(-3f, 3f));

            final Noise2D rainfallNoise = baseNoise(true, settings.rainfallScale(), settings.rainfallConstant())
                .scaled(0f, 500f)
                .add(new OpenSimplex2D(random.nextInt())
                    .octaves(2)
                    .spread(0.15f)
                    .scaled(-80f, 40f));

            final AreaFactory biomeAreaFactory = createUniformLayer(random, 2);
            final AreaFactory rockAreaFactory = createUniformLayer(random, 3);

            final RegionGenerator generator = (RegionGenerator) UNSAFE.allocateInstance(RegionGenerator.class);
            setField(generator, "seed", seed);
            setField(generator, "cellNoise", cellNoise);
            setField(generator, "continentNoise", continentNoise);
            setField(generator, "temperatureNoise", temperatureNoise);
            setField(generator, "rainfallNoise", rainfallNoise);
            setField(generator, "cellCache", new FastConcurrentCache<>(256));
            setField(generator, "partitionCache", new FastConcurrentCache<>(256));
            setField(generator, "biomeArea", ThreadLocal.withInitial(biomeAreaFactory));
            setField(generator, "rockArea", ThreadLocal.withInitial(rockAreaFactory));
            return generator;
        }
        catch (InstantiationException e)
        {
            throw new IllegalStateException("Unable to allocate bootstrap-free RegionGenerator", e);
        }
    }

    private static AreaFactory createUniformLayer(RandomSource random, int zoomLevels)
    {
        AreaFactory layer = UniformLayer.INSTANCE.apply(random.nextLong());
        for (int i = 0; i < zoomLevels; i++)
        {
            layer = ZoomLayer.NORMAL.apply(random.nextLong(), layer);
            layer = SmoothLayer.INSTANCE.apply(random.nextLong(), layer);
        }
        return layer;
    }

    private static AreaFactory createLayeredRegionBiomeLayer(LocalRegionBiomeSelector selector, long seed)
    {
        final Random random = new Random(seed);

        AreaFactory mainLayer = () -> new Area(selector::sampleRegionBiomeId, 1024);
        mainLayer = LocalRegionEdgeBiomeLayer.INSTANCE.apply(random.nextLong(), mainLayer);
        mainLayer = ZoomLayer.NORMAL.apply(random.nextLong(), mainLayer);

        mainLayer = LocalShoreLayer.INSTANCE.apply(random.nextLong(), mainLayer);
        mainLayer = MoreShoresLayer.INSTANCE.apply(random.nextLong(), mainLayer);
        mainLayer = NTEIceSheetEdgeLayer.INSTANCE.apply(random.nextLong(), mainLayer);
        mainLayer = ZoomLayer.NORMAL.apply(random.nextLong(), mainLayer);
        mainLayer = ZoomLayer.NORMAL.apply(random.nextLong(), mainLayer);

        mainLayer = ZoomLayer.NORMAL.apply(random.nextLong(), mainLayer);
        mainLayer = ZoomLayer.NORMAL.apply(random.nextLong(), mainLayer);
        mainLayer = SmoothLayer.INSTANCE.apply(random.nextLong(), mainLayer);
        return mainLayer;
    }

    private static BiomeExtension createOceanBiome(String path, LongFunction<Noise2D> heightNoiseFactory)
    {
        return createBiome(path, BiomeBuilder.builder()
            .heightmap(heightNoiseFactory)
            .surface(NormalSurfaceBuilder.INSTANCE)
            .aquiferHeightOffset(-24)
            .salty()
            .type(BiomeBlendType.OCEAN)
            .noRivers());
    }

    private static BiomeExtension createLandBiome(String path, LongFunction<Noise2D> heightNoiseFactory, RiverBlendType riverBlendType)
    {
        return createBiome(path, BiomeBuilder.builder()
            .heightmap(heightNoiseFactory)
            .surface(NormalSurfaceBuilder.INSTANCE)
            .spawnable()
            .type(riverBlendType));
    }

    private static BiomeExtension createMountainBiome(String path, LongFunction<Noise2D> heightNoiseFactory, boolean salty)
    {
        BiomeBuilder builder = BiomeBuilder.builder()
            .heightmap(heightNoiseFactory)
            .surface(NormalSurfaceBuilder.ROCKY)
            .spawnable()
            .type(RiverBlendType.CAVE)
            .noSandyRiverShores();
        if (salty)
        {
            builder = builder.salty();
        }
        return createBiome(path, builder);
    }

    private static BiomeExtension createShoreBiome(String path, LongFunction<Noise2D> heightNoiseFactory, boolean salty, boolean oceanLike)
    {
        BiomeBuilder builder = BiomeBuilder.builder()
            .heightmap(heightNoiseFactory)
            .surface(NormalSurfaceBuilder.INSTANCE)
            .shore()
            .type(oceanLike ? BiomeBlendType.OCEAN : BiomeBlendType.LAND)
            .type(RiverBlendType.WIDE)
            .noRivers()
            .noSandyRiverShores();
        if (salty)
        {
            builder = builder.salty();
        }
        return createBiome(path, builder);
    }

    private static BiomeExtension createLakeBiome(String path, LongFunction<Noise2D> heightNoiseFactory, boolean salty)
    {
        BiomeBuilder builder = BiomeBuilder.builder()
            .heightmap(heightNoiseFactory)
            .surface(NormalSurfaceBuilder.INSTANCE)
            .type(BiomeBlendType.LAKE)
            .noRivers();
        if (salty)
        {
            builder = builder.salty();
        }
        return createBiome(path, builder);
    }

    private static BiomeExtension createBiome(String path, BiomeBuilder builder)
    {
        final ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, new ResourceLocation("tfc", path));
        return builder.build(key);
    }

    private static void setField(Object instance, String fieldName, Object value)
    {
        try
        {
            final Field field = RegionGenerator.class.getDeclaredField(fieldName);
            final long offset = UNSAFE.objectFieldOffset(field);
            UNSAFE.putObject(instance, offset, value);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Unable to set RegionGenerator field: " + fieldName, e);
        }
    }

    private static void setField(Object instance, String fieldName, long value)
    {
        try
        {
            final Field field = RegionGenerator.class.getDeclaredField(fieldName);
            final long offset = UNSAFE.objectFieldOffset(field);
            UNSAFE.putLong(instance, offset, value);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Unable to set RegionGenerator field: " + fieldName, e);
        }
    }

    private record LocalRuntimeMetrics(String biomeName, int sampleCount, HeightMetrics columnHeights, HeightMetrics terrainHeights)
    {
    }

    private record RuntimeBiomeMetrics(int sampleCount, HeightMetrics heights)
    {
    }

    private record RuntimeBiomeContext(Noise2D continentNoise)
    {
        private static RuntimeBiomeContext create(long seed)
        {
            final RandomSource random = new XoroshiroRandomSource(seed);
            random.nextLong(); // RegionGenerator seed field

            final Cellular2D cellNoise = new Cellular2D(random.nextLong()).spread(1f / Units.CELL_WIDTH_IN_GRID);
            final float min = 2.5f; // Default overworld continentalness = 0.5
            final Noise2D continentNoise = cellNoise.then(cell -> 1 - cell.f1() / (0.37f + cell.f2()))
                .lazyProduct(new OpenSimplex2D(random.nextLong())
                    .spread(0.24f)
                    .scaled(min, 8.7f)
                    .octaves(4));
            return new RuntimeBiomeContext(continentNoise);
        }

        private boolean isLand(int gridX, int gridZ)
        {
            return continentNoise.noise(gridX, gridZ) > 4.4d;
        }
    }

    private enum LocalRegionEdgeBiomeLayer implements AdjacentTransformLayer
    {
        INSTANCE;

        @Override
        public int apply(AreaContext context, int north, int east, int south, int west, int center)
        {
            final Predicate<IntPredicate> matcher = p -> p.test(north) || p.test(east) || p.test(south) || p.test(west);

            if (TFCLayersMixin.isLow(center))
            {
                if (matcher.test(TFCLayers::isOcean) && matcher.test(TFCLayersMixin::isMountains))
                {
                    return TFCLayers.OCEANIC_MOUNTAINS;
                }
                else if (matcher.test(TFCLayers::isOcean) && matcher.test(i -> i == TFCLayers.LOWLANDS))
                {
                    return TFCLayers.SALT_MARSH;
                }
            }

            if (isFlats(center))
            {
                if (matcher.test(TFCLayers::isOcean) && matcher.test(LocalRegionEdgeBiomeLayer::isFlats))
                {
                    return TFCLayers.CANYONS;
                }
            }

            if (center == TFCLayers.PLATEAU || center == TFCLayers.BADLANDS)
            {
                if (matcher.test(i -> i == TFCLayers.LOW_CANYONS || i == TFCLayers.LOWLANDS))
                {
                    return TFCLayers.HILLS;
                }
                else if (matcher.test(i -> i == TFCLayers.PLAINS || i == TFCLayers.HILLS))
                {
                    return TFCLayers.ROLLING_HILLS;
                }
            }
            else if (TFCLayersMixin.isMountains(center))
            {
                if (matcher.test(TFCLayersMixin::isLow))
                {
                    return TFCLayers.ROLLING_HILLS;
                }
            }
            else if (center == TFCLayers.LOWLANDS || center == TFCLayers.LOW_CANYONS)
            {
                if (matcher.test(i -> i == TFCLayers.PLATEAU || i == TFCLayers.BADLANDS))
                {
                    return TFCLayers.HILLS;
                }
                else if (matcher.test(TFCLayersMixin::isMountains))
                {
                    return TFCLayers.ROLLING_HILLS;
                }
            }
            else if (center == TFCLayers.PLAINS || center == TFCLayers.HILLS)
            {
                if (matcher.test(i -> i == TFCLayers.PLATEAU || i == TFCLayers.BADLANDS))
                {
                    return TFCLayers.HILLS;
                }
                else if (matcher.test(TFCLayersMixin::isMountains))
                {
                    return TFCLayers.ROLLING_HILLS;
                }
            }
            else if (center == TFCLayers.DEEP_OCEAN_TRENCH)
            {
                if (matcher.test(i -> !TFCLayers.isOcean(i)))
                {
                    return TFCLayers.OCEAN;
                }
            }
            return center;
        }

        private static boolean isFlats(int value)
        {
            return value == NTELayerIds.MUD_FLATS || value == NTELayerIds.SALT_FLATS;
        }
    }

    private enum LocalShoreLayer implements AdjacentTransformLayer
    {
        INSTANCE;

        @Override
        public int apply(AreaContext context, int north, int east, int south, int west, int center)
        {
            final Predicate<IntPredicate> matcher = p -> p.test(north) || p.test(east) || p.test(south) || p.test(west);
            if (!TFCLayers.isOcean(center) && TFCLayersMixin.hasShore(center) && matcher.test(TFCLayers::isOcean))
            {
                return TFCLayersMixin.shoreFor(center);
            }
            return center;
        }
    }

    private static final class LocalRuntimeContext
    {
        private final long seed;
        private final Grid grid;
        private final LocalBiomeSource biomeSource;
        private final Map<Long, LocalRuntimeHeightSampler> heightFillers;

        private LocalRuntimeContext(long seed, Grid grid, LocalBiomeSource biomeSource)
        {
            this.seed = seed;
            this.grid = grid;
            this.biomeSource = biomeSource;
            this.heightFillers = new ConcurrentHashMap<>();
        }

        private static LocalRuntimeContext create(long seed, Grid grid)
        {
            final Settings settings = loadDefaultSettings();
            final RandomSource random = new XoroshiroRandomSource(seed);
            final RegionGenerator regionGenerator = createBootstrapFreeRegionGenerator(settings, random);

            random.nextLong(); // RegionChunkDataGenerator.create(...)
            final long biomeLayerSeed = random.nextLong(); // TFCLayers.createRegionBiomeLayer(...)

            return new LocalRuntimeContext(seed, grid, new LocalBiomeSource(regionGenerator, grid, biomeLayerSeed));
        }

        private BiomeExtension biomeAtBlock(int blockX, int blockZ)
        {
            return biomeSource.getBiomeExtensionNoRiver(QuartPos.fromBlock(blockX), QuartPos.fromBlock(blockZ));
        }

        private double sampleHeight(int blockX, int blockZ)
        {
            final ChunkPos chunkPos = new ChunkPos(blockX >> 4, blockZ >> 4);
            final long chunkKey = chunkPos.toLong();
            return heightFillers.computeIfAbsent(chunkKey, ignored -> createHeightFiller(chunkPos)).sampleHeight(blockX, blockZ);
        }

        private double sampleTerrainSurfaceHeight(int blockX, int blockZ)
        {
            final ChunkPos chunkPos = new ChunkPos(blockX >> 4, blockZ >> 4);
            final long chunkKey = chunkPos.toLong();
            return heightFillers.computeIfAbsent(chunkKey, ignored -> createHeightFiller(chunkPos)).sampleTerrainSurfaceHeight(blockX, blockZ);
        }

        private LocalRuntimeHeightSampler createHeightFiller(ChunkPos chunkPos)
        {
            final Object2DoubleMap<BiomeExtension>[] biomeWeights = ChunkBiomeSampler.sampleBiomes(chunkPos, this::sampleBiomeNoRiver, BiomeExtension::biomeBlendType);

            final Map<BiomeExtension, BiomeNoiseSampler> biomeNoiseSamplers = new ConcurrentHashMap<>();
            for (BiomeExtension extension : biomeSource.allBiomes())
            {
                final BiomeNoiseSampler sampler = extension.createNoiseSampler(seed);
                if (sampler != null)
                {
                    sampler.prepare(null, null);
                    biomeNoiseSamplers.put(extension, sampler);
                }
            }

            final Map<NTERiverBlendType, NTERiverNoiseSampler> exactRiverSamplers = new ConcurrentHashMap<>();
            final NTESeed riverSeed = NTESeed.of(seed);
            for (NTERiverBlendType blendType : NTERiverBlendType.ALL)
            {
                exactRiverSamplers.put(blendType, blendType.createNoiseSampler(riverSeed));
            }

            final Map<NTEShoreBlendType, NTEShoreNoiseSampler> shoreSamplers = new ConcurrentHashMap<>();
            final NTESeed shoreSeed = NTESeed.unsafeOf(seed);
            for (NTEShoreBlendType blendType : NTEShoreBlendType.ALL)
            {
                shoreSamplers.put(blendType, blendType.createNoiseSampler(shoreSeed));
            }

            return new LocalRuntimeHeightSampler(
                biomeWeights,
                biomeSource,
                biomeNoiseSamplers,
                shoreSamplers,
                NTEShoreNoiseHelpers.shoreTideLevelNoise(NTESeed.unsafeOf(seed)),
                exactRiverSamplers,
                Map.of(),
                SEA_LEVEL_Y
            );
        }

        private BiomeExtension sampleBiomeNoRiver(int blockX, int blockZ)
        {
            return biomeSource.getBiomeExtensionNoRiver(QuartPos.fromBlock(blockX), QuartPos.fromBlock(blockZ));
        }
    }

    private static final class LocalRegionBiomeSelector
    {
        private final RegionGenerator regionGenerator;
        private final Grid grid;

        private LocalRegionBiomeSelector(RegionGenerator regionGenerator, Grid grid)
        {
            this.regionGenerator = regionGenerator;
            this.grid = grid;
        }

        private int sampleRegionBiomeId(int gridX, int gridZ)
        {
            final Region.Point point = regionGenerator.getOrCreateRegionPoint(gridX, gridZ);
            int biome = approximateBaseBiomeId(point);
            final byte hotSpotAge = grid.inBounds(gridX, gridZ) ? grid.ageAt(gridX, gridZ) : 0;
            if (hotSpotAge <= 0)
            {
                return biome;
            }

            if (hotSpotAge == 4 && isOceanLike(biome))
            {
                biome = NTELayerIds.SUNKEN_SHIELD_VOLCANO;
            }
            else
            {
                biome = switch (hotSpotAge)
                {
                    case 1 -> NTELayerIds.ACTIVE_SHIELD_VOLCANO;
                    case 2 -> NTELayerIds.DORMANT_SHIELD_VOLCANO;
                    case 3 -> NTELayerIds.EXTINCT_SHIELD_VOLCANO;
                    case 4 -> NTELayerIds.ANCIENT_SHIELD_VOLCANO;
                    default -> biome;
                };
            }

            final float maxIceSheetTemp = -14f + 0.006f * point.rainfall;
            if (point.land() && hotSpotAge <= 3 && point.temperature < maxIceSheetTemp)
            {
                return NTELayerIds.ICE_SHEET_SHIELD_VOLCANO;
            }
            if (hotSpotAge <= 3 && point.temperature < maxIceSheetTemp + 4f)
            {
                return NTELayerIds.GLACIATED_SHIELD_VOLCANO;
            }
            return biome;
        }

        private int approximateBaseBiomeId(Region.Point point)
        {
            if (!point.land())
            {
                if (point.baseOceanDepth > 9)
                {
                    return TFCLayers.DEEP_OCEAN_TRENCH;
                }
                if (point.baseOceanDepth >= 5 || point.distanceToEdge < 2)
                {
                    return TFCLayers.DEEP_OCEAN;
                }
                return TFCLayers.OCEAN;
            }

            if (point.mountain())
            {
                final float rainfall = point.rainfall;
                final float temperature = point.temperature;
                if (point.coastalMountain())
                {
                    final float maxIceSheetTemp = -16f + 0.006f * rainfall;
                    if (temperature < maxIceSheetTemp + 2f)
                    {
                        return NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS;
                    }
                    if (temperature < maxIceSheetTemp + 6f)
                    {
                        return NTELayerIds.GLACIATED_OCEANIC_MOUNTAINS;
                    }
                    if (temperature < maxIceSheetTemp + 10f)
                    {
                        return NTELayerIds.GLACIALLY_CARVED_OCEANIC_MOUNTAINS;
                    }
                    return TFCLayers.OCEANIC_MOUNTAINS;
                }

                final float maxIceSheetTemp = -14f + 0.006f * rainfall;
                if (temperature < maxIceSheetTemp)
                {
                    return NTELayerIds.ICE_SHEET_MOUNTAINS;
                }
                if (temperature < maxIceSheetTemp + 4f)
                {
                    return NTELayerIds.GLACIATED_MOUNTAINS;
                }
                if (temperature < maxIceSheetTemp + 10f)
                {
                    return NTELayerIds.GLACIALLY_CARVED_MOUNTAINS;
                }
                return point.discreteBiomeAltitude() >= 2 ? TFCLayers.MOUNTAINS : TFCLayers.OLD_MOUNTAINS;
            }

            final float rainfall = point.rainfall;
            final float temperature = point.temperature;
            final float maxIceSheetTemp = -17f + 0.006f * rainfall;
            if (temperature < maxIceSheetTemp)
            {
                return point.distanceToOcean < 3 ? NTELayerIds.ICE_SHEET_OCEANIC : NTELayerIds.ICE_SHEET;
            }
            if (temperature < maxIceSheetTemp + 1f)
            {
                return NTELayerIds.ICE_SHEET_EDGE;
            }

            return switch (point.discreteBiomeAltitude())
            {
                case 0 -> TFCLayers.PLAINS;
                case 1 -> TFCLayers.HILLS;
                default -> TFCLayers.HIGHLANDS;
            };
        }

        private static boolean isOceanLike(int biome)
        {
            return biome == TFCLayers.OCEAN
                || biome == TFCLayers.OCEAN_REEF
                || biome == TFCLayers.DEEP_OCEAN
                || biome == TFCLayers.DEEP_OCEAN_TRENCH;
        }
    }

    private static final class LocalLayerRegistry
    {
        private BiomeExtension[] layers;
        private final Set<BiomeExtension> biomes;
        private int nextId;

        private LocalLayerRegistry()
        {
            this.layers = new BiomeExtension[128];
            this.biomes = new LinkedHashSet<>();
            this.nextId = 64;

            registerBase(TFCLayers.OCEAN, createOceanBiome("debug_ocean", seed -> BiomeNoise.ocean(seed, -26, -12)));
            registerBase(TFCLayers.OCEAN_REEF, createOceanBiome("debug_ocean_reef", seed -> BiomeNoise.ocean(seed, -16, -8)));
            registerBase(TFCLayers.DEEP_OCEAN, createOceanBiome("debug_deep_ocean", seed -> BiomeNoise.ocean(seed, -30, -16)));
            registerBase(TFCLayers.DEEP_OCEAN_TRENCH, createOceanBiome("debug_deep_ocean_trench", seed -> BiomeNoise.oceanRidge(seed, -30, -16)));
            registerBase(TFCLayers.PLAINS, createLandBiome("debug_plains", seed -> BiomeNoise.hills(seed, 4, 10), RiverBlendType.WIDE));
            registerBase(TFCLayers.HILLS, createLandBiome("debug_hills", seed -> BiomeNoise.hills(seed, -5, 16), RiverBlendType.WIDE));
            registerBase(TFCLayers.LOWLANDS, createLandBiome("debug_lowlands", BiomeNoise::lowlands, RiverBlendType.WIDE));
            registerBase(TFCLayers.SALT_MARSH, createLandBiome("debug_salt_marsh", BiomeNoise::lowlands, RiverBlendType.WIDE));
            registerBase(TFCLayers.LOW_CANYONS, createLandBiome("debug_low_canyons", seed -> BiomeNoise.canyons(seed, -8, 21), RiverBlendType.WIDE));
            registerBase(TFCLayers.ROLLING_HILLS, createLandBiome("debug_rolling_hills", seed -> BiomeNoise.hills(seed, -5, 28), RiverBlendType.CANYON));
            registerBase(TFCLayers.HIGHLANDS, createLandBiome("debug_highlands", NTEBiomeNoise::sharpHills, RiverBlendType.CANYON));
            registerBase(TFCLayers.BADLANDS, createLandBiome("debug_badlands", NTEBiomeNoise::badlands, RiverBlendType.CANYON));
            registerBase(TFCLayers.PLATEAU, createLandBiome("debug_plateau", seed -> BiomeNoise.hills(seed, 20, 30), RiverBlendType.TALL_CANYON));
            registerBase(TFCLayers.OLD_MOUNTAINS, createMountainBiome("debug_old_mountains", seed -> BiomeNoise.mountains(seed, 16, 40), false));
            registerBase(TFCLayers.MOUNTAINS, createMountainBiome("debug_mountains", seed -> BiomeNoise.mountains(seed, 10, 70), false));
            registerBase(TFCLayers.VOLCANIC_MOUNTAINS, createMountainBiome("debug_volcanic_mountains", seed -> BiomeNoise.mountains(seed, 10, 60), false));
            registerBase(TFCLayers.OCEANIC_MOUNTAINS, createMountainBiome("debug_oceanic_mountains", seed -> BiomeNoise.mountains(seed, -16, 60), true));
            registerBase(TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS, createMountainBiome("debug_volcanic_oceanic_mountains", seed -> BiomeNoise.mountains(seed, -24, 50), true));
            registerBase(TFCLayers.CANYONS, createLandBiome("debug_canyons", seed -> BiomeNoise.canyons(seed, -2, 40), RiverBlendType.CANYON));
            registerBase(TFCLayers.SHORE, createShoreBiome("debug_shore", BiomeNoise::shore, true, false));
            registerBase(TFCLayers.TIDAL_FLATS, createShoreBiome("debug_tidal_flats", BiomeNoise::shore, true, true));
            registerBase(TFCLayers.LAKE, createLakeBiome("debug_lake", BiomeNoise::lake, false));
            registerBase(TFCLayers.RIVER, get(TFCLayers.PLAINS));
            registerBase(TFCLayers.MOUNTAIN_LAKE, createLakeBiome("debug_mountain_lake", seed -> BiomeNoise.mountains(seed, 10, 70), false));
            registerBase(TFCLayers.VOLCANIC_MOUNTAIN_LAKE, createLakeBiome("debug_volcanic_mountain_lake", seed -> BiomeNoise.mountains(seed, 10, 60), false));
            registerBase(TFCLayers.OLD_MOUNTAIN_LAKE, createLakeBiome("debug_old_mountain_lake", seed -> BiomeNoise.mountains(seed, -16, 60), false));
            registerBase(TFCLayers.OCEANIC_MOUNTAIN_LAKE, createLakeBiome("debug_oceanic_mountain_lake", seed -> BiomeNoise.mountains(seed, -16, 60), true));
            registerBase(TFCLayers.VOLCANIC_OCEANIC_MOUNTAIN_LAKE, createLakeBiome("debug_volcanic_oceanic_mountain_lake", seed -> BiomeNoise.mountains(seed, -24, 50), true));
            registerBase(TFCLayers.PLATEAU_LAKE, createLakeBiome("debug_plateau_lake", seed -> BiomeNoise.hills(seed, 20, 30), false));

            final BiomeExtension plains = get(TFCLayers.PLAINS);
            final BiomeExtension hills = get(TFCLayers.HILLS);
            final BiomeExtension lowlands = get(TFCLayers.LOWLANDS);
            final BiomeExtension rollingHills = get(TFCLayers.ROLLING_HILLS);
            final BiomeExtension highlands = get(TFCLayers.HIGHLANDS);
            final BiomeExtension badlands = get(TFCLayers.BADLANDS);
            final BiomeExtension plateau = get(TFCLayers.PLATEAU);
            final BiomeExtension mountains = get(TFCLayers.MOUNTAINS);
            final BiomeExtension oldMountains = get(TFCLayers.OLD_MOUNTAINS);
            final BiomeExtension oceanicMountains = get(TFCLayers.OCEANIC_MOUNTAINS);
            final BiomeExtension shore = get(TFCLayers.SHORE);
            final BiomeExtension lake = get(TFCLayers.LAKE);
            final BiomeExtension canyons = get(TFCLayers.CANYONS);

            NTELayerIds.PLATEAU_WIDE = registerAlias(plateau);
            NTELayerIds.GUANO_ISLAND = registerAlias(oceanicMountains);
            NTELayerIds.SEA_STACKS = registerAlias(shore);
            NTELayerIds.TERRACE_UPPER = registerAlias(shore);
            NTELayerIds.TERRACE_LOWER = registerAlias(shore);
            NTELayerIds.SETBACK_CLIFFS = registerAlias(shore);
            NTELayerIds.COASTAL_DUNES = registerAlias(shore);
            NTELayerIds.ROCKY_SHORES = registerAlias(shore);
            NTELayerIds.EMBAYMENTS = registerAlias(shore);
            NTELayerIds.MUD_FLATS = registerAlias(lowlands);
            NTELayerIds.SALT_FLATS = registerAlias(lowlands);
            NTELayerIds.DUNE_SEA = registerAlias(hills);
            NTELayerIds.GRASSY_DUNES = registerAlias(hills);
            NTELayerIds.WHORLED_CANYONS = registerAlias(canyons);
            NTELayerIds.STAIR_STEP_CANYONS = registerAlias(canyons);
            NTELayerIds.MESAS = registerAlias(badlands);
            NTELayerIds.BUTTES = registerAlias(badlands);
            NTELayerIds.HOODOOS = registerAlias(badlands);
            NTELayerIds.ROCKY_PLATEAU = registerAlias(plateau);
            NTELayerIds.TOWER_KARST_PLAINS = registerAlias(plains);
            NTELayerIds.TOWER_KARST_CANYONS = registerAlias(canyons);
            NTELayerIds.TOWER_KARST_HILLS = registerAlias(hills);
            NTELayerIds.TOWER_KARST_HIGHLANDS = registerAlias(highlands);
            NTELayerIds.TOWER_KARST_LAKE = registerAlias(lake);
            NTELayerIds.TOWER_KARST_BAY = registerAlias(shore);
            NTELayerIds.BURREN_PLATEAU = registerAlias(plateau);
            NTELayerIds.BURREN_BADLANDS = registerAlias(badlands);
            NTELayerIds.BURREN_BADLANDS_TALL = registerAlias(highlands);
            NTELayerIds.BURREN_ROCHE_MOUTONEE = registerAlias(rollingHills);
            NTELayerIds.BURREN_PLAINS = registerAlias(plains);
            NTELayerIds.SHILIN_PLAINS = registerAlias(plains);
            NTELayerIds.SHILIN_CANYONS = registerAlias(canyons);
            NTELayerIds.SHILIN_HILLS = registerAlias(hills);
            NTELayerIds.SHILIN_HIGHLANDS = registerAlias(highlands);
            NTELayerIds.SHILIN_PLATEAU = registerAlias(plateau);
            NTELayerIds.DOLINE_PLAINS = registerAlias(plains);
            NTELayerIds.DOLINE_HILLS = registerAlias(hills);
            NTELayerIds.DOLINE_ROLLING_HILLS = registerAlias(rollingHills);
            NTELayerIds.DOLINE_HIGHLANDS = registerAlias(highlands);
            NTELayerIds.DOLINE_PLATEAU = registerAlias(plateau);
            NTELayerIds.DOLINE_CANYONS = registerAlias(canyons);
            NTELayerIds.CENOTE_PLAINS = registerAlias(plains);
            NTELayerIds.CENOTE_HILLS = registerAlias(hills);
            NTELayerIds.CENOTE_ROLLING_HILLS = registerAlias(rollingHills);
            NTELayerIds.CENOTE_CANYONS = registerAlias(canyons);
            NTELayerIds.CENOTE_HIGHLANDS = registerAlias(highlands);
            NTELayerIds.CENOTE_PLATEAU = registerAlias(plateau);
            NTELayerIds.EXTREME_DOLINE_PLATEAU = registerAlias(plateau);
            NTELayerIds.EXTREME_DOLINE_MOUNTAINS = registerAlias(mountains);

            NTELayerIds.ACTIVE_SHIELD_VOLCANO = registerBiome(createMountainBiome("debug_active_shield_volcano", NTEBiomeNoise::activeShieldVolcano, false));
            NTELayerIds.DORMANT_SHIELD_VOLCANO = registerBiome(createMountainBiome("debug_dormant_shield_volcano", NTEBiomeNoise::dormantShieldVolcano, false));
            NTELayerIds.EXTINCT_SHIELD_VOLCANO = registerBiome(createMountainBiome("debug_extinct_shield_volcano", NTEBiomeNoise::extinctShieldVolcano, false));
            NTELayerIds.ANCIENT_SHIELD_VOLCANO = registerBiome(createMountainBiome("debug_ancient_shield_volcano", NTEBiomeNoise::ancientShieldVolcano, false));
            NTELayerIds.SUNKEN_SHIELD_VOLCANO = registerBiome(createMountainBiome("debug_sunken_shield_volcano", NTEBiomeNoise::sunkenShieldVolcano, true));
            NTELayerIds.SHIELD_VOLCANO_SHORE = registerBiome(createShoreBiome("debug_shield_volcano_shore", BiomeNoise::shore, true, false));
            NTELayerIds.OLD_SHIELD_VOLCANO_SHORE = registerBiome(createShoreBiome("debug_old_shield_volcano_shore", BiomeNoise::shore, true, false));

            NTELayerIds.ICE_SHEET = registerBiome(createLandBiome("debug_ice_sheet", NTEBiomeNoise::iceSheet, RiverBlendType.CAVE));
            NTELayerIds.ICE_SHEET_MOUNTAINS = registerBiome(createMountainBiome("debug_ice_sheet_mountains", seed -> NTEBiomeNoise.iceSheetMountains(seed, false), false));
            NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS = registerBiome(createMountainBiome("debug_ice_sheet_oceanic_mountains", seed -> NTEBiomeNoise.iceSheetMountains(seed, true), true));
            NTELayerIds.ICE_SHEET_SHIELD_VOLCANO = registerBiome(createMountainBiome("debug_ice_sheet_shield_volcano", NTEBiomeNoise::iceSheetShieldVolcanoTerrain, false));
            NTELayerIds.ICE_SHEET_TUYAS = registerBiome(createLandBiome("debug_ice_sheet_tuyas", NTEBiomeNoise::iceSheet, RiverBlendType.CAVE));
            NTELayerIds.SUBGLACIAL_LAKE = registerBiome(createLakeBiome("debug_subglacial_lake", NTEBiomeNoise::iceSheet, false));
            NTELayerIds.ICE_SHEET_EDGE = registerBiome(createLandBiome("debug_ice_sheet_edge", NTEBiomeNoise::iceSheetEdge, RiverBlendType.TALL_CANYON));
            NTELayerIds.ICE_SHEET_TUYAS_EDGE = registerBiome(createLandBiome("debug_ice_sheet_tuyas_edge", NTEBiomeNoise::iceSheetEdge, RiverBlendType.TALL_CANYON));
            NTELayerIds.ICE_SHEET_OCEANIC = registerBiome(createLandBiome("debug_ice_sheet_oceanic", NTEBiomeNoise::iceSheetOceanic, RiverBlendType.CAVE));
            NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS_EDGE = registerBiome(createMountainBiome("debug_ice_sheet_oceanic_mountains_edge", seed -> NTEBiomeNoise.glaciatedMountains(seed, true), true));
            NTELayerIds.ICE_SHEET_MOUNTAINS_EDGE = registerBiome(createMountainBiome("debug_ice_sheet_mountains_edge", seed -> NTEBiomeNoise.glaciatedMountains(seed, false), false));
            NTELayerIds.GLACIATED_MOUNTAINS = registerBiome(createMountainBiome("debug_glaciated_mountains", seed -> NTEBiomeNoise.glaciatedMountains(seed, false), false));
            NTELayerIds.GLACIATED_OCEANIC_MOUNTAINS = registerBiome(createMountainBiome("debug_glaciated_oceanic_mountains", seed -> NTEBiomeNoise.glaciatedMountains(seed, true), true));
            NTELayerIds.MELTWATER_LAKE = registerBiome(createLakeBiome("debug_meltwater_lake", BiomeNoise::lake, false));
            NTELayerIds.GLACIATED_SHIELD_VOLCANO = registerBiome(createMountainBiome("debug_glaciated_shield_volcano", NTEBiomeNoise::glaciatedShieldVolcanoTerrain, false));
            NTELayerIds.ICE_SHEET_SHORE = registerBiome(createShoreBiome("debug_ice_sheet_shore", seed -> BiomeNoise.ocean(seed, -16, -8), true, false));
            NTELayerIds.GLACIALLY_CARVED_MOUNTAINS = registerBiome(createMountainBiome("debug_glacially_carved_mountains", seed -> NTEBiomeNoise.glaciallyCarvedMountains(seed, false), false));
            NTELayerIds.GLACIALLY_CARVED_OCEANIC_MOUNTAINS = registerBiome(createMountainBiome("debug_glacially_carved_oceanic_mountains", seed -> NTEBiomeNoise.glaciallyCarvedMountains(seed, true), true));
            NTELayerIds.DRUMLINS = registerBiome(createLandBiome("debug_drumlins", NTEBiomeNoise::drumlins, RiverBlendType.WIDE));
            NTELayerIds.TUYAS = registerAlias(rollingHills);
            NTELayerIds.KNOB_AND_KETTLE = registerBiome(createLandBiome("debug_knob_and_kettle", NTEBiomeNoise::knobAndKettle, RiverBlendType.WIDE));
            NTELayerIds.PATTERNED_GROUND = registerBiome(createLandBiome("debug_patterned_ground", seed -> BiomeNoise.hills(seed, -4, 3).add(NTEBiomeNoise.patternedGround(seed)), RiverBlendType.WIDE));
            NTELayerIds.INVERTED_PATTERNED_GROUND = registerBiome(createLandBiome("debug_inverted_patterned_ground", NTEBiomeNoise::invertedPatternedGround, RiverBlendType.WIDE));
            NTELayerIds.STONE_CIRCLES = registerBiome(createLandBiome("debug_stone_circles", seed -> BiomeNoise.hills(seed, -2, 4).add(NTEBiomeNoise.stoneCircles(seed)), RiverBlendType.WIDE));
        }

        private void registerBase(int id, BiomeExtension biome)
        {
            ensureCapacity(id);
            layers[id] = biome;
            biomes.add(biome);
        }

        private int registerAlias(BiomeExtension biome)
        {
            return registerBiome(biome);
        }

        private int registerBiome(BiomeExtension biome)
        {
            final int id = nextId++;
            ensureCapacity(id);
            layers[id] = biome;
            biomes.add(biome);
            return id;
        }

        private void ensureCapacity(int id)
        {
            if (id >= layers.length)
            {
                layers = Arrays.copyOf(layers, Math.max(layers.length * 2, id + 1));
            }
        }

        private BiomeExtension get(int id)
        {
            if (id < 0 || id >= layers.length || layers[id] == null)
            {
                throw new IllegalStateException("Missing local layered biome mapping for id " + id);
            }
            return layers[id];
        }

        private List<BiomeExtension> allBiomes()
        {
            return List.copyOf(biomes);
        }
    }

    private static final class LocalBiomeSource implements BiomeSourceExtension
    {
        private final RegionGenerator regionGenerator;
        private final LocalLayerRegistry layerRegistry;
        private final ConcurrentArea<BiomeExtension> biomeLayer;

        private LocalBiomeSource(RegionGenerator regionGenerator, Grid grid, long seed)
        {
            this.regionGenerator = regionGenerator;
            this.layerRegistry = new LocalLayerRegistry();
            final LocalRegionBiomeSelector selector = new LocalRegionBiomeSelector(regionGenerator, grid);
            this.biomeLayer = new ConcurrentArea<>(createLayeredRegionBiomeLayer(selector, seed), layerRegistry::get);
        }

        @Override
        public BiomeExtension getBiomeExtensionNoRiver(int quartX, int quartZ)
        {
            return biomeLayer.get(quartX, quartZ);
        }

        @Override
        public Holder<Biome> getBiomeFromExtension(BiomeExtension extension)
        {
            throw new UnsupportedOperationException("Debug biome source does not expose biome holders");
        }

        @Override
        public RegionPartition.Point getPartition(int blockX, int blockZ)
        {
            return regionGenerator.getOrCreatePartitionPoint(Units.blockToGrid(blockX), Units.blockToGrid(blockZ));
        }

        private List<BiomeExtension> allBiomes()
        {
            return layerRegistry.allBiomes();
        }
    }

    private static final class LocalRuntimeHeightSampler
    {
        private final Object2DoubleMap<BiomeExtension>[] sampledBiomeWeights;
        private final Object2DoubleMap<BiomeExtension> biomeWeights1;
        private final Object2DoubleMap<BiomeNoiseSampler> columnBiomeNoiseSamplers;
        private final BiomeSourceExtension biomeSource;
        private final Map<BiomeExtension, BiomeNoiseSampler> biomeNoiseSamplers;
        private final Map<NTERiverBlendType, NTERiverNoiseSampler> exactRiverNoiseSamplers;
        private final double[] exactRiverBlendWeights;
        private final Map<NTEShoreBlendType, NTEShoreNoiseSampler> shoreNoiseSamplers;
        private final double[] shoreBlendWeights;
        private final Noise2D tideHeightNoise;
        private final Map<NTECenteredFeatureBlendType, NTECenteredFeatureNoiseSampler> centeredFeatureNoiseSamplers;
        private final int seaLevel;
        private boolean volcanicColumn;
        private int blockX;
        private int blockZ;
        private int localX;
        private int localZ;
        private double preExactRiverHeight;

        private LocalRuntimeHeightSampler(
            Object2DoubleMap<BiomeExtension>[] sampledBiomeWeights,
            BiomeSourceExtension biomeSource,
            Map<BiomeExtension, BiomeNoiseSampler> biomeNoiseSamplers,
            Map<NTEShoreBlendType, NTEShoreNoiseSampler> shoreNoiseSamplers,
            Noise2D tideHeightNoise,
            Map<NTERiverBlendType, NTERiverNoiseSampler> exactRiverNoiseSamplers,
            Map<NTECenteredFeatureBlendType, NTECenteredFeatureNoiseSampler> centeredFeatureNoiseSamplers,
            int seaLevel
        )
        {
            this.sampledBiomeWeights = sampledBiomeWeights;
            this.biomeWeights1 = new Object2DoubleOpenHashMap<>();
            this.columnBiomeNoiseSamplers = new Object2DoubleOpenHashMap<>();
            this.biomeSource = biomeSource;
            this.biomeNoiseSamplers = biomeNoiseSamplers;
            this.exactRiverNoiseSamplers = exactRiverNoiseSamplers;
            this.exactRiverBlendWeights = new double[NTERiverBlendType.SIZE];
            this.shoreNoiseSamplers = shoreNoiseSamplers;
            this.shoreBlendWeights = new double[NTEShoreBlendType.SIZE];
            this.tideHeightNoise = tideHeightNoise;
            this.centeredFeatureNoiseSamplers = centeredFeatureNoiseSamplers;
            this.seaLevel = seaLevel;
        }

        private double sampleHeight(int x, int z)
        {
            setupColumn(x, z);
            ChunkBiomeSampler.sampleBiomesColumn(biomeWeights1, sampledBiomeWeights, localX, localZ);
            return sampleColumnHeightAndBiome(biomeWeights1);
        }

        private double sampleTerrainSurfaceHeight(int x, int z)
        {
            setupColumn(x, z);
            ChunkBiomeSampler.sampleBiomesColumn(biomeWeights1, sampledBiomeWeights, localX, localZ);
            final double heightNoiseValue = sampleColumnHeightAndBiome(biomeWeights1);
            final int maxY = Math.max(seaLevel + 48, Mth.floor(heightNoiseValue) + 48);
            for (int y = maxY; y >= -64; y--)
            {
                if (calculateTerrainNoiseAtHeight(y, heightNoiseValue) > 0d)
                {
                    return y;
                }
            }
            return -64;
        }

        private void setupColumn(int x, int z)
        {
            this.blockX = x;
            this.blockZ = z;
            this.localX = x & 15;
            this.localZ = z & 15;
        }

        private double sampleColumnHeightAndBiome(Object2DoubleMap<BiomeExtension> biomeWeights)
        {
            columnBiomeNoiseSamplers.clear();
            volcanicColumn = false;

            double height = 0;
            double normalHeight = 0;
            double shoreHeight = 0;
            double shoreWeight = 0;
            double oceanWeight = 0;

            BiomeExtension shoreBiomeAt = null;
            BiomeExtension oceanBiomeAt = null;
            double maxShoreWeight = 0;
            double maxOceanWeight = 0;

            for (Object2DoubleMap.Entry<BiomeExtension> entry : biomeWeights.object2DoubleEntrySet())
            {
                final double biomeWeight = entry.getDoubleValue();
                final BiomeExtension biome = entry.getKey();
                final BiomeNoiseSampler sampler = biomeNoiseSamplers.get(biome);

                if (isVolcanicBiome(biome))
                {
                    volcanicColumn = true;
                }
                assert sampler != null : "Non-existent sampler for biome: " + biome.key();

                if (columnBiomeNoiseSamplers.containsKey(sampler))
                {
                    columnBiomeNoiseSamplers.mergeDouble(sampler, biomeWeight, Double::sum);
                }
                else
                {
                    sampler.setColumn(blockX, blockZ);
                    columnBiomeNoiseSamplers.put(sampler, biomeWeight);
                }

                final double biomeHeight = biomeWeight * sampler.height();
                height += biomeHeight;

                if (biome.isShore())
                {
                    shoreHeight += biomeHeight;
                    shoreWeight += biomeWeight;
                    if (maxShoreWeight < biomeWeight)
                    {
                        shoreBiomeAt = biome;
                        maxShoreWeight = biomeWeight;
                    }
                }
                else if (biome.biomeBlendType() == BiomeBlendType.OCEAN)
                {
                    oceanWeight += biomeWeight;
                    if (maxOceanWeight < biomeWeight)
                    {
                        oceanBiomeAt = biome;
                        maxOceanWeight = biomeWeight;
                    }
                }
                else
                {
                    normalHeight += biomeHeight;
                }
            }

            computeInitialShoreWeights(biomeWeights);
            final double landWeight = 1 - oceanWeight - shoreWeight;
            if (shoreWeight > 0 && shoreBiomeAt != null)
            {
                height = adjustHeightForShoreContributions(height, oceanWeight, landWeight, shoreWeight, maxShoreWeight, shoreBiomeAt, shoreHeight, normalHeight);
            }

            if (oceanWeight >= 0.25)
            {
                final double tideAdjustedSeaEdgeHeight = tideHeightNoise.noise(blockX, blockZ) - 4;
                height = Mth.clampedMap(landWeight, 0.32, 0.36, Math.min(height, tideAdjustedSeaEdgeHeight), height);
            }

            height = adjustHeightForCenteredFeatures(height);
            preExactRiverHeight = height;
            computeInitialExactRiverWeights(biomeWeights);
            final double initialCaveWeight = adjustExactRiverWeightsForCaves();
            final RiverInfo info = sampleRiverInfo();
            return adjustHeightForExactRiverContributions(height, info, initialCaveWeight);
        }

        private void computeInitialShoreWeights(Object2DoubleMap<BiomeExtension> biomeWeights)
        {
            Arrays.fill(shoreBlendWeights, 0d);
            for (Object2DoubleMap.Entry<BiomeExtension> entry : biomeWeights.object2DoubleEntrySet())
            {
                shoreBlendWeights[NTEShoreBlendType.NONE.ordinal()] += entry.getDoubleValue();
            }
        }

        private double adjustHeightForShoreContributions(double height, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
        {
            double shoreBlendHeight = 0d;
            for (NTEShoreBlendType type : NTEShoreBlendType.ALL)
            {
                final double weight = shoreBlendWeights[type.ordinal()];
                final NTEShoreNoiseSampler sampler = shoreNoiseSamplers.get(type);
                if (type == NTEShoreBlendType.NONE)
                {
                    shoreBlendHeight += weight * height;
                }
                else if (weight > 0 && sampler != null)
                {
                    shoreBlendHeight += weight * sampler.setColumnAndSampleHeight(height, blockX, blockZ, oceanWeight, landWeight, shoreWeight, thisWeight, biome, shoreHeight, normalHeight);
                }
            }
            return shoreBlendHeight;
        }

        private double adjustHeightForCenteredFeatures(double heightIn)
        {
            double centeredFeatureHeight = NTECenteredFeatureNoiseSampler.NOT_PRESENT_RETURN;
            for (NTECenteredFeatureBlendType type : NTECenteredFeatureBlendType.ALL)
            {
                if (type == NTECenteredFeatureBlendType.NONE)
                {
                    continue;
                }
                final NTECenteredFeatureNoiseSampler sampler = centeredFeatureNoiseSamplers.get(type);
                if (sampler != null)
                {
                    centeredFeatureHeight = Math.max(sampler.setColumnAndSampleHeight(heightIn, blockX, blockZ, biomeSource), centeredFeatureHeight);
                }
            }
            return centeredFeatureHeight == NTECenteredFeatureNoiseSampler.NOT_PRESENT_RETURN ? heightIn : centeredFeatureHeight;
        }

        private void computeInitialExactRiverWeights(Object2DoubleMap<BiomeExtension> biomeWeights)
        {
            Arrays.fill(exactRiverBlendWeights, 0d);
            for (Object2DoubleMap.Entry<BiomeExtension> entry : biomeWeights.object2DoubleEntrySet())
            {
                final NTERiverBlendType blendType = NTERiverBlendType.fromLegacy(entry.getKey().riverBlendType());
                exactRiverBlendWeights[blendType.ordinal()] += entry.getDoubleValue();
            }
        }

        private double adjustExactRiverWeightsForCaves()
        {
            final double initialCaveWeight = exactRiverBlendWeights[NTERiverBlendType.CAVE.ordinal()];
            if (initialCaveWeight > 0)
            {
                final double totalWeight = 1.0 - exactRiverBlendWeights[NTERiverBlendType.NONE.ordinal()];
                final double adjustedCaveWeight = initialCaveWeight < 0.25
                    ? Mth.map(initialCaveWeight, 0.0, 0.25, 0, 0.1 * totalWeight)
                    : totalWeight;

                for (NTERiverBlendType type : NTERiverBlendType.ALL)
                {
                    final double weight = exactRiverBlendWeights[type.ordinal()];
                    exactRiverBlendWeights[type.ordinal()] = weight * (1.0 - adjustedCaveWeight) / (1.0 - initialCaveWeight);
                }
                exactRiverBlendWeights[NTERiverBlendType.CAVE.ordinal()] = adjustedCaveWeight;
            }
            return initialCaveWeight;
        }

        private @Nullable RiverInfo sampleRiverInfo()
        {
            return sampleRiverEdge(biomeSource.getPartition(blockX, blockZ));
        }

        private @Nullable RiverInfo sampleRiverEdge(RegionPartition.Point point)
        {
            final float limitDistInGridSq = 50f * 50f / (Units.GRID_WIDTH_IN_BLOCK * Units.GRID_WIDTH_IN_BLOCK);
            double minDist = limitDistInGridSq;
            double minDistAdjusted = Float.MAX_VALUE;
            RiverEdge minEdge = null;

            final double exactGridX = Units.blockToGridExact(blockX);
            final double exactGridZ = Units.blockToGridExact(blockZ);

            for (RiverEdge edge : point.rivers())
            {
                final MidpointFractal fractal = edge.fractal();
                if (fractal.maybeIntersect(exactGridX, exactGridZ, minDist))
                {
                    final double dist = fractal.intersectDistance(exactGridX, exactGridZ);
                    if (dist < limitDistInGridSq)
                    {
                        final double distAdjusted = dist / edge.widthSq();
                        if (distAdjusted < minDistAdjusted)
                        {
                            minDist = dist;
                            minDistAdjusted = distAdjusted;
                            minEdge = edge;
                        }
                    }
                }
            }

            if (minEdge != null)
            {
                final double realWidth = minEdge.widthSq(exactGridX, exactGridZ);
                final net.dries007.tfc.world.river.Flow flow = minEdge.fractal().calculateFlow(exactGridX, exactGridZ);
                minDist *= Units.GRID_WIDTH_IN_BLOCK * Units.GRID_WIDTH_IN_BLOCK;
                return new RiverInfo(minEdge, flow, minDist, realWidth);
            }
            return null;
        }

        private double adjustHeightForExactRiverContributions(double height, @Nullable RiverInfo info, double initialCaveWeight)
        {
            if (info != null)
            {
                double riverBlendHeight = 0d;
                for (NTERiverBlendType type : NTERiverBlendType.ALL)
                {
                    final double weight = exactRiverBlendWeights[type.ordinal()];
                    final NTERiverNoiseSampler sampler = exactRiverNoiseSamplers.get(type);
                    if (type == NTERiverBlendType.NONE)
                    {
                        riverBlendHeight += weight * height;
                    }
                    else if (weight > 0 && sampler != null)
                    {
                        riverBlendHeight += weight * sampler.setColumnAndSampleHeight(info, blockX, blockZ, height, initialCaveWeight, weight);
                    }
                }
                return riverBlendHeight;
            }

            Arrays.fill(exactRiverBlendWeights, 0);
            exactRiverBlendWeights[NTERiverBlendType.NONE.ordinal()] = 1.0;
            return height;
        }

        private double calculateTerrainNoiseAtHeight(int y, double heightNoiseValue)
        {
            double noise = 0d;
            for (Object2DoubleMap.Entry<BiomeNoiseSampler> entry : columnBiomeNoiseSamplers.object2DoubleEntrySet())
            {
                noise += entry.getKey().noise(y) * entry.getDoubleValue();
            }

            final double initialNoise = noise;
            noise = 0d;
            for (NTERiverBlendType type : NTERiverBlendType.ALL)
            {
                final double weight = exactRiverBlendWeights[type.ordinal()];
                if (type == NTERiverBlendType.NONE)
                {
                    noise += weight * initialNoise;
                }
                else if (weight > 0d)
                {
                    final NTERiverNoiseSampler sampler = exactRiverNoiseSamplers.get(type);
                    if (sampler != null)
                    {
                        noise += weight * sampler.noise(y, initialNoise);
                    }
                }
            }

            final double riverAdjustedNoise = noise;
            noise = 0d;
            for (NTEShoreBlendType type : NTEShoreBlendType.ALL)
            {
                final double weight = shoreBlendWeights[type.ordinal()];
                if (type == NTEShoreBlendType.NONE)
                {
                    noise += weight * riverAdjustedNoise;
                }
                else if (weight > 0d)
                {
                    final NTEShoreNoiseSampler sampler = shoreNoiseSamplers.get(type);
                    if (sampler != null)
                    {
                        noise += weight * sampler.noise(y, riverAdjustedNoise);
                    }
                }
            }

            noise = BiomeNoiseSampler.AIR_THRESHOLD - noise;
            if (y > heightNoiseValue)
            {
                noise -= (y - heightNoiseValue) * 0.2f;
            }
            return Mth.clamp(noise, -1d, 1d);
        }

        private static boolean isVolcanicBiome(BiomeExtension biome)
        {
            final String path = biome.key().location().getPath();
            return path.contains("volcano") || path.contains("tuya");
        }
    }

    private record Analysis(long seed, Grid grid, int[] componentIds, List<Component> components)
    {
        private int componentIdAt(int gridX, int gridZ)
        {
            return grid.inBounds(gridX, gridZ) ? componentIds[grid.index(gridX, gridZ)] : -1;
        }
    }

    @FunctionalInterface
    private interface HeightSampler
    {
        double height(int x, int z);
    }

    private record HeightMetrics(double maxHeight, double minHeight, int maxHeightX, int maxHeightZ, double diameterAt40, double diameterAt70, double diameterAt90)
    {
    }

    private record Component(
        int id,
        byte age,
        int count,
        int minGridX,
        int maxGridX,
        int minGridZ,
        int maxGridZ,
        int peakGridX,
        int peakGridZ,
        double maxIntensity,
        HeightMetrics heights)
    {
        int gridWidth()
        {
            return 1 + maxGridX - minGridX;
        }

        int gridHeight()
        {
            return 1 + maxGridZ - minGridZ;
        }

        double score()
        {
            return count * Math.max(1d, heights.maxHeight - SEA_LEVEL_Y);
        }

        String ageName()
        {
            return VolcanoDiagnosticMain.ageName(age);
        }
    }

    private static final class Grid
    {
        private final int radius;
        private final int size;
        private final byte[] ages;
        private final double[] intensities;

        private Grid(int radius)
        {
            this.radius = radius;
            this.size = radius * 2 + 1;
            this.ages = new byte[size * size];
            this.intensities = new double[size * size];
            Arrays.fill(intensities, Double.NEGATIVE_INFINITY);
        }

        private boolean inBounds(int x, int z)
        {
            return Math.abs(x) <= radius && Math.abs(z) <= radius;
        }

        private int index(int x, int z)
        {
            return (x + radius) + size * (z + radius);
        }

        private int x(int index)
        {
            return index % size - radius;
        }

        private int z(int index)
        {
            return index / size - radius;
        }

        private byte ageAt(int x, int z)
        {
            return inBounds(x, z) ? ages[index(x, z)] : 0;
        }
    }

    private record Config(List<Long> seeds, int gridRadius, int sampleStep, int top)
    {
        private static Config parse(String[] args)
        {
            List<Long> seeds = null;
            int gridRadius = 512;
            int sampleStep = 32;
            int top = 3;

            for (String arg : args)
            {
                if (arg.startsWith("--seed="))
                {
                    seeds = List.of(Long.parseLong(arg.substring("--seed=".length())));
                }
                else if (arg.startsWith("--seeds="))
                {
                    final String[] split = arg.substring("--seeds=".length()).split(",");
                    final List<Long> parsed = new ArrayList<>(split.length);
                    for (String seed : split)
                    {
                        if (!seed.isBlank())
                        {
                            parsed.add(Long.parseLong(seed.trim()));
                        }
                    }
                    seeds = parsed;
                }
                else if (arg.startsWith("--grid-radius="))
                {
                    gridRadius = Integer.parseInt(arg.substring("--grid-radius=".length()));
                }
                else if (arg.startsWith("--sample-step="))
                {
                    sampleStep = Integer.parseInt(arg.substring("--sample-step=".length()));
                }
                else if (arg.startsWith("--top="))
                {
                    top = Integer.parseInt(arg.substring("--top=".length()));
                }
            }

            if (seeds == null || seeds.isEmpty())
            {
                seeds = List.of(1L, 2L, 3L);
            }

            gridRadius = Mth.clamp(gridRadius, 64, 2048);
            sampleStep = Mth.clamp(sampleStep, 8, 128);
            top = Mth.clamp(top, 1, 16);
            return new Config(seeds, gridRadius, sampleStep, top);
        }
    }

}
