package com.newterraearth.tfe.world;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.network.PacketDistributor;

import net.dries007.tfc.world.chunkdata.LerpFloatLayer;

import com.newterraearth.tfe.network.NTEPacketHandler;
import com.newterraearth.tfe.network.NTERainVarianceChunkPacket;

public final class NTEClimateDisplaySync
{
    private NTEClimateDisplaySync()
    {
    }

    public static void init()
    {
        MinecraftForge.EVENT_BUS.addListener(NTEClimateDisplaySync::onChunkWatch);
    }

    private static void onChunkWatch(ChunkWatchEvent.Watch event)
    {
        final ChunkGenerator generator = event.getPlayer().serverLevel().getChunkSource().getGenerator();
        final LerpFloatLayer rainVarianceLayer = NTE121ClimateHelpers.getRainVarianceLayer(event.getPlayer().serverLevel().getSeed(), generator, event.getChunk().getPos());
        if (rainVarianceLayer != null)
        {
            NTEPacketHandler.send(PacketDistributor.PLAYER.with(event::getPlayer), new NTERainVarianceChunkPacket(event.getChunk().getPos(), rainVarianceLayer));
        }
    }
}
