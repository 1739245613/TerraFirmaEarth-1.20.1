package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.surface.SurfaceStates;

import com.newterraearth.tfe.world.surface.NTESurfaceStates;

@Mixin(value = SurfaceStates.class, remap = false)
public abstract class SurfaceStatesMixin
{
    @Shadow @Final @Mutable public static SurfaceState GRASS;
    @Shadow @Final @Mutable public static SurfaceState DIRT;
    @Shadow @Final @Mutable public static SurfaceState MUD;
    @Shadow @Final @Mutable public static SurfaceState SHORE_SAND;
    @Shadow @Final @Mutable public static SurfaceState SHORE_SANDSTONE;
    @Shadow @Final @Mutable public static SurfaceState SHORE_MUD;
    @Shadow @Final @Mutable public static SurfaceState RARE_SHORE_SAND;
    @Shadow @Final @Mutable public static SurfaceState RARE_SHORE_SANDSTONE;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void tfe$replaceSurfaceStates(CallbackInfo ci)
    {
        GRASS = NTESurfaceStates.TOP_GRASS_TO_GRAVEL;
        DIRT = NTESurfaceStates.MID_DIRT_TO_GRAVEL;
        MUD = NTESurfaceStates.MUD;
        SHORE_SAND = NTESurfaceStates.SHORE_SAND;
        SHORE_SANDSTONE = NTESurfaceStates.SHORE_SANDSTONE;
        SHORE_MUD = NTESurfaceStates.OCEAN_MUD;
        RARE_SHORE_SAND = NTESurfaceStates.RARE_SHORE_SAND;
        RARE_SHORE_SANDSTONE = NTESurfaceStates.RARE_SHORE_SANDSTONE;
    }
}
