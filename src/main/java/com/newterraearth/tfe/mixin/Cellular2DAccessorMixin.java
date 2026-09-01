package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.dries007.tfc.world.noise.Cellular2D;

@Mixin(value = Cellular2D.class, remap = false)
public interface Cellular2DAccessorMixin
{
    @Accessor("seed")
    int tfe$getSeed();
}
