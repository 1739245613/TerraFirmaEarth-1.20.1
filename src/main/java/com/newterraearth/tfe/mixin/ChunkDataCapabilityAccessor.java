package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.ChunkDataCapability;

@Mixin(value = ChunkDataCapability.class, remap = false)
public interface ChunkDataCapabilityAccessor
{
    @Accessor(value = "data", remap = false)
    ChunkData tfe$getData();
}
