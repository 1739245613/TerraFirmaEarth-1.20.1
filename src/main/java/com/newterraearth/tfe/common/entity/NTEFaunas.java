package com.newterraearth.tfe.common.entity;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import net.dries007.tfc.common.entities.Fauna;
import net.dries007.tfc.common.entities.aquatic.AquaticMob;
import net.dries007.tfc.common.entities.prey.RammingPrey;
import net.dries007.tfc.util.Helpers;

import com.newterraearth.tfe.common.entity.aquatic.NTELeopardSeal;

public final class NTEFaunas
{
    private static final FaunaType<NTELeopardSeal> LEOPARD_SEAL = registerAnimal(NTEEntities.LEOPARD_SEAL);
    private static final FaunaType<RammingPrey> BISON = registerAnimal(NTEEntities.BISON);

    private NTEFaunas()
    {
    }

    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event)
    {
        registerSpawnPlacement(event, LEOPARD_SEAL);
        registerSpawnPlacement(event, BISON);
    }

    private static <E extends Mob> FaunaType<E> registerAnimal(RegistryObject<EntityType<E>> entity)
    {
        return register(entity, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
    }

    private static <E extends Mob> FaunaType<E> register(RegistryObject<EntityType<E>> entity, SpawnPlacements.Type spawnPlacement, Heightmap.Types heightmapType)
    {
        final Supplier<Fauna> fauna = Fauna.MANAGER.register(entity.getId());
        return new FaunaType<>(entity, fauna, spawnPlacement, heightmapType);
    }

    private static <E extends Mob> void registerSpawnPlacement(SpawnPlacementRegisterEvent event, FaunaType<E> type)
    {
        event.register(type.entity().get(), type.spawnPlacementType(), type.heightmapType(), (mob, level, heightmap, pos, rand) -> {
            final Fauna fauna = type.fauna().get();
            final ChunkGenerator generator = level.getLevel().getChunkSource().getGenerator();
            if (rand.nextInt(fauna.getChance()) != 0)
            {
                return false;
            }

            if (mob instanceof AquaticMob aquaticMob && !aquaticMob.canSpawnIn(level.getFluidState(pos).getType()))
            {
                return false;
            }

            final int seaLevel = generator.getSeaLevel();
            if (fauna.getDistanceBelowSeaLevel() != -1 && pos.getY() > seaLevel - fauna.getDistanceBelowSeaLevel())
            {
                return false;
            }

            if (!NTEFaunaSpawnHelpers.isClimateValid(fauna, level, pos, rand) || !NTEFaunaSpawnHelpers.isMonthValid(fauna, level, pos))
            {
                return false;
            }

            final BlockPos below = pos.below();
            if (fauna.isSolidGround() && !Helpers.isBlock(level.getBlockState(below), BlockTags.VALID_SPAWN))
            {
                return false;
            }
            return fauna.getMaxBrightness() == -1 || level.getRawBrightness(pos, 0) <= fauna.getMaxBrightness();
        }, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private record FaunaType<E extends Mob>(Supplier<EntityType<E>> entity, Supplier<Fauna> fauna, SpawnPlacements.Type spawnPlacementType, Heightmap.Types heightmapType) {}
}
