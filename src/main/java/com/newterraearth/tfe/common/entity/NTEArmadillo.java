package com.newterraearth.tfe.common.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.gameevent.GameEvent;

import net.dries007.tfc.util.calendar.Calendars;


/** A 1.20 implementation of the 4.2.x armadillo prey behaviour. */
public final class NTEArmadillo extends Animal
{
    private static final long SCUTE_COOLDOWN_TICKS = 30L * 60L * 20L;
    private static final TagKey<EntityType<?>> LAND_PREDATORS = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("tfc", "land_predators"));
    private static final EntityDataAccessor<Boolean> MALE = SynchedEntityData.defineId(NTEArmadillo.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ROLLED = SynchedEntityData.defineId(NTEArmadillo.class, EntityDataSerializers.BOOLEAN);
    private long producedTick;

    public NTEArmadillo(EntityType<? extends NTEArmadillo> type, Level level)
    {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Animal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 12.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.18D);
    }

    @Override
    protected void registerGoals()
    {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.35D));
        goalSelector.addGoal(2, new AvoidEntityGoal<LivingEntity>(this, LivingEntity.class,
            entity -> (entity instanceof Player player && !player.isCreative() && !player.isSpectator())
                || (entity.getType().is(LAND_PREDATORS) && !entity.isSleeping()),
            10.0F, 1.25D, 1.5D, entity -> true));
        goalSelector.addGoal(3, new BreedGoal(this, 1.0D));
        goalSelector.addGoal(4, new TemptGoal(this, 1.1D, Ingredient.of(Items.SPIDER_EYE), false));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack)
    {
        return stack.is(Items.SPIDER_EYE);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other)
    {
        return NTEEntities.ARMADILLO.get().create(level);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(MALE, true);
        entityData.define(ROLLED, false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnData, CompoundTag data)
    {
        setMale(random.nextBoolean());
        producedTick = Calendars.get(level).getTicks() - random.nextInt(20_000);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData, data);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("male", isMale());
        tag.putLong("produced", producedTick);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        setMale(tag.getBoolean("male"));
        final long now = Calendars.get(level()).getTicks();
        if (tag.contains("produced"))
        {
            producedTick = tag.getLong("produced");
        }
        else if (tag.contains("scute_cooldown"))
        {
            final long remaining = Math.max(0L, tag.getInt("scute_cooldown"));
            producedTick = now - Math.max(0L, SCUTE_COOLDOWN_TICKS - remaining);
        }
        else
        {
            producedTick = 0L;
        }
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!level().isClientSide)
        {
            final LivingEntity threat = level().getNearestPlayer(getX(), getY(), getZ(), 8.0D, entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator());
            final boolean predatorNearby = !level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10.0D),
                entity -> entity != this && entity.getType().is(LAND_PREDATORS) && !entity.isSleeping()).isEmpty();
            setRolled(threat != null || predatorNearby || getLastHurtByMob() != null && getLastHurtByMob().isAlive());
        }
        if (!level().isClientSide && !isBaby())
        {
            if (getProductsCooldown() == 0)
            {
                spawnAtLocation(NTEItems.ARMADILLO_SCUTE.get());
                playSound(SoundEvents.ITEM_PICKUP, 1.0F, 1.0F);
                gameEvent(GameEvent.ENTITY_PLACE);
                producedTick = Calendars.get(level()).getTicks();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        return super.hurt(source, isRolled() ? amount * 0.5F : amount);
    }

    public boolean isMale()
    {
        return entityData.get(MALE);
    }

    public void setMale(boolean male)
    {
        entityData.set(MALE, male);
    }

    public boolean isRolled()
    {
        return entityData.get(ROLLED);
    }

    public void setRolled(boolean rolled)
    {
        entityData.set(ROLLED, rolled);
    }

    public long getProductsCooldown()
    {
        return Math.max(0L, SCUTE_COOLDOWN_TICKS + producedTick - Calendars.get(level()).getTicks());
    }
}
