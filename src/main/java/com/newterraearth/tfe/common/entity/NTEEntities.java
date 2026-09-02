package com.newterraearth.tfe.common.entity;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.newterraearth.tfe.common.entity.aquatic.NTELeopardSeal;
import com.newterraearth.tfe.common.entity.misc.NTERopeKnot;

import net.dries007.tfc.client.TFCSounds;
import net.dries007.tfc.common.entities.aquatic.FreshwaterFish;
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

    public static final RegistryObject<EntityType<NTEBactrianCamel>> BACTRIAN_CAMEL = ENTITIES.register("bactrian_camel", () ->
        EntityType.Builder.<NTEBactrianCamel>of(NTEBactrianCamel::new, MobCategory.CREATURE)
            .sized(1.7F, 2.375F).clientTrackingRange(10).build(TFC_NAMESPACE + ":bactrian_camel")
    );
    public static final RegistryObject<EntityType<NTEDromedaryCamel>> DROMEDARY_CAMEL = ENTITIES.register("dromedary_camel", () ->
        EntityType.Builder.<NTEDromedaryCamel>of(NTEDromedaryCamel::new, MobCategory.CREATURE)
            .sized(1.7F, 2.375F).clientTrackingRange(10).build(TFC_NAMESPACE + ":dromedary_camel")
    );
    public static final RegistryObject<EntityType<NTERopeKnot>> ROPE_KNOT = ENTITIES.register("rope_knot", () ->
        EntityType.Builder.<NTERopeKnot>of(NTERopeKnot::new, MobCategory.MISC)
            .noSave().sized(0.375F, 0.5F).clientTrackingRange(10).updateInterval(Integer.MAX_VALUE)
            .build(TFC_NAMESPACE + ":rope_knot")
    );
    public static final RegistryObject<EntityType<NTEArmadillo>> ARMADILLO = ENTITIES.register("armadillo", () ->
        EntityType.Builder.<NTEArmadillo>of(NTEArmadillo::new, MobCategory.CREATURE)
            .sized(0.7F, 0.65F).clientTrackingRange(8).build(TFC_NAMESPACE + ":armadillo")
    );

    /** The 4.2.x freshwater fish absent from the 1.20 TFC dependency. */
    public static final Map<NTEFish, RegistryObject<EntityType<FreshwaterFish>>> NEW_FRESHWATER_FISH = new EnumMap<>(NTEFish.class);

    static
    {
        for (NTEFish fish : NTEFish.values())
        {
            NEW_FRESHWATER_FISH.put(fish, ENTITIES.register(fish.getSerializedName(), () ->
                EntityType.Builder.<FreshwaterFish>of((type, level) -> new FreshwaterFish(type, level,
                    NTEEntitySounds.NEW_FRESHWATER_FISHES.get(fish),
                    () -> NTEItems.NEW_FRESHWATER_FISH_BUCKETS.get(fish).get()), MobCategory.WATER_AMBIENT)
                    .sized(fish.getWidth(), fish.getHeight())
                    .clientTrackingRange(4)
                    .build(TFC_NAMESPACE + ":" + fish.getSerializedName())
            ));
        }
    }

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
        event.put(BACTRIAN_CAMEL.get(), NTECamel.createAttributes().build());
        event.put(DROMEDARY_CAMEL.get(), NTEDromedaryCamel.createAttributes().build());
        event.put(ARMADILLO.get(), NTEArmadillo.createAttributes().build());
        for (RegistryObject<EntityType<FreshwaterFish>> fish : NEW_FRESHWATER_FISH.values())
        {
            event.put(fish.get(), AbstractFish.createAttributes().build());
        }
    }
}
