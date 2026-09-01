package com.newterraearth.tfe.world.placement;

import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import net.dries007.tfc.world.Codecs;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.TFCBiomes;

import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoise;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoiseSampler;

public final class NTEAtollPlacement extends PlacementModifier
{
    public static final Codec<NTEAtollPlacement> PLACEMENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("center", false).forGetter(c -> c.center),
        Codecs.UNIT_FLOAT.optionalFieldOf("min_easing", 0f).forGetter(c -> c.minEasing),
        Codecs.UNIT_FLOAT.optionalFieldOf("max_easing", 1f).forGetter(c -> c.maxEasing)
    ).apply(instance, NTEAtollPlacement::new));

    private final boolean center;
    private final float minEasing;
    private final float maxEasing;
    private final ThreadLocal<tfe$LocalContext> localContext = ThreadLocal.withInitial(() -> null);

    public NTEAtollPlacement(boolean center, float minEasing, float maxEasing)
    {
        this.center = center;
        this.minEasing = minEasing;
        this.maxEasing = maxEasing;
    }

    @Override
    public PlacementModifierType<?> type()
    {
        return NTEPlacements.TFC_ATOLL.isPresent() ? NTEPlacements.TFC_ATOLL.get() : NTEPlacements.ATOLL.get();
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos)
    {
        final WorldGenLevel level = context.getLevel();
        final long seed = level.getSeed();
        tfe$LocalContext local = localContext.get();
        if (local == null || local.seed() != seed)
        {
            local = new tfe$LocalContext(seed, NTECenteredFeatureNoise.atoll(NTESeed.of(seed)));
            localContext.set(local);
        }

        final Biome biome = level.getBiome(pos).value();
        final BiomeExtension extension = TFCBiomes.getExtensionOrThrow(level, biome);
        final NTECenteredFeatureNoiseSampler sampler = local.context();
        if (!sampler.isValidBiome(extension))
        {
            return Stream.empty();
        }

        if (center)
        {
            final BlockPos centerPos = sampler.calculateCenter(pos, extension);
            if (centerPos != null
                && SectionPos.blockToSectionCoord(centerPos.getX()) == SectionPos.blockToSectionCoord(pos.getX())
                && SectionPos.blockToSectionCoord(centerPos.getZ()) == SectionPos.blockToSectionCoord(pos.getZ())
                && sampler.isValidBiome(TFCBiomes.getExtensionOrThrow(level, level.getBiome(centerPos).value())))
            {
                return Stream.of(centerPos);
            }
            return Stream.empty();
        }

        final float easing = sampler.calculateEasing(pos, extension);
        return easing > minEasing && easing < maxEasing ? Stream.of(pos) : Stream.empty();
    }

    private record tfe$LocalContext(long seed, NTECenteredFeatureNoiseSampler context)
    {
    }
}
