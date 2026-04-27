package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.dries007.tfc.util.tracker.WorldTracker;

@Mixin(value = WorldTracker.class, remap = false)
public interface WorldTrackerAccessor
{
    @Accessor(value = "rainStartTick", remap = false)
    long tfe$getRainStartTick();

    @Accessor(value = "rainEndTick", remap = false)
    long tfe$getRainEndTick();

    @Invoker(value = "exactRainfallIntensity", remap = false)
    float tfe$invokeExactRainfallIntensity(long tick);
}
