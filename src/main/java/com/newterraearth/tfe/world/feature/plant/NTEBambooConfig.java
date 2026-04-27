package com.newterraearth.tfe.world.feature.plant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import net.dries007.tfc.world.Codecs;

public record NTEBambooConfig(float probability, BlockState state) implements FeatureConfiguration
{
    public static final Codec<NTEBambooConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(NTEBambooConfig::probability),
            Codecs.BLOCK_STATE.fieldOf("state").forGetter(NTEBambooConfig::state)
        ).apply(instance, NTEBambooConfig::new)
    );
}
