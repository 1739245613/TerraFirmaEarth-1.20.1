package com.newterraearth.tfe.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin
{
    /**
     * Keep the 1.20 spawning chain intact and only retarget aquatic start Y into the real water band.
     * This avoids rewriting candidate selection while compensating for the much taller 1.21-style ocean columns.
     */
    @Redirect(
        method = "spawnCategoryForChunk",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/NaturalSpawner;getRandomPosWithin(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/chunk/LevelChunk;)Lnet/minecraft/core/BlockPos;"
        )
    )
    private static BlockPos tfe$getRandomPosWithinForAquaticCategories(Level level, LevelChunk chunk, MobCategory category, net.minecraft.server.level.ServerLevel serverLevel, LevelChunk ignoredChunk, NaturalSpawner.SpawnPredicate predicate, NaturalSpawner.AfterSpawnCallback callback)
    {
        final BlockPos original = NaturalSpawnerAccessor.tfe$getRandomPosWithin(level, chunk);
        if (!isAquaticCategory(category))
        {
            return original;
        }

        final int blockX = original.getX();
        final int blockZ = original.getZ();
        final int worldSurfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ) - 1;
        final int oceanFloorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, blockX, blockZ) - 1;
        final int minWaterY = oceanFloorY + 1;
        final int maxWaterY = worldSurfaceY;
        if (maxWaterY < minWaterY)
        {
            return original;
        }

        final BlockPos surfacePos = new BlockPos(blockX, maxWaterY, blockZ);
        if (level.getFluidState(surfacePos).isEmpty())
        {
            return original;
        }

        final int aquaticY = Mth.randomBetweenInclusive(level.random, minWaterY, maxWaterY);
        return new BlockPos(blockX, aquaticY, blockZ);
    }

    private static boolean isAquaticCategory(MobCategory category)
    {
        return category == MobCategory.WATER_AMBIENT
            || category == MobCategory.WATER_CREATURE
            || category == MobCategory.UNDERGROUND_WATER_CREATURE;
    }
}
