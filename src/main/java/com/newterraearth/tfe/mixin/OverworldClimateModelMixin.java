package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dries007.tfc.util.climate.OverworldClimateModel;

import com.newterraearth.tfe.world.NTEClimateSeasonModel;

@Mixin(value = OverworldClimateModel.class, remap = false)
public abstract class OverworldClimateModelMixin
{
    @Shadow private float temperatureScale;

    @Inject(method = "calculateMonthlyTemperature", at = @At("HEAD"), cancellable = true, remap = false)
    private void tfe$useSeasonalAmplitude(int z, float monthTemperatureModifier, CallbackInfoReturnable<Float> cir)
    {
        cir.setReturnValue(monthTemperatureModifier * NTEClimateSeasonModel.seasonalTemperatureAmplitude(z, temperatureScale));
    }

    @Redirect(
        method = "getTemperature(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/dries007/tfc/world/chunkdata/ChunkData;JI)F",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/climate/OverworldClimateModel;calculateMonthlyTemperature(IF)F"
        ),
        remap = false
    )
    private float tfe$useLocalHemisphereSeason(OverworldClimateModel model, int z, float monthTemperatureModifier)
    {
        return NTEClimateSeasonModel.seasonalTemperature(z, temperatureScale, monthTemperatureModifier);
    }
}
