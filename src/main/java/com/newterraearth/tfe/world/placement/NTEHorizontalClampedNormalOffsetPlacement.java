package com.newterraearth.tfe.world.placement;

import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ClampedNormalInt;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public final class NTEHorizontalClampedNormalOffsetPlacement extends PlacementModifier
{
    public static final Codec<NTEHorizontalClampedNormalOffsetPlacement> PLACEMENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.FLOAT.fieldOf("mean").forGetter(c -> c.mean),
        Codec.FLOAT.fieldOf("deviation").forGetter(c -> c.deviation),
        Codec.INT.fieldOf("min_inclusive").forGetter(c -> c.minInclusive),
        Codec.INT.fieldOf("max_inclusive").forGetter(c -> c.maxInclusive)
    ).apply(instance, NTEHorizontalClampedNormalOffsetPlacement::new));

    private final float mean;
    private final float deviation;
    private final int minInclusive;
    private final int maxInclusive;

    public NTEHorizontalClampedNormalOffsetPlacement(float mean, float deviation, int minInclusive, int maxInclusive)
    {
        this.mean = mean;
        this.deviation = deviation;
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos)
    {
        final int x = pos.getX() + sample(random);
        final int z = pos.getZ() + sample(random);
        return Stream.of(new BlockPos(x, pos.getY(), z));
    }

    @Override
    public PlacementModifierType<?> type()
    {
        return NTEPlacements.HORIZONTAL_CLAMPED_NORMAL_OFFSET.get();
    }

    private int sample(RandomSource random)
    {
        return ClampedNormalInt.sample(random, mean, deviation, minInclusive, maxInclusive);
    }
}
