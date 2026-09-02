package com.newterraearth.tfe.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class NTEDromedaryCamel extends NTECamel
{
    public static AttributeSupplier.Builder createAttributes()
    {
        return Camel.createAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.1F);
    }

    public NTEDromedaryCamel(EntityType<? extends NTECamel> type, Level level)
    {
        super(type, level);
        // 1.21 exposes STEP_HEIGHT as an attribute; 1.20 stores the same
        // movement capability on Entity, so apply the equivalent value here.
        setMaxUpStep(1.5F);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source)
    {
        return source.is(DamageTypes.CACTUS) || super.isInvulnerableTo(source);
    }

    @Override
    public void setInLove(@Nullable Player player)
    {
        // Dromedaries are not a breeding species in TFC 4.2.x.
    }
}
