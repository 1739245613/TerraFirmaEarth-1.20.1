package com.newterraearth.tfe.world.feature;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import net.dries007.tfc.world.feature.BlockConfig;

import com.newterraearth.tfe.NewTerraEarthMod;
import com.newterraearth.tfe.common.block.plant.NTERotatableWaterPlantBlock;
import com.newterraearth.tfe.world.feature.plant.NTEBambooConfig;
import com.newterraearth.tfe.world.feature.plant.NTEBambooFeature;
import com.newterraearth.tfe.world.feature.plant.NTECreepingOceanPlantFeature;
import com.newterraearth.tfe.world.feature.plant.NTECreepingPlantConfig;
import com.newterraearth.tfe.world.feature.plant.NTERotatableWaterPlantFeature;
import com.newterraearth.tfe.world.feature.tree.NTEForestConfig;
import com.newterraearth.tfe.world.feature.tree.NTEForestFeature;
import com.newterraearth.tfe.world.feature.volcano.NTEBlockStateMapConfig;
import com.newterraearth.tfe.world.feature.volcano.NTEMapRivuletFeature;

public final class NTEFeatures
{
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, NewTerraEarthMod.MOD_ID);
    public static final DeferredRegister<Feature<?>> TFC_FEATURE_ALIASES = DeferredRegister.create(Registries.FEATURE, "tfc");

    public static final RegistryObject<SeaStacksFeature> SEA_STACKS = register("sea_stacks", SeaStacksFeature::new, NoneFeatureConfiguration.CODEC);
    public static final RegistryObject<NTECreepingOceanPlantFeature> CREEPING_OCEAN_PLANT = register("creeping_ocean_plant", NTECreepingOceanPlantFeature::new, NTECreepingPlantConfig.CODEC);
    public static final RegistryObject<NTERotatableWaterPlantFeature> ROTATABLE_WATER_PLANT = register("rotatable_water_plant", NTERotatableWaterPlantFeature::new, NTERotatableWaterPlantFeature.CODEC);
    public static final RegistryObject<NTEBambooFeature> BAMBOO = register("bamboo", NTEBambooFeature::new, NTEBambooConfig.CODEC);
    public static final RegistryObject<NTEForestFeature> FOREST_121 = register("forest_121", NTEForestFeature::new, NTEForestConfig.CODEC);
    public static final RegistryObject<NTEForestFeature.Entry> FOREST_ENTRY_121 = register("forest_entry_121", NTEForestFeature.Entry::new, NTEForestConfig.Entry.CODEC);
    public static final RegistryObject<NTEMapRivuletFeature> MAPPED_RIVULET = register("mapped_rivulet", NTEMapRivuletFeature::new, NTEBlockStateMapConfig.CODEC);
    public static final RegistryObject<SeaStacksFeature> TFC_SEA_STACKS = register(TFC_FEATURE_ALIASES, "sea_stacks", SeaStacksFeature::new, NoneFeatureConfiguration.CODEC);
    public static final RegistryObject<NTECreepingOceanPlantFeature> TFC_CREEPING_OCEAN_PLANT = register(TFC_FEATURE_ALIASES, "creeping_ocean_plant", NTECreepingOceanPlantFeature::new, NTECreepingPlantConfig.CODEC);
    public static final RegistryObject<NTERotatableWaterPlantFeature> TFC_ROTATABLE_WATER_PLANT = register(TFC_FEATURE_ALIASES, "rotatable_water_plant", NTERotatableWaterPlantFeature::new, NTERotatableWaterPlantFeature.CODEC);
    public static final RegistryObject<NTEBambooFeature> TFC_BAMBOO = register(TFC_FEATURE_ALIASES, "bamboo", NTEBambooFeature::new, NTEBambooConfig.CODEC);

    private NTEFeatures()
    {
    }

    public static void register(IEventBus bus)
    {
        FEATURES.register(bus);
        TFC_FEATURE_ALIASES.register(bus);
    }

    private static <C extends FeatureConfiguration, F extends Feature<C>> RegistryObject<F> register(String name, Function<Codec<C>, F> factory, Codec<C> codec)
    {
        return register(FEATURES, name, factory, codec);
    }

    private static <C extends FeatureConfiguration, F extends Feature<C>> RegistryObject<F> register(DeferredRegister<Feature<?>> register, String name, Function<Codec<C>, F> factory, Codec<C> codec)
    {
        return register.register(name, () -> factory.apply(codec));
    }
}
