package com.newterraearth.tfe.debug;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;

import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.dries007.tfc.world.chunkdata.ChunkData;

import com.newterraearth.tfe.world.spawn.AquaticSpawnHelper;
import com.newterraearth.tfe.world.spawn.AquaticSpawnHelper.AquaticSpawnInspection;
import com.newterraearth.tfe.world.spawn.AquaticSpawnHelper.FailureReason;

public final class AquaticSpawnTrace
{
    private static final Config CONFIG = Config.read();
    private static final List<MobCategory> AQUATIC_CATEGORIES = List.of(
        MobCategory.WATER_AMBIENT,
        MobCategory.WATER_CREATURE,
        MobCategory.UNDERGROUND_WATER_CREATURE
    );

    private AquaticSpawnTrace()
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

    public static void dump(Logger logger, ServerLevel level)
    {
        if (!CONFIG.enabled())
        {
            return;
        }

        final ChunkGenerator chunkGenerator = level.getChunkSource().getGenerator();
        final int seaLevel = chunkGenerator.getSeaLevel();
        final Map<String, EntitySummary> summaries = new LinkedHashMap<>();
        final List<ColumnDetail> focusedColumns = new ArrayList<>();
        int scannedColumns = 0;
        int wateryColumns = 0;

        for (int blockZ = CONFIG.blockZ() - CONFIG.blockRadius(); blockZ <= CONFIG.blockZ() + CONFIG.blockRadius(); blockZ++)
        {
            for (int blockX = CONFIG.blockX() - CONFIG.blockRadius(); blockX <= CONFIG.blockX() + CONFIG.blockRadius(); blockX++)
            {
                scannedColumns++;
                final int sampleY = Mth.clamp(seaLevel - 1, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
                final BlockPos samplePos = new BlockPos(blockX, sampleY, blockZ);
                final ChunkData data = ChunkData.get(level, samplePos);
                final float averageTemp = data.getAverageTemp(samplePos);
                final float rainfall = data.getRainfall(samplePos);
                final int worldSurfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ) - 1;
                final int oceanFloorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, blockX, blockZ) - 1;
                final int fluidY = Math.max(oceanFloorY + 1, Math.min(worldSurfaceY, seaLevel - 1));
                final BlockPos fluidPos = new BlockPos(blockX, fluidY, blockZ);
                final String fluidKey = BuiltInRegistries.FLUID.getKey(level.getFluidState(fluidPos).getType()).toString();
                final Holder<Biome> biome = level.getBiome(samplePos);
                final String biomeKey = biome.unwrapKey().map(key -> key.location().toString()).orElse("<unregistered>");

                final boolean watery = worldSurfaceY >= oceanFloorY + 1 && !level.getFluidState(fluidPos).isEmpty();
                if (watery)
                {
                    wateryColumns++;
                }

                final ColumnDetail detail = matchesFocusedColumn(blockX, blockZ)
                    ? new ColumnDetail(
                        blockX,
                        blockZ,
                        biomeKey,
                        fluidKey,
                        worldSurfaceY,
                        oceanFloorY,
                        averageTemp,
                        rainfall,
                        OverworldClimateModel.getAdjustedAverageTempByElevation(Math.max(oceanFloorY + 1, level.getMinBuildHeight()), averageTemp),
                        new ArrayList<>())
                    : null;

                for (MobCategory category : AQUATIC_CATEGORIES)
                {
                    final List<MobSpawnSettings.SpawnerData> spawners = biome.value().getMobSettings().getMobs(category).unwrap();
                    for (MobSpawnSettings.SpawnerData spawner : spawners)
                    {
                        final AquaticSpawnInspection inspection = AquaticSpawnHelper.inspect(level, chunkGenerator, spawner, samplePos);
                        final String entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(spawner.type).toString();
                        summaries.computeIfAbsent(entityKey, ignored -> new EntitySummary(entityKey, category.getName(), spawner.getWeight().asInt()))
                            .record(inspection);
                        if (detail != null)
                        {
                            detail.candidates().add(new CandidateDetail(
                                category.getName(),
                                entityKey,
                                spawner.getWeight().asInt(),
                                inspection.validY(),
                                inspection.minWaterY(),
                                inspection.maxWaterY(),
                                inspection.failureCounts()
                            ));
                        }
                    }
                }

                if (detail != null)
                {
                    focusedColumns.add(detail);
                }
            }
        }

        logger.info(
            "[TFE][AquaticTrace] target=({}, {}) blockRadius={} scannedColumns={} wateryColumns={} seaLevel={}",
            CONFIG.blockX(),
            CONFIG.blockZ(),
            CONFIG.blockRadius(),
            scannedColumns,
            wateryColumns,
            seaLevel
        );

        focusedColumns.sort(Comparator.comparingInt(detail -> detail.distanceSqTo(CONFIG.blockX(), CONFIG.blockZ())));
        for (ColumnDetail detail : focusedColumns.stream().limit(CONFIG.summaryTopN()).toList())
        {
            logger.info(
                "[TFE][AquaticTrace] column=({}, {}) biome={} fluid={} worldSurface={} oceanFloor={} avgTemp={} adjustedFloorTemp={} rainfall={}",
                detail.blockX(),
                detail.blockZ(),
                detail.biomeKey(),
                detail.fluidKey(),
                detail.worldSurfaceY(),
                detail.oceanFloorY(),
                fmt(detail.averageTemp()),
                fmt(detail.adjustedFloorTemp()),
                fmt(detail.rainfall())
            );
            for (CandidateDetail candidate : detail.candidates().stream().sorted(Comparator
                .comparing((CandidateDetail c) -> c.validY() == AquaticSpawnHelper.NO_VALID_Y)
                .thenComparing(CandidateDetail::entityKey)).toList())
            {
                logger.info(
                    "[TFE][AquaticTrace] column=({}, {}) category={} entity={} weight={} validY={} band=[{}..{}] failures={}",
                    detail.blockX(),
                    detail.blockZ(),
                    candidate.category(),
                    candidate.entityKey(),
                    candidate.weight(),
                    candidate.validY() == AquaticSpawnHelper.NO_VALID_Y ? "<none>" : candidate.validY(),
                    candidate.minWaterY(),
                    candidate.maxWaterY(),
                    formatFailures(candidate.failureCounts())
                );
            }
        }

        final List<EntitySummary> sortedSummaries = new ArrayList<>(summaries.values());
        sortedSummaries.sort(Comparator
            .comparingInt(EntitySummary::validColumns).reversed()
            .thenComparing(EntitySummary::entityKey));
        logger.info("[TFE][AquaticTrace] entitySummaries={}", Math.min(CONFIG.summaryTopN(), sortedSummaries.size()));
        for (EntitySummary summary : sortedSummaries.stream().limit(CONFIG.summaryTopN()).toList())
        {
            logger.info(
                "[TFE][AquaticTrace] entity={} category={} weight={} validColumns={}/{} validYRange={} dominantFailures={}",
                summary.entityKey(),
                summary.category(),
                summary.weight(),
                summary.validColumns(),
                scannedColumns,
                summary.validColumns() == 0 ? "<none>" : summary.minValidY() + ".." + summary.maxValidY(),
                formatFailures(summary.failureCounts())
            );
        }
    }

    private static boolean matchesFocusedColumn(int blockX, int blockZ)
    {
        return Math.abs(blockX - CONFIG.blockX()) <= 1 && Math.abs(blockZ - CONFIG.blockZ()) <= 1;
    }

    private static String formatFailures(Map<FailureReason, Integer> failureCounts)
    {
        if (failureCounts.isEmpty())
        {
            return "{}";
        }
        final StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (FailureReason reason : FailureReason.values())
        {
            final int count = failureCounts.getOrDefault(reason, 0);
            if (count <= 0)
            {
                continue;
            }
            if (!first)
            {
                builder.append(", ");
            }
            builder.append(reason.name().toLowerCase(Locale.ROOT)).append('=').append(count);
            first = false;
        }
        builder.append('}');
        return builder.toString();
    }

    private static String fmt(float value)
    {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public record Config(boolean enabled, int blockX, int blockZ, int blockRadius, int summaryTopN, boolean stopAfterTrace)
    {
        private static Config read()
        {
            final boolean enabled = Boolean.parseBoolean(System.getProperty("tfe.debug.aquaticTrace", "false"));
            final int blockX = Integer.getInteger("tfe.debug.x", 0);
            final int blockZ = Integer.getInteger("tfe.debug.z", 0);
            final int blockRadius = Math.max(0, Integer.getInteger("tfe.debug.blockRadius", 8));
            final int summaryTopN = Math.max(1, Integer.getInteger("tfe.debug.summaryTopN", 12));
            final boolean stopAfterTrace = Boolean.parseBoolean(System.getProperty("tfe.debug.stopAfterTrace", "false"));
            return new Config(enabled, blockX, blockZ, blockRadius, summaryTopN, stopAfterTrace);
        }
    }

    private static final class EntitySummary
    {
        private final String entityKey;
        private final String category;
        private final int weight;
        private int validColumns;
        private int minValidY = Integer.MAX_VALUE;
        private int maxValidY = Integer.MIN_VALUE;
        private final EnumMap<FailureReason, Integer> failureCounts = new EnumMap<>(FailureReason.class);

        private EntitySummary(String entityKey, String category, int weight)
        {
            this.entityKey = entityKey;
            this.category = category;
            this.weight = weight;
        }

        private void record(AquaticSpawnInspection inspection)
        {
            inspection.failureCounts().forEach((reason, count) -> failureCounts.merge(reason, count, Integer::sum));
            if (inspection.isValid())
            {
                validColumns++;
                minValidY = Math.min(minValidY, inspection.validY());
                maxValidY = Math.max(maxValidY, inspection.validY());
            }
        }

        private String entityKey()
        {
            return entityKey;
        }

        private String category()
        {
            return category;
        }

        private int weight()
        {
            return weight;
        }

        private int validColumns()
        {
            return validColumns;
        }

        private int minValidY()
        {
            return minValidY;
        }

        private int maxValidY()
        {
            return maxValidY;
        }

        private Map<FailureReason, Integer> failureCounts()
        {
            return failureCounts;
        }
    }

    private record ColumnDetail(
        int blockX,
        int blockZ,
        String biomeKey,
        String fluidKey,
        int worldSurfaceY,
        int oceanFloorY,
        float averageTemp,
        float rainfall,
        float adjustedFloorTemp,
        List<CandidateDetail> candidates)
    {
        private int distanceSqTo(int x, int z)
        {
            final int dx = blockX - x;
            final int dz = blockZ - z;
            return dx * dx + dz * dz;
        }
    }

    private record CandidateDetail(
        String category,
        String entityKey,
        int weight,
        int validY,
        int minWaterY,
        int maxWaterY,
        Map<FailureReason, Integer> failureCounts)
    {
    }
}
