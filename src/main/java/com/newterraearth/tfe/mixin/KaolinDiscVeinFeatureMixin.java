package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.world.feature.vein.DiscVeinConfig;
import net.dries007.tfc.world.feature.vein.KaolinDiscVeinFeature;

@Mixin(KaolinDiscVeinFeature.class)
public abstract class KaolinDiscVeinFeatureMixin
{
    @Unique
    private static final TagKey<Block> TFE$DUFF = BlockTags.create(new ResourceLocation("tfc", "duff"));

    @Inject(method = "getStateToGenerate", at = @At("HEAD"), cancellable = true, remap = false)
    private void tfe$replaceDuffWithKaolinClayGrass(BlockState stoneState, RandomSource random, DiscVeinConfig config, int x, int y, int z, CallbackInfoReturnable<BlockState> cir)
    {
        if (stoneState.is(TFE$DUFF))
        {
            cir.setReturnValue(TFCBlocks.KAOLIN_CLAY_GRASS.get().defaultBlockState());
        }
    }
}
