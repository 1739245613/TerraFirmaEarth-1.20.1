package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.util.RandomSource;
import net.dries007.tfc.world.region.AddIslands;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;

import com.newterraearth.tfe.world.region.NTEPointAccess;
import com.newterraearth.tfe.world.region.NTERegionGeneratorAccess;

@Mixin(value = AddIslands.class, remap = false)
public abstract class AddIslandsMixin
{
    /**
     * @author Codex
     * @reason Backport the 4.2.9 continent-factor and shelf-depth guards for
     * island chains while retaining the 1.20 Region representation.
     */
    @Overwrite(remap = false)
    public void apply(RegionGenerator.Context context)
    {
        final Region region = context.region;
        final Region.Point[] points = region.data();
        final RandomSource random = context.random;
        final NTERegionGeneratorAccess generator = (NTERegionGeneratorAccess) context.generator();

        for (int attempt = 0, placed = 0; attempt < 130 && placed < 15; attempt++)
        {
            final int startIndex = random.nextInt(points.length);
            final Region.Point point = points[startIndex];
            final int startX = region.minX() + startIndex % region.sizeX();
            final int startZ = region.minZ() + startIndex / region.sizeX();
            if (point == null || point.land() || point.shore() || point.distanceToEdge <= 2
                || generator.nte$continentFactor(startX, startZ) <= 0.5f)
            {
                continue;
            }

            int x = startX;
            int z = startZ;
            Region.Point current = point;
            for (int island = 0; island < 12; island++)
            {
                current.setLand();
                current.setIsland();
                current.distanceToOcean = 1;
                x += random.nextInt(4) - random.nextInt(4);
                z += random.nextInt(4) - random.nextInt(4);
                current = region.maybeAt(x, z);
                if (current == null || (current.land() && !current.island()) || current.distanceToEdge <= 2
                    || ((NTEPointAccess) current).nte$getOceanDepth() == 1)
                {
                    break;
                }
            }
            placed++;
        }
    }
}
