package com.newterraearth.tfe.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.feature.cave.CaveColumnFeature;

import com.newterraearth.tfe.world.river.NTERiverHydrology;

/** Prevent late hardened cave columns from rebuilding blocks inside a wet creek. */
@Mixin(CaveColumnFeature.class)
public abstract class CaveColumnFeatureMixin
{
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void tfe$protectSupplementalRiverCorridor(
        FeaturePlaceContext<NoneFeatureConfiguration> context,
        CallbackInfoReturnable<Boolean> cir
    )
    {
        if (!(context.chunkGenerator() instanceof TFCChunkGenerator))
        {
            return;
        }

        final BlockPos origin = context.origin();
        // Cave columns vary between roughly two and three blocks in radius.
        // Test their complete possible footprint against already-planned wet
        // cores instead of judging only the feature origin.
        for (int offsetZ = -3; offsetZ <= 3; offsetZ++)
        {
            for (int offsetX = -3; offsetX <= 3; offsetX++)
            {
                final NTERiverHydrology.ColumnProfile profile = NTERiverHydrology.activeGenerationProfile(
                    origin.getX() + offsetX,
                    origin.getZ() + offsetZ
                );
                if (profile != null && NTERiverHydrology.blocksCaveColumn(profile, origin.getY()))
                {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
    }
}
