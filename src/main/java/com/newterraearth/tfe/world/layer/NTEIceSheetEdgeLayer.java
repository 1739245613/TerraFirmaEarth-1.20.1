package com.newterraearth.tfe.world.layer;

import java.util.function.IntPredicate;
import java.util.function.Predicate;

import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.AdjacentTransformLayer;
import net.dries007.tfc.world.layer.framework.AreaContext;

import com.newterraearth.tfe.world.NTELayerIds;

public enum NTEIceSheetEdgeLayer implements AdjacentTransformLayer
{
    INSTANCE;

    @Override
    public int apply(AreaContext context, int north, int east, int south, int west, int center)
    {
        final Predicate<IntPredicate> matcher = p -> p.test(north) || p.test(east) || p.test(south) || p.test(west);

        if (center == NTELayerIds.KNOB_AND_KETTLE || center == NTELayerIds.PATTERNED_GROUND || center == NTELayerIds.INVERTED_PATTERNED_GROUND || center == NTELayerIds.STONE_CIRCLES)
        {
            if (matcher.test(i -> i == NTELayerIds.ICE_SHEET_TUYAS))
            {
                return NTELayerIds.ICE_SHEET_TUYAS_EDGE;
            }
            if (matcher.test(NTEIceSheetEdgeLayer::isFlatIceSheet))
            {
                return NTELayerIds.ICE_SHEET_EDGE;
            }
        }

        if ((center == NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS || center == NTELayerIds.ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS) && matcher.test(NTEIceSheetEdgeLayer::isNotIceSheet))
        {
            return NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS_EDGE;
        }
        if ((center == NTELayerIds.ICE_SHEET_MOUNTAINS || center == NTELayerIds.ICE_SHEET_VOLCANIC_MOUNTAINS) && matcher.test(NTEIceSheetEdgeLayer::isNotIceSheet))
        {
            return NTELayerIds.ICE_SHEET_MOUNTAINS_EDGE;
        }

        if (center == NTELayerIds.ICE_SHEET_EDGE && matcher.test(i ->
            i == NTELayerIds.ICE_SHEET_MOUNTAINS
                || i == NTELayerIds.ICE_SHEET_MOUNTAINS_EDGE
                || i == NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS
                || i == NTELayerIds.ICE_SHEET_OCEANIC))
        {
            return NTELayerIds.KNOB_AND_KETTLE;
        }

        if (center == TFCLayers.LAKE
            && matcher.test(NTEIceSheetEdgeLayer::isFlatIceSheet)
            && !matcher.test(i ->
                i == NTELayerIds.ICE_SHEET_MOUNTAINS
                    || i == NTELayerIds.ICE_SHEET_MOUNTAINS_EDGE
                    || i == NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS
                    || i == NTELayerIds.ICE_SHEET_OCEANIC))
        {
            return NTELayerIds.SUBGLACIAL_LAKE;
        }

        if (isFlatIceSheet(center) && matcher.test(i -> i == NTELayerIds.MELTWATER_LAKE))
        {
            if (matcher.test(NTEIceSheetEdgeLayer::isNotIceSheet))
            {
                return NTELayerIds.SUBGLACIAL_LAKE;
            }
        }

        if (isFlatIceSheet(center) && matcher.test(i ->
            i == TFCLayers.OCEAN
                || i == TFCLayers.OCEAN_REEF
                || i == TFCLayers.DEEP_OCEAN
                || i == TFCLayers.DEEP_OCEAN_TRENCH
                || i == NTELayerIds.ICE_SHEET_SHORE))
        {
            return NTELayerIds.ICE_SHEET_OCEANIC;
        }

        if (isNotIceSheetOrGlaciated(center))
        {
            if (matcher.test(i -> i == NTELayerIds.GLACIATED_MOUNTAINS))
            {
                return NTELayerIds.GLACIALLY_CARVED_MOUNTAINS;
            }
            if (matcher.test(i -> i == NTELayerIds.GLACIATED_OCEANIC_MOUNTAINS))
            {
                return NTELayerIds.GLACIALLY_CARVED_OCEANIC_MOUNTAINS;
            }
            if (matcher.test(i -> i == NTELayerIds.GLACIATED_VOLCANIC_MOUNTAINS))
            {
                return NTELayerIds.GLACIALLY_CARVED_VOLCANIC_MOUNTAINS;
            }
            if (matcher.test(i -> i == NTELayerIds.GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS))
            {
                return NTELayerIds.GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS;
            }
        }

        if ((center == TFCLayers.PLATEAU
            || center == TFCLayers.BADLANDS
            || center == NTELayerIds.BURREN_BADLANDS
            || center == NTELayerIds.BURREN_BADLANDS_TALL
            || center == NTELayerIds.GLACIATED_SHIELD_VOLCANO)
            && matcher.test(i -> i == NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS_EDGE))
        {
            return NTELayerIds.GLACIATED_OCEANIC_MOUNTAINS;
        }

        if ((center == NTELayerIds.ICE_SHEET
            || center == NTELayerIds.ICE_SHEET_TUYAS
            || center == NTELayerIds.ICE_SHEET_SHIELD_VOLCANO)
            && matcher.test(i -> i == NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS_EDGE))
        {
            return NTELayerIds.ICE_SHEET_OCEANIC;
        }

        if (center == NTELayerIds.ICE_SHEET_MOUNTAINS && matcher.test(i -> i == NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS_EDGE))
        {
            return NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS;
        }

        return center;
    }

    private static boolean isFlatIceSheet(int value)
    {
        return value == NTELayerIds.ICE_SHEET
            || value == NTELayerIds.ICE_SHEET_TUYAS
            || value == NTELayerIds.SUBGLACIAL_LAKE;
    }

    private static boolean isNotIceSheet(int value)
    {
        return value != NTELayerIds.ICE_SHEET
            && value != NTELayerIds.ICE_SHEET_TUYAS
            && value != NTELayerIds.SUBGLACIAL_LAKE
            && value != NTELayerIds.ICE_SHEET_MOUNTAINS
            && value != NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS
            && value != NTELayerIds.ICE_SHEET_SHIELD_VOLCANO
            && value != NTELayerIds.ICE_SHEET_VOLCANIC_MOUNTAINS
            && value != NTELayerIds.ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS;
    }

    private static boolean isNotIceSheetOrGlaciated(int value)
    {
        return isNotIceSheet(value)
            && value != NTELayerIds.GLACIATED_MOUNTAINS
            && value != NTELayerIds.GLACIATED_OCEANIC_MOUNTAINS
            && value != NTELayerIds.GLACIATED_SHIELD_VOLCANO
            && value != NTELayerIds.GLACIATED_VOLCANIC_MOUNTAINS
            && value != NTELayerIds.GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS;
    }
}
