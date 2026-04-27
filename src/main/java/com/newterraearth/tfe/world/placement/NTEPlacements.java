package com.newterraearth.tfe.world.placement;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import com.newterraearth.tfe.NewTerraEarthMod;

public final class NTEPlacements
{
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, NewTerraEarthMod.MOD_ID);
    public static final DeferredRegister<PlacementModifierType<?>> TFC_PLACEMENT_ALIASES = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, "tfc");

    public static final RegistryObject<PlacementModifierType<IntertidalPlacement>> INTERTIDAL = register("intertidal", () -> IntertidalPlacement.PLACEMENT_CODEC);
    public static final RegistryObject<PlacementModifierType<TuyaPlacement>> TUYA = register("tuya", () -> TuyaPlacement.PLACEMENT_CODEC);
    public static final RegistryObject<PlacementModifierType<IntertidalPlacement>> TFC_INTERTIDAL = register(TFC_PLACEMENT_ALIASES, "intertidal", () -> IntertidalPlacement.PLACEMENT_CODEC);
    public static final RegistryObject<PlacementModifierType<TuyaPlacement>> TFC_TUYA = register(TFC_PLACEMENT_ALIASES, "tuya", () -> TuyaPlacement.PLACEMENT_CODEC);

    private NTEPlacements()
    {
    }

    public static void register(IEventBus bus)
    {
        PLACEMENT_MODIFIERS.register(bus);
        TFC_PLACEMENT_ALIASES.register(bus);
    }

    private static <C extends PlacementModifier> RegistryObject<PlacementModifierType<C>> register(String name, PlacementModifierType<C> codec)
    {
        return register(PLACEMENT_MODIFIERS, name, codec);
    }

    private static <C extends PlacementModifier> RegistryObject<PlacementModifierType<C>> register(DeferredRegister<PlacementModifierType<?>> register, String name, PlacementModifierType<C> codec)
    {
        return register.register(name, () -> codec);
    }
}
