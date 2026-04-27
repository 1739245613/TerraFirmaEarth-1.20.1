package com.newterraearth.tfe.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.server.ServerLifecycleHooks;

import net.dries007.tfc.client.ClientHelpers;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.calendar.Month;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.OverworldClimateModel;

import com.newterraearth.tfe.world.NTEChunkDataHelpers;
import com.newterraearth.tfe.world.NTE121ClimateHelpers;

public final class NTEClimateRenderHelpers
{
    private static final float DEFAULT_HEMISPHERE_SCALE = 20_000f;

    @Nullable private static Field temperatureScaleField;
    @Nullable private static Field climateSeedField;
    private static boolean lookedUpClimateFields;
    @Nullable private static volatile Level cachedGeneratorLevel;
    @Nullable private static volatile ChunkGenerator cachedChunkGenerator;
    @Nullable private static volatile Class<?> cachedChunkSourceClass;
    @Nullable private static volatile Method cachedChunkGeneratorMethod;

    private NTEClimateRenderHelpers()
    {
    }

    public static float getAdjustedAverageTemperature(Level level, BlockPos pos)
    {
        return OverworldClimateModel.getAdjustedAverageTempByElevation(pos, NTEChunkDataHelpers.get(level, pos));
    }

    public static float getAverageGroundwater(Level level, BlockPos pos)
    {
        return Mth.clamp(getBaseGroundwater(level, pos) + Climate.getRainfall(level, pos), 0f, 500f);
    }

    public static float getBaseGroundwater(Level level, BlockPos pos)
    {
        final ChunkGenerator generator = getChunkGenerator(level);
        if (generator != null)
        {
            final float groundwater = NTE121ClimateHelpers.getBaseGroundwater(generator, pos);
            if (groundwater != Float.NEGATIVE_INFINITY)
            {
                return Mth.clamp(groundwater, 0f, 500f);
            }
        }
        return 0f;
    }

    public static float getInstantGroundwater(Level level, BlockPos pos)
    {
        return Mth.clamp(getBaseGroundwater(level, pos) + getInstantRainfall(level, pos), 0f, 500f);
    }

    public static float getInstantRainfall(Level level, BlockPos pos)
    {
        final float averageRainfall = Climate.getRainfall(level, pos);
        final float syncedRainVariance = NTEClientRainVarianceCache.getRainVariance(pos);
        if (!Float.isNaN(syncedRainVariance))
        {
            return syncedRainVariance == 0f
                ? averageRainfall
                : Helpers.triangle(syncedRainVariance * averageRainfall, averageRainfall, 1f, Calendars.get(level).getCalendarFractionOfYear() + 0.75f);
        }

        final ChunkGenerator generator = getChunkGenerator(level);
        if (generator == null)
        {
            return averageRainfall;
        }

        final float rainVariance = NTE121ClimateHelpers.getRainVariance(getClimateSeed(level), generator, pos);
        if (rainVariance == 0f)
        {
            return averageRainfall;
        }
        return Helpers.triangle(rainVariance * averageRainfall, averageRainfall, 1f, Calendars.get(level).getCalendarFractionOfYear() + 0.75f);
    }

    public static float getRainVarianceOrNaN(Level level, BlockPos pos)
    {
        final float syncedRainVariance = NTEClientRainVarianceCache.getRainVariance(pos);
        if (!Float.isNaN(syncedRainVariance))
        {
            return syncedRainVariance;
        }

        final ChunkGenerator generator = getChunkGenerator(level);
        return generator != null ? NTE121ClimateHelpers.getRainVariance(getClimateSeed(level), generator, pos) : Float.NaN;
    }

    public static boolean isNorthernHemisphere(Level level, BlockPos pos)
    {
        return NTE121ClimateHelpers.isNorthernHemisphere(pos.getZ(), getHemisphereScale(level));
    }

    public static Month getHemispheralCalendarMonthOfYear(Level level, BlockPos pos)
    {
        final ICalendar calendar = Calendars.get(level);
        long calendarTicks = calendar.getCalendarTicks();
        if (!isNorthernHemisphere(level, pos))
        {
            calendarTicks += calendar.getCalendarTicksInYear() / 2L;
        }
        return ICalendar.getMonthOfYear(calendarTicks, calendar.getCalendarDaysInMonth());
    }

    public static Month getClientHemispheralCalendarMonthOfYear()
    {
        final Level level = ClientHelpers.getLevel();
        final Player player = ClientHelpers.getPlayer();
        return level != null && player != null
            ? getHemispheralCalendarMonthOfYear(level, player.blockPosition())
            : Calendars.CLIENT.getCalendarMonthOfYear();
    }

    public static float getHemisphereScale(@Nullable Level level)
    {
        final OverworldClimateModel model = getClimateModel(level);
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

    private static long getClimateSeed(Level level)
    {
        final OverworldClimateModel model = getClimateModel(level);
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

    @Nullable
    private static ChunkGenerator getChunkGenerator(Level level)
    {
        if (cachedGeneratorLevel != level)
        {
            synchronized (NTEClimateRenderHelpers.class)
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

    @Nullable
    private static ChunkGenerator resolveChunkGenerator(Level level)
    {
        final ChunkGenerator localGenerator = resolveChunkGeneratorFromSource(level.getChunkSource());
        if (localGenerator != null)
        {
            return localGenerator;
        }

        final ServerLevel serverLevel = getIntegratedServerLevel(level);
        return serverLevel != null ? resolveChunkGeneratorFromSource(serverLevel.getChunkSource()) : null;
    }

    @Nullable
    private static ChunkGenerator resolveChunkGeneratorFromSource(@Nullable Object chunkSource)
    {
        try
        {
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

    @Nullable
    private static OverworldClimateModel getClimateModel(@Nullable Level level)
    {
        if (level == null)
        {
            return null;
        }

        final OverworldClimateModel localModel = OverworldClimateModel.getIfPresent(level);
        if (localModel != null)
        {
            return localModel;
        }

        final ServerLevel serverLevel = getIntegratedServerLevel(level);
        return serverLevel != null ? OverworldClimateModel.getIfPresent(serverLevel) : null;
    }

    @Nullable
    private static ServerLevel getIntegratedServerLevel(Level level)
    {
        if (!level.isClientSide)
        {
            return null;
        }

        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.getLevel(level.dimension()) : null;
    }
}
