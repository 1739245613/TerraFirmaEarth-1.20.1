package com.newterraearth.tfe.common.entity.misc;

import java.util.UUID;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import com.newterraearth.tfe.common.block.rope.NTERopeAnchorBlock;
import com.newterraearth.tfe.common.entity.NTEEntities;
import com.newterraearth.tfe.common.entity.NTEItems;

public class NTERopeKnot extends LeashFenceKnotEntity
{
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
        SynchedEntityData.defineId(NTERopeKnot.class, EntityDataSerializers.OPTIONAL_UUID);

    @Nullable
    public static NTERopeKnot getNewKnotAtLocation(Level level, BlockPos pos)
    {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
        final AABB bounds = new AABB(x - 1.0D, y - 1.0D, z - 1.0D, x + 1.0D, y + 1.0D, z + 1.0D);
        for (NTERopeKnot entity : level.getEntitiesOfClass(NTERopeKnot.class, bounds))
        {
            if (entity.blockPosition().equals(pos)) return null;
        }
        final NTERopeKnot knot = new NTERopeKnot(level, pos);
        level.addFreshEntity(knot);
        return knot;
    }

    @Nullable
    private UUID ownerUuid;

    public NTERopeKnot(EntityType<? extends NTERopeKnot> type, Level level)
    {
        super(type, level);
    }

    public NTERopeKnot(Level level, BlockPos pos)
    {
        super(level, pos);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(OWNER_UUID, Optional.empty());
    }

    public void setOwner(Player player)
    {
        ownerUuid = player.getUUID();
        entityData.set(OWNER_UUID, Optional.of(ownerUuid));
    }

    public boolean isOwnedBy(Player player)
    {
        final UUID owner = getOwnerUuid();
        return owner != null && owner.equals(player.getUUID());
    }

    @Nullable
    private UUID getOwnerUuid()
    {
        return ownerUuid != null ? ownerUuid : entityData.get(OWNER_UUID).orElse(null);
    }

    @Override
    public @Nullable ItemEntity spawnAtLocation(ItemStack stack, float y)
    {
        return stack.getItem() == Items.LEAD ? null : super.spawnAtLocation(stack, y);
    }

    @Override
    public EntityType<?> getType()
    {
        return NTEEntities.ROPE_KNOT.get();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand)
    {
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getPickResult()
    {
        return NTEItems.ROPE.get().getDefaultInstance();
    }

    @Override
    public boolean survives()
    {
        if (level().isClientSide)
            return level().getBlockState(blockPosition()).getBlock() instanceof NTERopeAnchorBlock;
        final UUID owner = getOwnerUuid();
        if (owner == null || !(level().getBlockState(blockPosition()).getBlock() instanceof NTERopeAnchorBlock))
            return false;
        final Entity ownerEntity = level() instanceof net.minecraft.server.level.ServerLevel server ? server.getEntity(owner) : null;
        return ownerEntity != null && ownerEntity.distanceToSqr(this) <= 16 * 16;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        entityData.set(OWNER_UUID, Optional.ofNullable(ownerUuid));
    }
}
