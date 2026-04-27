package com.newterraearth.tfe.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacementContext;

import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.chunkdata.ForestType;
import net.dries007.tfc.world.placement.ClimatePlacement;

import com.newterraearth.tfe.world.NTE121ClimateHelpers;
import com.newterraearth.tfe.world.NTEClimatePlacementAccess;
import com.newterraearth.tfe.world.forest.NTE121ForestHelpers;
import com.newterraearth.tfe.world.forest.NTEForestType;

@Mixin(ClimatePlacement.class)
public abstract class ClimatePlacementMixin implements NTEClimatePlacementAccess
{
    @Mutable
    @Shadow(remap = false)
    @Final
    public static Codec<ClimatePlacement> PLACEMENT_CODEC;

    @Shadow(remap = false)
    @Final
    private float minTemp;

    @Shadow(remap = false)
    @Final
    private float maxTemp;

    @Shadow(remap = false)
    @Final
    private float targetTemp;

    @Shadow(remap = false)
    @Final
    private float minRainfall;

    @Shadow(remap = false)
    @Final
    private float maxRainfall;

    @Shadow(remap = false)
    @Final
    private float targetRainfall;

    @Shadow(remap = false)
    @Final
    private ForestType minForest;

    @Shadow(remap = false)
    @Final
    private ForestType maxForest;

    @Shadow(remap = false)
    @Final
    private boolean fuzzy;

    @Unique
    private static final Codec<Integer> NTE_FOREST_DENSITY_CODEC = Codec.either(ForestType.CODEC, Codec.INT)
        .xmap(either -> either.map(ForestType::ordinal, i -> i), Either::right);

    @Unique
    private static final Map<ClimatePlacement, ExtraData> NTE_EXTRA = Collections.synchronizedMap(new WeakHashMap<>());

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void nte$replaceCodec(CallbackInfo ci)
    {
        PLACEMENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("min_temperature", -Float.MAX_VALUE).forGetter(c -> ((ClimatePlacementMixin) (Object) c).minTemp),
            Codec.FLOAT.optionalFieldOf("max_temperature", Float.MAX_VALUE).forGetter(c -> ((ClimatePlacementMixin) (Object) c).maxTemp),
            Codec.FLOAT.optionalFieldOf("min_rainfall").forGetter(c -> ((ClimatePlacementMixin) (Object) c).nte$getMinRainfallField()),
            Codec.FLOAT.optionalFieldOf("max_rainfall").forGetter(c -> ((ClimatePlacementMixin) (Object) c).nte$getMaxRainfallField()),
            Codec.FLOAT.optionalFieldOf("min_groundwater").forGetter(c -> ((ClimatePlacementMixin) (Object) c).nte$getMinGroundwaterField()),
            Codec.FLOAT.optionalFieldOf("max_groundwater").forGetter(c -> ((ClimatePlacementMixin) (Object) c).nte$getMaxGroundwaterField()),
            Codec.FLOAT.optionalFieldOf("min_rain_variance").forGetter(c -> ((ClimatePlacementMixin) (Object) c).nte$getMinRainVarianceField()),
            Codec.FLOAT.optionalFieldOf("max_rain_variance").forGetter(c -> ((ClimatePlacementMixin) (Object) c).nte$getMaxRainVarianceField()),
            Codec.BOOL.optionalFieldOf("rain_variance_absolute").forGetter(c -> ((ClimatePlacementMixin) (Object) c).nte$getRainVarianceAbsoluteField()),
            NTE_FOREST_DENSITY_CODEC.optionalFieldOf("min_forest", 0).forGetter(c -> ((ClimatePlacementMixin) (Object) c).nte$getExtra().minForest()),
            NTE_FOREST_DENSITY_CODEC.optionalFieldOf("max_forest", 4).forGetter(c -> ((ClimatePlacementMixin) (Object) c).nte$getExtra().maxForest()),
            NTEForestType.CODEC.listOf().optionalFieldOf("forest_types", Collections.emptyList()).forGetter(c -> ((ClimatePlacementMixin) (Object) c).nte$getExtra().forestTypes()),
            Codec.INT.optionalFieldOf("min_elevation").forGetter(c -> ((ClimatePlacementMixin) (Object) c).nte$getMinElevationField()),
            Codec.INT.optionalFieldOf("max_elevation").forGetter(c -> ((ClimatePlacementMixin) (Object) c).nte$getMaxElevationField()),
            Codec.BOOL.optionalFieldOf("fuzzy", false).forGetter(c -> ((ClimatePlacementMixin) (Object) c).fuzzy),
            Codec.BOOL.optionalFieldOf("ignore_rivers").forGetter(c -> ((ClimatePlacementMixin) (Object) c).nte$getIgnoreRiversField())
        ).apply(instance, ClimatePlacementMixin::nte$decode));
    }

    /**
     * @author Codex
     * @reason `shore`-related 1.21 resources use groundwater-based climate checks that 1.20 does not understand.
     */
    @Overwrite(remap = false)
    public boolean isValid(ChunkData data, BlockPos pos, RandomSource random)
    {
        return nte$isValidInternal(data, pos, random, null, 0L);
    }

    /**
     * @author Codex
     * @reason placed features need exact 1.21 groundwater checks during worldgen, which require access to the chunk generator.
     */
    @Overwrite
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos)
    {
        final ChunkDataProvider provider = ChunkDataProvider.get(context.getLevel());
        final ChunkData data = provider.get(context.getLevel(), pos);
        final ChunkGenerator generator = context.getLevel().getLevel().getChunkSource().getGenerator();
        return nte$isValidInternal(data, pos, random, generator, context.getLevel().getSeed()) ? Stream.of(pos) : Stream.empty();
    }

    @Unique
    private static ClimatePlacement nte$decode(float minTemp, float maxTemp, Optional<Float> minRainfall, Optional<Float> maxRainfall, Optional<Float> minGroundwater, Optional<Float> maxGroundwater, Optional<Float> minRainVariance, Optional<Float> maxRainVariance, Optional<Boolean> rainVarianceAbsolute, int minForest, int maxForest, List<NTEForestType> forestTypes, Optional<Integer> minElevation, Optional<Integer> maxElevation, boolean fuzzy, Optional<Boolean> ignoreRivers)
    {
        final boolean usesGroundwater = minGroundwater.isPresent() || maxGroundwater.isPresent();
        final boolean hasRainVariance = minRainVariance.isPresent() || maxRainVariance.isPresent() || rainVarianceAbsolute.isPresent();
        final boolean hasElevation = minElevation.isPresent() || maxElevation.isPresent();
        final boolean ignoresRivers = ignoreRivers.orElse(false);
        final boolean extended = usesGroundwater || hasRainVariance || hasElevation || !forestTypes.isEmpty() || ignoresRivers;
        final float lowerMoisture = usesGroundwater ? minGroundwater.orElse(-Float.MAX_VALUE) : minRainfall.orElse(-Float.MAX_VALUE);
        final float upperMoisture = usesGroundwater ? maxGroundwater.orElse(Float.MAX_VALUE) : maxRainfall.orElse(Float.MAX_VALUE);
        final ClimatePlacement placement = new ClimatePlacement(minTemp, maxTemp, lowerMoisture, upperMoisture, ForestType.valueOf(minForest), ForestType.valueOf(maxForest), fuzzy);
        NTE_EXTRA.put(placement, new ExtraData(
            extended,
            usesGroundwater,
            minForest,
            maxForest,
            List.copyOf(forestTypes),
            minElevation.orElse(-64),
            maxElevation.orElse(320),
            ignoresRivers,
            minRainVariance.orElse(-1f),
            maxRainVariance.orElse(1f),
            rainVarianceAbsolute.orElse(false)
        ));
        return placement;
    }

    @Unique
    private ExtraData nte$getExtra()
    {
        final ClimatePlacement self = (ClimatePlacement) (Object) this;
        return NTE_EXTRA.getOrDefault(self, new ExtraData(false, false, minForest.ordinal(), maxForest.ordinal(), List.of(), -64, 320, false, -1f, 1f, false));
    }

    @Unique
    private Optional<Float> nte$getMinRainfallField()
    {
        return nte$getExtra().usesGroundwater() ? Optional.empty() : Optional.of(minRainfall);
    }

    @Unique
    private Optional<Float> nte$getMaxRainfallField()
    {
        return nte$getExtra().usesGroundwater() ? Optional.empty() : Optional.of(maxRainfall);
    }

    @Unique
    private Optional<Float> nte$getMinGroundwaterField()
    {
        return nte$getExtra().usesGroundwater() ? Optional.of(minRainfall) : Optional.empty();
    }

    @Unique
    private Optional<Float> nte$getMaxGroundwaterField()
    {
        return nte$getExtra().usesGroundwater() ? Optional.of(maxRainfall) : Optional.empty();
    }

    @Unique
    private Optional<Float> nte$getMinRainVarianceField()
    {
        return nte$getExtra().extended() ? Optional.of(nte$getExtra().minRainVariance()) : Optional.empty();
    }

    @Unique
    private Optional<Float> nte$getMaxRainVarianceField()
    {
        return nte$getExtra().extended() ? Optional.of(nte$getExtra().maxRainVariance()) : Optional.empty();
    }

    @Unique
    private Optional<Boolean> nte$getRainVarianceAbsoluteField()
    {
        return nte$getExtra().extended() ? Optional.of(nte$getExtra().rainVarianceAbsolute()) : Optional.empty();
    }

    @Unique
    private Optional<Integer> nte$getMinElevationField()
    {
        return nte$getExtra().extended() ? Optional.of(nte$getExtra().minElevation()) : Optional.empty();
    }

    @Unique
    private Optional<Integer> nte$getMaxElevationField()
    {
        return nte$getExtra().extended() ? Optional.of(nte$getExtra().maxElevation()) : Optional.empty();
    }

    @Unique
    private Optional<Boolean> nte$getIgnoreRiversField()
    {
        return nte$getExtra().ignoreRivers() ? Optional.of(true) : Optional.empty();
    }

    @Unique
    private boolean nte$isValidInternal(ChunkData data, BlockPos pos, RandomSource random, ChunkGenerator generator, long levelSeed)
    {
        final ExtraData extra = nte$getExtra();
        if (!extra.extended())
        {
            final float temperature = OverworldClimateModel.getAdjustedAverageTempByElevation(pos, data);
            final float rainfall = data.getRainfall(pos);
            final ForestType forestType = data.getForestType();
            if (minTemp <= temperature && temperature <= maxTemp && minRainfall <= rainfall && rainfall <= maxRainfall && minForest.ordinal() <= forestType.ordinal() && forestType.ordinal() <= maxForest.ordinal())
            {
                if (fuzzy)
                {
                    final float normTempDelta = Math.abs(temperature - targetTemp) / (maxTemp - minTemp);
                    final float normRainfallDelta = Math.abs(rainfall - targetRainfall) / (maxRainfall - minRainfall);
                    return random.nextFloat() * random.nextFloat() > Math.max(normTempDelta, normRainfallDelta);
                }
                return true;
            }
            return false;
        }

        final int y = pos.getY();
        if (y < extra.minElevation() || y > extra.maxElevation())
        {
            return false;
        }

        final float temperature = OverworldClimateModel.getAdjustedAverageTempByElevation(pos, data);
        final ForestType legacyForestType = data.getForestType();
        final NTEForestType forestType = generator != null
            ? NTE121ForestHelpers.getForestType(levelSeed, pos)
            : NTE121ForestHelpers.mapLegacyForestType(legacyForestType);
        final int forestDensity = forestType.getDensity();
        if (forestDensity < extra.minForest() || forestDensity > extra.maxForest())
        {
            return false;
        }
        if (!extra.forestTypes().isEmpty() && !extra.forestTypes().contains(forestType))
        {
            return false;
        }

        final float moisture = extra.usesGroundwater()
            ? extra.ignoreRivers()
                ? data.getRainfall(pos)
                : generator != null
                    ? NTE121ClimateHelpers.getAverageGroundwater(generator, pos)
                    : data.getRainfall(pos)
            : data.getRainfall(pos);
        final float minMoisture = minRainfall;
        final float maxMoisture = maxRainfall;
        final float rawRainVariance = generator != null ? NTE121ClimateHelpers.getRainVariance(levelSeed, generator, pos) : 0f;
        final float rainVariance = extra.rainVarianceAbsolute()
            ? Math.abs(rawRainVariance)
            : rawRainVariance * (generator == null || NTE121ClimateHelpers.isNorthernHemisphere(generator, pos.getZ()) ? 1f : -1f);
        if (minTemp <= temperature && temperature <= maxTemp && minMoisture <= moisture && moisture <= maxMoisture && extra.minRainVariance() <= rainVariance && rainVariance <= extra.maxRainVariance())
        {
            if (fuzzy)
            {
                final float targetMoisture = (minMoisture + maxMoisture) / 2f;
                final float normTempDelta = Math.abs(temperature - targetTemp) / (maxTemp - minTemp);
                final float normMoistureDelta = Math.abs(moisture - targetMoisture) / (maxMoisture - minMoisture);
                final float targetRainVariance = (extra.minRainVariance() + extra.maxRainVariance()) / 2f;
                final float normRainVarianceDelta = Math.abs(rainVariance - targetRainVariance) / (extra.maxRainVariance() - extra.minRainVariance());
                return random.nextFloat() * random.nextFloat() > Math.max(normTempDelta, Math.max(normMoistureDelta, normRainVarianceDelta));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean nte$isValid(ChunkData data, BlockPos pos, RandomSource random, ChunkGenerator generator, long levelSeed)
    {
        return nte$isValidInternal(data, pos, random, generator, levelSeed);
    }

    @Unique
    private record ExtraData(boolean extended, boolean usesGroundwater, int minForest, int maxForest, List<NTEForestType> forestTypes, int minElevation, int maxElevation, boolean ignoreRivers, float minRainVariance, float maxRainVariance, boolean rainVarianceAbsolute) {}

    @Override
    public float nte$getMinGroundwater()
    {
        return nte$getExtra().usesGroundwater() ? minRainfall : -Float.MAX_VALUE;
    }

    @Override
    public float nte$getMaxGroundwater()
    {
        return nte$getExtra().usesGroundwater() ? maxRainfall : Float.MAX_VALUE;
    }

    @Override
    public float nte$getMinRainVariance()
    {
        return nte$getExtra().minRainVariance();
    }

    @Override
    public float nte$getMaxRainVariance()
    {
        return nte$getExtra().maxRainVariance();
    }

    @Override
    public boolean nte$isRainVarianceAbsolute()
    {
        return nte$getExtra().rainVarianceAbsolute();
    }

    @Override
    public List<NTEForestType> nte$getForestTypes()
    {
        return nte$getExtra().forestTypes();
    }

    @Override
    public int nte$getMinElevation()
    {
        return nte$getExtra().minElevation();
    }

    @Override
    public int nte$getMaxElevation()
    {
        return nte$getExtra().maxElevation();
    }
}
