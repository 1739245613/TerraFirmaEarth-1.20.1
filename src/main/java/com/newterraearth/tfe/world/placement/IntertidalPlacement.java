package com.newterraearth.tfe.world.placement;

import java.util.stream.Stream;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;

public class IntertidalPlacement extends PlacementModifier
{
    public static final Codec<IntertidalPlacement> PLACEMENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("min_elevation", -64).forGetter(c -> c.minElevation),
        Codec.INT.optionalFieldOf("max_elevation", 320).forGetter(c -> c.maxElevation)
    ).apply(instance, IntertidalPlacement::new));

    private final int minElevation;
    private final int maxElevation;

    public IntertidalPlacement(int minElevation, int maxElevation)
    {
        this.minElevation = minElevation;
        this.maxElevation = maxElevation;
    }

    @Override
    public PlacementModifierType<?> type()
    {
        return NTEPlacements.TFC_INTERTIDAL.isPresent() ? NTEPlacements.TFC_INTERTIDAL.get() : NTEPlacements.INTERTIDAL.get();
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos)
    {
        final Noise2D tideNoise = highTideNoise(context.getLevel().getSeed());
        final int heightDiff = (int) (pos.getY() - tideNoise.noise(pos.getX(), pos.getZ()));
        return minElevation <= heightDiff && heightDiff <= maxElevation ? Stream.of(pos) : Stream.empty();
    }

    private static Noise2D highTideNoise(long seed)
    {
        return new OpenSimplex2D(seed)
            .octaves(3)
            .spread(0.005f)
            .scaled(TFCChunkGenerator.SEA_LEVEL_Y - 6, TFCChunkGenerator.SEA_LEVEL_Y + 6)
            .clamped(TFCChunkGenerator.SEA_LEVEL_Y, TFCChunkGenerator.SEA_LEVEL_Y + 4)
            .add(new OpenSimplex2D(seed).spread(0.03));
    }
}
