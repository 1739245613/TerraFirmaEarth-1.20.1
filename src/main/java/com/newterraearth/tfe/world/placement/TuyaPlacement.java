package com.newterraearth.tfe.world.placement;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoise;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoiseSampler;

public class TuyaPlacement extends NTECenterOrDistancePlacement<NTECenteredFeatureNoiseSampler>
{
    public static final Codec<TuyaPlacement> PLACEMENT_CODEC = codec(TuyaPlacement::new);

    public TuyaPlacement(boolean center, float distance)
    {
        super(center, distance);
    }

    @Override
    public PlacementModifierType<?> type()
    {
        return NTEPlacements.TFC_TUYA.isPresent() ? NTEPlacements.TFC_TUYA.get() : NTEPlacements.TUYA.get();
    }

    @Override
    protected NTECenteredFeatureNoiseSampler createContext(NTESeed seed)
    {
        return NTECenteredFeatureNoise.tuya(seed);
    }
}
