package com.newterraearth.tfe.world;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.calendar.Month;
import net.dries007.tfc.util.calendar.Season;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateModel;
import net.dries007.tfc.util.climate.ClimateRange;
import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.dries007.tfc.world.chunkdata.ChunkData;

import com.newterraearth.tfe.client.NTEClientRainVarianceCache;
import com.newterraearth.tfe.compat.NTEFirmalifeGreenhouseCompat;
import com.newterraearth.tfe.config.NTECommonConfig;

public final class NTESeasonalHelpers
{
    private static final float DEFAULT_HEMISPHERE_SCALE = 20_000f;
    private static final int MAX_RAIN_HYDRATION = 60;
    private static final long CLIMATE_SEED_SALT = 719_283_741_234L;

    @Nullable private static Field temperatureScaleField;
    @Nullable private static Field climateSeedField;
    private static boolean lookedUpClimateFields;

    @Nullable private static volatile Level cachedGeneratorLevel;
    @Nullable private static volatile ChunkGenerator cachedChunkGenerator;
    @Nullable private static volatile Class<?> cachedChunkSourceClass;
    @Nullable private static volatile Method cachedChunkGeneratorMethod;

    private NTESeasonalHelpers()
    {
    }

    public static boolean canPlantSpread(Level level, RandomSource random, BlockPos pos)
    {
        return random.nextFloat() < TFCConfig.SERVER.plantSpreadChance.get()
            && getHemispheralCalendarMonthOfYear(level, pos).getSeason() != Season.WINTER;
    }

    public static Month getHemispheralCalendarMonthOfYear(Level level, BlockPos pos)
    {
        return getHemispheralCalendarMonthOfYear(level, pos, Calendars.get(level).getCalendarTicks());
    }

    public static Month getHemispheralCalendarMonthOfYear(Level level, BlockPos pos, long calendarTicks)
    {
        final ICalendar calendar = Calendars.get(level);
        long adjustedTicks = calendarTicks;
        if (!isNorthernHemisphere(level, pos))
        {
            adjustedTicks += calendar.getCalendarTicksInYear() / 2L;
        }
        return ICalendar.getMonthOfYear(adjustedTicks, calendar.getCalendarDaysInMonth());
    }

    public static boolean isNorthernHemisphere(Level level, BlockPos pos)
    {
        return NTE121ClimateHelpers.isNorthernHemisphere(pos.getZ(), getHemisphereScale(level));
    }

    public static float getInstantRainfall(Level level, BlockPos pos)
    {
        final ICalendar calendar = Calendars.get(level);
        return getInstantRainfall(level, pos, calendar.getCalendarTicks(), calendar.getCalendarDaysInMonth());
    }

    public static float getInstantRainfall(Level level, BlockPos pos, long calendarTicks, int daysInMonth)
    {
        final float averageRainfall = Climate.getRainfall(level, pos);
        final float rainVariance = getRainVariance(level, pos);
        if (Float.isNaN(rainVariance) || rainVariance == 0f)
        {
            return averageRainfall;
        }

        return Helpers.triangle(rainVariance * averageRainfall, averageRainfall, 1f, getCalendarFractionOfYear(level, calendarTicks, daysInMonth) + 0.75f);
    }

    public static int getFruitBushHydrationFromRootPos(Level level, BlockPos rootPos)
    {
        if (useAverageHydrationInControlledGreenhouse(level, rootPos))
        {
            return FarmlandBlock.getHydration(level, rootPos);
        }
        return NTECommonConfig.useCurrentRainfallForFruit()
            ? getCurrentHydration(level, rootPos)
            : FarmlandBlock.getHydration(level, rootPos);
    }

    public static int getFruitTreeSaplingHydration(Level level, BlockPos rootPos)
    {
        if (useAverageHydrationInControlledGreenhouse(level, rootPos))
        {
            return FarmlandBlock.getHydration(level, rootPos);
        }
        if (!NTECommonConfig.useCurrentRainfallForFruit())
        {
            return FarmlandBlock.getHydration(level, rootPos);
        }
        return getCurrentHydration(level, rootPos);
    }

    public static int getConfiguredCropHydration(Level level, BlockPos rootPos)
    {
        if (useAverageHydrationInControlledGreenhouse(level, rootPos))
        {
            return FarmlandBlock.getHydration(level, rootPos);
        }
        if (!NTECommonConfig.useCurrentRainfallForCrops())
        {
            return FarmlandBlock.getHydration(level, rootPos);
        }
        return getCurrentHydration(level, rootPos);
    }

    public static int getConfiguredCropHydration(Level level, BlockPos rootPos, long calendarTicks, int daysInMonth)
    {
        if (useAverageHydrationInControlledGreenhouse(level, rootPos))
        {
            return FarmlandBlock.getHydration(level, rootPos);
        }
        if (!NTECommonConfig.useCurrentRainfallForCrops())
        {
            return FarmlandBlock.getHydration(level, rootPos);
        }
        return getCurrentHydration(level, rootPos, calendarTicks, daysInMonth);
    }

    public static int getInstantRainHydration(float rainfall)
    {
        return (int) Mth.clampedMap(rainfall, ClimateModel.MINIMUM_RAINFALL, ClimateModel.MAXIMUM_RAINFALL, 0f, MAX_RAIN_HYDRATION);
    }

    public static int getAverageRainHydration(LevelAccessor level, BlockPos pos)
    {
        return getInstantRainHydration(ChunkData.get(level, pos).getRainfall(pos));
    }

    public static int getInstantHydrationFromRainHydration(Level level, BlockPos pos, int rainHydration)
    {
        if (Helpers.isFluid(level.getFluidState(pos.above()), TFCTags.Fluids.HYDRATING))
        {
            return 100;
        }
        final int waterBoost = 20 * (5 - findMinCostHydratingSource(level, pos));
        return Mth.clamp(waterBoost + rainHydration, 0, 100);
    }

    public static int getCurrentHydration(Level level, BlockPos pos)
    {
        final ICalendar calendar = Calendars.get(level);
        return getCurrentHydration(level, pos, calendar.getCalendarTicks(), calendar.getCalendarDaysInMonth());
    }

    public static int getCurrentHydration(Level level, BlockPos pos, long calendarTicks, int daysInMonth)
    {
        final float rainfall = getInstantRainfall(level, pos, calendarTicks, daysInMonth);
        return getInstantHydrationFromRainHydration(level, pos, getInstantRainHydration(rainfall));
    }

    public static void addAverageHydrationTooltipIfNeeded(List<Component> text, LevelAccessor level, BlockPos pos, ClimateRange validRange, boolean allowWiggle, boolean showAverage)
    {
        if (showAverage && !useAverageHydrationInControlledGreenhouse(level, pos))
        {
            text.add(getAverageHydrationTooltip(level, pos, validRange, allowWiggle));
        }
    }

    public static BlockPos getFruitTreeStemPos(LevelReader level, BlockPos pos)
    {
        if (Helpers.isBlock(level.getBlockState(pos), TFCTags.Blocks.FRUIT_TREE_BRANCH))
        {
            return findFruitTreeBase(level, pos);
        }
        if (Helpers.isBlock(level.getBlockState(pos.below()), TFCTags.Blocks.FRUIT_TREE_BRANCH))
        {
            return findFruitTreeBase(level, pos.below());
        }

        for (Direction direction : Helpers.DIRECTIONS)
        {
            final BlockPos adjacentPos = pos.relative(direction);
            if (Helpers.isBlock(level.getBlockState(adjacentPos), TFCTags.Blocks.FRUIT_TREE_BRANCH))
            {
                return findFruitTreeBase(level, adjacentPos);
            }
        }
        return pos;
    }

    public static BlockPos getFruitTreeRootPos(LevelReader level, BlockPos pos)
    {
        return getFruitTreeStemPos(level, pos).below();
    }

    public static BlockPos getFruitTreeSaplingStemPos(LevelReader level, BlockPos pos)
    {
        final BlockPos belowPos = pos.below();
        return Helpers.isBlock(level.getBlockState(belowPos), TFCTags.Blocks.FRUIT_TREE_BRANCH) ? findFruitTreeBase(level, belowPos) : pos;
    }

    public static BlockPos getBananaRootPos(LevelReader level, BlockPos pos)
    {
        final BlockState state = level.getBlockState(pos);
        if (state.isAir())
        {
            return pos;
        }

        final BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < 16; i++)
        {
            final BlockPos belowPos = cursor.below();
            if (level.getBlockState(belowPos).getBlock() != state.getBlock())
            {
                break;
            }
            cursor.move(Direction.DOWN);
        }
        return cursor.immutable();
    }

    public static float getHemisphereScale(@Nullable Level level)
    {
        if (level == null)
        {
            return DEFAULT_HEMISPHERE_SCALE;
        }

        if (level instanceof ServerLevel serverLevel)
        {
            final ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
            if (generator instanceof net.dries007.tfc.world.ChunkGeneratorExtension extension)
            {
                return extension.settings().temperatureScale();
            }
        }

        final OverworldClimateModel model = OverworldClimateModel.getIfPresent(level);
        if (model == null)
        {
            return DEFAULT_HEMISPHERE_SCALE;
        }

        ensureClimateFields();
        if (temperatureScaleField != null)
        {
            try
            {
                return temperatureScaleField.getFloat(model);
            }
            catch (IllegalAccessException ignored)
            {
            }
        }
        return DEFAULT_HEMISPHERE_SCALE;
    }

    @Nullable
    public static ChunkGenerator getChunkGenerator(Level level)
    {
        if (cachedGeneratorLevel != level)
        {
            synchronized (NTESeasonalHelpers.class)
            {
                if (cachedGeneratorLevel != level)
                {
                    cachedGeneratorLevel = level;
                    cachedChunkGenerator = resolveChunkGenerator(level);
                }
            }
        }
        return cachedChunkGenerator;
    }

    private static float getCalendarFractionOfYear(Level level, long calendarTicks, int daysInMonth)
    {
        final ICalendar calendar = Calendars.get(level);
        final long ticksInYear = calendar.getCalendarTicksInYear();
        if (ticksInYear <= 0L)
        {
            return 0f;
        }

        final long normalizedTicks = Math.floorMod(calendarTicks, ticksInYear);
        final int resolvedDaysInMonth = daysInMonth > 0 ? daysInMonth : calendar.getCalendarDaysInMonth();
        final long resolvedTicksInYear = (long) ICalendar.TICKS_IN_DAY * resolvedDaysInMonth * 12L;
        return normalizedTicks / (float) (resolvedTicksInYear > 0L ? resolvedTicksInYear : ticksInYear);
    }

    private static Component getAverageHydrationTooltip(LevelAccessor level, BlockPos pos, ClimateRange validRange, boolean allowWiggle)
    {
        return getNamedHydrationTooltip(validRange, allowWiggle, getAverageRainHydration(level, pos), "tfc.tooltip.farmland.average_hydration");
    }

    public static boolean useAverageHydrationInControlledGreenhouse(LevelAccessor level, BlockPos pos)
    {
        return level instanceof Level world && (
            NTEFirmalifeGreenhouseCompat.isControlledGreenhouse(world, pos)
                || NTEFirmalifeGreenhouseCompat.isControlledGreenhouse(world, pos.above())
        );
    }

    private static Component getNamedHydrationTooltip(ClimateRange validRange, boolean allowWiggle, int hydration, String translationKey)
    {
        final MutableComponent tooltip = Component.translatable(translationKey, hydration);
        tooltip.append(switch (validRange.checkHydration(hydration, allowWiggle))
            {
                case VALID -> Component.translatable("tfc.tooltip.farmland.just_right");
                case LOW -> Component.translatable("tfc.tooltip.farmland.hydration_too_low", validRange.getMinHydration(allowWiggle));
                case HIGH -> Component.translatable("tfc.tooltip.farmland.hydration_too_high", validRange.getMaxHydration(allowWiggle));
            });
        return tooltip;
    }

    private static BlockPos findFruitTreeBase(LevelReader level, BlockPos startPos)
    {
        final BlockPos.MutableBlockPos cursor = startPos.mutable();
        for (int i = 0; i < 32; i++)
        {
            final BlockPos belowPos = cursor.below();
            if (!Helpers.isBlock(level.getBlockState(belowPos), TFCTags.Blocks.FRUIT_TREE_BRANCH))
            {
                break;
            }
            cursor.move(Direction.DOWN);
        }

        final BlockState state = level.getBlockState(cursor);
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            final var property = PipeBlock.PROPERTY_BY_DIRECTION.get(direction);
            if (state.hasProperty(property) && state.getValue(property))
            {
                final BlockPos sidePos = cursor.relative(direction);
                if (Helpers.isBlock(level.getBlockState(sidePos), TFCTags.Blocks.FRUIT_TREE_BRANCH))
                {
                    final BlockPos.MutableBlockPos sideCursor = sidePos.mutable();
                    for (int i = 0; i < 32; i++)
                    {
                        final BlockPos belowPos = sideCursor.below();
                        if (!Helpers.isBlock(level.getBlockState(belowPos), TFCTags.Blocks.FRUIT_TREE_BRANCH))
                        {
                            break;
                        }
                        sideCursor.move(Direction.DOWN);
                    }
                    if (sideCursor.getY() < cursor.getY())
                    {
                        return sideCursor.immutable();
                    }
                }
            }
        }
        return cursor.immutable();
    }

    private static long getClimateSeed(Level level)
    {
        if (level instanceof ServerLevel serverLevel)
        {
            return LinearCongruentialGenerator.next(serverLevel.getSeed(), CLIMATE_SEED_SALT);
        }

        final OverworldClimateModel model = OverworldClimateModel.getIfPresent(level);
        if (model == null)
        {
            return 0L;
        }

        ensureClimateFields();
        if (climateSeedField != null)
        {
            try
            {
                return climateSeedField.getLong(model);
            }
            catch (IllegalAccessException ignored)
            {
            }
        }
        return 0L;
    }

    private static float getRainVariance(Level level, BlockPos pos)
    {
        if (level.isClientSide)
        {
            final float syncedRainVariance = NTEClientRainVarianceCache.getRainVariance(pos);
            if (!Float.isNaN(syncedRainVariance))
            {
                return syncedRainVariance;
            }
        }

        final ChunkGenerator generator = getChunkGenerator(level);
        return generator != null ? NTE121ClimateHelpers.getRainVariance(getClimateSeed(level), generator, pos) : Float.NaN;
    }

    private static void ensureClimateFields()
    {
        if (lookedUpClimateFields)
        {
            return;
        }

        lookedUpClimateFields = true;
        try
        {
            temperatureScaleField = OverworldClimateModel.class.getDeclaredField("temperatureScale");
            temperatureScaleField.setAccessible(true);
        }
        catch (ReflectiveOperationException ignored)
        {
            temperatureScaleField = null;
        }

        try
        {
            climateSeedField = OverworldClimateModel.class.getDeclaredField("climateSeed");
            climateSeedField.setAccessible(true);
        }
        catch (ReflectiveOperationException ignored)
        {
            climateSeedField = null;
        }
    }

    private static int findMinCostHydratingSource(LevelAccessor level, BlockPos pos)
    {
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int minCostWater = 5;
        for (int dx = -4; dx <= 4; dx++)
        {
            for (int dz = -4; dz <= 4; dz++)
            {
                for (int dy = -1; dy <= 0; dy++)
                {
                    final int cost = Math.max(Math.abs(dx), Math.abs(dz)) + (-2 * dy);
                    if (cost < minCostWater && Helpers.isFluid(level.getFluidState(cursor.setWithOffset(pos, dx, dy, dz)).getType(), TFCTags.Fluids.HYDRATING))
                    {
                        minCostWater = cost;
                        if (minCostWater == 1)
                        {
                            return 1;
                        }
                    }
                }
            }
        }
        return minCostWater;
    }

    @Nullable
    private static ChunkGenerator resolveChunkGenerator(Level level)
    {
        if (level instanceof ServerLevel serverLevel)
        {
            return serverLevel.getChunkSource().getGenerator();
        }

        try
        {
            final Object chunkSource = level.getChunkSource();
            if (chunkSource == null)
            {
                return null;
            }

            final Class<?> chunkSourceClass = chunkSource.getClass();
            Method method = cachedChunkGeneratorMethod;
            if (cachedChunkSourceClass != chunkSourceClass || method == null)
            {
                method = chunkSourceClass.getMethod("getGenerator");
                method.setAccessible(true);
                cachedChunkSourceClass = chunkSourceClass;
                cachedChunkGeneratorMethod = method;
            }

            final Object result = method.invoke(chunkSource);
            return result instanceof ChunkGenerator generator ? generator : null;
        }
        catch (ReflectiveOperationException | RuntimeException ignored)
        {
            return null;
        }
    }
}
