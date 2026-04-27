package com.newterraearth.tfe.mixin;

import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.dries007.tfc.world.region.AnnotateClimate;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;

import com.newterraearth.tfe.world.NTE121ClimateHelpers;
import com.newterraearth.tfe.world.region.NTEPointAccess;
import com.newterraearth.tfe.world.region.NTERegionGeneratorAccess;

@Mixin(value = AnnotateClimate.class, remap = false)
public abstract class AnnotateClimateMixin
{
    /**
     * @author Codex
     * @reason Align 1.20 region climate annotation with the local 1.21 climate model before biome selection.
     */
    @Overwrite(remap = false)
    public void apply(RegionGenerator.Context context)
    {
        final Region region = context.region;
        final NTERegionGeneratorAccess generator = (NTERegionGeneratorAccess) context.generator();
        final byte[] distanceToWestCoast = NTE121ClimateHelpers.getDistanceToWestCoast(region, generator.nte$getSettings().temperatureScale());

        for (int x = region.minX(); x <= region.maxX(); x++)
        {
            for (int z = region.minZ(); z <= region.maxZ(); z++)
            {
                final Region.Point point = region.maybeAt(x, z);
                if (point == null)
                {
                    continue;
                }

                point.temperature = (float) context.generator().temperatureNoise.noise(x, z);
                point.rainfall = (float) context.generator().rainfallNoise.noise(x, z);

                final float bias;
                if (point.land())
                {
                    final float potentialBias = Mth.clampedMap(point.distanceToEdge, 2f, 6f, 0f, 1f);
                    final float oceanProximityBias = Mth.clampedMap(point.distanceToOcean, 2f, 6f, 0f, 1f);
                    bias = Math.min(potentialBias, oceanProximityBias);
                }
                else
                {
                    bias = 0f;
                }

                float rainfallVariance = Mth.clampedMap(
                    distanceToWestCoast[region.index(x, z)] + (float) generator.nte$getRainfallVarianceNoise().noise(x, z),
                    0f,
                    80f,
                    -1f,
                    1f
                );
                final float edgeBiasScale = Mth.clampedMap(point.distanceToEdge, 0f, 12f, 1f, 0f);
                rainfallVariance = Mth.lerp(edgeBiasScale, rainfallVariance, 0f);

                final NTEPointAccess pointAccess = (NTEPointAccess) point;
                pointAccess.nte$setDistanceToWestCoast(distanceToWestCoast[region.index(x, z)]);
                pointAccess.nte$setRainfallVariance(Mth.clamp(rainfallVariance, -1f, 1f));

                final float biasTargetTemperature = Mth.lerp(bias, 5f, point.temperature);
                final float biasTargetRainfall = Mth.lerp(bias, Math.min(point.rainfall + 350f, 500f), point.rainfall);

                final float tempDelta = Mth.clampedMap((float) generator.nte$getOceanicInfluenceNoise().noise(x, z), -0.8f, 0.9f, -0.07f, 0.23f);
                final float oldTemperature = point.temperature;
                point.temperature = Mth.lerp(tempDelta, oldTemperature, biasTargetTemperature);

                final float rainDelta = Mth.clampedMap(point.temperature - oldTemperature, -2f, 2f, 0f, 0.25f);
                point.rainfall = Mth.clamp(Mth.lerp(rainDelta, point.rainfall, biasTargetRainfall), 0f, 500f);
            }
        }
    }
}
