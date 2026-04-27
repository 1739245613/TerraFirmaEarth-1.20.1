package com.newterraearth.tfe.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.newterraearth.tfe.common.entity.aquatic.NTELeopardSeal;

import net.dries007.tfc.client.TFCSounds;
import net.dries007.tfc.common.entities.prey.Pest;
import net.dries007.tfc.common.entities.prey.RammingPrey;

public final class NTEEntities
{
    private static final String TFC_NAMESPACE = "tfc";

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TFC_NAMESPACE);

    public static final RegistryObject<EntityType<NTELeopardSeal>> LEOPARD_SEAL = ENTITIES.register("leopard_seal", () ->
        EntityType.Builder.of(NTELeopardSeal::new, MobCategory.CREATURE)
            .sized(1.2F, 0.7F)
            .clientTrackingRange(10)
            .build(TFC_NAMESPACE + ":leopard_seal")
    );
    public static final RegistryObject<EntityType<RammingPrey>> BISON = ENTITIES.register("bison", () ->
        EntityType.Builder.<RammingPrey>of((type, level) -> new RammingPrey(type, level, NTEEntitySounds.BISON, 0.75d), MobCategory.CREATURE)
            .sized(1.5F, 2.0F)
            .clientTrackingRange(10)
            .build(TFC_NAMESPACE + ":bison")
    );
    public static final RegistryObject<EntityType<Pest>> LEMMING = ENTITIES.register("lemming", () ->
        EntityType.Builder.<Pest>of((type, level) -> new Pest(type, level, TFCSounds.RAT), MobCategory.CREATURE)
            .sized(0.4F, 0.3F)
            .clientTrackingRange(8)
            .build(TFC_NAMESPACE + ":lemming")
    );
    public static final RegistryObject<EntityType<Pest>> MONGOOSE = ENTITIES.register("mongoose", () ->
        EntityType.Builder.<Pest>of((type, level) -> new Pest(type, level, TFCSounds.RAT), MobCategory.CREATURE)
            .sized(0.6F, 0.4F)
            .clientTrackingRange(8)
            .build(TFC_NAMESPACE + ":mongoose")
    );
    public static final RegistryObject<EntityType<Pest>> JERBOA = ENTITIES.register("jerboa", () ->
        EntityType.Builder.<Pest>of((type, level) -> new Pest(type, level, TFCSounds.RAT), MobCategory.CREATURE)
            .sized(0.4F, 0.4F)
            .clientTrackingRange(8)
            .build(TFC_NAMESPACE + ":jerboa")
    );

    private NTEEntities()
    {
    }

    public static void register(IEventBus bus)
    {
        ENTITIES.register(bus);
    }

    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event)
    {
        event.put(LEOPARD_SEAL.get(), NTELeopardSeal.createAttributes().build());
        event.put(BISON.get(), RammingPrey.createLargeAttributes().build());
        event.put(LEMMING.get(), Pest.createAttributes().build());
        event.put(MONGOOSE.get(), Pest.createAttributes().build());
        event.put(JERBOA.get(), Pest.createAttributes().build());
    }
}
