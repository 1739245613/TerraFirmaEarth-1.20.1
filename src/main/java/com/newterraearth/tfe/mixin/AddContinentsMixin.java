package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.dries007.tfc.world.region.AddContinents;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;

import com.newterraearth.tfe.world.region.NTERegionGeneratorAccess;

@Mixin(value = AddContinents.class, remap = false)
public abstract class AddContinentsMixin
{
    /**
     * @author Codex
     * @reason Run AddContinents on the 1.21-style initialized region points instead of the old 1.20 scan-and-init pass.
     */
    @Overwrite(remap = false)
    public void apply(RegionGenerator.Context context)
    {
        final NTERegionGeneratorAccess generator = (NTERegionGeneratorAccess) context.generator();
        final Region region = context.region;
        final Region.Point[] points = region.data();
        final int sizeX = region.sizeX();
        final int minX = region.minX();
        final int minZ = region.minZ();

        for (int index = 0; index < points.length; index++)
        {
            final Region.Point point = points[index];
            if (point == null)
            {
                continue;
            }

            final int gridX = minX + index % sizeX;
            final int gridZ = minZ + index / sizeX;
            final double continent = context.generator().continentNoise.noise(gridX, gridZ) * generator.nte$continentFactor(gridX, gridZ);
            if (continent > 4.4)
            {
                point.setLand();
            }
        }
    }
}
