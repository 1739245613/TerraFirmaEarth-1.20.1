package com.newterraearth.tfe.world.spawn;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.entities.EntityHelpers;
import net.dries007.tfc.common.entities.Fauna;
import net.dries007.tfc.world.chunkdata.ChunkData;

import com.newterraearth.tfe.mixin.NaturalSpawnerAccessor;

public final class AquaticSpawnHelper
{
    public static final int NO_VALID_Y = Integer.MIN_VALUE;

    private AquaticSpawnHelper()
    {
    }

    public static boolean isAquaticCategory(MobCategory category)
    {
        return category == MobCategory.WATER_AMBIENT
            || category == MobCategory.WATER_CREATURE
            || category == MobCategory.UNDERGROUND_WATER_CREATURE;
    }

    public static int findAquaticSpawnY(net.minecraft.server.level.ServerLevel level, ChunkGenerator chunkGenerator, MobSpawnSettings.SpawnerData spawner, BlockPos pos)
    {
        return inspect(level, chunkGenerator, spawner, pos).validY();
    }

    public static AquaticSpawnInspection inspect(net.minecraft.server.level.ServerLevel level, ChunkGenerator chunkGenerator, MobSpawnSettings.SpawnerData spawner, BlockPos pos)
    {
        final int blockX = pos.getX();
        final int blockZ = pos.getZ();
        final int worldSurfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ) - 1;
        final int oceanFloorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, blockX, blockZ) - 1;
        final StructureManager structureManager = level.structureManager();
        final Fauna fauna = getFauna(spawner.type);
        final int minWaterY = oceanFloorY + 1;
        int maxWaterY = worldSurfaceY;

        if (fauna != null && fauna.getDistanceBelowSeaLevel() != -1)
        {
            maxWaterY = Math.min(maxWaterY, chunkGenerator.getSeaLevel() - fauna.getDistanceBelowSeaLevel());
        }

        final int preferredY = maxWaterY < minWaterY ? NO_VALID_Y : Mth.clamp(pos.getY(), minWaterY, maxWaterY);
        final ChunkData data = EntityHelpers.getChunkDataForSpawning(level, pos);
        final float averageTemp = data.getAverageTemp(pos);
        final float rainfall = data.getRainfall(pos);
        final EnumMap<FailureReason, Integer> failures = new EnumMap<>(FailureReason.class);

        if (maxWaterY < minWaterY)
        {
            failures.merge(FailureReason.NO_WATER_BAND, 1, Integer::sum);
            return new AquaticSpawnInspection(
                spawner.type,
                fauna,
                worldSurfaceY,
                oceanFloorY,
                minWaterY,
                maxWaterY,
                preferredY,
                NO_VALID_Y,
                averageTemp,
                rainfall,
                Map.copyOf(failures)
            );
        }

        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(blockX, preferredY, blockZ);
        final @Nullable Player nearestPlayer = level.getNearestPlayer(blockX + 0.5d, preferredY, blockZ + 0.5d, -1.0d, false);

        final int downward = scan(level, chunkGenerator, structureManager, spawner, cursor, nearestPlayer, preferredY, minWaterY, -1, failures);
        final int validY = downward != NO_VALID_Y
            ? downward
            : scan(level, chunkGenerator, structureManager, spawner, cursor, nearestPlayer, preferredY + 1, maxWaterY, 1, failures);

        return new AquaticSpawnInspection(
            spawner.type,
            fauna,
            worldSurfaceY,
            oceanFloorY,
            minWaterY,
            maxWaterY,
            preferredY,
            validY,
            averageTemp,
            rainfall,
            Map.copyOf(failures)
        );
    }

    private static int scan(net.minecraft.server.level.ServerLevel level, ChunkGenerator chunkGenerator, StructureManager structureManager, MobSpawnSettings.SpawnerData spawner, BlockPos.MutableBlockPos cursor, @Nullable Player nearestPlayer, int startY, int boundY, int step, EnumMap<FailureReason, Integer> failures)
    {
        if (step < 0)
        {
            for (int y = startY; y >= boundY; y--)
            {
                final FailureReason reason = validateY(level, chunkGenerator, structureManager, spawner, cursor, nearestPlayer, y);
                failures.merge(reason, 1, Integer::sum);
                if (reason == FailureReason.OK)
                {
                    return y;
                }
            }
        }
        else
        {
            for (int y = startY; y <= boundY; y++)
            {
                final FailureReason reason = validateY(level, chunkGenerator, structureManager, spawner, cursor, nearestPlayer, y);
                failures.merge(reason, 1, Integer::sum);
                if (reason == FailureReason.OK)
                {
                    return y;
                }
            }
        }
        return NO_VALID_Y;
    }

    private static FailureReason validateY(net.minecraft.server.level.ServerLevel level, ChunkGenerator chunkGenerator, StructureManager structureManager, MobSpawnSettings.SpawnerData spawner, BlockPos.MutableBlockPos cursor, @Nullable Player nearestPlayer, int y)
    {
        cursor.setY(y);
        final EntityType<?> type = spawner.type;
        final MobCategory category = type.getCategory();
        if (category == MobCategory.MISC || !type.canSummon())
        {
            return FailureReason.INVALID_ENTITY;
        }

        if (nearestPlayer != null && !type.canSpawnFarFromPlayer())
        {
            final int despawnDistance = category.getDespawnDistance();
            final double distanceToPlayer = nearestPlayer.distanceToSqr(cursor.getX() + 0.5d, y, cursor.getZ() + 0.5d);
            if (distanceToPlayer > (double) despawnDistance * despawnDistance)
            {
                return FailureReason.TOO_FAR_FROM_PLAYER;
            }
        }

        if (!NaturalSpawnerAccessor.tfe$canSpawnMobAt(level, structureManager, chunkGenerator, category, spawner, cursor))
        {
            return FailureReason.NOT_IN_SPAWNER_LIST;
        }

        final Type placementType = SpawnPlacements.getPlacementType(type);
        if (!NaturalSpawnerAccessor.tfe$isSpawnPositionOk(placementType, level, cursor, type))
        {
            return FailureReason.SPAWN_POSITION;
        }

        final RandomSource spawnRandom = RandomSource.create(cursor.asLong() ^ BuiltInRegistries.ENTITY_TYPE.getId(type));
        if (!SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, cursor, spawnRandom))
        {
            return FailureReason.SPAWN_RULES;
        }

        if (!level.noCollision(type.getAABB(cursor.getX() + 0.5d, y, cursor.getZ() + 0.5d)))
        {
            return FailureReason.NO_COLLISION;
        }

        return FailureReason.OK;
    }

    @Nullable
    private static Fauna getFauna(EntityType<?> type)
    {
        final net.dries007.tfc.util.RegisteredDataManager.Entry<Fauna> entry = Fauna.MANAGER.get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        return entry != null ? entry.get() : null;
    }

    public enum FailureReason
    {
        OK,
        NO_WATER_BAND,
        INVALID_ENTITY,
        TOO_FAR_FROM_PLAYER,
        NOT_IN_SPAWNER_LIST,
        SPAWN_POSITION,
        SPAWN_RULES,
        NO_COLLISION
    }

    public record AquaticSpawnInspection(
        EntityType<?> type,
        @Nullable Fauna fauna,
        int worldSurfaceY,
        int oceanFloorY,
        int minWaterY,
        int maxWaterY,
        int preferredY,
        int validY,
        float averageTemp,
        float rainfall,
        Map<FailureReason, Integer> failureCounts)
    {
        public boolean isValid()
        {
            return validY != NO_VALID_Y;
        }
    }
}
