package com.newterraearth.tfe.world.feature.tree;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import net.dries007.tfc.util.collections.IWeighted;
import net.dries007.tfc.world.Codecs;
import net.dries007.tfc.world.placement.ClimatePlacement;

import com.newterraearth.tfe.world.NTEClimatePlacementAccess;

public record NTEForestConfig(HolderSet<ConfiguredFeature<?, ?>> entries) implements FeatureConfiguration
{
    public static final Codec<NTEForestConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ExtraCodecs.nonEmptyHolderSet(ConfiguredFeature.LIST_CODEC).fieldOf("entries").forGetter(NTEForestConfig::entries)
    ).apply(instance, NTEForestConfig::new));

    public record Entry(ClimatePlacement climate, Optional<BlockState> bushLog, Optional<BlockState> bushLeaves, Optional<BlockState> fallenLog, Optional<BlockState> fallenLeaves, Optional<IWeighted<BlockState>> groundcover, Holder<ConfiguredFeature<?, ?>> treeFeature, Holder<ConfiguredFeature<?, ?>> deadFeature, Optional<Holder<ConfiguredFeature<?, ?>>> oldGrowthFeature, Optional<Holder<ConfiguredFeature<?, ?>>> krummholz, Optional<Holder<ConfiguredFeature<?, ?>>> soilDiscFeature, Optional<Holder<ConfiguredFeature<?, ?>>> transitionSoilDiscFeature, int oldGrowthChance, int spoilerOldGrowthChance, int fallenChance, int deadChance, boolean floating) implements FeatureConfiguration
    {
        private record SoilDiscConfig(Optional<Holder<ConfiguredFeature<?, ?>>> soilDiscFeature, Optional<Holder<ConfiguredFeature<?, ?>>> transitionSoilDiscFeature)
        {
            private static final MapCodec<SoilDiscConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codecs.optionalFieldOf(ConfiguredFeature.CODEC, "soil_disc").forGetter(SoilDiscConfig::soilDiscFeature),
                Codecs.optionalFieldOf(ConfiguredFeature.CODEC, "transition_soil_disc").forGetter(SoilDiscConfig::transitionSoilDiscFeature)
            ).apply(instance, SoilDiscConfig::new));
        }

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ClimatePlacement.PLACEMENT_CODEC.fieldOf("climate").forGetter(Entry::climate),
            Codecs.optionalFieldOf(Codecs.BLOCK_STATE, "bush_log").forGetter(Entry::bushLog),
            Codecs.optionalFieldOf(Codecs.BLOCK_STATE, "bush_leaves").forGetter(Entry::bushLeaves),
            Codecs.optionalFieldOf(Codecs.BLOCK_STATE, "fallen_log").forGetter(Entry::fallenLog),
            Codecs.optionalFieldOf(Codecs.BLOCK_STATE, "fallen_leaves").forGetter(Entry::fallenLeaves),
            Codecs.optionalFieldOf(Codecs.weightedCodec(Codecs.BLOCK_STATE, "block"), "groundcover").forGetter(Entry::groundcover),
            ConfiguredFeature.CODEC.fieldOf("normal_tree").forGetter(Entry::treeFeature),
            ConfiguredFeature.CODEC.fieldOf("dead_tree").forGetter(Entry::deadFeature),
            Codecs.optionalFieldOf(ConfiguredFeature.CODEC, "old_growth_tree").forGetter(Entry::oldGrowthFeature),
            Codecs.optionalFieldOf(ConfiguredFeature.CODEC, "krummholz").forGetter(Entry::krummholz),
            SoilDiscConfig.CODEC.forGetter(entry -> new SoilDiscConfig(entry.soilDiscFeature(), entry.transitionSoilDiscFeature())),
            Codecs.optionalFieldOf(Codec.INT, "old_growth_chance", 6).forGetter(Entry::oldGrowthChance),
            Codecs.optionalFieldOf(Codec.INT, "spoiler_old_growth_chance", 200).forGetter(Entry::spoilerOldGrowthChance),
            Codecs.optionalFieldOf(Codec.INT, "fallen_tree_chance", 14).forGetter(Entry::fallenChance),
            Codecs.optionalFieldOf(Codec.INT, "dead_chance", 75).forGetter(Entry::deadChance),
            Codecs.optionalFieldOf(Codec.BOOL, "floating", false).forGetter(Entry::floating)
        ).apply(instance, (climate, bushLog, bushLeaves, fallenLog, fallenLeaves, groundcover, treeFeature, deadFeature, oldGrowthFeature, krummholz, soilDiscs, oldGrowthChance, spoilerOldGrowthChance, fallenChance, deadChance, floating) ->
            new Entry(climate, bushLog, bushLeaves, fallenLog, fallenLeaves, groundcover, treeFeature, deadFeature, oldGrowthFeature, krummholz, soilDiscs.soilDiscFeature(), soilDiscs.transitionSoilDiscFeature(), oldGrowthChance, spoilerOldGrowthChance, fallenChance, deadChance, floating)
        ));

        public boolean isValid(float temperature, float groundwater, float rainVar, float elevation)
        {
            final NTEClimatePlacementAccess access = access();
            final float adjustedRainVar = access.nte$isRainVarianceAbsolute() ? Math.abs(rainVar) : rainVar;
            return groundwater >= access.nte$getMinGroundwater() && groundwater <= access.nte$getMaxGroundwater()
                && adjustedRainVar >= access.nte$getMinRainVariance() && adjustedRainVar <= access.nte$getMaxRainVariance()
                && temperature >= climate.getMinTemp() && temperature <= climate.getMaxTemp()
                && elevation >= access.nte$getMinElevation() && elevation <= access.nte$getMaxElevation();
        }

        public float distanceFromMean(float temperature, float groundwater, float rainVar, float elevation)
        {
            final NTEClimatePlacementAccess access = access();
            final float adjustedRainVar = access.nte$isRainVarianceAbsolute() ? Math.abs(rainVar) : rainVar;
            final float tempDist = (temperature - getAverageTemp()) * 10f;
            final float waterDist = groundwater - getAverageGroundwater();
            final float rainVarDist = (adjustedRainVar - getAverageRainVar()) * 250f;
            final float elevationDist = (elevation - getAverageElevation()) * 5f;
            return tempDist + waterDist + rainVarDist + elevationDist;
        }

        public float getAverageTemp()
        {
            return (climate.getMaxTemp() - climate.getMinTemp()) / 2f;
        }

        public ClimatePlacement getClimatePlacement()
        {
            return climate;
        }

        public float getAverageGroundwater()
        {
            final NTEClimatePlacementAccess access = access();
            return (access.nte$getMaxGroundwater() - access.nte$getMinGroundwater()) / 2f;
        }

        public float getAverageRainVar()
        {
            final NTEClimatePlacementAccess access = access();
            return (access.nte$getMaxRainVariance() - access.nte$getMinRainVariance()) / 2f;
        }

        public float getAverageElevation()
        {
            final NTEClimatePlacementAccess access = access();
            return (access.nte$getMaxElevation() - access.nte$getMinElevation()) / 2f;
        }

        public ConfiguredFeature<?, ?> getFeature()
        {
            return treeFeature.value();
        }

        public ConfiguredFeature<?, ?> getDeadFeature()
        {
            return deadFeature.value();
        }

        public ConfiguredFeature<?, ?> getOldGrowthFeature()
        {
            return oldGrowthFeature.orElse(treeFeature).value();
        }

        private NTEClimatePlacementAccess access()
        {
            return (NTEClimatePlacementAccess) climate;
        }
    }
}
