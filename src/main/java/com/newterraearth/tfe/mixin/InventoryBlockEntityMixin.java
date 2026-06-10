package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.nbt.CompoundTag;

import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.common.blockentities.LogPileBlockEntity;

import com.newterraearth.tfe.common.NTELogPileHelpers;

@Mixin(value = InventoryBlockEntity.class, remap = false)
public abstract class InventoryBlockEntityMixin
{
    @Inject(method = "loadAdditional", at = @At("TAIL"), remap = false)
    private void tfe$expandLoadedLogPileInventory(CompoundTag nbt, CallbackInfo ci)
    {
        if ((Object) this instanceof LogPileBlockEntity logPile)
        {
            NTELogPileHelpers.expandAndDisperse(logPile);
        }
    }
}
