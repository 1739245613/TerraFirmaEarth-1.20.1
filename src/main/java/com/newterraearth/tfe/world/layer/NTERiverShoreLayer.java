package com.newterraearth.tfe.world.layer;

import java.util.function.IntPredicate;
import java.util.function.Predicate;

import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.AdjacentTransformLayer;
import net.dries007.tfc.world.layer.framework.AreaContext;

import com.newterraearth.tfe.world.NTELayerIds;

/** 4.2.9 shore pass, including the river-valley edge rules. */
public enum NTERiverShoreLayer implements AdjacentTransformLayer
{
    INSTANCE;

    @Override
    public int apply(AreaContext context, int north, int east, int south, int west, int center)
    {
        final Predicate<IntPredicate> matcher = p -> p.test(north) || p.test(east) || p.test(south) || p.test(west);
        if (hasShore(center) && matcher.test(TFCLayers::isOcean))
        {
            return shoreFor(center);
        }
        if (center == NTELayerIds.RIVER_VALLEY)
        {
            if (matcher.test(TFCLayers::isMountains))
            {
                return NTELayerIds.PLATEAU_WIDE;
            }
            if (matcher.test(TFCLayers::isOcean))
            {
                return TFCLayers.SALT_MARSH;
            }
        }
        return center;
    }

    private static boolean hasShore(int value)
    {
        return !TFCLayers.isOcean(value)
            && value != TFCLayers.LOW_CANYONS
            && value != TFCLayers.CANYONS
            && value != TFCLayers.OCEANIC_MOUNTAINS
            && value != TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS
            && value != NTELayerIds.TOWER_KARST_BAY
            && value != NTELayerIds.SUNKEN_SHIELD_VOLCANO
            && value != NTELayerIds.GLACIALLY_CARVED_OCEANIC_MOUNTAINS
            && value != NTELayerIds.GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
            && value != NTELayerIds.GLACIATED_OCEANIC_MOUNTAINS
            && value != NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS_EDGE
            && value != NTELayerIds.ICE_SHEET_SHIELD_VOLCANO
            && value != NTELayerIds.GLACIATED_SHIELD_VOLCANO
            && value != NTELayerIds.GUANO_ISLAND
            && value != NTELayerIds.VOLCANIC_MOUNTAIN_ISLANDS;
    }

    private static int shoreFor(int value)
    {
        if (value == TFCLayers.LOWLANDS || value == TFCLayers.SALT_MARSH) return TFCLayers.SALT_MARSH;
        if (value == TFCLayers.MOUNTAINS || value == NTELayerIds.COLLISIONAL_MOUNTAINS) return TFCLayers.OCEANIC_MOUNTAINS;
        if (value == TFCLayers.VOLCANIC_MOUNTAINS) return TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS;
        if (value == NTELayerIds.TOWER_KARST_LAKE) return NTELayerIds.TOWER_KARST_BAY;
        if (value == NTELayerIds.ACTIVE_SHIELD_VOLCANO) return NTELayerIds.SHIELD_VOLCANO_SHORE;
        if (value == NTELayerIds.DORMANT_SHIELD_VOLCANO || value == NTELayerIds.EXTINCT_SHIELD_VOLCANO || value == NTELayerIds.ANCIENT_SHIELD_VOLCANO) return NTELayerIds.OLD_SHIELD_VOLCANO_SHORE;
        if (isFlatIceSheet(value) || value == NTELayerIds.ICE_SHEET_EDGE || value == NTELayerIds.ICE_SHEET_OCEANIC) return NTELayerIds.ICE_SHEET_SHORE;
        if (value == NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS || value == NTELayerIds.ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS) return NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS_EDGE;
        if (value == NTELayerIds.GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS || value == NTELayerIds.GLACIATED_VOLCANIC_MOUNTAINS || value == NTELayerIds.GLACIALLY_CARVED_VOLCANIC_MOUNTAINS) return NTELayerIds.GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS;
        if (value == NTELayerIds.GLACIATED_OCEANIC_MOUNTAINS || value == NTELayerIds.GLACIATED_MOUNTAINS || value == NTELayerIds.GLACIALLY_CARVED_MOUNTAINS) return NTELayerIds.GLACIATED_OCEANIC_MOUNTAINS;
        if (value == TFCLayers.OLD_MOUNTAINS || value == NTELayerIds.EXTREME_DOLINE_MOUNTAINS) return NTELayerIds.TERRACE_LOWER;
        if (value == TFCLayers.PLATEAU || value == NTELayerIds.EXTREME_DOLINE_PLATEAU || value == NTELayerIds.BURREN_PLATEAU || value == NTELayerIds.SHILIN_PLATEAU) return NTELayerIds.SEA_STACKS;
        if (value == NTELayerIds.PLATEAU_WIDE || value == NTELayerIds.ROCKY_PLATEAU || value == NTELayerIds.DOLINE_PLATEAU) return NTELayerIds.SETBACK_CLIFFS;
        if (value == TFCLayers.HIGHLANDS || value == NTELayerIds.CENOTE_HIGHLANDS || value == NTELayerIds.DOLINE_HIGHLANDS || value == NTELayerIds.SHILIN_HIGHLANDS || value == NTELayerIds.TOWER_KARST_HIGHLANDS) return NTELayerIds.ROCKY_SHORES;
        if (value == TFCLayers.ROLLING_HILLS || value == NTELayerIds.DOLINE_ROLLING_HILLS || value == NTELayerIds.CENOTE_ROLLING_HILLS) return NTELayerIds.EMBAYMENTS;
        if (value == TFCLayers.HILLS || value == NTELayerIds.CENOTE_HILLS || value == NTELayerIds.DOLINE_HILLS || value == NTELayerIds.SHILIN_HILLS || value == NTELayerIds.TOWER_KARST_HILLS || value == NTELayerIds.GRASSY_DUNES || value == NTELayerIds.DUNE_SEA) return NTELayerIds.COASTAL_DUNES;
        if (value == NTELayerIds.VOLCANIC_ISLAND) return TFCLayers.SHORE;
        return TFCLayers.TIDAL_FLATS;
    }

    private static boolean isFlatIceSheet(int value)
    {
        return value == NTELayerIds.ICE_SHEET || value == NTELayerIds.ICE_SHEET_TUYAS || value == NTELayerIds.SUBGLACIAL_LAKE;
    }

}
