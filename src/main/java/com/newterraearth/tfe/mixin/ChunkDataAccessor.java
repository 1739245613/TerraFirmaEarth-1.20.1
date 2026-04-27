package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.ForestType;

@Mixin(value = ChunkData.class, remap = false)
public interface ChunkDataAccessor
{
    @Accessor(value = "forestType", remap = false)
    void tfe$setForestType(ForestType forestType);
}
