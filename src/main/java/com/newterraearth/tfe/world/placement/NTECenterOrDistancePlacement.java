package com.newterraearth.tfe.world.placement;

import java.util.function.BiFunction;
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

import net.dries007.tfc.world.Codecs;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.TFCBiomes;

import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoiseSampler;

public abstract class NTECenterOrDistancePlacement<T extends NTECenteredFeatureNoiseSampler> extends PlacementModifier
{
    public static <E extends NTECenterOrDistancePlacement<?>> Codec<E> codec(BiFunction<Boolean, Float, E> factory)
    {
        return RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("center", false).forGetter(c -> c.center),
            Codecs.UNIT_FLOAT.optionalFieldOf("distance", 0f).forGetter(c -> c.distance)
        ).apply(instance, factory));
    }

    protected final boolean center;
    protected final float distance;

    private final ThreadLocal<tfe$LocalContext<T>> localContext;

    protected NTECenterOrDistancePlacement(boolean center, float distance)
    {
        this.center = center;
        this.distance = distance;
        this.localContext = ThreadLocal.withInitial(() -> null);
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos)
    {
        final WorldGenLevel level = context.getLevel();
        final long seed = level.getSeed();

        tfe$LocalContext<T> local = localContext.get();
        if (local == null || local.seed() != seed)
        {
            local = new tfe$LocalContext<>(seed, createContext(NTESeed.of(seed)));
            localContext.set(local);
        }

        final Biome biome = level.getBiome(pos).value();
        final BiomeExtension extension = TFCBiomes.getExtensionOrThrow(level, biome);
        if (!local.context().isValidBiome(extension))
        {
            return Stream.empty();
        }

        if (center)
        {
            final BlockPos centerPos = local.context().calculateCenter(pos, extension);
            if (centerPos != null
                && SectionPos.blockToSectionCoord(centerPos.getX()) == SectionPos.blockToSectionCoord(pos.getX())
                && SectionPos.blockToSectionCoord(centerPos.getZ()) == SectionPos.blockToSectionCoord(pos.getZ())
                && local.context().isValidBiome(TFCBiomes.getExtensionOrThrow(level, level.getBiome(centerPos).value())))
            {
                return Stream.of(centerPos);
            }
            return Stream.empty();
        }

        return local.context().calculateEasing(pos, extension) > distance ? Stream.of(pos) : Stream.empty();
    }

    protected abstract T createContext(NTESeed seed);

    private record tfe$LocalContext<T>(long seed, T context)
    {
    }
}
