package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.world.region.AddRiversAndLakes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.RiverEdge;

import com.newterraearth.tfe.world.NTELayerIds;
import com.newterraearth.tfe.world.region.NTEPointAccess;

import static net.dries007.tfc.world.layer.TFCLayers.*;

/** Restores the 4.2.9 river-valley grid annotation after the 1.20 river task. */
@Mixin(value = AddRiversAndLakes.class, remap = false)
public abstract class AddRiversAndLakesMixin
{
    private static final int MIN_VALLEY_WIDTH = 20;

    @Inject(method = "apply", at = @At("TAIL"))
    private void tfe$annotateRiverValleys(RegionGenerator.Context context, CallbackInfo ci)
    {
        final Region region = context.region;
        for (RiverEdge edge : region.rivers())
        {
            tfe$markSource(region, edge);
            if (edge.width > MIN_VALLEY_WIDTH)
            {
                tfe$annotateRiverGridScale(region, edge);
            }
        }

        for (Region.Point point : region.data())
        {
            if (point == null || point.island() || !point.land() || !point.river() || tfe$isLakeBiome(point.biome))
            {
                continue;
            }
            if (point.distanceToEdge <= 4 || point.temperature <= -16f + 0.006f * point.rainfall)
            {
                continue;
            }

            final NTEPointAccess access = (NTEPointAccess) point;
            // In 4.2.9, hotspot replacement runs after the river-valley branch.
            // Keep that ordering when the 1.20 river task has to backfill valleys.
            if (access.nte$getHotSpotAge() > 0)
            {
                continue;
            }

            // The 4.2.9 chooser gives the continental rift branch priority over rivers.
            if (point.distanceToEdge < 3 && access.nte$getDivergence() > 0d)
            {
                continue;
            }
            point.biome = NTELayerIds.RIVER_VALLEY;
        }
    }

    private void tfe$markSource(Region region, RiverEdge edge)
    {
        final Region.Point point = region.maybeAt((int) edge.source().x(), (int) edge.source().y());
        if (point != null && point.land())
        {
            point.setRiver();
        }
    }

    private void tfe$annotateRiverGridScale(Region region, RiverEdge edge)
    {
        final int ux = (int) edge.source().x();
        final int uz = (int) edge.source().y();
        final int vx = (int) edge.drain().x();
        final int vz = (int) edge.drain().y();
        final int dx = vx - ux;
        final int dz = vz - uz;
        final double magnitude = Math.sqrt(dx * dx + dz * dz);
        if (magnitude <= 0d)
        {
            return;
        }

        final double unitX = dx / magnitude;
        final double unitZ = dz / magnitude;
        for (double distance = 0d; distance <= magnitude; distance += 1d)
        {
            final int x = (int) (ux + unitX * distance);
            final int z = (int) (uz + unitZ * distance);
            tfe$setRiver(region.maybeAt(x, z));
            tfe$setRiver(region.maybeAt(x + 1, z));
            tfe$setRiver(region.maybeAt(x, z + 1));
            tfe$setRiver(region.maybeAt(x + 1, z + 1));
        }
    }

    private void tfe$setRiver(Region.Point point)
    {
        if (point != null && point.land())
        {
            point.setRiver();
        }
    }

    private boolean tfe$isLakeBiome(int biome)
    {
        return biome == LAKE
            || biome == MOUNTAIN_LAKE
            || biome == VOLCANIC_MOUNTAIN_LAKE
            || biome == OLD_MOUNTAIN_LAKE
            || biome == OCEANIC_MOUNTAIN_LAKE
            || biome == VOLCANIC_OCEANIC_MOUNTAIN_LAKE
            || biome == PLATEAU_LAKE
            || biome == NTELayerIds.RIFT_LAKE
            || biome == NTELayerIds.TOWER_KARST_LAKE
            || biome == NTELayerIds.SUBGLACIAL_LAKE
            || biome == NTELayerIds.MELTWATER_LAKE;
    }
}
