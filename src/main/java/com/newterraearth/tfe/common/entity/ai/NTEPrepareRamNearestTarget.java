/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.newterraearth.tfe.common.entity.ai;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PrepareRamNearestTarget;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.dries007.tfc.common.entities.ai.predator.PredatorAi;
import net.dries007.tfc.common.entities.prey.RammingPrey;

/**
 * 1.21-compatible ram preparation behavior. The animal starts from its current
 * position instead of searching for a separate pathing position.
 */
public class NTEPrepareRamNearestTarget<E extends PathfinderMob> extends Behavior<E>
{
    private final ToIntFunction<E> getCooldownOnFail;
    private final float walkSpeed;
    private final int ramPrepareTime;
    private final Function<E, SoundEvent> getPrepareRamSound;
    private Optional<Long> reachedRamPositionTimestamp = Optional.empty();
    private Optional<PrepareRamNearestTarget.RamCandidate> ramCandidate = Optional.empty();

    public NTEPrepareRamNearestTarget(ToIntFunction<E> getCooldownOnFail, float walkSpeed, int ramPrepareTime, Function<E, SoundEvent> getPrepareRamSound)
    {
        super(ImmutableMap.of(
            MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
            MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.RAM_TARGET, MemoryStatus.VALUE_ABSENT
        ), 160);
        this.getCooldownOnFail = getCooldownOnFail;
        this.walkSpeed = walkSpeed;
        this.ramPrepareTime = ramPrepareTime;
        this.getPrepareRamSound = getPrepareRamSound;
    }

    @Override
    protected void start(ServerLevel level, PathfinderMob rammingPrey, long time)
    {
        final Brain<?> brain = rammingPrey.getBrain();
        final TargetingConditions ramTargeting = NTERammingPreyAi.targetingConditions((RammingPrey) rammingPrey);
        brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).flatMap(nearestVisibleLivingEntities ->
            nearestVisibleLivingEntities.findClosest(nearestVisibleEntity -> ramTargeting.test(rammingPrey, nearestVisibleEntity))
        ).ifPresent(target -> chooseRamPosition(rammingPrey, target));
    }

    @Override
    protected void stop(ServerLevel level, E rammingPrey, long time)
    {
        final Brain<?> brain = rammingPrey.getBrain();
        if (!brain.hasMemoryValue(MemoryModuleType.RAM_TARGET))
        {
            level.broadcastEntityEvent(rammingPrey, (byte) 59);
            brain.setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, getCooldownOnFail.applyAsInt(rammingPrey));
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, PathfinderMob rammingPrey, long time)
    {
        return ramCandidate.isPresent() && ramCandidate.get().getTarget().isAlive();
    }

    @Override
    protected void tick(ServerLevel level, E rammingPrey, long time)
    {
        if (ramCandidate.isEmpty())
        {
            return;
        }

        final PrepareRamNearestTarget.RamCandidate candidate = ramCandidate.get();
        rammingPrey.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(candidate.getStartPosition(), walkSpeed, 0));
        rammingPrey.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(candidate.getTarget(), true));

        if (!candidate.getTarget().blockPosition().equals(candidate.getTargetPosition()))
        {
            if (PredatorAi.hasNearbyAttacker(rammingPrey))
            {
                level.broadcastEntityEvent(rammingPrey, (byte) 58);
            }
            else
            {
                level.broadcastEntityEvent(rammingPrey, (byte) 59);
            }
            rammingPrey.getNavigation().stop();
            chooseRamPosition(rammingPrey, candidate.getTarget());
            return;
        }

        final BlockPos blockPos = rammingPrey.blockPosition();
        if (blockPos.equals(candidate.getStartPosition()))
        {
            level.broadcastEntityEvent(rammingPrey, (byte) 58);
            if (reachedRamPositionTimestamp.isEmpty())
            {
                reachedRamPositionTimestamp = Optional.of(time);
            }

            if (time - reachedRamPositionTimestamp.get() >= ramPrepareTime)
            {
                if (rammingPrey instanceof RammingPrey prey)
                {
                    prey.setAttackDamageMultiplier(calcRamDamageMultiplier(blockPos, candidate.getTargetPosition()));
                }
                rammingPrey.getBrain().setMemory(MemoryModuleType.RAM_TARGET, getEdgeOfBlock(blockPos, candidate.getTargetPosition()));
                level.playSound((Player) null, rammingPrey, getPrepareRamSound.apply(rammingPrey), SoundSource.NEUTRAL, 1.0F, rammingPrey.getVoicePitch());
                ramCandidate = Optional.empty();
                reachedRamPositionTimestamp = Optional.empty();
            }
        }
    }

    private float calcRamDamageMultiplier(BlockPos startPos, BlockPos finishPos)
    {
        final float distance = (float) startPos.distSqr(finishPos);
        return Mth.clamp(1.2F * distance / NTERammingPreyAi.RAM_MAX_DISTANCE, 0.25F, 2.0F);
    }

    private Vec3 getEdgeOfBlock(BlockPos pos1, BlockPos pos2)
    {
        final double x = 0.5D * Mth.sign(pos2.getX() - pos1.getX());
        final double z = 0.5D * Mth.sign(pos2.getZ() - pos1.getZ());
        return Vec3.atBottomCenterOf(pos2).add(x, 0.0D, z);
    }

    private void chooseRamPosition(PathfinderMob rammingPrey, LivingEntity target)
    {
        ramCandidate = Optional.of(new PrepareRamNearestTarget.RamCandidate(
            rammingPrey.blockPosition(), target.blockPosition(), target
        ));
    }
}
