package com.newterraearth.tfe.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dries007.tfc.common.blockentities.TFCBlockEntities;

import com.newterraearth.tfe.common.NTEDevices;

/**
 * Extends TFC's FIREPIT / POT BlockEntityType validity so our tfc:stove and tfc:stove_pot blocks
 * share the same BlockEntityType.  Without this, BlockEntity instances created on stove blocks
 * fail vanilla's chunk-load validation (since FirepitBlockEntity / PotBlockEntity constructors
 * hard-code TFCBlockEntities.FIREPIT / POT), and the firepit-style interactions, fire-start
 * event handlers and rendering for stoves never engage.
 */
@Mixin(BlockEntityType.class)
public class BlockEntityTypeMixin
{
    @Inject(method = "isValid(Lnet/minecraft/world/level/block/state/BlockState;)Z", at = @At("HEAD"), cancellable = true)
    private void tfe$includeStoveBlocks(BlockState state, CallbackInfoReturnable<Boolean> cir)
    {
        final Object self = this;
        if (self == TFCBlockEntities.FIREPIT.get() && state.is(NTEDevices.STOVE.get()))
        {
            cir.setReturnValue(true);
        }
        else if (self == TFCBlockEntities.POT.get() && state.is(NTEDevices.STOVE_POT.get()))
        {
            cir.setReturnValue(true);
        }
    }
}
