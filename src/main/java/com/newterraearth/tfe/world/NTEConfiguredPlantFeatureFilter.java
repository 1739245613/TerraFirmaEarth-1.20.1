package com.newterraearth.tfe.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import com.newterraearth.tfe.config.NTECommonConfig;

public final class NTEConfiguredPlantFeatureFilter
{
    private NTEConfiguredPlantFeatureFilter()
    {
    }

    public static List<HolderSet<PlacedFeature>> filter(List<HolderSet<PlacedFeature>> original)
    {
        if (original == null || !NTECommonConfig.hasDisabledConfiguredPlantFeatures())
        {
            return original;
        }

        boolean changed = false;
        final List<HolderSet<PlacedFeature>> filteredSteps = new ArrayList<>(original.size());
        for (HolderSet<PlacedFeature> step : original)
        {
            final List<Holder<PlacedFeature>> kept = new ArrayList<>();
            int total = 0;
            for (Holder<PlacedFeature> holder : step)
            {
                total++;
                if (NTECommonConfig.isPlacedPlantFeatureEnabled(holder.unwrapKey().map(key -> key.location()).orElse(null)))
                {
                    kept.add(holder);
                }
            }

            if (kept.size() == total)
            {
                filteredSteps.add(step);
            }
            else
            {
                changed = true;
                filteredSteps.add(HolderSet.direct(kept));
            }
        }
        return changed ? List.copyOf(filteredSteps) : original;
    }
}
