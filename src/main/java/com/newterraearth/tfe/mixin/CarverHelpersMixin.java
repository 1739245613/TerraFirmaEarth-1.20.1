package com.newterraearth.tfe.mixin;

import java.util.function.BiPredicate;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarvingContext;

import net.dries007.tfc.world.carver.CarverHelpers;

import com.newterraearth.tfe.world.river.NTERiverCaveProtection;

@Mixin(value = CarverHelpers.class, remap = false)
public abstract class CarverHelpersMixin
{
    @Inject(method = "carveBlock", at = @At("HEAD"), cancellable = true)
    private static <C extends CarverConfiguration> void tfe$keepShallowSupplementalRiverRoof(
        CarvingContext context,
        C config,
        ChunkAccess chunk,
        BlockPos.MutableBlockPos pos,
        BlockPos.MutableBlockPos checkPos,
        Aquifer aquifer,
        MutableBoolean reachedSurface,
        BiPredicate<C, BlockState> canReplaceBlock,
        CallbackInfoReturnable<Boolean> cir
    )
    {
        if (NTERiverCaveProtection.protectsCarver(pos.getX(), pos.getY(), pos.getZ()))
        {
            cir.setReturnValue(false);
        }
    }
}
