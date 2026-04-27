package com.newterraearth.tfe.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NaturalSpawner.class)
public interface NaturalSpawnerAccessor
{
    @Invoker("getRandomPosWithin")
    static BlockPos tfe$getRandomPosWithin(Level level, LevelChunk chunk)
    {
        throw new AssertionError();
    }

    @Invoker("canSpawnMobAt")
    static boolean tfe$canSpawnMobAt(ServerLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator, MobCategory category, MobSpawnSettings.SpawnerData spawner, BlockPos pos)
    {
        throw new AssertionError();
    }

    @Invoker("isSpawnPositionOk")
    static boolean tfe$isSpawnPositionOk(Type placementType, LevelReader level, BlockPos pos, net.minecraft.world.entity.EntityType<?> type)
    {
        throw new AssertionError();
    }
}
