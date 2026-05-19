package com.newterraearth.tfe.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.rock.RockSpikeBlock;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.world.feature.cave.CaveSpikesFeature;

@Mixin(CaveSpikesFeature.class)
public abstract class CaveSpikesFeatureMixin
{
    @Inject(method = "replaceBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void tfe$replaceSaltWaterSpike(WorldGenLevel level, BlockPos pos, BlockState state, CallbackInfo ci)
    {
        final Block block = level.getBlockState(pos).getBlock();
        if (block == TFCBlocks.SALT_WATER.get())
        {
            level.setBlock(pos, state.setValue(RockSpikeBlock.FLUID, RockSpikeBlock.FLUID.keyFor(TFCFluids.SALT_WATER.getSource())), 3);
            ci.cancel();
        }
    }

    @Inject(method = "replaceBlockWithoutFluid", at = @At("HEAD"), cancellable = true, remap = false)
    private void tfe$replaceSaltWaterSpikeBase(WorldGenLevel level, BlockPos pos, BlockState state, CallbackInfo ci)
    {
        final Block block = level.getBlockState(pos).getBlock();
        if (block == TFCBlocks.SALT_WATER.get())
        {
            level.setBlock(pos, state, 3);
            ci.cancel();
        }
    }
}
