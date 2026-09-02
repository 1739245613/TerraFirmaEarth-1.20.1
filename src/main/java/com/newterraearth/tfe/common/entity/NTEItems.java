package com.newterraearth.tfe.common.entity;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.newterraearth.tfe.NewTerraEarthMod;
import com.newterraearth.tfe.common.item.NTEProvidedItem;
import com.newterraearth.tfe.common.item.NTERopeItem;

public final class NTEItems
{
    private static final String TFC_NAMESPACE = "tfc";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TFC_NAMESPACE);

    public static final RegistryObject<Item> BISON = registerFood("food/bison", true);
    public static final RegistryObject<Item> COOKED_BISON = registerFood("food/cooked_bison", true);
    public static final RegistryObject<Item> ARMADILLO = registerFood("food/armadillo", true);
    public static final RegistryObject<Item> COOKED_ARMADILLO = registerFood("food/cooked_armadillo", true);
    public static final RegistryObject<Item> ARMADILLO_SCUTE = register("armadillo_scute");
    public static final RegistryObject<Item> ROPE = ITEMS.register("rope", () -> new NTERopeItem(new Item.Properties()));

    public static final Map<NTEFish, RegistryObject<Item>> NEW_FRESHWATER_FISH = new EnumMap<>(NTEFish.class);
    public static final Map<NTEFish, RegistryObject<Item>> NEW_COOKED_FRESHWATER_FISH = new EnumMap<>(NTEFish.class);
    public static final Map<NTEFish, RegistryObject<MobBucketItem>> NEW_FRESHWATER_FISH_BUCKETS = new EnumMap<>(NTEFish.class);
    public static final Map<NTEFish, RegistryObject<Item>> NEW_FRESHWATER_FISH_SPAWN_EGGS = new EnumMap<>(NTEFish.class);

    static
    {
        for (NTEFish fish : NTEFish.values())
        {
            final String name = fish.getSerializedName();
            NEW_FRESHWATER_FISH.put(fish, registerFood("food/" + name, true));
            NEW_COOKED_FRESHWATER_FISH.put(fish, registerFood("food/cooked_" + name, true));
            NEW_FRESHWATER_FISH_BUCKETS.put(fish, ITEMS.register("bucket/" + name, () ->
                new MobBucketItem(() -> NTEEntities.NEW_FRESHWATER_FISH.get(fish).get(), () -> Fluids.WATER,
                    () -> SoundEvents.BUCKET_EMPTY_FISH, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1))));
            NEW_FRESHWATER_FISH_SPAWN_EGGS.put(fish, ITEMS.register("spawn_egg/" + name, () ->
                new NTEProvidedSpawnEggItem(NTEEntities.NEW_FRESHWATER_FISH.get(fish), fish.getEggColor1(), fish.getEggColor2(), new Item.Properties())));
        }
    }
    public static final RegistryObject<Item> LEOPARD_SEAL_SPAWN_EGG = ITEMS.register("spawn_egg/leopard_seal", () ->
        new NTEProvidedSpawnEggItem(NTEEntities.LEOPARD_SEAL, 0xFFFFFF, 0xFFFFFF, new Item.Properties())
    );
    public static final RegistryObject<Item> BISON_SPAWN_EGG = ITEMS.register("spawn_egg/bison", () ->
        new NTEProvidedSpawnEggItem(NTEEntities.BISON, 0x7A5736, 0x3B2615, new Item.Properties())
    );
    public static final RegistryObject<Item> LEMMING_SPAWN_EGG = ITEMS.register("spawn_egg/lemming", () ->
        new NTEProvidedSpawnEggItem(NTEEntities.LEMMING, 0xB6A28B, 0x6C5848, new Item.Properties())
    );
    public static final RegistryObject<Item> MONGOOSE_SPAWN_EGG = ITEMS.register("spawn_egg/mongoose", () ->
        new NTEProvidedSpawnEggItem(NTEEntities.MONGOOSE, 0x94704B, 0xD3B38D, new Item.Properties())
    );
    public static final RegistryObject<Item> JERBOA_SPAWN_EGG = ITEMS.register("spawn_egg/jerboa", () ->
        new NTEProvidedSpawnEggItem(NTEEntities.JERBOA, 0xD3B07B, 0xF4E2C3, new Item.Properties())
    );
    public static final RegistryObject<Item> DROMEDARY_CAMEL_SPAWN_EGG = ITEMS.register("spawn_egg/dromedary_camel", () ->
        new NTEProvidedSpawnEggItem(NTEEntities.DROMEDARY_CAMEL, 0xB37D4A, 0xE2C39B, new Item.Properties())
    );
    public static final RegistryObject<Item> BACTRIAN_CAMEL_SPAWN_EGG = ITEMS.register("spawn_egg/bactrian_camel", () ->
        new NTEProvidedSpawnEggItem(NTEEntities.BACTRIAN_CAMEL, 0x6E4E36, 0xC5A477, new Item.Properties())
    );
    public static final RegistryObject<Item> ARMADILLO_SPAWN_EGG = ITEMS.register("spawn_egg/armadillo", () ->
        new NTEProvidedSpawnEggItem(NTEEntities.ARMADILLO, 0x8A6546, 0xD0B58C, new Item.Properties())
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
        return ITEMS.register(name, () -> new NTEProvidedItem(new Item.Properties().food(builder.build())));
    }

    private static RegistryObject<Item> register(String name)
    {
        return ITEMS.register(name, () -> new NTEProvidedItem(new Item.Properties()));
    }

    private static final class NTEProvidedSpawnEggItem extends ForgeSpawnEggItem
    {
        private NTEProvidedSpawnEggItem(java.util.function.Supplier<? extends EntityType<? extends Mob>> type, int backgroundColor, int highlightColor, Properties properties)
        {
            super(type, backgroundColor, highlightColor, properties);
        }

        @Override
        public String getCreatorModId(ItemStack itemStack)
        {
            return NewTerraEarthMod.MOD_ID;
        }
    }
}
