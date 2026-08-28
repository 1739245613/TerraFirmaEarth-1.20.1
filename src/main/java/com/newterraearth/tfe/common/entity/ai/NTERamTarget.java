/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.newterraearth.tfe.common.entity.ai;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.dries007.tfc.common.entities.prey.RammingPrey;

/**
 * 1.21-compatible impact behavior with attacker-dependent target filtering.
 */
public class NTERamTarget extends Behavior<RammingPrey>
{
    public static final int TIME_OUT_DURATION = 200;
    public static final float RAM_SPEED_FORCE_FACTOR = 1.65F;
    private final Function<RammingPrey, UniformInt> getTimeBetweenRams;
    private final float speed;
    private final ToDoubleFunction<RammingPrey> getKnockbackForce;
    private Vec3 ramDirection;
    private final Function<RammingPrey, SoundEvent> getImpactSound;

    public NTERamTarget(Function<RammingPrey, UniformInt> getTimeBetweenRams, float speed, ToDoubleFunction<RammingPrey> getKnockbackForce, Function<RammingPrey, SoundEvent> getImpactSound)
    {
        super(ImmutableMap.of(
            MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.RAM_TARGET, MemoryStatus.VALUE_PRESENT
        ), TIME_OUT_DURATION);
        this.getTimeBetweenRams = getTimeBetweenRams;
        this.speed = speed;
        this.getKnockbackForce = getKnockbackForce;
        this.getImpactSound = getImpactSound;
        this.ramDirection = Vec3.ZERO;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, RammingPrey rammingPrey)
    {
        return rammingPrey.getBrain().hasMemoryValue(MemoryModuleType.RAM_TARGET);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, RammingPrey rammingPrey, long time)
    {
        return rammingPrey.getBrain().hasMemoryValue(MemoryModuleType.RAM_TARGET);
    }

    @Override
    protected void start(ServerLevel level, RammingPrey rammingPrey, long time)
    {
        final BlockPos blockPos = rammingPrey.blockPosition();
        final Brain<?> brain = rammingPrey.getBrain();
        final Vec3 ramTargetVector = brain.getMemory(MemoryModuleType.RAM_TARGET).get();
        ramDirection = new Vec3(blockPos.getX() - ramTargetVector.x(), 0.0D, blockPos.getZ() - ramTargetVector.z()).normalize();
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(ramTargetVector, speed, 0));
    }

    @Override
    protected void tick(ServerLevel level, RammingPrey rammingPrey, long time)
    {
        final Brain<?> brain = rammingPrey.getBrain();
        final TargetingConditions ramTargeting = NTERammingPreyAi.targetingConditions(rammingPrey);
        final List<LivingEntity> list = level.getNearbyEntities(
            LivingEntity.class,
            ramTargeting,
            rammingPrey,
            rammingPrey.getBoundingBox().inflate(rammingPrey.getRammingReach())
        );

        if (!list.isEmpty())
        {
            final LivingEntity target = list.get(0);
            target.hurt(level.damageSources().noAggroMobAttack(rammingPrey), (float) rammingPrey.getAttributeValue(Attributes.ATTACK_DAMAGE) * rammingPrey.getAttackDamageMultiplier());
            final int speedAmplifier = rammingPrey.hasEffect(MobEffects.MOVEMENT_SPEED) ? rammingPrey.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 1 : 0;
            final int slowdownAmplifier = rammingPrey.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) ? rammingPrey.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() + 1 : 0;
            final float effectAdjustment = 0.25F * (speedAmplifier - slowdownAmplifier);
            final float knockbackSpeed = Mth.clamp(rammingPrey.getSpeed() * RAM_SPEED_FORCE_FACTOR, 0.2F, 3.0F) + effectAdjustment;
            final float blockedMultiplier = target.isDamageSourceBlocked(level.damageSources().mobAttack(rammingPrey)) ? 0.5F : 1.0F;
            final float damageMultiplier = rammingPrey.getAttackDamageMultiplier() * 0.6F;
            target.knockback(
                (double) (knockbackSpeed * blockedMultiplier * damageMultiplier) * getKnockbackForce.applyAsDouble(rammingPrey),
                ramDirection.x(),
                ramDirection.z()
            );
            finishRam(level, rammingPrey);
            level.playSound((Player) null, rammingPrey, getImpactSound.apply(rammingPrey), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        else
        {
            final Optional<WalkTarget> walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET);
            final Optional<Vec3> ramTarget = brain.getMemory(MemoryModuleType.RAM_TARGET);
            final boolean reachedTarget = walkTarget.isEmpty()
                || ramTarget.isEmpty()
                || walkTarget.get().getTarget().currentPosition().closerThan(ramTarget.get(), 0.25D);
            if (reachedTarget)
            {
                finishRam(level, rammingPrey);
            }
        }
    }

    private void finishRam(ServerLevel level, RammingPrey rammingPrey)
    {
        level.broadcastEntityEvent(rammingPrey, (byte) 59);
        rammingPrey.getBrain().setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, getTimeBetweenRams.apply(rammingPrey).sample(level.random));
        rammingPrey.getBrain().eraseMemory(MemoryModuleType.RAM_TARGET);
    }
}
