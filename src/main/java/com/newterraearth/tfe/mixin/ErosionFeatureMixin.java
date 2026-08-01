package com.newterraearth.tfe.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.world.feature.ErosionFeature;

import com.newterraearth.tfe.world.river.NTERiverHydrology;

/** Keep late erosion support blocks out of realized creek water and falls. */
@Mixin(ErosionFeature.class)
public abstract class ErosionFeatureMixin
{
    @Inject(method = "setBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void tfe$protectSupplementalRiverCorridor(
        WorldGenLevel level,
        ChunkAccess chunk,
        BlockPos pos,
        BlockState state,
        CallbackInfo ci
    )
    {
        final NTERiverHydrology.ColumnProfile profile = NTERiverHydrology.activeGenerationProfile(
            pos.getX(),
            pos.getZ()
        );
        if (profile != null && NTERiverHydrology.blocksErosionSupport(profile, pos.getY()))
        {
            if (pos.getY() > profile.waterBlockY())
            {
                // Erosion reached this raw block only because it is exposed
                // above the realized creek. Merely cancelling its hardened
                // replacement leaves the original floating stone in place.
                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
            }
            ci.cancel();
        }
    }
}
