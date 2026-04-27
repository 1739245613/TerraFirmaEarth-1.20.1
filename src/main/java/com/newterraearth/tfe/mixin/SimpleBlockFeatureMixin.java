package com.newterraearth.tfe.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.newterraearth.tfe.config.NTECommonConfig;

@Mixin(SimpleBlockFeature.class)
public abstract class SimpleBlockFeatureMixin
{
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void tfe$disableConfiguredWildCrops(FeaturePlaceContext<SimpleBlockConfiguration> context, CallbackInfoReturnable<Boolean> cir)
    {
        final ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(context.config().toPlace().getState(context.random(), context.origin()).getBlock());
        if (!NTECommonConfig.isWildCropEnabled(blockId))
        {
            cir.setReturnValue(false);
        }
    }
}
