package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.noise.FastNoiseLite;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.Units;

@Mixin(value = Region.class, remap = false)
public abstract class RegionMixin
{
    private static final int TFE_REGION_RADIUS_IN_GRID = Units.CELL_WIDTH_IN_GRID + 5;
    private static final int TFE_REGION_WIDTH_IN_GRID = 1 + 2 * TFE_REGION_RADIUS_IN_GRID;

    @Shadow @Final private double cellX;
    @Shadow @Final private double cellY;
    @Shadow private int minX;
    @Shadow private int minZ;
    @Shadow private int maxX;
    @Shadow private int maxZ;
    @Shadow private int sizeX;
    @Shadow private int sizeZ;
    @Shadow private Region.Point[] data;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void tfe$expandRegionBounds(Cellular2D.Cell cell, CallbackInfo ci)
    {
        final int cellGridX = FastNoiseLite.FastRound(cellX);
        final int cellGridZ = FastNoiseLite.FastRound(cellY);

        this.minX = cellGridX - TFE_REGION_RADIUS_IN_GRID;
        this.minZ = cellGridZ - TFE_REGION_RADIUS_IN_GRID;
        this.maxX = cellGridX + TFE_REGION_RADIUS_IN_GRID;
        this.maxZ = cellGridZ + TFE_REGION_RADIUS_IN_GRID;

        this.sizeX = TFE_REGION_WIDTH_IN_GRID;
        this.sizeZ = TFE_REGION_WIDTH_IN_GRID;
        this.data = new Region.Point[TFE_REGION_WIDTH_IN_GRID * TFE_REGION_WIDTH_IN_GRID];
    }
}
