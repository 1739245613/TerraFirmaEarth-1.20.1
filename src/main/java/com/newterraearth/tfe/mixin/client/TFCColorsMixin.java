package com.newterraearth.tfe.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Final;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import net.dries007.tfc.client.ClientHelpers;
import net.dries007.tfc.client.TFCColors;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;

import com.newterraearth.tfe.client.NTEClimateRenderHelpers;

@Mixin(value = TFCColors.class, remap = false)
public abstract class TFCColorsMixin
{
    @Shadow @Final private static int[] FOLIAGE_COLORS_CACHE;
    @Shadow @Final private static int[] FOLIAGE_FALL_COLORS_CACHE;
    @Shadow @Final private static int[] FOLIAGE_WINTER_COLORS_CACHE;

    /**
     * @author Codex
     * @reason The 1.21 port uses groundwater-aware seasonal color sampling and hemisphere-aware autumn timing.
     */
    @Overwrite(remap = false)
    private static int getSeasonalFoliageColor(BlockPos pos, LevelAccessor level, int autumnIndex)
    {
        if (!(level instanceof Level actualLevel))
        {
            return getClimateColor(FOLIAGE_COLORS_CACHE, pos);
        }

        final float temp = NTEClimateRenderHelpers.getAdjustedAverageTemperature(actualLevel, pos);
        float timeOfYear = Calendars.CLIENT.getCalendarFractionOfYear();
        if (!NTEClimateRenderHelpers.isNorthernHemisphere(actualLevel, pos))
        {
            timeOfYear = (timeOfYear + 0.5f) % 1f;
        }
        final float tempClamped = temp > 12f ? 12f : Math.max(temp, -20f);

        final float cubedTerm = 1.5f * (float) Math.pow(tempClamped + 3f, 3f) / 4913f;
        final float squaredTerm = 0.5f * (float) Math.pow(tempClamped + 3f, 2f) / 289f;
        final float autumnStart = (cubedTerm + squaredTerm + 8.5f) / 12f;
        final float autumnEnd = temp > 12f ? autumnStart : (cubedTerm - squaredTerm + 10.5f) / 12f;
        final float springStart = 1f - autumnEnd;

        if (timeOfYear > autumnEnd)
        {
            return getAverageTempClimateColor(FOLIAGE_WINTER_COLORS_CACHE, pos, temp);
        }
        else if (timeOfYear > autumnStart)
        {
            return nte$getAutumnColor(FOLIAGE_FALL_COLORS_CACHE, timeOfYear, autumnStart, autumnEnd, pos, autumnIndex);
        }
        else if (timeOfYear > springStart)
        {
            return getClimateColor(FOLIAGE_COLORS_CACHE, pos);
        }
        else
        {
            return getAverageTempClimateColor(FOLIAGE_WINTER_COLORS_CACHE, pos, temp);
        }
    }

    /**
     * @author Codex
     * @reason Sample colormaps from the ported 1.21 instant groundwater semantics when available.
     */
    @Overwrite(remap = false)
    private static int getClimateColor(int[] colorCache, BlockPos pos)
    {
        final Level level = ClientHelpers.getLevel();
        if (level != null)
        {
            final float temperature = net.dries007.tfc.util.climate.Climate.getTemperature(level, pos);
            final float groundwater = NTEClimateRenderHelpers.getInstantGroundwater(level, pos);
            return nte$getClimateColor(colorCache, temperature, groundwater);
        }
        return 0;
    }

    /**
     * @author Codex
     * @reason Seasonal winter foliage should use the same groundwater-aware palette lookup as the 1.21 port.
     */
    @Overwrite(remap = false)
    private static int getAverageTempClimateColor(int[] colorCache, BlockPos pos)
    {
        final Level level = ClientHelpers.getLevel();
        if (level != null)
        {
            final float averageTemperature = NTEClimateRenderHelpers.getAdjustedAverageTemperature(level, pos);
            final float groundwater = NTEClimateRenderHelpers.getAverageGroundwater(level, pos);
            return nte$getClimateColor(colorCache, averageTemperature, groundwater);
        }
        return 0;
    }

    /**
     * @author Codex
     * @reason Seasonal winter foliage should use the same groundwater-aware palette lookup as the 1.21 port.
     */
    @Overwrite(remap = false)
    private static int getAverageTempClimateColor(int[] colorCache, BlockPos pos, float averageTemperature)
    {
        final Level level = ClientHelpers.getLevel();
        if (level != null)
        {
            final float groundwater = NTEClimateRenderHelpers.getAverageGroundwater(level, pos);
            return nte$getClimateColor(colorCache, averageTemperature, groundwater);
        }
        return 0;
    }

    @Unique
    private static int nte$getClimateColor(int[] colorCache, float temperature, float groundwater)
    {
        final int temperatureIndex = 255 - Mth.clamp((int) ((temperature + 20f) * 255f / 50f), 0, 255);
        final int groundwaterIndex = 255 - Mth.clamp((int) (groundwater * 255f / 500f), 0, 255);
        return colorCache[temperatureIndex | (groundwaterIndex << 8)];
    }

    @Unique
    private static int nte$getAutumnColor(int[] colorCache, float timeOfYear, float autumnStart, float autumnEnd, BlockPos pos, int autumnIndex)
    {
        final int positionDeltaHash = (Helpers.hash(836494186029734123L, pos) & 127) - 63;
        final int autumnProgressIndex = (int) Mth.clamp(255f * (timeOfYear - autumnStart) / (autumnEnd - autumnStart) + positionDeltaHash, 0, 255);
        return colorCache[autumnProgressIndex | (autumnIndex << 8)];
    }
}
