package com.newterraearth.tfe.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.util.EnvironmentHelpers;

import com.newterraearth.tfe.world.NTEWeatherHelpers;

@Mixin(value = EnvironmentHelpers.class, remap = false)
public abstract class EnvironmentHelpersMixin
{
    @Inject(method = "tickChunk", at = @At("HEAD"), cancellable = true)
    private static void tfe$replaceLegacySnowTick(ServerLevel level, LevelChunk chunk, ProfilerFiller profiler, CallbackInfo ci)
    {
        if (NTEWeatherHelpers.shouldUseCatchUpSnow(level))
        {
            NTEWeatherHelpers.onTickChunk(level, chunk);
            ci.cancel();
        }
    }
}
