package com.newterraearth.tfe.mixin;

import java.util.function.IntPredicate;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.dries007.tfc.world.layer.MoreShoresLayer;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.AreaContext;

import com.newterraearth.tfe.world.NTELayerIds;

@Mixin(value = MoreShoresLayer.class, remap = false)
public abstract class MoreShoresLayerMixin
{
    /**
     * @author Codex
     * @reason Align shore expansion to the local 1.21 MoreShoresLayer implementation.
     */
    @Overwrite(remap = false)
    public int apply(AreaContext context, int north, int east, int south, int west, int center)
    {
        final Predicate<IntPredicate> matcher = p -> p.test(north) || p.test(east) || p.test(south) || p.test(west);
        if (matcher.test(TFCLayers::isOcean))
        {
            if (matcher.test(layer -> layer == NTELayerIds.TERRACE_LOWER))
            {
                return NTELayerIds.TERRACE_UPPER;
            }
            if (matcher.test(layer -> layer == NTELayerIds.SEA_STACKS))
            {
                return NTELayerIds.SEA_STACKS;
            }
            if (matcher.test(layer -> layer == TFCLayers.TIDAL_FLATS))
            {
                return TFCLayers.SHORE;
            }
            if (matcher.test(layer -> layer == NTELayerIds.COASTAL_DUNES))
            {
                return NTELayerIds.COASTAL_DUNES;
            }
            if (matcher.test(layer -> layer == NTELayerIds.SETBACK_CLIFFS))
            {
                return NTELayerIds.SETBACK_CLIFFS;
            }
            if (matcher.test(layer -> layer == NTELayerIds.ROCKY_SHORES))
            {
                return NTELayerIds.ROCKY_SHORES;
            }
            if (matcher.test(layer -> layer == NTELayerIds.EMBAYMENTS))
            {
                return NTELayerIds.EMBAYMENTS;
            }
        }
        return center;
    }
}
