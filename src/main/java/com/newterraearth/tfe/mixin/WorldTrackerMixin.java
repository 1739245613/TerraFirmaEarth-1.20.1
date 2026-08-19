package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import net.dries007.tfc.util.tracker.WorldTracker;

import com.newterraearth.tfe.world.NTESeasonalHelpers;

/**
 * Routes TFC's standard location-aware rain check through TFE's current
 * seasonal rainfall while retaining WorldTracker's weather window and
 * intensity comparison.
 */
@Mixin(value = WorldTracker.class, remap = false)
public abstract class WorldTrackerMixin
{
    @Redirect(
        method = "isRaining(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/climate/Climate;getRainfall(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)F",
            remap = false
        ),
        remap = false
    )
    private float tfe$useInstantRainfall(Level level, BlockPos pos)
    {
        return NTESeasonalHelpers.getInstantRainfall(level, pos);
    }
}
