package com.newterraearth.tfe.world.placement;

import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import net.dries007.tfc.world.noise.Noise2D;

import com.newterraearth.tfe.world.NTEBiomeNoise;

/** 4.2.9-compatible placement filter for spreading ocean ridges. */
public final class NTEOceanRidgePlacement extends PlacementModifier
{
    public static final Codec<NTEOceanRidgePlacement> PLACEMENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("min_distance", Integer.MIN_VALUE).forGetter(c -> c.minDistance),
        Codec.INT.optionalFieldOf("max_distance", Integer.MAX_VALUE).forGetter(c -> c.maxDistance)
    ).apply(instance, NTEOceanRidgePlacement::new));

    private final int minDistance;
    private final int maxDistance;
    private final ThreadLocal<tfe$LocalContext> localContext = ThreadLocal.withInitial(() -> null);

    public NTEOceanRidgePlacement(int minDistance, int maxDistance)
    {
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos)
    {
        final long seed = context.getLevel().getSeed();
        tfe$LocalContext local = localContext.get();
        if (local == null || local.seed() != seed)
        {
            local = new tfe$LocalContext(seed, NTEBiomeNoise.oceanRidgeDistance(seed));
            localContext.set(local);
        }
        final double distance = local.distance().noise(pos.getX(), pos.getZ());
        return minDistance <= distance && distance <= maxDistance ? Stream.of(pos) : Stream.empty();
    }

    @Override
    public PlacementModifierType<?> type()
    {
        return NTEPlacements.TFC_OCEAN_RIDGE.isPresent() ? NTEPlacements.TFC_OCEAN_RIDGE.get() : NTEPlacements.OCEAN_RIDGE.get();
    }

    private record tfe$LocalContext(long seed, Noise2D distance)
    {
    }
}
