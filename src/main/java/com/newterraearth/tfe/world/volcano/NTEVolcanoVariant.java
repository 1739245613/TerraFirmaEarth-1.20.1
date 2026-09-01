package com.newterraearth.tfe.world.volcano;

import net.dries007.tfc.world.surface.SurfaceBuilderContext;

import com.newterraearth.tfe.world.noise.NTECellular2D;

/** One of the 4.2.9 stratovolcano body profiles. */
public interface NTEVolcanoVariant
{
    String name();

    default double getHeight(double heightIn, int x, int z, double maxDiam, double biomeScaleHeight, double biomeBaseHeight, NTECellular2D.Cell cell)
    {
        return getLandHeight(heightIn, x, z, maxDiam, biomeScaleHeight, biomeBaseHeight, cell);
    }

    default double getLandHeight(double heightIn, int x, int z, double maxDiam, double biomeScaleHeight, double biomeBaseHeight, NTECellular2D.Cell cell)
    {
        return heightIn;
    }

    default double getFluidHeight(double heightIn, int x, int z, double maxDiam, double biomeScaleHeight, double biomeBaseHeight, NTECellular2D.Cell cell)
    {
        return 0;
    }

    /**
     * Applies the same variant-specific surface treatment as the 4.2.9
     * stratovolcano builder. The compatibility layer passes the generated top
     * and lower surface bounds in these two arguments.
     */
    boolean buildSurface(SurfaceBuilderContext context, int oceanFloorHeight, int preVolcanicHeight, NTECenteredFeatureNoiseSampler sampler);
}
