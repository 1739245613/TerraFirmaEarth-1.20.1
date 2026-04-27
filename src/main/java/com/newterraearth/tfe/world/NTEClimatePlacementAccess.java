package com.newterraearth.tfe.world;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkGenerator;

import net.dries007.tfc.world.chunkdata.ChunkData;

import com.newterraearth.tfe.world.forest.NTEForestType;

public interface NTEClimatePlacementAccess
{
    boolean nte$isValid(ChunkData data, BlockPos pos, RandomSource random, ChunkGenerator generator, long levelSeed);

    float nte$getMinGroundwater();

    float nte$getMaxGroundwater();

    float nte$getMinRainVariance();

    float nte$getMaxRainVariance();

    boolean nte$isRainVarianceAbsolute();

    List<NTEForestType> nte$getForestTypes();

    int nte$getMinElevation();

    int nte$getMaxElevation();
}
