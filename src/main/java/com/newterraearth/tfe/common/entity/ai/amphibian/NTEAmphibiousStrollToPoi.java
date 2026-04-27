package com.newterraearth.tfe.common.entity.ai.amphibian;

import java.util.function.Function;

import org.apache.commons.lang3.mutable.MutableLong;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public final class NTEAmphibiousStrollToPoi
{
    private NTEAmphibiousStrollToPoi()
    {
    }

    public static BehaviorControl<PathfinderMob> create(MemoryModuleType<GlobalPos> poiPosMemory, Function<LivingEntity, Float> speedModifier, int closeEnoughDist, int maxDistFromPoi)
    {
        final MutableLong nextWalkTargetAt = new MutableLong(0L);
        return BehaviorBuilder.create(instance -> instance.group(
            instance.registered(MemoryModuleType.WALK_TARGET),
            instance.present(poiPosMemory)
        ).apply(instance, (walkTarget, poiPos) -> (level, mob, gameTime) -> {
            final GlobalPos globalPos = instance.get(poiPos);
            if (level.dimension() != globalPos.dimension() || !globalPos.pos().closerToCenterThan(mob.position(), maxDistFromPoi))
            {
                return false;
            }
            if (gameTime <= nextWalkTargetAt.getValue())
            {
                return true;
            }
            walkTarget.set(new WalkTarget(globalPos.pos(), speedModifier.apply(mob), closeEnoughDist));
            nextWalkTargetAt.setValue(gameTime + 80L);
            return true;
        }));
    }
}
