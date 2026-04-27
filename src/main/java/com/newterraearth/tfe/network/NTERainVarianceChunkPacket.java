package com.newterraearth.tfe.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import net.dries007.tfc.world.chunkdata.LerpFloatLayer;

import com.newterraearth.tfe.client.NTEClientRainVarianceCache;

public record NTERainVarianceChunkPacket(
    int chunkX,
    int chunkZ,
    LerpFloatLayer rainVarianceLayer
)
{
    public NTERainVarianceChunkPacket(ChunkPos pos, LerpFloatLayer rainVarianceLayer)
    {
        this(pos.x, pos.z, rainVarianceLayer);
    }

    public NTERainVarianceChunkPacket(FriendlyByteBuf buffer)
    {
        this(
            buffer.readVarInt(),
            buffer.readVarInt(),
            new LerpFloatLayer(buffer)
        );
    }

    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(chunkX);
        buffer.writeVarInt(chunkZ);
        rainVarianceLayer.encode(buffer);
    }

    public void handle()
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            NTEClientRainVarianceCache.put(new ChunkPos(chunkX, chunkZ), rainVarianceLayer)
        );
    }
}
