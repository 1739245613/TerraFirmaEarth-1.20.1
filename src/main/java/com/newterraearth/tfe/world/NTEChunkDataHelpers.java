package com.newterraearth.tfe.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.LevelChunk;

import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.ChunkDataCapability;

import com.newterraearth.tfe.mixin.ChunkDataCapabilityAccessor;

public final class NTEChunkDataHelpers
{
    private NTEChunkDataHelpers()
    {
    }

    public static ChunkData get(LevelReader level, BlockPos pos)
    {
        return get(level, new ChunkPos(pos));
    }

    @SuppressWarnings("deprecation")
    public static ChunkData get(LevelReader level, ChunkPos pos)
    {
        return level.hasChunk(pos.x, pos.z)
            && level.getChunk(pos.x, pos.z) instanceof LevelChunk levelChunk
            ? get(levelChunk)
            : ChunkData.EMPTY;
    }

    public static ChunkData get(LevelChunk chunk)
    {
        return chunk.getCapability(ChunkDataCapability.CAPABILITY)
            .map(capability -> ((ChunkDataCapabilityAccessor) (Object) capability).tfe$getData())
            .orElse(ChunkData.EMPTY);
    }
}
