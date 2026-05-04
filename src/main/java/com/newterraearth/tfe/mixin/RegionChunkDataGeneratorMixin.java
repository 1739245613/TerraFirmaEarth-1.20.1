package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.dries007.tfc.world.chunkdata.RegionChunkDataGenerator;

@Mixin(value = RegionChunkDataGenerator.class, remap = false)
public abstract class RegionChunkDataGeneratorMixin
{
    @Redirect(
        method = "generate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/world/chunkdata/RegionChunkDataGenerator;adjustRiverRainfall(FFFLnet/dries007/tfc/world/river/MidpointFractal;DD)F"
        )
    )
    private float tfe$keepRiverInfluenceOutOfRainfall(
        RegionChunkDataGenerator instance,
        float currentRainfall,
        float originalRainfall,
        float widthInfluence,
        net.dries007.tfc.world.river.MidpointFractal fractal,
        double gridX,
        double gridZ
    )
    {
        return currentRainfall;
    }
}
