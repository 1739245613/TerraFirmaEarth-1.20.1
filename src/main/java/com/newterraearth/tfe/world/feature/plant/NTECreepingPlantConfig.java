package com.newterraearth.tfe.world.feature.plant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import net.dries007.tfc.world.Codecs;

public record NTECreepingPlantConfig(Block block, int radius, int height, int heightAboveTide, float integrity) implements FeatureConfiguration
{
    public static final Codec<NTECreepingPlantConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.BLOCK.fieldOf("block").forGetter(NTECreepingPlantConfig::block),
            Codec.INT.fieldOf("radius").forGetter(NTECreepingPlantConfig::radius),
            Codec.INT.fieldOf("height").forGetter(NTECreepingPlantConfig::height),
            Codec.INT.optionalFieldOf("tide_height", 0).forGetter(NTECreepingPlantConfig::heightAboveTide),
            Codec.FLOAT.optionalFieldOf("integrity", 1f).forGetter(NTECreepingPlantConfig::integrity)
        ).apply(instance, NTECreepingPlantConfig::new)
    );
}
