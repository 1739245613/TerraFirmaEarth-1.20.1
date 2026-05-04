package com.newterraearth.tfe.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;

import net.dries007.tfc.world.chunkdata.LerpFloatLayer;

public final class NTEClientRainVarianceCache
{
    private static final ConcurrentMap<Long, LerpFloatLayer> CACHE = new ConcurrentHashMap<>();

    private NTEClientRainVarianceCache()
    {
    }

    public static void init()
    {
        MinecraftForge.EVENT_BUS.addListener(NTEClientRainVarianceCache::onChunkUnload);
        MinecraftForge.EVENT_BUS.addListener(NTEClientRainVarianceCache::onLevelUnload);
        MinecraftForge.EVENT_BUS.addListener(NTEClientRainVarianceCache::onPlayerLoggedOut);
    }

    public static void put(ChunkPos pos, LerpFloatLayer layer)
    {
        CACHE.put(pos.toLong(), layer);
    }

    public static float getRainVariance(BlockPos pos)
    {
        final LerpFloatLayer layer = CACHE.get(ChunkPos.asLong(pos));
        return layer == null ? Float.NaN : layer.getValue((pos.getX() & 15) / 16f, (pos.getZ() & 15) / 16f);
    }

    public static void clear()
    {
        CACHE.clear();
    }

    private static void onChunkUnload(ChunkEvent.Unload event)
    {
        if (event.getLevel().isClientSide())
        {
            CACHE.remove(event.getChunk().getPos().toLong());
        }
    }

    private static void onLevelUnload(LevelEvent.Unload event)
    {
        if (event.getLevel().isClientSide())
        {
            clear();
        }
    }

    private static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        clear();
    }
}
