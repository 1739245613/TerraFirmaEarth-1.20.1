/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.newterraearth.tfe.common.entity.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BabyFollowAdult;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.EraseMemoryIf;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetAwayFrom;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.schedule.Activity;

import net.dries007.tfc.client.TFCSounds;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.entities.ai.predator.PredatorAi;
import net.dries007.tfc.common.entities.ai.prey.AvoidPredatorBehavior;
import net.dries007.tfc.common.entities.ai.prey.PreyAi;
import net.dries007.tfc.common.entities.ai.SetLookTarget;
import net.dries007.tfc.common.entities.prey.RammingPrey;
import net.dries007.tfc.util.Helpers;

/**
 * 1.21-compatible RammingPrey brain implementation for the 1.20 runtime.
 */
public final class NTERammingPreyAi
{
    public static final int RAM_PREPARE_TIME = 20;
    public static final int RAM_MAX_DISTANCE = 9;
    public static final float SPEED_MULTIPLIER_WHEN_PREPARING_TO_RAM = 1.6F;
    public static final float ADULT_RAM_KNOCKBACK_FORCE = 2.5F;
    public static final float BABY_RAM_KNOCKBACK_FORCE = 1.0F;
    public static final UniformInt TIME_BETWEEN_RAMS_MALE = UniformInt.of(600, 1000);
    public static final UniformInt TIME_BETWEEN_RAMS_FEMALE = UniformInt.of(1000, 1600);
    public static final TagKey<EntityType<?>> NOT_RAMMED_BY_RAMMERS = TagKey.create(
        Registries.ENTITY_TYPE,
        new ResourceLocation("tfc", "not_rammed_by_rammers")
    );

    public static final TargetingConditions RAM_TARGET_CONDITIONS = TargetingConditions.forCombat().selector(target ->
        target.level().getWorldBorder().isWithinBounds(target.getBoundingBox())
            && !(target instanceof RammingPrey)
            && !Helpers.isEntity(target, NOT_RAMMED_BY_RAMMERS)
    );

    public static final TargetingConditions RAM_TARGET_CONDITIONS_ADULT_MALE = TargetingConditions.forCombat().selector(target ->
        target.level().getWorldBorder().isWithinBounds(target.getBoundingBox())
            && !(target instanceof RammingPrey
                && !Helpers.isEntity(target, NOT_RAMMED_BY_RAMMERS)
                && (!((RammingPrey) target).isMale()
                    || target.isBaby()
                    || (target.getHealth() / target.getMaxHealth() < 0.7)))
    );

    private NTERammingPreyAi()
    {
    }

    public static UniformInt timeBetweenRams(RammingPrey prey)
    {
        return prey.isMale() ? TIME_BETWEEN_RAMS_MALE : TIME_BETWEEN_RAMS_FEMALE;
    }

    public static TargetingConditions targetingConditions(RammingPrey prey)
    {
        return prey.isMale() && !prey.isBaby() ? RAM_TARGET_CONDITIONS_ADULT_MALE : RAM_TARGET_CONDITIONS;
    }

    public static void initMemories(RammingPrey prey, RandomSource random)
    {
        prey.getBrain().setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, timeBetweenRams(prey).sample(random));
    }

    public static Brain<?> makeBrain(Brain<? extends RammingPrey> brain)
    {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initRetreatActivity(brain);
        initRamActivity(brain);

        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    private static void initCoreActivity(Brain<? extends RammingPrey> brain)
    {
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(
            new Swim(0.7F),
            new LookAtTargetSink(45, 90),
            new MoveToTargetSink(),
            new CountDownCooldownTicks(MemoryModuleType.RAM_COOLDOWN_TICKS),
            EraseMemoryIf.create(PredatorAi::hasNearbyAttacker, MemoryModuleType.RAM_COOLDOWN_TICKS),
            EraseMemoryIf.create(NTERammingPreyAi::attackerHasLeft, MemoryModuleType.HURT_BY_ENTITY)
        ));
    }

    private static void initIdleActivity(Brain<? extends RammingPrey> brain)
    {
        brain.addActivity(Activity.IDLE, ImmutableList.of(
            Pair.of(0, SetLookTarget.create(EntityType.PLAYER, 6.0F, UniformInt.of(30, 60))),
            Pair.of(1, AvoidPredatorBehavior.create(true)),
            Pair.of(2, BabyFollowAdult.create(UniformInt.of(5, 16), 1.25F)),
            Pair.of(3, createIdleMovementBehaviors())
        ));
    }

    private static void initRetreatActivity(Brain<? extends RammingPrey> brain)
    {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.AVOID, 10, ImmutableList.of(
            SetWalkTargetAwayFrom.entity(MemoryModuleType.AVOID_TARGET, 1.1F, 15, false),
            createIdleMovementBehaviors(),
            SetLookTarget.create(8.0F, UniformInt.of(30, 60)),
            EraseMemoryIf.create(PreyAi::wantsToStopFleeing, MemoryModuleType.AVOID_TARGET)
        ), MemoryModuleType.AVOID_TARGET);
    }

    private static RunOne<RammingPrey> createIdleMovementBehaviors()
    {
        return new RunOne<>(
            ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
            ImmutableList.of(
                Pair.of(RandomStroll.stroll(1.0F), 1),
                Pair.of(SetWalkTargetFromLookTarget.create(1.0F, 3), 1),
                Pair.of(new DoNothing(30, 60), 1)
            )
        );
    }

    private static void initRamActivity(Brain<? extends RammingPrey> brain)
    {
        brain.addActivityWithConditions(Activity.RAM, ImmutableList.of(
            Pair.of(0, new NTERamTarget(
                NTERammingPreyAi::timeBetweenRams,
                3.0F,
                prey -> prey.isBaby() ? BABY_RAM_KNOCKBACK_FORCE : ADULT_RAM_KNOCKBACK_FORCE,
                prey -> TFCSounds.RAMMING_IMPACT.get()
            )),
            Pair.of(1, new NTEPrepareRamNearestTarget<>(
                prey -> 10,
                SPEED_MULTIPLIER_WHEN_PREPARING_TO_RAM,
                RAM_PREPARE_TIME,
                prey -> prey.getAttackSound().get()
            ))
        ), ImmutableSet.of(
            Pair.of(MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT)
        ));
    }

    public static void updateActivity(RammingPrey prey)
    {
        prey.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.AVOID, Activity.RAM, Activity.IDLE));
    }

    public static boolean attackerHasLeft(LivingEntity rammingPrey)
    {
        return !rammingPrey.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY)
            .map(entity -> entity.distanceToSqr(rammingPrey) < 400)
            .orElse(false);
    }
}
