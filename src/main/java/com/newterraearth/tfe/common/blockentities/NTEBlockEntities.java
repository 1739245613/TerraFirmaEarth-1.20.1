package com.newterraearth.tfe.common.blockentities;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import net.dries007.tfc.util.registry.RegistrationHelpers;

import com.newterraearth.tfe.NewTerraEarthMod;
import com.newterraearth.tfe.common.NTEBlocks;

public final class NTEBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NewTerraEarthMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<NTEFarmlandBlockEntity>> FARMLAND = RegistrationHelpers.register(BLOCK_ENTITIES, "farmland", NTEFarmlandBlockEntity::new, NTEBlocks.farmlandBlocks());
    public static final RegistryObject<BlockEntityType<NTECropBlockEntity>> CROP = RegistrationHelpers.register(BLOCK_ENTITIES, "crop", NTECropBlockEntity::new, NTEBlocks.cropBlocks());
    public static final RegistryObject<BlockEntityType<NTETickCounterBlockEntity>> TICK_COUNTER = RegistrationHelpers.register(BLOCK_ENTITIES, "tick_counter", NTETickCounterBlockEntity::new, NTEBlocks.dryingBrickBlocks());

    private NTEBlockEntities()
    {
    }

    public static void register(IEventBus bus)
    {
        BLOCK_ENTITIES.register(bus);
    }
}
