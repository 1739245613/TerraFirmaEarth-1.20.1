package com.newterraearth.tfe.mixin;

import java.util.function.IntPredicate;
import java.util.function.Predicate;

import net.dries007.tfc.world.layer.RegionEdgeBiomeLayer;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.AreaContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.newterraearth.tfe.world.NTELayerIds;

import static net.dries007.tfc.world.layer.TFCLayers.*;

@Mixin(value = RegionEdgeBiomeLayer.class, remap = false)
public abstract class RegionEdgeBiomeLayerMixin
{
    /**
     * @author Codex
     * @reason Align regional biome edge smoothing to the local 1.21 RegionEdgeBiomeLayer implementation.
     */
    @Overwrite(remap = false)
    public int apply(AreaContext context, int north, int east, int south, int west, int center)
    {
        final Predicate<IntPredicate> matcher = p -> p.test(north) || p.test(east) || p.test(south) || p.test(west);

        if (TFCLayers.isLow(center))
        {
            if (matcher.test(TFCLayers::isOcean) && matcher.test(TFCLayers::isMountains))
            {
                return OCEANIC_MOUNTAINS;
            }
            else if (matcher.test(TFCLayers::isOcean) && matcher.test(i -> i == LOWLANDS))
            {
                return SALT_MARSH;
            }
        }

        if (isFlats(center))
        {
            if (matcher.test(TFCLayers::isOcean) && matcher.test(RegionEdgeBiomeLayerMixin::isFlats))
            {
                return CANYONS;
            }
        }

        if (center == PLATEAU || center == BADLANDS)
        {
            if (matcher.test(i -> i == LOW_CANYONS || i == LOWLANDS))
            {
                return HILLS;
            }
            else if (matcher.test(i -> i == PLAINS || i == HILLS))
            {
                return ROLLING_HILLS;
            }
        }
        else if (TFCLayers.isMountains(center))
        {
            if (matcher.test(TFCLayers::isLow))
            {
                return ROLLING_HILLS;
            }
        }
        else if (center == LOWLANDS || center == LOW_CANYONS)
        {
            if (matcher.test(i -> i == PLATEAU || i == BADLANDS))
            {
                return HILLS;
            }
            else if (matcher.test(TFCLayers::isMountains))
            {
                return ROLLING_HILLS;
            }
        }
        else if (center == PLAINS || center == HILLS)
        {
            if (matcher.test(i -> i == PLATEAU || i == BADLANDS))
            {
                return HILLS;
            }
            else if (matcher.test(TFCLayers::isMountains))
            {
                return ROLLING_HILLS;
            }
        }
        else if (center == DEEP_OCEAN_TRENCH)
        {
            if (matcher.test(i -> !TFCLayers.isOcean(i)))
            {
                return OCEAN;
            }
        }
        return center;
    }

    private static boolean isFlats(int value)
    {
        return value == NTELayerIds.MUD_FLATS || value == NTELayerIds.SALT_FLATS;
    }
}
