package com.newterraearth.tfe.world.placement;

import java.util.Objects;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
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
import com.newterraearth.tfe.world.noise.NTECellular2D;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoise;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoiseSampler;
import com.newterraearth.tfe.world.volcano.NTEVolcanoVariant;

/** 4.2.9-compatible stratovolcano placement with variant and hash filters. */
public final class NTEStratovolcanoPlacement extends PlacementModifier
{
    public static final Codec<NTEStratovolcanoPlacement> PLACEMENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("center", false).forGetter(c -> c.center),
        Codec.BOOL.optionalFieldOf("use_offset_center", false).forGetter(c -> c.useOffsetCenter),
        Codec.STRING.optionalFieldOf("variant", "all").forGetter(c -> c.variant),
        Codecs.UNIT_FLOAT.optionalFieldOf("min_easing", 0f).forGetter(c -> c.minEasing),
        Codecs.UNIT_FLOAT.optionalFieldOf("max_easing", 1f).forGetter(c -> c.maxEasing),
        Codecs.UNIT_FLOAT.optionalFieldOf("hash_min", 0f).forGetter(c -> c.hashMin),
        Codecs.UNIT_FLOAT.optionalFieldOf("hash_max", 1f).forGetter(c -> c.hashMax)
    ).apply(instance, NTEStratovolcanoPlacement::new));

    private final boolean center;
    private final boolean useOffsetCenter;
    private final String variant;
    private final float minEasing;
    private final float maxEasing;
    private final float hashMin;
    private final float hashMax;
    private final ThreadLocal<tfe$LocalContext> localContext = ThreadLocal.withInitial(() -> null);

    public NTEStratovolcanoPlacement(boolean center, boolean useOffsetCenter, String variant, float minEasing, float maxEasing, float hashMin, float hashMax)
    {
        this.center = center;
        this.useOffsetCenter = useOffsetCenter;
        this.variant = variant;
        this.minEasing = minEasing;
        this.maxEasing = maxEasing;
        this.hashMin = hashMin;
        this.hashMax = hashMax;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos)
    {
        final WorldGenLevel level = context.getLevel();
        final long seed = level.getSeed();
        tfe$LocalContext local = localContext.get();
        if (local == null || local.seed() != seed)
        {
            local = new tfe$LocalContext(seed, NTECenteredFeatureNoise.stratovolcano(NTESeed.of(seed)));
            localContext.set(local);
        }

        final BiomeExtension extension = TFCBiomes.getExtensionOrThrow(level, level.getBiome(pos).value());
        final NTECenteredFeatureNoiseSampler sampler = local.context();
        if (!sampler.isValidBiome(extension))
        {
            return Stream.empty();
        }

        final NTECellular2D.Cell cell = sampler.getCellularNoise().cell(pos.getX(), pos.getZ());
        final NTEVolcanoVariant variantAt = sampler.getVolcanoVariant(cell);
        if (variantAt == null || !(variant.equals("all") || Objects.equals(variantAt.name(), variant)))
        {
            return Stream.empty();
        }

        if (useOffsetCenter)
        {
            if (Objects.equals(variantAt.name(), "crater_lake"))
            {
                return craterLakePositions(level, pos, cell, sampler);
            }
            if (Objects.equals(variantAt.name(), "kelimutu"))
            {
                return kelimutuPositions(level, pos, cell, sampler);
            }
        }

        final double hash = NTECenteredFeatureNoise.hashDouble(cell.noise(), 3199);
        if (hash < hashMin || hash > hashMax)
        {
            return Stream.empty();
        }
        if (center)
        {
            final BlockPos centerPos = sampler.calculateCenter(pos, extension);
            return isValidCenter(level, pos, centerPos, sampler) ? Stream.of(centerPos) : Stream.empty();
        }
        final float easing = sampler.calculateEasing(pos, extension);
        return easing > minEasing && easing < maxEasing ? Stream.of(pos) : Stream.empty();
    }

    private Stream<BlockPos> craterLakePositions(WorldGenLevel level, BlockPos pos, NTECellular2D.Cell cell, NTECenteredFeatureNoiseSampler sampler)
    {
        final double centerX = cell.x() - 45 + 90 * NTECenteredFeatureNoise.hashDouble(cell.noise(), 6);
        final double centerZ = cell.y() - 45 + 90 * NTECenteredFeatureNoise.hashDouble(cell.noise(), 7);
        final double hash = NTECenteredFeatureNoise.hashDouble(cell.noise(), 3200);
        if (hash < hashMin || hash > hashMax)
        {
            return Stream.empty();
        }
        if (center)
        {
            final BlockPos centerPos = new BlockPos((int) centerX, pos.getY(), (int) centerZ);
            return isValidCenter(level, pos, centerPos, sampler) ? Stream.of(centerPos) : Stream.empty();
        }
        final double distance = squaredDistance(pos, centerX, centerZ);
        return Mth.clampedMap(distance, 0, 900, 1, 0) > minEasing ? Stream.of(pos) : Stream.empty();
    }

    private Stream<BlockPos> kelimutuPositions(WorldGenLevel level, BlockPos pos, NTECellular2D.Cell cell, NTECenteredFeatureNoiseSampler sampler)
    {
        final double noise = cell.noise();
        final double center0X = cell.x();
        final double center0Z = cell.y();
        double centerX = center0X;
        double centerZ = center0Z;
        double distance = squaredDistance(pos, centerX, centerZ);
        double hash = NTECenteredFeatureNoise.hashDouble(noise, 3199);

        final double offsetX1 = 2 * (0.5 - NTECenteredFeatureNoise.hashDouble(noise, 68));
        final double offsetZ1 = 2 * (0.5 - NTECenteredFeatureNoise.hashDouble(noise, 69));
        final double center1X = center0X + signedOffset(offsetX1, 70);
        final double center1Z = center0Z + signedOffset(offsetZ1, 70);
        final double distance1 = squaredDistance(pos, center1X, center1Z);
        if (distance1 < distance)
        {
            distance = distance1;
            centerX = center1X;
            centerZ = center1Z;
            hash = NTECenteredFeatureNoise.hashDouble(noise, 3201);
        }

        if (NTECenteredFeatureNoise.hashDouble(noise, 1066) > 0.3)
        {
            final double offsetX2 = 2 * (0.5 - NTECenteredFeatureNoise.hashDouble(noise, 71));
            final double offsetZ2 = 2 * (0.5 - NTECenteredFeatureNoise.hashDouble(noise, 72));
            final double center2X = center0X + signedOffset(offsetX2, offsetX2 > 0 ? 70 : 73);
            final double center2Z = center0Z + signedOffset(offsetZ2, offsetZ2 > 0 ? 70 : 74);
            final double distance2 = squaredDistance(pos, center2X, center2Z);
            if (distance2 < distance)
            {
                distance = distance2;
                centerX = center2X;
                centerZ = center2Z;
                hash = NTECenteredFeatureNoise.hashDouble(noise, 3202);
            }
        }

        if (hash < hashMin || hash > hashMax)
        {
            return Stream.empty();
        }
        if (center)
        {
            final BlockPos centerPos = new BlockPos((int) centerX, pos.getY(), (int) centerZ);
            return isValidCenter(level, pos, centerPos, sampler) ? Stream.of(centerPos) : Stream.empty();
        }
        return Mth.clampedMap(distance, 0, 2500, 1, 0) > minEasing ? Stream.of(pos) : Stream.empty();
    }

    private static double signedOffset(double value, double multiplier)
    {
        return value > 0 ? 20 + value * multiplier : -20 + value * multiplier;
    }

    private static double squaredDistance(BlockPos pos, double x, double z)
    {
        return (pos.getX() - x) * (pos.getX() - x) + (pos.getZ() - z) * (pos.getZ() - z);
    }

    private static boolean isValidCenter(WorldGenLevel level, BlockPos origin, BlockPos center, NTECenteredFeatureNoiseSampler sampler)
    {
        return center != null
            && SectionPos.blockToSectionCoord(center.getX()) == SectionPos.blockToSectionCoord(origin.getX())
            && SectionPos.blockToSectionCoord(center.getZ()) == SectionPos.blockToSectionCoord(origin.getZ())
            && sampler.isValidBiome(TFCBiomes.getExtensionOrThrow(level, level.getBiome(center).value()));
    }

    @Override
    public PlacementModifierType<?> type()
    {
        return NTEPlacements.TFC_STRATOVOLCANO.isPresent() ? NTEPlacements.TFC_STRATOVOLCANO.get() : NTEPlacements.STRATOVOLCANO.get();
    }

    private record tfe$LocalContext(long seed, NTECenteredFeatureNoiseSampler context)
    {
    }
}
