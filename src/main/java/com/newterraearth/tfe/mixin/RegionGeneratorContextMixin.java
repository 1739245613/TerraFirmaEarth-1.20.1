package com.newterraearth.tfe.mixin;

import java.util.BitSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.region.AnnotateDistanceToCellEdge;
import net.dries007.tfc.world.region.ChooseRocks;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;

import com.newterraearth.tfe.world.NTELayerIds;
import com.newterraearth.tfe.world.region.NTEAddHotspots;
import com.newterraearth.tfe.world.region.NTEKarstSurfaceRocks;
import com.newterraearth.tfe.world.region.NTERegionFeatureAnnotations;

@Mixin(value = RegionGenerator.Context.class, remap = false)
public abstract class RegionGeneratorContextMixin
{
    @Unique private static final int TFE_REGION_RADIUS_IN_GRID = Units.CELL_WIDTH_IN_GRID + 5;
    @Unique private static final Logger TFE_LOGGER = LogManager.getLogger();

    @Inject(method = "run", at = @At("HEAD"))
    private void tfe$runBiomePrerequisites(RegionGenerator.Task task, CallbackInfo ci)
    {
        if (task == RegionGenerator.Task.INIT)
        {
            final RegionGenerator.Context context = (RegionGenerator.Context) (Object) this;
            final Region region = context.region;
            final int centerX = region.minX() + region.sizeX() / 2;
            final int centerZ = region.minZ() + region.sizeZ() / 2;
            final int scanMinX = centerX - TFE_REGION_RADIUS_IN_GRID;
            final int scanMinZ = centerZ - TFE_REGION_RADIUS_IN_GRID;
            final int scanWidth = 1 + 2 * TFE_REGION_RADIUS_IN_GRID;
            final BitSet cell = new BitSet(scanWidth * scanWidth);
            final Cellular2D.Cell centerSample = context.generator().sampleCell(centerX, centerZ);

            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            int exactMatchCount = 0;
            int idMatchCount = 0;
            int firstIdOnlyGridX = 0;
            int firstIdOnlyGridZ = 0;
            Cellular2D.Cell firstIdOnlyCell = null;
            int nearestGridX = centerX;
            int nearestGridZ = centerZ;
            double nearestDistanceSq = Double.POSITIVE_INFINITY;
            Cellular2D.Cell nearestCell = null;

            for (int dx = 0; dx < scanWidth; dx++)
            {
                for (int dz = 0; dz < scanWidth; dz++)
                {
                    final int gridX = scanMinX + dx;
                    final int gridZ = scanMinZ + dz;
                    final Cellular2D.Cell otherCell = context.generator().sampleCell(gridX, gridZ);
                    final double distanceSq = tfe$cellCenterDistanceSq(otherCell, context.regionCell);
                    if (distanceSq < nearestDistanceSq)
                    {
                        nearestDistanceSq = distanceSq;
                        nearestGridX = gridX;
                        nearestGridZ = gridZ;
                        nearestCell = otherCell;
                    }

                    final boolean idMatch = tfe$sameCellId(otherCell, context.regionCell);
                    final boolean exactMatch = tfe$sameCellExact(otherCell, context.regionCell);
                    if (idMatch)
                    {
                        idMatchCount++;
                        if (!exactMatch && firstIdOnlyCell == null)
                        {
                            firstIdOnlyGridX = gridX;
                            firstIdOnlyGridZ = gridZ;
                            firstIdOnlyCell = otherCell;
                        }
                    }

                    if (exactMatch)
                    {
                        exactMatchCount++;
                        cell.set(dx + scanWidth * dz);
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

            if (minX == Integer.MAX_VALUE || idMatchCount != exactMatchCount)
            {
                final String message = tfe$regionInitFailureMessage(
                    context,
                    region,
                    centerX,
                    centerZ,
                    scanMinX,
                    scanMinZ,
                    scanWidth,
                    exactMatchCount,
                    idMatchCount,
                    centerSample,
                    nearestGridX,
                    nearestGridZ,
                    nearestDistanceSq,
                    nearestCell,
                    firstIdOnlyGridX,
                    firstIdOnlyGridZ,
                    firstIdOnlyCell
                );
                TFE_LOGGER.error(message);
                throw new IllegalStateException(message);
            }

            final int modifiedSizeX = 1 + maxX - minX;
            final int modifiedSizeZ = 1 + maxZ - minZ;
            final long modifiedLength = (long) modifiedSizeX * modifiedSizeZ;
            if (modifiedSizeX <= 0 || modifiedSizeZ <= 0 || modifiedLength > Integer.MAX_VALUE)
            {
                final String message = "TFE region INIT produced invalid array bounds after a non-empty scan: "
                    + "sizeX=%d, sizeZ=%d, length=%d, min=(%d,%d), max=(%d,%d), exactMatches=%d, idMatches=%d"
                    .formatted(modifiedSizeX, modifiedSizeZ, modifiedLength, minX, minZ, maxX, maxZ, exactMatchCount, idMatchCount);
                TFE_LOGGER.error(message);
                throw new IllegalStateException(message);
            }
            final int offsetX = minX - scanMinX;
            final int offsetZ = minZ - scanMinZ;
            final Region.Point[] points = new Region.Point[modifiedSizeX * modifiedSizeZ];

            region.setRegionArea(points, minX, minZ, maxX, maxZ);
            for (int dx = 0; dx < modifiedSizeX; dx++)
            {
                for (int dz = 0; dz < modifiedSizeZ; dz++)
                {
                    if (cell.get((offsetX + dx) + scanWidth * (offsetZ + dz)))
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

        if (task == RegionGenerator.Task.ADD_MOUNTAINS)
        {
            final RegionGenerator.Context context = (RegionGenerator.Context) (Object) this;
            NTERegionFeatureAnnotations.INSTANCE.prepareForMountainPlacement(context);
        }

        if (task == RegionGenerator.Task.CHOOSE_BIOMES)
        {
            final RegionGenerator.Context context = (RegionGenerator.Context) (Object) this;
            NTERegionFeatureAnnotations.INSTANCE.apply(context);
            ChooseRocks.INSTANCE.apply(context);
            NTEKarstSurfaceRocks.INSTANCE.apply(context);
        }
    }

    @Inject(method = "run", at = @At("TAIL"))
    private void tfe$recomputeFinalCellEdgeDistance(RegionGenerator.Task task, CallbackInfo ci)
    {
        if (task != RegionGenerator.Task.SHRINK_TO_CELL)
        {
            return;
        }

        final RegionGenerator.Context context = (RegionGenerator.Context) (Object) this;
        // AddContinents runs before ShrinkToCell in 1.20, so its local
        // edge-distance pass used the expanded working area. Recompute
        // the distance on the final cell area before biome selection.
        for (Region.Point point : context.region.data())
        {
            if (point != null)
            {
                point.distanceToEdge = 0;
            }
        }
        AnnotateDistanceToCellEdge.INSTANCE.apply(context);
    }

    @Unique
    private boolean tfe$sameCellExact(Cellular2D.Cell first, Cellular2D.Cell second)
    {
        return first.x() == second.x() && first.y() == second.y();
    }

    @Unique
    private boolean tfe$sameCellId(Cellular2D.Cell first, Cellular2D.Cell second)
    {
        return first.cx() == second.cx() && first.cy() == second.cy();
    }

    @Unique
    private double tfe$cellCenterDistanceSq(Cellular2D.Cell first, Cellular2D.Cell second)
    {
        final double dx = first.x() - second.x();
        final double dz = first.y() - second.y();
        return dx * dx + dz * dz;
    }

    @Unique
    private String tfe$regionInitFailureMessage(
        RegionGenerator.Context context,
        Region region,
        int centerX,
        int centerZ,
        int scanMinX,
        int scanMinZ,
        int scanWidth,
        int exactMatchCount,
        int idMatchCount,
        Cellular2D.Cell centerSample,
        int nearestGridX,
        int nearestGridZ,
        double nearestDistanceSq,
        Cellular2D.Cell nearestCell,
        int firstIdOnlyGridX,
        int firstIdOnlyGridZ,
        Cellular2D.Cell firstIdOnlyCell
    )
    {
        final int scanMaxX = scanMinX + scanWidth - 1;
        final int scanMaxZ = scanMinZ + scanWidth - 1;
        final String reason = exactMatchCount == 0
            ? "no exact cell matches found"
            : "cell id matches differ from exact center-coordinate matches";
        return "TFE region INIT invariant failed (%s): targetCell=%s, initialRegion=[%d..%d]x[%d..%d] size=%dx%d, "
            .formatted(reason, tfe$formatCell(context.regionCell), region.minX(), region.maxX(), region.minZ(), region.maxZ(), region.sizeX(), region.sizeZ())
            + "centerGrid=(%d,%d), centerBlock=(%d,%d), scanRadius=%d, scanWidth=%d, scanGrid=[%d..%d]x[%d..%d], scanBlock=[%d..%d]x[%d..%d], "
            .formatted(centerX, centerZ, tfe$gridToBlock(centerX), tfe$gridToBlock(centerZ), TFE_REGION_RADIUS_IN_GRID, scanWidth, scanMinX, scanMaxX, scanMinZ, scanMaxZ, tfe$gridToBlock(scanMinX), tfe$gridToBlock(scanMaxX), tfe$gridToBlock(scanMinZ), tfe$gridToBlock(scanMaxZ))
            + "matches={exact=%d,id=%d}, centerSample=%s, nearestSample={grid=(%d,%d), distanceSq=%.6f, cell=%s}, firstIdOnlySample=%s"
            .formatted(exactMatchCount, idMatchCount, tfe$formatCell(centerSample), nearestGridX, nearestGridZ, nearestDistanceSq, tfe$formatCell(nearestCell), tfe$formatIdOnlySample(firstIdOnlyGridX, firstIdOnlyGridZ, firstIdOnlyCell));
    }

    @Unique
    private String tfe$formatCell(Cellular2D.Cell cell)
    {
        return cell == null
            ? "none"
            : "{cx=%d,cy=%d,x=%.6f,z=%.6f,f1=%.6f,f2=%.6f,noise=%.6f}"
                .formatted(cell.cx(), cell.cy(), cell.x(), cell.y(), cell.f1(), cell.f2(), cell.noise());
    }

    @Unique
    private String tfe$formatIdOnlySample(int gridX, int gridZ, Cellular2D.Cell cell)
    {
        return cell == null ? "none" : "{grid=(%d,%d), cell=%s}".formatted(gridX, gridZ, tfe$formatCell(cell));
    }

    @Unique
    private long tfe$gridToBlock(int grid)
    {
        return (long) grid * Units.GRID_WIDTH_IN_BLOCK;
    }
}
