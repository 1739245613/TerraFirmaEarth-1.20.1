package com.newterraearth.tfe.common.entity;

import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.dries007.tfc.client.TFCSounds;

import com.newterraearth.tfe.NewTerraEarthMod;

public final class NTEEntitySounds
{
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, NewTerraEarthMod.MOD_ID);

    public static final RegistryObject<SoundEvent> SEAL_AMBIENT = create("entity.seal.ambient");
    public static final RegistryObject<SoundEvent> SEAL_DEATH = create("entity.seal.death");
    public static final RegistryObject<SoundEvent> SEAL_HURT = create("entity.seal.hurt");
    public static final RegistryObject<SoundEvent> SEAL_STEP = create("entity.seal.step");
    public static final RegistryObject<SoundEvent> SEAL_ATTACK = create("entity.seal.attack");
    public static final RegistryObject<SoundEvent> BISON_AMBIENT = create("entity.bison.ambient");
    public static final RegistryObject<SoundEvent> BISON_DEATH = create("entity.bison.death");
    public static final RegistryObject<SoundEvent> BISON_HURT = create("entity.bison.hurt");
    public static final RegistryObject<SoundEvent> BISON_STEP = create("entity.bison.step");
    public static final RegistryObject<SoundEvent> BISON_ATTACK = create("entity.bison.attack");

    public static final TFCSounds.EntitySound SEAL = new TFCSounds.EntitySound(
        SEAL_AMBIENT,
        SEAL_DEATH,
        SEAL_HURT,
        SEAL_STEP,
        Optional.of(SEAL_ATTACK),
        Optional.empty()
    );
    public static final TFCSounds.EntitySound BISON = new TFCSounds.EntitySound(
        BISON_AMBIENT,
        BISON_DEATH,
        BISON_HURT,
        BISON_STEP,
        Optional.of(BISON_ATTACK),
        Optional.empty()
    );

    private NTEEntitySounds()
    {
    }

    public static void register(IEventBus bus)
    {
        SOUNDS.register(bus);
    }

    private static RegistryObject<SoundEvent> create(String name)
    {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(NewTerraEarthMod.MOD_ID, name)));
    }
}
