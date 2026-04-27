package com.newterraearth.tfe.common.entity.ai.amphibian;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BabyFollowAdult;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.EraseMemoryIf;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.TryFindWater;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.entities.ai.SetLookTarget;
import net.dries007.tfc.common.entities.ai.TFCBrain;
import net.dries007.tfc.common.entities.ai.predator.PredatorAi;
import net.dries007.tfc.common.entities.aquatic.AmphibiousAnimal;
import net.dries007.tfc.util.Helpers;

public final class NTEPinnipedAI
{
    private static final UniformInt ADULT_FOLLOW_RANGE = UniformInt.of(15, 30);

    public static final ImmutableList<SensorType<? extends Sensor<? super AmphibiousAnimal>>> SENSOR_TYPES = ImmutableList.of(
        SensorType.NEAREST_LIVING_ENTITIES,
        SensorType.NEAREST_ADULT,
        SensorType.HURT_BY,
        TFCBrain.TEMPTATION_SENSOR.get()
    );

    public static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
        MemoryModuleType.NEAREST_LIVING_ENTITIES,
        MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
        MemoryModuleType.NEAREST_VISIBLE_PLAYER,
        MemoryModuleType.LOOK_TARGET,
        MemoryModuleType.NEAREST_VISIBLE_ADULT,
        MemoryModuleType.WALK_TARGET,
        MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
        MemoryModuleType.PATH,
        MemoryModuleType.ATTACK_TARGET,
        MemoryModuleType.ATTACK_COOLING_DOWN,
        MemoryModuleType.HUNTED_RECENTLY,
        MemoryModuleType.HURT_BY_ENTITY,
        MemoryModuleType.HOME,
        MemoryModuleType.TEMPTING_PLAYER,
        MemoryModuleType.IS_TEMPTED,
        MemoryModuleType.TEMPTATION_COOLDOWN_TICKS,
        MemoryModuleType.BREED_TARGET,
        MemoryModuleType.IS_PANICKING
    );

    public static final int MAX_WANDER_DISTANCE = 100 * 100;

    private NTEPinnipedAI()
    {
    }

    public static Brain<?> makeBrain(Brain<? extends AmphibiousAnimal> brain)
    {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initRetreatActivity(brain);
        initFightActivity(brain);

        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    private static void initCoreActivity(Brain<? extends AmphibiousAnimal> brain)
    {
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(
            new LookAtTargetSink(45, 90),
            new MoveToTargetSink(),
            new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS)
        ));
    }

    private static void initIdleActivity(Brain<? extends AmphibiousAnimal> brain)
    {
        brain.addActivity(Activity.IDLE, ImmutableList.of(
            Pair.of(0, SetLookTarget.create(TFCTags.Entities.TURTLE_FRIENDS, 6.0F, UniformInt.of(30, 60))),
            Pair.of(2, new RunOne<>(ImmutableList.of(
                Pair.of(BabyFollowAdult.create(ADULT_FOLLOW_RANGE, NTEPinnipedAI::getChasingSpeedModifier), 1),
                Pair.of(new FollowTemptation(NTEPinnipedAI::getSpeedModifier), 1)
            ))),
            Pair.of(3, TryFindWater.create(6, 0.15F)),
            Pair.of(4, createIdleMovementBehaviors())
        ));
    }

    private static void initRetreatActivity(Brain<? extends AmphibiousAnimal> brain)
    {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.AVOID, 10, ImmutableList.of(
            BehaviorBuilder.triggerIf(PredatorAi::hasNearbyAttacker, NTEAmphibiousSetWalkTargetAwayFrom.entity(MemoryModuleType.HURT_BY_ENTITY, NTEPinnipedAI::getFleeingSpeedModifier, 16, true)),
            NTEAmphibiousStrollToPoi.create(MemoryModuleType.HOME, NTEPinnipedAI::getSpeedModifier, 5, MAX_WANDER_DISTANCE),
            createIdleMovementBehaviors()
        ), MemoryModuleType.HURT_BY_ENTITY);
    }

    private static void initFightActivity(Brain<? extends AmphibiousAnimal> brain)
    {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, 0, ImmutableList.of(
            StopAttackingIfTargetInvalid.create(),
            SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(NTEPinnipedAI::getChasingSpeedModifier),
            MeleeAttack.create(20),
            EraseMemoryIf.create(NTEPinnipedAI::isTempted, MemoryModuleType.ATTACK_TARGET)
        ), MemoryModuleType.ATTACK_TARGET);
    }

    public static void updateActivity(AmphibiousAnimal animal)
    {
        final Brain<AmphibiousAnimal> brain = animal.getBrain();
        final Activity current = brain.getActiveNonCoreActivity().orElse(null);
        if (current != Activity.AVOID)
        {
            brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.AVOID, Activity.FIGHT, Activity.IDLE));
        }
    }

    public static GateBehavior<AmphibiousAnimal> createIdleMovementBehaviors()
    {
        return new GateBehavior<>(
            ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
            ImmutableSet.of(),
            GateBehavior.OrderPolicy.ORDERED,
            GateBehavior.RunningPolicy.TRY_ALL,
            ImmutableList.of(
                Pair.of(StartAttacking.create(NTEPinnipedAI::getAttackTarget), 2),
                Pair.of(RandomStroll.swim(0.5F), 2),
                Pair.of(RandomStroll.stroll(0.15F, false), 2),
                Pair.of(SetWalkTargetFromLookTarget.create(NTEPinnipedAI::canSetWalkTargetFromLookTarget, NTEPinnipedAI::getSpeedModifier, 3), 3),
                Pair.of(new DoNothing(30, 60), 3),
                Pair.of(NTEAmphibiousStrollToPoi.create(MemoryModuleType.HOME, NTEPinnipedAI::getSpeedModifier, 5, 100), 3)
            )
        );
    }

    private static boolean isTempted(AmphibiousAnimal entity)
    {
        return entity.getBrain().hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER);
    }

    private static Optional<? extends LivingEntity> getAttackTarget(AmphibiousAnimal entity)
    {
        if (entity.getBrain().hasMemoryValue(MemoryModuleType.HUNTED_RECENTLY))
        {
            return Optional.empty();
        }
        return entity.getBrain()
            .getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            .flatMap(nearest -> nearest.findClosest(p -> Helpers.isEntity(p, TFCTags.Entities.SMALL_FISH) && p.isAlive()));
    }

    private static boolean canSetWalkTargetFromLookTarget(LivingEntity entity)
    {
        final Level level = entity.level();
        final Optional<PositionTracker> tracker = entity.getBrain().getMemory(MemoryModuleType.LOOK_TARGET);
        if (tracker.isPresent())
        {
            final BlockPos pos = tracker.get().currentBlockPosition();
            return level.isWaterAt(pos) == entity.isInWaterOrBubble();
        }
        return false;
    }

    private static float getSpeedModifier(LivingEntity entity)
    {
        return entity.isInWaterOrBubble() ? 0.5F : 0.15F;
    }

    private static float getChasingSpeedModifier(LivingEntity entity)
    {
        return entity.isInWaterOrBubble() ? 0.6F : 0.17F;
    }

    private static float getFleeingSpeedModifier(LivingEntity entity)
    {
        return entity.isInWaterOrBubble() ? 0.8F : 0.2F;
    }
}
