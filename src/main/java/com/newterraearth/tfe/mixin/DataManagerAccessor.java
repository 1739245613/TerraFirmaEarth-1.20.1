package com.newterraearth.tfe.mixin;

import com.google.common.collect.BiMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.resources.ResourceLocation;

import net.dries007.tfc.util.DataManager;

@Mixin(value = DataManager.class, remap = false)
public interface DataManagerAccessor<T>
{
    @Accessor(value = "types", remap = false)
    BiMap<ResourceLocation, T> tfe$getTypes();
}
