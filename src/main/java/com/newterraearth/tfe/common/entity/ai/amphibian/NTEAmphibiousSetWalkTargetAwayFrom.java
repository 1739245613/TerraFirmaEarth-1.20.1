package com.newterraearth.tfe.common.entity.ai.amphibian;

import java.util.Optional;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.util.Helpers;

public final class NTEAmphibiousSetWalkTargetAwayFrom
{
    private NTEAmphibiousSetWalkTargetAwayFrom()
    {
    }

    public static OneShot<PathfinderMob> entity(MemoryModuleType<LivingEntity> walkTargetAwayFromMemory, Function<LivingEntity, Float> speedModifier, int desiredDistance, boolean hasTarget)
    {
        return create(walkTargetAwayFromMemory, speedModifier, desiredDistance, hasTarget, Entity::position);
    }

    private static OneShot<PathfinderMob> create(MemoryModuleType<LivingEntity> walkTargetAwayFromMemory, Function<LivingEntity, Float> speedModifier, int desiredDistance, boolean hasTarget, Function<LivingEntity, Vec3> toPosition)
    {
        return BehaviorBuilder.create(instance -> instance.group(
            instance.registered(MemoryModuleType.WALK_TARGET),
            instance.present(walkTargetAwayFromMemory)
        ).apply(instance, (pathToTarget, memoryAccessor) -> (level, mob, gameTime) -> {
            final Optional<WalkTarget> currentTarget = instance.tryGet(pathToTarget);
            if (currentTarget.isPresent() && !hasTarget)
            {
                return false;
            }

            final Vec3 currentPos = mob.position();
            final LivingEntity attacker = instance.get(memoryAccessor);
            final Vec3 escapeFromPos = toPosition.apply(attacker);
            if (!currentPos.closerThan(escapeFromPos, desiredDistance))
            {
                return false;
            }

            if (currentTarget.isPresent() && currentTarget.get().getSpeedModifier() == speedModifier.apply(mob) && isTargetPosValidForEscape(currentPos, escapeFromPos, currentTarget.get().getTarget().currentPosition()))
            {
                return false;
            }

            final boolean isOceanPredator = Helpers.isEntity(attacker, TFCTags.Entities.OCEAN_PREDATORS);
            Vec3 bestLandEscapePos = currentPos;
            Vec3 bestWaterEscapePos = currentPos;
            boolean foundLand = false;
            boolean foundWater = false;

            for (int i = 0; i < 10; ++i)
            {
                final Vec3 destination = DefaultRandomPos.getPosAway(mob, 10, 7, escapeFromPos);
                if (destination == null)
                {
                    continue;
                }

                final BlockPos pos = BlockPos.containing(destination);
                final BlockState aboveState = level.getBlockState(pos.above());
                if (Helpers.isFluid(aboveState.getFluidState(), TFCTags.Fluids.ANY_INFINITE_WATER))
                {
                    bestWaterEscapePos = getVectorFartherFromChaser(escapeFromPos, bestWaterEscapePos, destination);
                    foundWater = true;
                    if (!isOceanPredator)
                    {
                        break;
                    }
                }
                else
                {
                    final BlockState belowState = level.getBlockState(pos);
                    if (!belowState.isAir() && !Helpers.isFluid(belowState.getFluidState(), TFCTags.Fluids.ANY_INFINITE_WATER))
                    {
                        bestLandEscapePos = getVectorFartherFromChaser(escapeFromPos, bestLandEscapePos, destination);
                        foundLand = true;
                        if (isOceanPredator)
                        {
                            break;
                        }
                    }
                }
            }

            final Vec3 bestEscapePos;
            if (isOceanPredator)
            {
                bestEscapePos = foundLand ? bestLandEscapePos : (foundWater ? bestWaterEscapePos : null);
            }
            else
            {
                bestEscapePos = foundWater ? bestWaterEscapePos : (foundLand ? bestLandEscapePos : null);
            }

            if (bestEscapePos == null)
            {
                return false;
            }

            pathToTarget.set(new WalkTarget(bestEscapePos, speedModifier.apply(mob), 0));
            return true;
        }));
    }

    private static Vec3 getVectorFartherFromChaser(Vec3 escapeFromPos, Vec3 vector0, Vec3 vector1)
    {
        return escapeFromPos.subtract(vector0).lengthSqr() > escapeFromPos.subtract(vector1).lengthSqr() ? vector0 : vector1;
    }

    private static boolean isTargetPosValidForEscape(Vec3 currentPos, Vec3 escapeFromPos, Vec3 destinationPos)
    {
        final Vec3 destinationDelta = destinationPos.subtract(currentPos);
        final Vec3 escapeFromDelta = escapeFromPos.subtract(currentPos);
        return destinationDelta.dot(escapeFromDelta) < 0.0;
    }
}
