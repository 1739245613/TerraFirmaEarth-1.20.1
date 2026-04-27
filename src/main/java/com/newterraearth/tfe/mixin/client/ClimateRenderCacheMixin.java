package com.newterraearth.tfe.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

import net.dries007.tfc.client.ClientHelpers;
import net.dries007.tfc.client.ClimateRenderCache;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.tracker.WorldTracker;
import net.dries007.tfc.world.TFCChunkGenerator;

import com.newterraearth.tfe.client.NTEClimateRenderCacheBridge;
import com.newterraearth.tfe.client.NTEClimateRenderHelpers;

@Mixin(value = ClimateRenderCache.class, remap = false)
public abstract class ClimateRenderCacheMixin implements NTEClimateRenderCacheBridge
{
    @Shadow private long ticks;
    @Shadow private float averageTemperature;
    @Shadow private float temperature;
    @Shadow private float rainfall;
    @Shadow private Vec2 wind;
    @Shadow private float lastRainLevel;
    @Shadow private float currRainLevel;

    @Unique private float nte$hemisphereScale = 20_000f;
    @Unique private float nte$averageSeaLevelTemperature;
    @Unique private float nte$averageRainfall;
    @Unique private float nte$rainVariance;
    @Unique private float nte$instantRainfall;
    @Unique private float nte$baseGroundwater;
    @Unique private float nte$averageGroundwater;
    @Unique private float nte$instantGroundwater;

    /**
     * @author Codex
     * @reason Bridge the 1.21 client cache fields onto the 1.20 climate model and keep legacy getters compatible.
     */
    @Overwrite(remap = false)
    public void onClientTick()
    {
        final Level level = ClientHelpers.getLevel();
        final Player player = ClientHelpers.getPlayer();
        if (level != null && player != null)
        {
            final BlockPos pos = player.blockPosition();
            final BlockPos seaLevelPos = pos.atY(TFCChunkGenerator.SEA_LEVEL_Y);

            ticks = Calendars.CLIENT.getTicks();
            averageTemperature = NTEClimateRenderHelpers.getAdjustedAverageTemperature(level, pos);
            temperature = Climate.getTemperature(level, pos);
            rainfall = Climate.getRainfall(level, pos);
            wind = Climate.getWindVector(level, pos);

            nte$hemisphereScale = NTEClimateRenderHelpers.getHemisphereScale(level);
            nte$averageSeaLevelTemperature = NTEClimateRenderHelpers.getAdjustedAverageTemperature(level, seaLevelPos);
            nte$averageRainfall = rainfall;
            nte$instantRainfall = NTEClimateRenderHelpers.getInstantRainfall(level, pos);
            nte$baseGroundwater = NTEClimateRenderHelpers.getBaseGroundwater(level, pos);
            nte$averageGroundwater = NTEClimateRenderHelpers.getAverageGroundwater(level, pos);
            nte$instantGroundwater = NTEClimateRenderHelpers.getInstantGroundwater(level, pos);

            final float rainVariance = NTEClimateRenderHelpers.getRainVarianceOrNaN(level, pos);
            nte$rainVariance = Float.isNaN(rainVariance) ? 0f : rainVariance;

            final float targetRainLevel = level instanceof ClientLevel clientLevel ? clientLevel.rainLevel : 0f;
            final float adjustedTargetRainLevel = WorldTracker.get(level).isRaining(ticks, nte$instantRainfall) ? targetRainLevel : 0f;

            lastRainLevel = currRainLevel;
            if (currRainLevel < adjustedTargetRainLevel)
            {
                currRainLevel += 0.01f;
            }
            else if (currRainLevel > adjustedTargetRainLevel)
            {
                currRainLevel -= 0.01f;
            }
            currRainLevel = Mth.clamp(currRainLevel, 0f, 1f);
        }
    }

    @Override
    public float getAverageSeaLevelTemperature()
    {
        return nte$averageSeaLevelTemperature;
    }

    @Override
    public float getInstantTemperature()
    {
        return temperature;
    }

    @Override
    public float getAverageRainfall()
    {
        return nte$averageRainfall;
    }

    @Override
    public float getRainVariance()
    {
        return nte$rainVariance;
    }

    @Override
    public float getInstantRainfall()
    {
        return nte$instantRainfall;
    }

    @Override
    public float getBaseGroundwater()
    {
        return nte$baseGroundwater;
    }

    @Override
    public float getAverageGroundwater()
    {
        return nte$averageGroundwater;
    }

    @Override
    public float getInstantGroundwater()
    {
        return nte$instantGroundwater;
    }

    @Override
    public float getHemisphereScale()
    {
        return nte$hemisphereScale;
    }
}
