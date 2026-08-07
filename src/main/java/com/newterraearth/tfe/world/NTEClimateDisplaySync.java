package com.newterraearth.tfe.world;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.network.PacketDistributor;

import net.dries007.tfc.world.chunkdata.LerpFloatLayer;

import com.newterraearth.tfe.network.NTEPacketHandler;
import com.newterraearth.tfe.network.NTERainVarianceChunkPacket;

public final class NTEClimateDisplaySync
{
    private static final int RAIN_VARIANCE_CACHE_SIZE = 8_192;
    private static final int RAIN_VARIANCE_CACHE_MASK = RAIN_VARIANCE_CACHE_SIZE - 1;
    private static final ConcurrentMap<ServerLevel, RainVarianceCache> RAIN_VARIANCE_CACHE = new ConcurrentHashMap<>();

    private NTEClimateDisplaySync()
    {
    }

    public static void init()
    {
        MinecraftForge.EVENT_BUS.addListener(NTEClimateDisplaySync::onChunkWatch);
        MinecraftForge.EVENT_BUS.addListener(NTEClimateDisplaySync::onChunkUnload);
        MinecraftForge.EVENT_BUS.addListener(NTEClimateDisplaySync::onLevelUnload);
    }

    private static void onChunkWatch(ChunkWatchEvent.Watch event)
    {
        final ServerLevel level = event.getPlayer().serverLevel();
        final LerpFloatLayer rainVarianceLayer = getRainVarianceLayer(level, event.getChunk().getPos());
        if (rainVarianceLayer != null)
        {
            NTEPacketHandler.send(PacketDistributor.PLAYER.with(event::getPlayer), new NTERainVarianceChunkPacket(event.getChunk().getPos(), rainVarianceLayer));
        }
    }

    static float getRainVariance(ServerLevel level, BlockPos pos)
    {
        final LerpFloatLayer layer = getRainVarianceLayer(level, ChunkPos.asLong(pos));
        return layer == null
            ? Float.NaN
            : layer.getValue((pos.getX() & 15) / 16f, (pos.getZ() & 15) / 16f);
    }

    @Nullable
    private static LerpFloatLayer getRainVarianceLayer(ServerLevel level, ChunkPos chunkPos)
    {
        return getRainVarianceLayer(level, chunkPos.toLong());
    }

    @Nullable
    private static LerpFloatLayer getRainVarianceLayer(ServerLevel level, long chunkKey)
    {
        RainVarianceCache levelCache = RAIN_VARIANCE_CACHE.get(level);
        if (levelCache == null)
        {
            final RainVarianceCache newLevelCache = new RainVarianceCache();
            final RainVarianceCache existingLevelCache = RAIN_VARIANCE_CACHE.putIfAbsent(level, newLevelCache);
            levelCache = existingLevelCache == null ? newLevelCache : existingLevelCache;
        }
        return levelCache.getOrCreate(level, chunkKey);
    }

    @Nullable
    private static LerpFloatLayer createRainVarianceLayer(ServerLevel level, ChunkPos chunkPos)
    {
        final ChunkGenerator generator = level.getChunkSource().getGenerator();
        return NTE121ClimateHelpers.getRainVarianceLayer(level.getSeed(), generator, chunkPos);
    }

    private static void onChunkUnload(ChunkEvent.Unload event)
    {
        if (event.getLevel() instanceof ServerLevel level)
        {
            final RainVarianceCache levelCache = RAIN_VARIANCE_CACHE.get(level);
            if (levelCache != null)
            {
                levelCache.remove(event.getChunk().getPos().toLong());
            }
        }
    }

    private static void onLevelUnload(LevelEvent.Unload event)
    {
        if (event.getLevel() instanceof ServerLevel level)
        {
            RAIN_VARIANCE_CACHE.remove(level);
        }
    }

    private static int cacheIndex(long chunkKey)
    {
        long mixed = chunkKey;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        return (int) mixed & RAIN_VARIANCE_CACHE_MASK;
    }

    private static final class RainVarianceCache
    {
        private final AtomicReferenceArray<RainVarianceEntry> entries = new AtomicReferenceArray<>(RAIN_VARIANCE_CACHE_SIZE);

        @Nullable
        LerpFloatLayer getOrCreate(ServerLevel level, long chunkKey)
        {
            final int index = cacheIndex(chunkKey);
            RainVarianceEntry entry = entries.get(index);
            if (entry != null && entry.chunkKey() == chunkKey)
            {
                return entry.layer();
            }

            final LerpFloatLayer createdLayer = createRainVarianceLayer(level, new ChunkPos(chunkKey));
            if (createdLayer == null)
            {
                return null;
            }

            entry = entries.get(index);
            if (entry != null && entry.chunkKey() == chunkKey)
            {
                return entry.layer();
            }
            entries.set(index, new RainVarianceEntry(chunkKey, createdLayer));
            return createdLayer;
        }

        void remove(long chunkKey)
        {
            final int index = cacheIndex(chunkKey);
            final RainVarianceEntry entry = entries.get(index);
            if (entry != null && entry.chunkKey() == chunkKey)
            {
                entries.compareAndSet(index, entry, null);
            }
        }
    }

    private record RainVarianceEntry(long chunkKey, LerpFloatLayer layer) {}
}
