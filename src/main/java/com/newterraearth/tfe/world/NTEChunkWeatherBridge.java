package com.newterraearth.tfe.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

public interface NTEChunkWeatherBridge
{
    long tfe$getLastRandomTick();

    void tfe$setLastRandomTick(ChunkAccess chunk, long lastRandomTick);

    long tfe$getLastCalendarTick();

    void tfe$setLastCalendarTick(ChunkAccess chunk, long lastCalendarTick);

    BlockPos tfe$getNextSnowPos(ChunkPos chunkPos);

    void tfe$iterateSnowPos(ChunkAccess chunk);
}
