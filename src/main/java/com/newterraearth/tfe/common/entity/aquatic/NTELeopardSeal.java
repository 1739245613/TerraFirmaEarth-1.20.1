/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.newterraearth.tfe.common.entity.aquatic;

import com.mojang.serialization.Dynamic;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.dries007.tfc.common.entities.aquatic.AmphibiousAnimal;
import net.dries007.tfc.util.Helpers;

import com.newterraearth.tfe.common.entity.NTEEntitySounds;
import com.newterraearth.tfe.common.entity.ai.amphibian.NTEPinnipedAI;

public class NTELeopardSeal extends AmphibiousAnimal
{
    private static final TagKey<Item> SEAL_FOOD = ItemTags.create(new ResourceLocation("tfc", "seal_food"));

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 24.0D)
            .add(Attributes.MOVEMENT_SPEED, 1.0D)
            .add(Attributes.ATTACK_DAMAGE, 6.0D);
    }

    public NTELeopardSeal(EntityType<? extends AmphibiousAnimal> type, Level level)
    {
        super(type, level, NTEEntitySounds.SEAL);
    }

    // TODO: Would like leopard seals to defend themselves rather than play dead
    @Override
    public boolean isPlayingDeadEffective()
    {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack)
    {
        return Helpers.isItem(stack, SEAL_FOOD);
    }

    // Bridge for runtime TFC builds where Temptable#isFood is reobfuscated in the dependency jar.
    public boolean m_6898_(ItemStack stack)
    {
        return isFood(stack);
    }

    @Override
    protected SoundEvent getAmbientSound()
    {
        return ambient.get();
    }

    public void playAmbientSound()
    {
        if (!this.isInWaterOrBubble())
        {
            super.playAmbientSound();
        }
    }

    @Override
    protected Brain.Provider<? extends AmphibiousAnimal> brainProvider()
    {
        return Brain.provider(NTEPinnipedAI.MEMORY_TYPES, NTEPinnipedAI.SENSOR_TYPES);
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic)
    {
        return NTEPinnipedAI.makeBrain(brainProvider().makeBrain(dynamic));
    }

    @Override
    protected void customServerAiStep()
    {
        getBrain().tick((ServerLevel) level(), this);
        NTEPinnipedAI.updateActivity(this);
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        return super.hurt(source, amount);
    }
}
