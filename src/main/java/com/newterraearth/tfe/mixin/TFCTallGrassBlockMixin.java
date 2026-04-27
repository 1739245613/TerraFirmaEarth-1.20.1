package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blocks.plant.TFCTallGrassBlock;

import com.newterraearth.tfe.world.NTESeasonalHelpers;

@Mixin(value = TFCTallGrassBlock.class, remap = false)
public abstract class TFCTallGrassBlockMixin
{
    @Redirect(
        method = "randomTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blocks/plant/PlantRegrowth;canSpread(Lnet/minecraft/world/level/Level;Lnet/minecraft/util/RandomSource;)Z",
            remap = false
        ),
        remap = true
    )
    private boolean tfe$useHemispheralSpread(Level level, RandomSource random, BlockState state, ServerLevel serverLevel, BlockPos pos, RandomSource methodRandom)
    {
        return NTESeasonalHelpers.canPlantSpread(serverLevel, methodRandom, pos);
    }
}
