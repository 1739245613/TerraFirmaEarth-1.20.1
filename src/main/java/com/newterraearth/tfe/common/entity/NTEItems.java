package com.newterraearth.tfe.common.entity;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class NTEItems
{
    private static final String TFC_NAMESPACE = "tfc";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TFC_NAMESPACE);

    public static final RegistryObject<Item> BISON = registerFood("food/bison", true);
    public static final RegistryObject<Item> COOKED_BISON = registerFood("food/cooked_bison", true);
    public static final RegistryObject<Item> LEOPARD_SEAL_SPAWN_EGG = ITEMS.register("spawn_egg/leopard_seal", () ->
        new ForgeSpawnEggItem(NTEEntities.LEOPARD_SEAL, 0xFFFFFF, 0xFFFFFF, new Item.Properties())
    );
    public static final RegistryObject<Item> BISON_SPAWN_EGG = ITEMS.register("spawn_egg/bison", () ->
        new ForgeSpawnEggItem(NTEEntities.BISON, 0x7A5736, 0x3B2615, new Item.Properties())
    );
    public static final RegistryObject<Item> LEMMING_SPAWN_EGG = ITEMS.register("spawn_egg/lemming", () ->
        new ForgeSpawnEggItem(NTEEntities.LEMMING, 0xB6A28B, 0x6C5848, new Item.Properties())
    );
    public static final RegistryObject<Item> MONGOOSE_SPAWN_EGG = ITEMS.register("spawn_egg/mongoose", () ->
        new ForgeSpawnEggItem(NTEEntities.MONGOOSE, 0x94704B, 0xD3B38D, new Item.Properties())
    );
    public static final RegistryObject<Item> JERBOA_SPAWN_EGG = ITEMS.register("spawn_egg/jerboa", () ->
        new ForgeSpawnEggItem(NTEEntities.JERBOA, 0xD3B07B, 0xF4E2C3, new Item.Properties())
    );

    private NTEItems()
    {
    }

    public static void register(IEventBus bus)
    {
        ITEMS.register(bus);
    }

    private static RegistryObject<Item> registerFood(String name, boolean meat)
    {
        final FoodProperties.Builder builder = new FoodProperties.Builder().nutrition(4).saturationMod(0.3f);
        if (meat)
        {
            builder.meat();
        }
        return ITEMS.register(name, () -> new Item(new Item.Properties().food(builder.build())));
    }
}
