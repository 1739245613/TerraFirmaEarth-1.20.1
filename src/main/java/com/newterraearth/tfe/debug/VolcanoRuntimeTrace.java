package com.newterraearth.tfe.debug;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.river.RiverInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class VolcanoRuntimeTrace
{
    private static final Config CONFIG = Config.read();
    private static final ConcurrentHashMap<Long, ColumnTrace> COLUMNS = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<String> CHUNK_PHASES = new CopyOnWriteArrayList<>();

    private VolcanoRuntimeTrace()
    {
    }

    public static boolean isEnabled()
    {
        return CONFIG.enabled();
    }

    public static Config config()
    {
        return CONFIG;
    }

    public static boolean matchesChunk(ChunkPos chunkPos)
    {
        if (!CONFIG.enabled())
        {
            return false;
        }
        final int targetChunkX = CONFIG.blockX() >> 4;
        final int targetChunkZ = CONFIG.blockZ() >> 4;
        return Math.abs(chunkPos.x - targetChunkX) <= CONFIG.effectiveChunkRadius()
            && Math.abs(chunkPos.z - targetChunkZ) <= CONFIG.effectiveChunkRadius();
    }

    public static boolean matchesColumn(int blockX, int blockZ)
    {
        if (!CONFIG.enabled())
        {
            return false;
        }
        return matchesFocusedColumn(blockX, blockZ);
    }

    public static void recordChunkPhase(String phase, ChunkPos chunkPos)
    {
        if (!matchesChunk(chunkPos))
        {
            return;
        }
        CHUNK_PHASES.add(String.format(Locale.ROOT, "%s chunk=(%d,%d)", phase, chunkPos.x, chunkPos.z));
    }

    public static String summarizeBiomeWeights(Object2DoubleMap<BiomeExtension> biomeWeights, int limit)
    {
        final List<Map.Entry<String, Double>> entries = new ArrayList<>();
        for (Object2DoubleMap.Entry<BiomeExtension> entry : biomeWeights.object2DoubleEntrySet())
        {
            entries.add(Map.entry(entry.getKey().key().location().toString(), entry.getDoubleValue()));
        }
        entries.sort(Comparator.comparingDouble((Map.Entry<String, Double> entry) -> entry.getValue()).reversed());

        final StringBuilder builder = new StringBuilder();
        final int count = Math.min(limit, entries.size());
        for (int i = 0; i < count; i++)
        {
            final Map.Entry<String, Double> entry = entries.get(i);
            if (builder.length() > 0)
            {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append('=').append(format(entry.getValue()));
        }
        return builder.length() == 0 ? "<empty>" : builder.toString();
    }

    public static void recordHeightPipeline(
        int blockX,
        int blockZ,
        boolean useCache,
        String dominantBiomes,
        String centeredFeatureType,
        boolean volcanicColumn,
        boolean couldBeSalty,
        double baseHeight,
        double shoreAdjustedHeight,
        double tideAdjustedHeight,
        double centeredFeatureHeight,
        double preExactRiverHeight,
        double postExactRiverHeight,
        double finalColumnHeight,
        double initialCaveWeight,
        double adjustedCaveWeight,
        boolean forceSubterraneanCaveRiver,
        @Nullable RiverInfo riverInfo)
    {
        if (!matchesColumn(blockX, blockZ))
        {
            return;
        }
        trace(blockX, blockZ).recordHeightPipeline(
            useCache,
            dominantBiomes,
            centeredFeatureType,
            volcanicColumn,
            couldBeSalty,
            baseHeight,
            shoreAdjustedHeight,
            tideAdjustedHeight,
            centeredFeatureHeight,
            preExactRiverHeight,
            postExactRiverHeight,
            finalColumnHeight,
            initialCaveWeight,
            adjustedCaveWeight,
            forceSubterraneanCaveRiver,
            describeRiver(riverInfo)
        );
    }

    public static void recordLocalCache(int blockX, int blockZ, String biomeKey, double biomeWeight, double cachedSurfaceHeight)
    {
        if (!matchesColumn(blockX, blockZ))
        {
            return;
        }
        trace(blockX, blockZ).recordLocalCache(biomeKey, biomeWeight, cachedSurfaceHeight);
    }

    public static void recordWorldSurface(ServerLevel level, int blockX, int blockZ)
    {
        if (!matchesColumn(blockX, blockZ))
        {
            return;
        }
        trace(blockX, blockZ).recordWorldSurface(sampleSurface(level, blockX, blockZ));
    }

    public static void dumpWorldSurfaceScan(Logger logger, ServerLevel level)
    {
        if (!CONFIG.enabled())
        {
            return;
        }

        final int targetChunkX = CONFIG.blockX() >> 4;
        final int targetChunkZ = CONFIG.blockZ() >> 4;
        final int minChunkX = targetChunkX - CONFIG.effectiveChunkRadius();
        final int maxChunkX = targetChunkX + CONFIG.effectiveChunkRadius();
        final int minChunkZ = targetChunkZ - CONFIG.effectiveChunkRadius();
        final int maxChunkZ = targetChunkZ + CONFIG.effectiveChunkRadius();
        final Map<Long, SurfaceSample> highestByChunk = new HashMap<>();
        final Map<Long, SurfaceSample> volcanicByChunk = new HashMap<>();
        final Map<Long, Integer> volcanicCountsByChunk = new HashMap<>();

        int checkedColumns = 0;
        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++)
        {
            final int minBlockZ = chunkZ << 4;
            final int maxBlockZ = minBlockZ + 15;
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++)
            {
                final int minBlockX = chunkX << 4;
                final int maxBlockX = minBlockX + 15;
                for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++)
                {
                    for (int blockX = minBlockX; blockX <= maxBlockX; blockX++)
                    {
                        checkedColumns++;
                        final SurfaceSample sample = sampleSurface(level, blockX, blockZ);
                        highestByChunk.merge(sample.chunkKey(), sample, VolcanoRuntimeTrace::preferHigherSurfaceSample);
                        if (containsVolcanicToken(sample.biomeKey()))
                        {
                            volcanicByChunk.merge(sample.chunkKey(), sample, VolcanoRuntimeTrace::preferHigherSurfaceSample);
                            volcanicCountsByChunk.merge(sample.chunkKey(), 1, Integer::sum);
                        }
                    }
                }
            }
        }

        logger.info(
            "[TFE][VolcanoTrace] world-scan checkedColumns={} checkedChunks={} volcanicChunks={}",
            checkedColumns,
            highestByChunk.size(),
            volcanicByChunk.size()
        );

        final List<SurfaceSample> volcanicCandidates = new ArrayList<>(volcanicByChunk.values());
        volcanicCandidates.sort(Comparator
            .comparingInt(SurfaceSample::worldSurfaceY).reversed()
            .thenComparingInt(sample -> sample.distanceSqTo(CONFIG.blockX(), CONFIG.blockZ())));
        if (volcanicCandidates.isEmpty())
        {
            logger.info("[TFE][VolcanoTrace] world-scan volcanic chunk candidates: none; consider increasing chunkRadius");
        }
        else
        {
            final List<SurfaceSample> limitedCandidates = volcanicCandidates.stream()
                .limit(CONFIG.summaryTopN())
                .toList();
            logger.info("[TFE][VolcanoTrace] world-scan volcanic chunk candidates={}", limitedCandidates.size());
            for (SurfaceSample sample : limitedCandidates)
            {
                logger.info(
                    "[TFE][VolcanoTrace] world-scan candidate chunk=({}, {}) volcanicColumns={} column=({}, {}) distSq={} worldSurface={} oceanFloor={} worldBiome={} topBlock={}",
                    sample.chunkX(),
                    sample.chunkZ(),
                    volcanicCountsByChunk.getOrDefault(sample.chunkKey(), 0),
                    sample.blockX(),
                    sample.blockZ(),
                    sample.distanceSqTo(CONFIG.blockX(), CONFIG.blockZ()),
                    sample.worldSurfaceY(),
                    sample.oceanFloorY(),
                    sample.biomeKey(),
                    sample.topBlock()
                );
            }
        }

        final List<SurfaceSample> highestChunks = new ArrayList<>(highestByChunk.values());
        highestChunks.sort(Comparator
            .comparingInt(SurfaceSample::worldSurfaceY).reversed()
            .thenComparingInt(sample -> sample.distanceSqTo(CONFIG.blockX(), CONFIG.blockZ())));
        final List<SurfaceSample> limitedPeaks = highestChunks.stream()
            .limit(CONFIG.summaryTopN())
            .toList();
        logger.info("[TFE][VolcanoTrace] world-scan highest chunks={}", limitedPeaks.size());
        for (SurfaceSample sample : limitedPeaks)
        {
            logger.info(
                "[TFE][VolcanoTrace] world-scan peak chunk=({}, {}) column=({}, {}) distSq={} worldSurface={} oceanFloor={} worldBiome={} topBlock={}",
                sample.chunkX(),
                sample.chunkZ(),
                sample.blockX(),
                sample.blockZ(),
                sample.distanceSqTo(CONFIG.blockX(), CONFIG.blockZ()),
                sample.worldSurfaceY(),
                sample.oceanFloorY(),
                sample.biomeKey(),
                sample.topBlock()
            );
        }
    }

    public static void dump(Logger logger, MinecraftServer server)
    {
        if (!CONFIG.enabled())
        {
            return;
        }

        final int targetChunkX = CONFIG.blockX() >> 4;
        final int targetChunkZ = CONFIG.blockZ() >> 4;
        final int minChunkX = targetChunkX - CONFIG.effectiveChunkRadius();
        final int maxChunkX = targetChunkX + CONFIG.effectiveChunkRadius();
        final int minChunkZ = targetChunkZ - CONFIG.effectiveChunkRadius();
        final int maxChunkZ = targetChunkZ + CONFIG.effectiveChunkRadius();

        logger.info(
            "[TFE][VolcanoTrace] seed={} expectedSeed={} target=({}, {}) chunkRadius={} effectiveChunkRadius={} blockRadius={} summaryTopN={}",
            server.overworld().getSeed(),
            CONFIG.expectedSeed().map(String::valueOf).orElse("<unset>"),
            CONFIG.blockX(),
            CONFIG.blockZ(),
            CONFIG.chunkRadius(),
            CONFIG.effectiveChunkRadius(),
            CONFIG.blockRadius(),
            CONFIG.summaryTopN()
        );
        logger.info(
            "[TFE][VolcanoTrace] scanChunks=[{}..{}]x[{}..{}] scanBlocks=[{}..{}]x[{}..{}]",
            minChunkX,
            maxChunkX,
            minChunkZ,
            maxChunkZ,
            minChunkX << 4,
            ((maxChunkX + 1) << 4) - 1,
            minChunkZ << 4,
            ((maxChunkZ + 1) << 4) - 1
        );

        for (String phase : CHUNK_PHASES)
        {
            logger.info("[TFE][VolcanoTrace] {}", phase);
        }

        final List<ColumnTrace> traces = new ArrayList<>(COLUMNS.values());
        traces.removeIf(trace -> !trace.hasData());
        if (traces.isEmpty())
        {
            logger.info("[TFE][VolcanoTrace] focused columns: none");
            return;
        }

        traces.sort(Comparator
            .comparingInt((ColumnTrace trace) -> trace.distanceSqTo(CONFIG.blockX(), CONFIG.blockZ()))
            .thenComparing(Comparator.comparingDouble(ColumnTrace::peakHeight).reversed()));
        logger.info("[TFE][VolcanoTrace] focused columns={}", traces.size());
        for (ColumnTrace trace : traces)
        {
            trace.dump(logger);
        }
    }

    private static ColumnTrace trace(int blockX, int blockZ)
    {
        return COLUMNS.computeIfAbsent(key(blockX, blockZ), ignored -> new ColumnTrace(blockX, blockZ));
    }

    private static long key(int blockX, int blockZ)
    {
        return (((long) blockX) << 32) ^ (blockZ & 0xffffffffL);
    }

    private static boolean matchesFocusedColumn(int blockX, int blockZ)
    {
        return Math.abs(blockX - CONFIG.blockX()) <= CONFIG.blockRadius()
            && Math.abs(blockZ - CONFIG.blockZ()) <= CONFIG.blockRadius();
    }

    private static SurfaceSample sampleSurface(ServerLevel level, int blockX, int blockZ)
    {
        final int worldSurfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ) - 1;
        final int oceanFloorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, blockX, blockZ) - 1;
        final int biomeY = Mth.clamp(worldSurfaceY, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        final String biomeKey = level.getBiome(new BlockPos(blockX, biomeY, blockZ))
            .unwrapKey()
            .map(key -> key.location().toString())
            .orElse("<unregistered>");
        final BlockState topState = worldSurfaceY >= level.getMinBuildHeight()
            ? level.getBlockState(new BlockPos(blockX, worldSurfaceY, blockZ))
            : null;
        final String topBlock = topState == null
            ? "minecraft:air"
            : BuiltInRegistries.BLOCK.getKey(topState.getBlock()).toString();
        return new SurfaceSample(blockX, blockZ, worldSurfaceY, oceanFloorY, biomeKey, topBlock);
    }

    private static SurfaceSample preferHigherSurfaceSample(SurfaceSample left, SurfaceSample right)
    {
        if (right.worldSurfaceY() > left.worldSurfaceY())
        {
            return right;
        }
        if (right.worldSurfaceY() < left.worldSurfaceY())
        {
            return left;
        }
        return right.distanceSqTo(CONFIG.blockX(), CONFIG.blockZ()) < left.distanceSqTo(CONFIG.blockX(), CONFIG.blockZ())
            ? right
            : left;
    }

    private static String describeRiver(@Nullable RiverInfo info)
    {
        if (info == null)
        {
            return "none";
        }
        return String.format(
            Locale.ROOT,
            "distSq=%s widthSq=%s normDistSq=%s",
            format(info.distSq()),
            format(info.widthSq()),
            format(info.normDistSq())
        );
    }

    private static String format(double value)
    {
        if (!Double.isFinite(value))
        {
            return "<unset>";
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static boolean containsVolcanicToken(String value)
    {
        if (value == null || value.isBlank())
        {
            return false;
        }
        final String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("volcano") || normalized.contains("volcanic") || normalized.contains("tuya");
    }

    public record Config(
        boolean enabled,
        int blockX,
        int blockZ,
        int chunkRadius,
        int blockRadius,
        int summaryTopN,
        Optional<Long> expectedSeed,
        boolean stopAfterTrace)
    {
        private static Config read()
        {
            final boolean enabled = Boolean.parseBoolean(System.getProperty("tfe.debug.volcanoTrace", "false"));
            final int blockX = Integer.getInteger("tfe.debug.x", 0);
            final int blockZ = Integer.getInteger("tfe.debug.z", 0);
            final int chunkRadius = Math.max(0, Integer.getInteger("tfe.debug.chunkRadius", 0));
            final int blockRadius = Math.max(0, Integer.getInteger("tfe.debug.blockRadius", 0));
            final int summaryTopN = Math.max(1, Integer.getInteger("tfe.debug.summaryTopN", 16));
            final String expectedSeedValue = System.getProperty("tfe.debug.expectedSeed");
            final Optional<Long> expectedSeed = expectedSeedValue == null || expectedSeedValue.isBlank()
                ? Optional.empty()
                : Optional.of(Long.parseLong(expectedSeedValue.trim()));
            final boolean stopAfterTrace = Boolean.parseBoolean(System.getProperty("tfe.debug.stopAfterTrace", "false"));
            return new Config(enabled, blockX, blockZ, chunkRadius, blockRadius, summaryTopN, expectedSeed, stopAfterTrace);
        }

        public int effectiveChunkRadius()
        {
            return Math.max(chunkRadius, (blockRadius + 15) >> 4);
        }
    }

    private record SurfaceSample(int blockX, int blockZ, int worldSurfaceY, int oceanFloorY, String biomeKey, String topBlock)
    {
        private int chunkX()
        {
            return blockX >> 4;
        }

        private int chunkZ()
        {
            return blockZ >> 4;
        }

        private long chunkKey()
        {
            return (((long) chunkX()) << 32) ^ (chunkZ() & 0xffffffffL);
        }

        private int distanceSqTo(int targetX, int targetZ)
        {
            final int dx = blockX - targetX;
            final int dz = blockZ - targetZ;
            return dx * dx + dz * dz;
        }
    }

    private static final class ColumnTrace
    {
        private final int blockX;
        private final int blockZ;
        private boolean useCache;
        private String dominantBiomes = "<unset>";
        private String centeredFeatureType = "none";
        private boolean volcanicColumn;
        private boolean couldBeSalty;
        private double baseHeight = Double.NaN;
        private double shoreAdjustedHeight = Double.NaN;
        private double tideAdjustedHeight = Double.NaN;
        private double centeredFeatureHeight = Double.NaN;
        private double preExactRiverHeight = Double.NaN;
        private double postExactRiverHeight = Double.NaN;
        private double finalColumnHeight = Double.NaN;
        private double initialCaveWeight = Double.NaN;
        private double adjustedCaveWeight = Double.NaN;
        private boolean forceSubterraneanCaveRiver;
        private String riverInfo = "none";
        private String cachedBiome = "<unset>";
        private double cachedBiomeWeight = Double.NaN;
        private double cachedSurfaceHeight = Double.NaN;
        private String worldBiome = "<unset>";
        private Integer worldSurfaceY;
        private Integer oceanFloorY;
        private String topBlock = "<unset>";

        private ColumnTrace(int blockX, int blockZ)
        {
            this.blockX = blockX;
            this.blockZ = blockZ;
        }

        private synchronized void recordHeightPipeline(
            boolean useCache,
            String dominantBiomes,
            String centeredFeatureType,
            boolean volcanicColumn,
            boolean couldBeSalty,
            double baseHeight,
            double shoreAdjustedHeight,
            double tideAdjustedHeight,
            double centeredFeatureHeight,
            double preExactRiverHeight,
            double postExactRiverHeight,
            double finalColumnHeight,
            double initialCaveWeight,
            double adjustedCaveWeight,
            boolean forceSubterraneanCaveRiver,
            String riverInfo)
        {
            this.useCache = useCache;
            this.dominantBiomes = dominantBiomes;
            this.centeredFeatureType = centeredFeatureType;
            this.volcanicColumn = volcanicColumn;
            this.couldBeSalty = couldBeSalty;
            this.baseHeight = baseHeight;
            this.shoreAdjustedHeight = shoreAdjustedHeight;
            this.tideAdjustedHeight = tideAdjustedHeight;
            this.centeredFeatureHeight = centeredFeatureHeight;
            this.preExactRiverHeight = preExactRiverHeight;
            this.postExactRiverHeight = postExactRiverHeight;
            this.finalColumnHeight = finalColumnHeight;
            this.initialCaveWeight = initialCaveWeight;
            this.adjustedCaveWeight = adjustedCaveWeight;
            this.forceSubterraneanCaveRiver = forceSubterraneanCaveRiver;
            this.riverInfo = riverInfo;
        }

        private synchronized void recordLocalCache(String biomeKey, double biomeWeight, double cachedSurfaceHeight)
        {
            this.cachedBiome = biomeKey;
            this.cachedBiomeWeight = biomeWeight;
            this.cachedSurfaceHeight = cachedSurfaceHeight;
        }

        private synchronized void recordWorldSurface(SurfaceSample sample)
        {
            this.worldBiome = sample.biomeKey();
            this.worldSurfaceY = sample.worldSurfaceY();
            this.oceanFloorY = sample.oceanFloorY();
            this.topBlock = sample.topBlock();
        }

        private synchronized boolean hasData()
        {
            return !Double.isNaN(finalColumnHeight) || !Double.isNaN(cachedSurfaceHeight) || worldSurfaceY != null;
        }

        private synchronized double peakHeight()
        {
            double peak = Double.NEGATIVE_INFINITY;
            peak = Math.max(peak, finalColumnHeight);
            peak = Math.max(peak, cachedSurfaceHeight);
            if (worldSurfaceY != null)
            {
                peak = Math.max(peak, worldSurfaceY.doubleValue());
            }
            return peak;
        }

        private synchronized void dump(Logger logger)
        {
            logger.info(
                "[TFE][VolcanoTrace] column=({}, {}) useCache={} biomes=[{}] volcanic={} salty={} centeredFeature={} river={} caveWeight={} -> {} forceSubterranean={}",
                blockX,
                blockZ,
                useCache,
                dominantBiomes,
                volcanicColumn,
                couldBeSalty,
                centeredFeatureType,
                riverInfo,
                format(initialCaveWeight),
                format(adjustedCaveWeight),
                forceSubterraneanCaveRiver
            );
            logger.info(
                "[TFE][VolcanoTrace] column=({}, {}) heights base={} shore={} tide={} centered={} preRiver={} postRiver={} finalColumn={} cachedSurface={} cachedBiome={} cachedWeight={} worldSurface={} oceanFloor={} worldBiome={} topBlock={}",
                blockX,
                blockZ,
                format(baseHeight),
                format(shoreAdjustedHeight),
                format(tideAdjustedHeight),
                format(centeredFeatureHeight),
                format(preExactRiverHeight),
                format(postExactRiverHeight),
                format(finalColumnHeight),
                format(cachedSurfaceHeight),
                cachedBiome,
                format(cachedBiomeWeight),
                worldSurfaceY == null ? "<unset>" : worldSurfaceY,
                oceanFloorY == null ? "<unset>" : oceanFloorY,
                worldBiome,
                topBlock
            );
        }

        private int distanceSqTo(int targetX, int targetZ)
        {
            final int dx = blockX - targetX;
            final int dz = blockZ - targetZ;
            return dx * dx + dz * dz;
        }
    }
}
