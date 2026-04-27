package com.newterraearth.tfe.mixin;

import java.util.BitSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.region.ChooseRocks;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;

import com.newterraearth.tfe.world.region.NTEAddHotspots;
import com.newterraearth.tfe.world.region.NTEKarstSurfaceRocks;

@Mixin(value = RegionGenerator.Context.class, remap = false)
public abstract class RegionGeneratorContextMixin
{
    @Inject(method = "run", at = @At("HEAD"))
    private void tfe$runBiomePrerequisites(RegionGenerator.Task task, CallbackInfo ci)
    {
        if (task == RegionGenerator.Task.INIT)
        {
            final RegionGenerator.Context context = (RegionGenerator.Context) (Object) this;
            final Region region = context.region;
            final BitSet cell = new BitSet(region.sizeX() * region.sizeZ());

            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            final int initialMinX = region.minX();
            final int initialMinZ = region.minZ();
            final int initialSizeX = region.sizeX();

            for (int dx = 0; dx <= 2 * (Units.CELL_WIDTH_IN_GRID + 5); dx++)
            {
                for (int dz = 0; dz <= 2 * (Units.CELL_WIDTH_IN_GRID + 5); dz++)
                {
                    final int gridX = initialMinX + dx;
                    final int gridZ = initialMinZ + dz;
                    final int index = (gridX - initialMinX) + initialSizeX * (gridZ - initialMinZ);
                    final Cellular2D.Cell otherCell = context.generator().sampleCell(gridX, gridZ);

                    if (otherCell.x() == context.regionCell.x() && otherCell.y() == context.regionCell.y())
                    {
                        cell.set(index);
                        if (gridX < minX)
                        {
                            minX = gridX;
                        }
                        if (gridZ < minZ)
                        {
                            minZ = gridZ;
                        }
                        if (gridX > maxX)
                        {
                            maxX = gridX;
                        }
                        if (gridZ > maxZ)
                        {
                            maxZ = gridZ;
                        }
                    }
                }
            }

            final int modifiedSizeX = 1 + maxX - minX;
            final int modifiedSizeZ = 1 + maxZ - minZ;
            final int offsetX = minX - initialMinX;
            final int offsetZ = minZ - initialMinZ;
            final Region.Point[] points = new Region.Point[modifiedSizeX * modifiedSizeZ];

            region.setRegionArea(points, minX, minZ, maxX, maxZ);
            for (int dx = 0; dx < modifiedSizeX; dx++)
            {
                for (int dz = 0; dz < modifiedSizeZ; dz++)
                {
                    if (cell.get((offsetX + dx) + initialSizeX * (offsetZ + dz)))
                    {
                        region.atInit(minX + dx, minZ + dz);
                    }
                }
            }

            context.minX = minX;
            context.minZ = minZ;
            context.maxX = maxX;
            context.maxZ = maxZ;
        }

        if (task == RegionGenerator.Task.ANNOTATE_DISTANCE_TO_OCEAN)
        {
            final RegionGenerator.Context context = (RegionGenerator.Context) (Object) this;
            NTEAddHotspots.INSTANCE.apply(context);
        }

        if (task == RegionGenerator.Task.CHOOSE_BIOMES)
        {
            final RegionGenerator.Context context = (RegionGenerator.Context) (Object) this;
            ChooseRocks.INSTANCE.apply(context);
            NTEKarstSurfaceRocks.INSTANCE.apply(context);
        }
    }
}
