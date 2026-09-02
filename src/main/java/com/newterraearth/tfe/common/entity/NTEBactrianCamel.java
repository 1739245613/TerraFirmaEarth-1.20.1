package com.newterraearth.tfe.common.entity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IForgeShearable;

import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.util.calendar.Calendars;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NTEBactrianCamel extends NTECamel implements IForgeShearable
{
    private static final long WOOL_COOLDOWN_TICKS = 168L * 60L * 20L;
    private static final EntityDataAccessor<Long> DATA_PRODUCED = SynchedEntityData.defineId(NTEBactrianCamel.class, EntityDataSerializers.LONG);

    public NTEBactrianCamel(EntityType<? extends NTECamel> type, Level level)
    {
        super(type, level);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source)
    {
        // TFE compatibility choice: both camel species share the dromedary's
        // cactus immunity, as requested for the 1.20.1 port.
        return source.is(DamageTypes.CACTUS) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean isShearable(@NotNull ItemStack item, Level level, BlockPos pos)
    {
        return !isBaby() && getProductsCooldown() == 0;
    }

    @Override
    @NotNull
    public List<ItemStack> onSheared(@Nullable Player player, @NotNull ItemStack item, Level level, BlockPos pos, int fortune)
    {
        setProducedTick(Calendars.get(level).getTicks());
        playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.0F);
        return List.of(new ItemStack(TFCItems.WOOL.get()));
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(DATA_PRODUCED, 0L);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putLong("produced", getProducedTick());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        final long now = Calendars.get(level()).getTicks();
        if (tag.contains("produced"))
        {
            setProducedTick(tag.getLong("produced"));
        }
        else if (tag.contains("wool_cooldown"))
        {
            final long remaining = Math.max(0L, tag.getInt("wool_cooldown"));
            setProducedTick(now - Math.max(0L, WOOL_COOLDOWN_TICKS - remaining));
        }
        else
        {
            setProducedTick(0L);
        }
    }

    public long getProducedTick()
    {
        return entityData.get(DATA_PRODUCED);
    }

    public void setProducedTick(long producedTick)
    {
        entityData.set(DATA_PRODUCED, producedTick);
    }

    public long getProductsCooldown()
    {
        // A zero timestamp is the upstream "never produced" state. Treat it
        // as ready instead of measuring the first product from world age 0.
        if (getProducedTick() <= 0L)
        {
            return 0L;
        }
        return Math.max(0L, WOOL_COOLDOWN_TICKS + getProducedTick() - Calendars.get(level()).getTicks());
    }

    /** Mirrors the 4.2 Bactrian product state used by the client wool layer. */
    public boolean hasProduct()
    {
        return getProducedTick() <= 0L || getProductsCooldown() <= 0L;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger)
    {
        // Keep TFC 4.2's <= 1 passenger-count check (at most two passengers)
        // instead of changing the Bactrian seat capacity to a 1.20 default.
        return getPassengers().size() <= 1;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction)
    {
        // 1.21 exposes getPassengerAttachmentPoint(), which TFC overrides to
        // put Bactrian riders on the centered body anchor. 1.20 has no such
        // hook, so retain Camel's animated vertical anchor and normalize only
        // the horizontal seat coordinates to the entity center.
        super.positionRider(passenger, (rider, x, y, z) -> moveFunction.accept(rider, getX(), y, getZ()));
    }

    @Override
    protected float getRiddenSpeed(Player player)
    {
        // TFC 4.2 uses a slightly smaller sprint bonus for Bactrian camels than
        // the vanilla Camel/Dromedary path.
        final float sprintBonus = player.isSprinting() && getJumpCooldown() == 0 ? 0.0875F : 0.0F;
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED) + sprintBonus;
    }
}
