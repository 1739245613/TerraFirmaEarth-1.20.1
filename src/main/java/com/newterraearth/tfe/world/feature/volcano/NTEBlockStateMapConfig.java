package com.newterraearth.tfe.world.feature.volcano;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.world.Codecs;

public record NTEBlockStateMapConfig(Map<Block, BlockState> states) implements FeatureConfiguration
{
    public static final Codec<NTEBlockStateMapConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codecs.mapListCodec(Codecs.recordPairCodec(
            Codecs.BLOCK, "replace",
            Codecs.BLOCK_STATE, "with"
        )).fieldOf("states").forGetter(NTEBlockStateMapConfig::states)
    ).apply(instance, NTEBlockStateMapConfig::new));

    @Nullable
    public BlockState getState(BlockState stateIn)
    {
        return states.get(stateIn.getBlock());
    }
}
