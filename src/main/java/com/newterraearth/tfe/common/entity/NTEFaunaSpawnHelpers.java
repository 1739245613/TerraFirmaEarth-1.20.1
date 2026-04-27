package com.newterraearth.tfe.common.entity;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;

import net.dries007.tfc.common.entities.EntityHelpers;
import net.dries007.tfc.common.entities.Fauna;
import net.dries007.tfc.util.RegisteredDataManager;
import net.dries007.tfc.util.calendar.Month;
import net.dries007.tfc.world.chunkdata.ChunkData;

import com.newterraearth.tfe.world.NTEClimatePlacementAccess;
import com.newterraearth.tfe.world.NTESeasonalHelpers;

public final class NTEFaunaSpawnHelpers
{
    private NTEFaunaSpawnHelpers()
    {
    }

    @Nullable
    public static Fauna getFauna(EntityType<?> entityType)
    {
        final RegisteredDataManager.Entry<Fauna> entry = Fauna.MANAGER.get(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
        return entry != null ? entry.get() : null;
    }

    public static boolean isClimateValid(Fauna fauna, ServerLevelAccessor level, BlockPos pos, RandomSource random)
    {
        final ChunkData data = EntityHelpers.getChunkDataForSpawning(level, pos);
        final ChunkGenerator generator = level.getLevel().getChunkSource().getGenerator();
        if (fauna.getClimate() instanceof NTEClimatePlacementAccess access)
        {
            return access.nte$isValid(data, pos, random, generator, level.getLevel().getSeed());
        }
        return fauna.getClimate().isValid(data, pos, random);
    }

    public static boolean isMonthValid(Fauna fauna, ServerLevelAccessor level, BlockPos pos)
    {
        if (!(fauna instanceof NTEFaunaAccess access))
        {
            return true;
        }

        final List<Month> months = access.tfe$getMonths();
        return months.isEmpty() || months.contains(NTESeasonalHelpers.getHemispheralCalendarMonthOfYear(level.getLevel(), pos));
    }
}
