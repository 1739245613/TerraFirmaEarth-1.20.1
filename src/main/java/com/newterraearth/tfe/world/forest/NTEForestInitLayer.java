package com.newterraearth.tfe.world.forest;

import net.dries007.tfc.world.layer.framework.AreaContext;
import net.dries007.tfc.world.layer.framework.SourceLayer;
import net.dries007.tfc.world.noise.Noise2D;

final class NTEForestInitLayer implements SourceLayer
{
    private final Noise2D forestBaseNoise;

    NTEForestInitLayer(Noise2D forestBaseNoise)
    {
        this.forestBaseNoise = forestBaseNoise;
    }

    @Override
    public int apply(AreaContext context, int x, int z)
    {
        final float noise = (float) forestBaseNoise.noise(x, z);
        return noise < 0 ? NTEForestType.GRASSLAND.ordinal() : NTEForestType.SECONDARY_DIVERSE.ordinal();
    }
}
